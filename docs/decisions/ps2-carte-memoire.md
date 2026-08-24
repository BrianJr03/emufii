# PS2 : la carte mémoire fabriquée, et le chiffrement YNCF

Le récit qui vivait dans `ps2/Ps2MemoryCard.kt` et `ps2/Ps2NetcnfConfig.kt`, sorti
du code le 2026-08-24 (cf. `docs/STYLE_COMMENTAIRES.md`). C'est de la
**spécification de format** : chaque constante ci-dessous a été mesurée sur une
vraie carte, pas recopiée d'un wiki. Titres = ancres citées depuis le code.

## Ce que l'émulateur vérifie d'une carte : presque rien

ARMSX2 ouvre le fichier, déduit la géométrie de sa taille, et transmet sinon les
pages telles quelles à l'invité — 512 octets de données plus 16 octets de réserve
chacune (`pcsx2/SIO/Memcard/MemoryCardFile.cpp`, `MemoryCardProtocol.cpp`).

**Tout jugement — chaîne magique, cohérence de la FAT, modes de répertoire, ECC —
est rendu par la console émulée**, contre des octets que l'image porte
littéralement. Une carte fabriquée est donc tenue à ce que le BIOS lui-même écrit,
et la disposition suivie ici est modelée sur une carte qu'il a réellement écrite :
celle que la PS2 a formatée à travers ARMSX2 le 2026-08-20, puis que l'utilitaire
réseau de Midnight Club 3 a remplie, **octet pour octet**, avant que les mêmes
mesures deviennent ces constantes.

## La disposition, dans l'ordre de la carte

L'image est la disposition RAW 8 Mo standard : **16 384 pages de 528 octets =
8 650 752 octets**, une sauvegarde `BWNETCNF` à la racine, et l'espace libre effacé
à `0xFF`. C'est aussi pourquoi la génération reste déterministe à bon compte : rien
sur la carte ne dépend d'autre chose que des octets de la sauvegarde et de
l'horloge.

- **Page 0, le superbloc** : `Sony PS2 Memory Card Format 1.2.0.0`, 512 octets par
  page, 2 pages par cluster, 16 par bloc, 8192 clusters, allocation à partir du
  cluster 41, et les constantes de queue que le BIOS écrit au formatage. **Il n'y a
  pas de somme de contrôle de superbloc dans ce format** — le mcman de Sony laisse
  `0x48`-`0x4F` en remplissage (ps2sdk `mcman-internal.h:308`) — et aucune entrée de
  répertoire n'en porte non plus.
- **Pages 1-15**, le reste du premier bloc réservé : des données effacées, mais
  **avec** leurs réserves ECC, parce que c'est ce que le formatage du BIOS laisse
  là. Les 8 premiers octets de la page 1 sont l'affaire d'ARMSX2 — il y garde une
  somme de contrôle côté hôte, au décalage `0x210` — et il les estampe lui-même : la
  génération les laisse effacés.
- **Cluster 8**, la FAT indirecte listant les 32 clusters de FAT, 9 à 40. Les
  entrées sont des u32 petit-boutistes indexées depuis le décalage d'allocation :
  `0x7FFFFFFF` libre, `0x80000000 | suivant` dans une chaîne, `0xFFFFFFFF` pour le
  dernier cluster.
- **À partir du cluster 41** : le répertoire racine (`.`, `..`, `BWNETCNF`), le
  répertoire propre de la sauvegarde, puis les données du fichier — alloués dans cet
  ordre pour que les clusters d'un fichier restent contigus, comme les laisse
  l'allocateur premier-ajusté de la console.

**Une entrée de répertoire est complétée de zéros après son nom** — c'est ainsi que
la console les laisse, et un lecteur parcourt les noms jusqu'au NUL — avec **un
emplacement entièrement à `0xFF` après la dernière entrée** pour terminer. Le nom
occupe 32 octets à `0x40`.

L'horodatage est **le temps de la PS2 : huit octets en heure du Japon**, quel que
soit le réglage de la console (Ross Ridge, « PlayStation 2 Memory Card File
System ») — réservé, seconde, minute, heure, jour, mois, puis une année
petit-boutiste.

Les **16 octets de réserve** d'une page écrite sont quatre codes de Hamming de 3
octets, un par tranche de 128 octets, puis quatre zéros. L'algorithme est celui que
portent le mcman de Sony, mymc et PCSX2 ; **l'émulateur ne le vérifie jamais, mais
la console le peut**, donc il est calculé plutôt que rempli. La contribution de
parité de colonne d'un octet est la parité impaire de l'octet masqué par le n-ième
masque, pour les sept masques du code — les bits 3 et 6 étant toujours nuls, d'où
le `0x77`.

## La sauvegarde, et pourquoi rien de Sony n'y voyage

`BWNETCNF` porte la configuration réseau en mode `0x842F` ; son bit `0x08` marque
le répertoire protégé contre la copie dans le navigateur du BIOS.

Dedans : l'index, `net000.cnf`, les deux moitiés chiffrées, et **une paire d'icônes
générée ici plutôt que livrée**. Il n'y a là rien de Sony qui vaille d'être
embarqué : `icon.sys` fait 964 octets de champs d'en-tête documentés — quatre
couleurs de coin, trois lumières et une ambiante, le titre, et le nom de l'icône
trois fois (normale, copie, suppression) — et l'icône elle-même est **un seul quad
texturé d'une couleur unie**.

