#!/usr/bin/env python3
"""Read-only inspector for raw PS2 memory-card images.

Supports the standard 8 MiB layout with or without the 16-byte spare/ECC area
stored after each 512-byte NAND page.  The implementation deliberately stays
small: it parses the superblock, indirect FAT, FAT chains, and directory entries
needed to inventory a card without modifying it.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import struct
from dataclasses import asdict, dataclass
from pathlib import Path


MAGIC = b"Sony PS2 Memory Card Format "
PAGE_SIZE = 512
SPARE_SIZE = 16
CLUSTER_SIZE = 1024
FAT_ALLOCATED = 0x80000000
FAT_END = 0xFFFFFFFF
FAT_CLUSTER_MASK = 0x7FFFFFFF
MODE_FILE = 0x0010
MODE_DIR = 0x0020
MODE_PROTECTED = 0x0008
MODE_EXISTS = 0x8000
DIRENT = struct.Struct("<HHL8sLL8sL28x448s")


def _make_ecc_tables() -> tuple[list[int], list[int]]:
    parity = [value.bit_count() & 1 for value in range(256)]
    masks = (0x55, 0x33, 0x0F, 0x00, 0xAA, 0xCC, 0xF0)
    columns = [
        sum(parity[value & mask] << index for index, mask in enumerate(masks))
        for value in range(256)
    ]
    return parity, columns


PARITY, COLUMN_PARITY = _make_ecc_tables()


def calculate_ecc(data: bytes) -> bytes:
    if len(data) != 128:
        raise ValueError("ECC chunks must be exactly 128 bytes")
    column = 0x77
    line_0 = 0x7F
    line_1 = 0x7F
    for index, value in enumerate(data):
        column ^= COLUMN_PARITY[value]
        if PARITY[value]:
            line_0 ^= ~index
            line_1 ^= index
    return bytes((column, line_0 & 0x7F, line_1))


@dataclass(frozen=True)
class Entry:
    path: str
    kind: str
    mode: int
    protected: bool
    size: int
    first_cluster: int
    sha256: str | None


class Card:
    def __init__(self, path: Path):
        self.path = path
        self.handle = path.open("rb")
        self.file_size = path.stat().st_size
        first_page = self.handle.read(PAGE_SIZE)
        if not first_page.startswith(MAGIC):
            raise ValueError("not a PS2 memory-card image")

        (
            _magic,
            version,
            self.page_size,
            self.pages_per_cluster,
            self.pages_per_erase_block,
            _unused,
            self.clusters_per_card,
            self.alloc_offset,
            self.alloc_end,
            self.root_cluster,
            self.backup_block_1,
            self.backup_block_2,
        ) = struct.unpack_from("<28s12sHHHHLLLLLL", first_page)
        self.version = version.rstrip(b"\0").decode("ascii")
        if self.page_size != PAGE_SIZE:
            raise ValueError(f"unsupported page size: {self.page_size}")
        if self.pages_per_cluster * self.page_size != CLUSTER_SIZE:
            raise ValueError("unsupported cluster size")

        expected_pages = self.clusters_per_card * self.pages_per_cluster
        data_only_size = expected_pages * self.page_size
        raw_size = expected_pages * (self.page_size + SPARE_SIZE)
        if self.file_size == raw_size:
            self.spare_size = SPARE_SIZE
        elif self.file_size == data_only_size:
            self.spare_size = 0
        else:
            raise ValueError(
                f"unexpected image size {self.file_size}; "
                f"expected {data_only_size} or {raw_size}"
            )
        self.raw_page_size = self.page_size + self.spare_size
        self.entries_per_cluster = CLUSTER_SIZE // 4
        self.ifc_list = struct.unpack_from("<32L", first_page, 0x50)

    def close(self) -> None:
        self.handle.close()

    def read_page(self, page: int) -> bytes:
        self.handle.seek(page * self.raw_page_size)
        data = self.handle.read(self.page_size)
        if len(data) != self.page_size:
            raise ValueError(f"short read at page {page}")
        return data

    def ecc_summary(self) -> dict | None:
        if not self.spare_size:
            return None
        erased = checked = 0
        invalid_pages: list[int] = []
        for page in range(self.clusters_per_card * self.pages_per_cluster):
            self.handle.seek(page * self.raw_page_size)
            data = self.handle.read(self.page_size)
            spare = self.handle.read(self.spare_size)
            if data == b"\xff" * self.page_size and spare == b"\xff" * self.spare_size:
                erased += 1
                continue
            checked += 1
            expected = b"".join(
                calculate_ecc(data[offset : offset + 128])
                for offset in range(0, self.page_size, 128)
            )
            if spare[: len(expected)] != expected:
                invalid_pages.append(page)
        return {
            "checked_pages": checked,
            "erased_pages": erased,
            "invalid_pages": invalid_pages,
        }

    def read_cluster(self, cluster: int) -> bytes:
        first_page = cluster * self.pages_per_cluster
        return b"".join(
            self.read_page(first_page + index)
            for index in range(self.pages_per_cluster)
        )

    def read_alloc_cluster(self, cluster: int) -> bytes:
        return self.read_cluster(self.alloc_offset + cluster)

    def fat_value(self, alloc_cluster: int) -> int:
        fat_cluster_index, entry_offset = divmod(
            alloc_cluster, self.entries_per_cluster
        )
        ifc_index, indirect_offset = divmod(
            fat_cluster_index, self.entries_per_cluster
        )
        indirect_cluster = self.ifc_list[ifc_index]
        indirect = struct.unpack("<256L", self.read_cluster(indirect_cluster))
        fat_cluster = indirect[indirect_offset]
        fat = struct.unpack("<256L", self.read_cluster(fat_cluster))
        return fat[entry_offset]

    def chain(self, first_cluster: int):
        seen: set[int] = set()
        current = first_cluster
        while current != FAT_END:
            current &= FAT_CLUSTER_MASK
            if current in seen:
                raise ValueError(f"FAT loop at allocatable cluster {current}")
            if current >= self.alloc_end:
                raise ValueError(f"FAT cluster out of range: {current}")
            seen.add(current)
            yield current
            current = self.fat_value(current)

    def read_chain(self, first_cluster: int, length: int) -> bytes:
        result = bytearray()
        for cluster in self.chain(first_cluster):
            result.extend(self.read_alloc_cluster(cluster))
            if len(result) >= length:
                break
        if len(result) < length:
            raise ValueError("FAT chain ended before declared file length")
        return bytes(result[:length])

    @staticmethod
    def parse_dirent(data: bytes):
        mode, _unused, length, created, cluster, parent, modified, attr, name = (
            DIRENT.unpack(data)
        )
        clean_name = name.split(b"\0", 1)[0].decode("ascii", errors="replace")
        return mode, length, created, cluster, parent, modified, attr, clean_name

    def entries(self) -> list[Entry]:
        output: list[Entry] = []
        root_data = self.read_alloc_cluster(self.root_cluster)
        root_entry = self.parse_dirent(root_data[: DIRENT.size])
        self._walk_dir("", self.root_cluster, root_entry[1], output)
        return output

    def _walk_dir(
        self, parent_path: str, first_cluster: int, count: int, output: list[Entry]
    ) -> None:
        data = self.read_chain(first_cluster, count * DIRENT.size)
        for index in range(count):
            fields = self.parse_dirent(data[index * DIRENT.size : (index + 1) * DIRENT.size])
            mode, length, _created, cluster, _parent, _modified, _attr, name = fields
            if not (mode & MODE_EXISTS) or name in (".", ".."):
                continue
            path = f"{parent_path}/{name}"
            if mode & MODE_DIR:
                output.append(
                    Entry(path, "directory", mode, bool(mode & MODE_PROTECTED), length, cluster, None)
                )
                self._walk_dir(path, cluster, length, output)
            elif mode & MODE_FILE:
                contents = self.read_chain(cluster, length)
                output.append(
                    Entry(
                        path,
                        "file",
                        mode,
                        bool(mode & MODE_PROTECTED),
                        length,
                        cluster,
                        hashlib.sha256(contents).hexdigest(),
                    )
                )

    def summary(self) -> dict:
        return {
            "path": str(self.path),
            "image_sha256": hashlib.sha256(self.path.read_bytes()).hexdigest(),
            "file_size": self.file_size,
            "version": self.version,
            "page_size": self.page_size,
            "spare_size": self.spare_size,
            "pages_per_cluster": self.pages_per_cluster,
            "pages_per_erase_block": self.pages_per_erase_block,
            "clusters_per_card": self.clusters_per_card,
            "alloc_offset": self.alloc_offset,
            "alloc_end": self.alloc_end,
            "root_cluster": self.root_cluster,
            "backup_blocks": [self.backup_block_1, self.backup_block_2],
            "ecc": self.ecc_summary(),
            "entries": [asdict(entry) for entry in self.entries()],
        }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("image", type=Path)
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args()

    card = Card(args.image)
    try:
        result = card.summary()
    finally:
        card.close()
    if args.json:
        print(json.dumps(result, indent=2))
        return

    print(f"{result['path']}: {result['version']}")
    print(
        f"{result['file_size']} bytes, pages={result['page_size']}+{result['spare_size']}, "
        f"clusters={result['clusters_per_card']}, alloc={result['alloc_offset']}..{result['alloc_end']}"
    )
    if result["ecc"]:
        ecc = result["ecc"]
        print(
            f"ECC: {ecc['checked_pages']} checked, {ecc['erased_pages']} erased, "
            f"{len(ecc['invalid_pages'])} invalid {ecc['invalid_pages']}"
        )
    for entry in result["entries"]:
        digest = f" sha256={entry['sha256']}" if entry["sha256"] else ""
        print(
            f"{entry['kind'][0]} mode=0x{entry['mode']:04x} size={entry['size']:6d} "
            f"cluster={entry['first_cluster']:4d} {entry['path']}{digest}"
        )


if __name__ == "__main__":
    main()
