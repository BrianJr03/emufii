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

## Les sauvegardes du dossier viennent sur l'image, pas l'inverse

Une carte mémoire « dossier » de PCSX2 ne peut pas porter le profil réseau :
PCSX2 l'indexe filtrée par le jeu qui tourne, et `BWNETCNF` ne correspond à aucun
serial, donc le profil serait écrit là où la console ne peut jamais le lire. Le
profil vit donc sur une image générée en emplacement 1, et la question qui reste
est ce que deviennent les sauvegardes du joueur.

**Laisser la carte dossier en emplacement 2 n'est pas la réponse**, et la raison
mérite d'être dite précisément, parce que la lecture évidente du journal est
fausse. ARMSX2 ouvre la carte avant de savoir ce qui démarre :

```
McdSlot 0 [File]: EmuFii-Network.ps2 [8 MB, Formatted]
McdSlot 1: [Folder] /storage/emulated/0/Armsx2/memcards/MemoryCard
FolderMcd: Indexing slot 1 with filter "".
```

Ce filtre vide n'est pas une carte que le jeu ne peut pas lire — mesuré sur la
Thor le 2026-08-23, le jeu y trouve bien son profil. Ce qui ne marche pas, c'est
tout le reste : le navigateur du BIOS montre cette carte comme vide, donc une
sauvegarde ne peut pas être recopiée à la main, et les deux cartes restent
séparées sans moyen de les réunir. Copier les sauvegardes sur la carte qui porte
le profil est ce qui met tout au même endroit, celui sur lequel la console est
d'accord.

## `_pcsx2_index` se lit, ne se copie jamais

La disposition qu'ARMSX2 écrit :

```
memcards/<carte>/_pcsx2_superblock
memcards/<carte>/<SAUVEGARDE>/_pcsx2_index
memcards/<carte>/<SAUVEGARDE>/<les fichiers de la sauvegarde>
```

`_pcsx2_index` est la comptabilité de PCSX2 et ne doit **jamais** atterrir dans
une image de carte : la console n'en sait rien, et une sauvegarde qui porte un
fichier en trop est une sauvegarde que le jeu peut refuser.

Ce pour quoi on le lit est l'**ordre** des fichiers — celui dans lequel la
console les a écrits, et celui qu'un répertoire d'une vraie carte porte. Un
fichier que l'index ne mentionne pas n'est pas jeté : il passe après les autres,
par ordre alphabétique. Perdre un octet de la sauvegarde de quelqu'un pour une
discordance de comptabilité n'est pas un échange qui vaut la peine.

Le fichier est une correspondance YAML en flux écrite par rapidyaml, pas du JSON,
donc il se lit au balayage tolérant plutôt qu'avec un analyseur : les noms
portent des points et des tirets, et le seul champ qui compte ici est `order`.

## Opérer la carte du joueur plutôt que lui en donner une neuve

L'image source du joueur est lue, `BWNETCNF` inséré ou remplacé, et un nouveau
tableau d'octets rendu pour que la couche de provisionnement le publie en clone.
Le tableau d'entrée n'est jamais modifié. Les charges utiles des sauvegardes
existantes survivent à la réécriture du système de fichiers sans changer, donc
aucune cérémonie de copie par le BIOS n'est nécessaire.

**La carte se lit par son propre superbloc** — géométrie, FAT indirecte, chaînes
de la FAT, répertoire racine — jamais par supposition : une carte de 8 Mo et une
de 64, un formatage BIOS et un formatage PCSX2, tous se déclarent.

Un `BWNETCNF` existant, s'il y en a un, est libéré : ses chaînes de fichier et
ses grappes de répertoire rendues à la FAT, son entrée racine compactée, les
sauvegardes qui le suivaient remontées avec leurs références arrière corrigées.
Une sauvegarde neuve est écrite pour l'identifiant de console visé, allouée au
premier trou libre de la FAT — la fragmentation est sans conséquence — et chaque
page touchée est réécrite avec ses données et un ECC recalculé.

Aucune somme de contrôle n'existe nulle part dans le format qu'il faudrait
maintenir, et le superbloc ne tient aucun compte de l'espace libre : rien hors de
la FAT et des deux répertoires ne change.

Une carte entièrement à `0xFF` — ce qu'ARMSX2 fabrique à l'installation, avant
que le BIOS ne l'ait jamais formatée — n'a pas de système de fichiers à lire.
Elle est donc formatée d'abord, avec les constantes du générateur, à la taille du
fichier reçu.

## Retrouver l'identifiant d'une carte déjà écrite

`recoverConsoleId` est un outil de diagnostic et de migration, pas un chemin
normal. Tout fichier YNCF commence par le même en-tête de 38 octets : un
`BWNETCNF` que la console a écrit livre donc son propre flux de chiffrement, et
ce flux vaut trois décalages par octet d'identifiant.

Ça couvre le joueur qui a déjà fait une configuration réseau avec n'importe quel
jeu compatible. **Il ne doit jamais l'emporter sur une identité contradictoire
prouvée par la NVM active.** Une carte que cette app a déjà écrite se décode sous
le même identifiant, ce qui rend aussi l'outil utile en validation.

## Une carte prête ne se vérifie pas octet par octet

C'est ce qui a été fait d'abord, et c'était faux d'une façon qui ne se voit qu'à
l'usage : une carte mémoire est un disque **vivant**. Dès qu'un jeu sauvegarde —
ou qu'ARMSX2 la monte simplement — ses octets changent, la somme de contrôle
cesse de correspondre, et on annonce au joueur que sa préparation a disparu
pendant que sa carte est là, parfaitement bonne, dans le bon emplacement. Ça a
coûté ses jeux PS2 à un joueur entre deux lancements de l'app.

Ce qui doit tenir est plus étroit et survit au jeu normal :

- l'emplacement 1 est toujours actif et nomme toujours cette carte ;
- la carte est toujours là ;
- la configuration réseau y est toujours, et la relire rend l'identifiant de
  console pour lequel elle a été écrite.

Ce dernier point est la vraie preuve : la sauvegarde est chiffrée par console,
donc en retrouver le bon identifiant signifie à la fois que notre profil est
présent et qu'il est à sa place. Les nouvelles sauvegardes à côté ne nous
regardent pas, et c'est précisément le but.