Le format d'icône est un en-tête de 20 octets, un bloc de sommets, une courte
section d'animation, et une texture BGR555 de 128×128 — 32 768 octets non
compressés, ce qui explique que le `SYS_NET.ICO` de Sony pèse 33 ko. **L'option de
texture compressée tient en deux passes RLE d'un seul texel**, donc l'icône entière
fait ici quelques centaines d'octets.

Le titre est la seule chose personnalisée sur la carte : le nom de profil du joueur,
réduit à de l'ASCII imprimable, `Emufii` si rien ne survit.

## YNCF : une sauvegarde ne se relit que sur la console qui l'a chiffrée

Trois des fichiers sont en clair : l'index de sauvegarde, `net000.cnf`, et l'en-tête
partagé de 38 octets des deux autres. **Les deux autres, `ifc000.dat` et
`dev000.dat`, sont ce même texte passé dans un chiffre verrouillé sur la console** :
la bibliothèque netcnf de Sony dérive une table de décalages de l'identifiant i.Link
de 8 octets, et encode chaque mot de 16 bits petit-boutiste en
`rotl16(mot, décalage) xor 0xFFFF` (ps2dev/ps2sdk, `netcnf.c`, encodage en :775,
initialisation de clé en :875).

**La conséquence, et la raison d'être de ce fichier : il n'y a aucun matériel de clé
dans le fichier, et aucune somme de contrôle pour échouer bruyamment.** Une console
qui ne correspond pas décode de la bouillie, et le jeu signale la configuration comme
invalide.

La table de décalages **cycle avec une période de 24 mots (48 octets)**, trois
décalages par octet d'identifiant — d'où le fait que deux fichiers chiffrés sous une
même console partagent leurs 48 premiers octets dès que leurs textes en clair les
partagent. Les 24 décalages sont `(b shr 5) + 1`, `((b shr 2) and 7) + 1`,
`(b and 3) + 1`, chacun de 1 à 8.

**Un écart avec ps2sdk, tranché par la mesure** : sa transcription n'initialise que
sept des huit octets d'identifiant et laisse deux cases de table déborder du tableau.
Décoder la carte du banc sous la table simple de huit octets **reproduit chaque mot
des deux fichiers chiffrés**, et c'est cette lecture qui est suivie ici.

## Pour quel identifiant chiffrer

ARMSX2 répond au `sceCdRI` de la bibliothèque netcnf depuis le `.nvm` posé à côté du
BIOS qui tourne, et **les deux chemins qui produisent cette réponse convergent vers
une seule constante** (`pcsx2/CDVD/CDVD.cpp`) :

- pas de `.nvm` lisible → `cdvdCreateNewNVM()` écrit l'identifiant factice
  `00 AC FF FF FF FF B9 86` (CDVD.cpp:158) ;
- un `.nvm` dont la zone i.Link paraît non programmée (octets 2 et 3 tous deux nuls)
  → `sceCdReadILinkId` remplace la lecture par la même constante
  (CDVD.cpp:2621-2631).

Une carte chiffrée pour cet identifiant marche donc **sur toute installation dont
ARMSX2 a fabriqué lui-même la NVRAM** — l'import de BIOS en un seul `.bin`, qui est
l'installation normale. Une installation ayant importé le `.nvm` d'une vraie console
garde l'identifiant réel de celle-ci et doit être chiffrée pour lui.

**C'est le défaut que ceci remplace : la carte qu'Emufii livrait était chiffrée pour
l'identifiant d'une console du banc, et ne marchait nulle part ailleurs.**

Deux précautions de lecture de la NVRAM : les callers doivent **choisir explicitement
la disposition** d'après le BIOS réellement détecté — inspecter les deux décalages
peut silencieusement ramasser des octets périmés laissés dans une zone sans rapport
d'un `.nvm` importé. Et ARMSX2 **écarte le contenu importé** puis rappelle
`cdvdCreateNewNVM()` si la NVM est courte, si le bloc de langue est vierge, ou si le
bloc de région slim l'est.

## Ce que la configuration dit, et ce qu'elle ne dit surtout pas

`type nic` + `dhcp`, rien d'autre. Les textes en clair sont **octet pour octet ceux
que la PS2 a écrits sur le banc** (mesuré le 2026-08-20, récupérés en décodant la
carte livrée) : `dhcp`, pas d'adresse, pas de serveur de noms — et pour la moitié
« périphérique », le nom de l'adaptateur Ethernet de SCE.

**L'adresse statique d'une PS2 n'est pas l'affaire de ce fichier dans Emufii** : le
Local Link d'ARMSX2 fait tourner son propre serveur DHCP et remet à chaque pair une
adresse distincte dérivée de son identifiant de pair
(`pcsx2/DEV9/LocalLinkAdapter.cpp:167`). La console demande donc un bail et
l'émulateur la distingue de tous les autres joueurs. **Une IP statique écrite à la
main ici mettrait au contraire tous les joueurs sur la même adresse.**
