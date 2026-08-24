# Identité des disques : reconnaître une console dans des octets

Le récit qui vivait dans `library/DiscImage.kt`, sorti du code le 2026-08-24
(cf. `docs/STYLE_COMMENTAIRES.md`). Le `CLAUDE.md` en porte le résumé ; ici, les
mesures. Titres = ancres citées depuis le code.

**Tous les décalages ci-dessous ont été mesurés sur de vrais fichiers présents
sur cette machine, jamais recopiés d'un wiki.**

## Lire les octets, et seulement pour promouvoir

La PSP a eu `.iso` en premier et ne le rend pas : un rip UMD et une image
GameCube partagent l'extension et rien d'autre. Les trier par nom serait un pile
ou face.

D'où la règle : on lit les premiers octets, **et on ne les lit que pour
promouvoir un fichier**. Tout ce qui ne peut pas être identifié positivement
reste ce que l'extension disait déjà — c'est pour ça qu'ajouter Dolphin ne peut
pas retirer un seul jeu à PPSSPP. `null` est la réponse ordinaire pour un rip
PSP, et c'est aussi celle d'une lecture tronquée ou d'un format non listé : les
trois veulent dire la même chose à l'appelant, **laisse le fichier où
l'extension l'a mis**.

Deux vérifications qui ferment la porte aux faux positifs :

- un rip PSP porte des zéros francs en `0x18` et `0x1C`, là où vivent les deux
  magies Nintendo (vérifié sur trois) ;
- un RVZ porte son type de disque en `0x48` et une copie verbatim de l'en-tête
  de disque en `0x58` — c'est pourquoi la magie Wii apparaît en `0x70` (vérifié
  sur quatre).

## Le disque dit lui-même ce qu'il est, en 0x8008

Un disque PS2 et un rip UMD sont tous deux des `.iso`, et sur la Thor ils se
ressemblent jusqu'au nom de fichier. Mais le disque le dit lui-même, mesuré sur
les vrais fichiers :

```
TimeSplitters 2 (PS2) : system id 'PLAYSTATION'  volume id 'SLES_50877'
WipEout Pulse  (PSP)  : system id 'PSP GAME'     volume id 'SCEE'
```

Le premier secteur de données d'un CD/DVD commence à `0x8000` (secteur 16, de
2048 octets) : le type et la signature `CD001` d'abord, puis l'identifiant système
sur 32 octets.

**La même règle s'applique où que commence le descripteur.** Sur un `.iso` il
commence à `0x8000` ; sur un secteur extrait d'un CHD, à 0, 16 ou 24 selon la façon
dont le disque a été pressé. Une règle, un seul endroit : une PS2 reconnue à
travers un conteneur compressé l'est sur exactement les mêmes preuves qu'une PS2
lue dans un fichier nu. Un secteur de 2048 octets est déjà de la donnée
utilisateur ; un secteur CD brut porte 16 octets de synchro et d'en-tête avant
elle (MODE1) ou 24 (MODE2 FORM1) — et le pressage PS2 mesuré ici est en MODE2,
donc il faut essayer les deux. Mesuré : `CD001` tombe à 24.

## `PLAYSTATION` ne suffit pas : il faut `BOOT2`

Un disque **PS1** porte le même `PLAYSTATION` à ce décalage. Cette seule preuve ne
tranche donc pas la console : le lecteur exige ensuite une entrée `BOOT2` dans
`SYSTEM.CNF`, qu'un disque PS2 a et qu'un disque PS1 n'a pas. Sans elle, le disque
est **refusé plutôt que mal étiqueté** — un CHD PS1 a déjà été listé comme un jeu
« PS2 » depuis un dossier qui n'avait rien de PS2.

## Le serial PS2 est dans `SYSTEM.CNF`, pas dans l'identifiant de volume

L'identifiant de volume servait à ça, et c'était le mauvais champ. **Mesuré sur
les huit disques PS2 du banc : deux portaient un serial** (`SLES_50877`,
`SCED_53990`). Les six autres disaient `MC3REMIX`, `FINAL_FANTASY_X`, `1_01`, ou
rien du tout — un éditeur y écrit ce qu'il veut, et rien ne l'oblige à y écrire le
numéro du disque.

Le serial que tout l'outillage PS2 utilise réellement est le fichier de démarrage
nommé dans `SYSTEM.CNF`, à la racine du disque :

```
BOOT2 = cdrom0:\SLES_537.17;1
```

C'est là-dessus que PCSX2 indexe sa propre base, donc c'est aussi la seule chose
qui puisse correspondre à la nôtre. L'atteindre veut dire **marcher l'ISO9660** —
lire le descripteur primaire, suivre l'enregistrement de son répertoire racine,
trouver le fichier — soit quelques centaines d'octets de lecture, et la raison
pour laquelle cette fonction prend un lecteur à accès aléatoire là où tout le
reste travaille sur un préfixe.

`cdrom0:\SLES_537.17;1` est réduit à `SLES-53717` : le point à l'intérieur du
nombre est une convention de nom de fichier et non une partie du serial, et le
tiret bas est la façon d'écrire un serial sur un système de fichiers qui n'a pas
de trait d'union. Les deux sont défaits pour que le résultat soit le serial tel
qu'il s'écrit partout ailleurs — sur la boîte, dans l'index de PCSX2, dans notre
base.

**Le repli est l'identifiant de volume, jamais rien.** Un disque qui était mal
identifié ne doit pas devenir un disque **pas** identifié du tout.

Le décalage de l'enregistrement racine est **relatif** au descripteur, pas absolu :
celui-ci a déjà été lu dans un tampon à lui à ce moment-là. C'était
`PVD_OFFSET + 156` au départ, ce qui indexait 32 ko dans un tableau de 2 ko —
attrapé par le test de marche, qui aurait sinon été la pastille ne paraissant
jamais sur un jeu PS2, en silence.

## Une marche ISO demande un canal, et ne vaut pas pour le CHD

Un canal, et non le flux séquentiel que le reste de cette classe utilise : le
répertoire racine se trouve là où le disque a été masterisé, plusieurs centaines
de mégaoctets plus loin sur un jeu double couche, et lire jusque-là voudrait dire
lire le jeu.

**Seulement pour une image nue.** Un CHD devrait décompresser un hunk par saut, et
le secteur qu'il lit déjà pour l'identification porte l'identifiant de volume, qui
reste la réponse là-bas.

Un CHD s'annonce dans ses huit premiers octets, donc rien ici ne dépend du fait
que le fichier soit *nommé* `.chd`. C'est en revanche le seul format qui ne peut
pas être traité en lisant vers l'avant — sa carte de hunks est près de la fin du
fichier — d'où un descripteur de fichier et un canal. Un fournisseur qui refuse
d'en donner un répond `null`, et le fichier garde la console de son extension.

## Les identifiants de jeu, et à quoi ils servent

**Nintendo** : les six caractères estampés tout au début d'un en-tête de disque —
`RMGP01`, `GALE01` — l'identité sur laquelle Dolphin lui-même trie ses jeux, et la
seule chose ici qui permette à un invité de reconnaître le jeu de l'hôte comme un
jeu qu'il possède. Une image de disque n'a ni SMDH ni bannière à un décalage fixe
(un RVZ est compressé), donc le **titre** vient encore du nom de fichier ; ceci est
la part qui ne dépend pas de la façon dont quelqu'un a nommé son fichier. Lu à la
même base que la console : 0 sur une image brute, `0x58` dans un conteneur
compressé.

**PS2** : le numéro est là où le disque le classe, pas au début du fichier. Mesuré
`SLES_50877` sur TimeSplitters 2, quand ARMSX2 affiche `SLES-50877` — le même
numéro au séparateur près, donc l'invité reconnaîtra le jeu de l'hôte tel que son
propre émulateur le lui nomme.

**RVZ et WIA** annoncent leur console franchement. L'en-tête embarqué est vérifié
en plus, et ce n'est pas redondant : c'est lui qui prouve que le fichier est bien
ce que son `disc_type` prétend, et c'est la réponse sur un conteneur dont le champ
de type est inconnu de cette build. Les deux partagent un conteneur : un en-tête de
fichier, puis un `WIADisc` dont le `disc_type` dit de quelle console il vient, puis
les 128 premiers octets du disque d'origine, verbatim — tous deux lisibles sans
toucher à la charge compressée.

## Quelles extensions valent la peine d'être ouvertes

`.iso`, parce que la PSP la possède et que seuls les octets peuvent trancher. Les
extensions propres à Dolphin aussi, parce qu'elles doivent encore dire **laquelle**
des deux consoles elles sont.

`.chd` depuis le 2026-08-20, et c'est celle qui a demandé du vrai travail : la PSP,
la PS2 et la Dreamcast s'y livrent toutes les trois, et les octets qui répondent à
la question sont compressés. `ChdImage` décode juste assez du conteneur pour rendre
un secteur, qui passe ensuite par la même règle de descripteur que tout le reste.
Un disque Dreamcast est refusé avant tout ça, sur son étiquette de métadonnées
GD-ROM.

**Délibérément absent : `.gcz`.** Il dit GameCube ou Wii dans un champ de sous-type
dont ce projet n'a aucun échantillon pour vérifier, et deviner risquerait de
déplacer le jeu de quelqu'un vers un émulateur incapable de l'ouvrir — ce qui est
pire que de ne pas lister un format.

## Le coût de tout ceci, et pourquoi il est invisible en cas d'échec

On lit **jusqu'au descripteur de volume**, pas seulement l'en-tête : les magies
GameCube et Wii tiennent dans les 128 premiers octets, mais la PS2 ne peut être
reconnue qu'à `0x8000`, là où commence l'ISO9660. D'où une lecture de 32 ko par
fichier reniflé, séquentielle, une fois, pendant la passe d'enrichissement de la
bibliothèque — laquelle ouvre déjà chaque fichier 3DS et DS pour son icône.

**Le tableau rendu est tronqué à ce qui a réellement été lu**, et c'est le point
délicat : un tableau de 32 ko dont la queue serait des zéros non lus ferait
examiner à l'identification des octets qui ne viennent pas du fichier. Un fichier
plus court que l'en-tête voulu n'est pas une image de disque du tout, et ne rend
rien.

Un fournisseur qui refuse la lecture répond `null`, que le scan lit comme « garde
la supposition de l'extension », exactement comme pour un rip PSP : c'est la raison
pour laquelle un échec ici est **invisible plutôt que destructeur**.

---

# Le CHD : décoder juste assez

Sorti de `library/ChdImage.kt`. **Tout ci-dessous a été mesuré sur deux vrais
fichiers**, jamais pris dans un wiki : un `Phantasy Star Online Ver. 2`
Dreamcast et un `Unreal Tournament` PS2. Les deux sont en v5, `cdlz/cdzl/cdfl`,
`hunkbytes 19584` sur `unitbytes 2448`.

## On s'arrête au secteur, on ne décide rien

`.chd` est le seul conteneur où l'extension ne tranche rien : la PSP, la PS2 et
la Dreamcast s'y livrent toutes les trois, et sur cette machine deux des trois
sont dans des dossiers voisins. Contrairement à un `.iso`, **les octets qui
répondraient à la question sont compressés**.

Ce décodeur va donc juste assez loin pour rendre **un secteur de disque**, et pas
plus : aucune extraction complète, aucun fichier temporaire, quelques centaines de
kilooctets lus par candidat. Le fichier PS2 mesuré rend, au secteur 16 décalage
24, exactement :

```
CD001   system id 'PLAYSTATION'   volume id 'UT'
```

— **le même descripteur que celui déjà lu sur un `.iso` nu**. C'est toute la
raison de s'arrêter au secteur : la console est tranchée en **un seul endroit**
pour tous les formats de disque, pas deux.

`null` est la réponse ordinaire pour un GD-ROM, pour un codec qu'on ne décode pas,
pour un CHD antérieur à la v5 et pour tout fichier tronqué. **Tous veulent dire la
même chose, et jamais « ce n'est pas une PS2 »** : « les octets n'ont pas parlé »,
ce qui laisse le fichier là où son extension l'a mis.

## La Dreamcast est écartée avant qu'un octet soit décompressé

C'est **le faux positif à éviter** : elle est en `unitbytes 2448` exactement comme
un CD PS2, et **seule l'étiquette de métadonnées les distingue**. Mesuré : le
fichier Dreamcast porte `CHGD "TRACK:1 TYPE:MODE1_RAW …"` là où le PS2 porte
`CHT2 "TRACK:1 TYPE:MODE2_RAW …"`.

La chaîne de métadonnées est à `0x7c` sur les deux fichiers mesurés — juste
derrière l'en-tête — donc ça coûte **une courte lecture** et ça règle la console
que ce projet ne doit jamais revendiquer.

## Un lecteur réutilisable, sinon le travail explose

L'ancien chemin « un secteur » redécodait la carte de Huffman **à chaque saut**. Un
ELF de démarrage s'étale sur des centaines de sauts dans un CHD de DVD : cette
approche transforme quelques mégaoctets en **gigaoctets de travail de carte
répété**. La carte est donc analysée une fois, et seul le hunk décodé le plus récent
est gardé — assez pour les lectures séquentielles de l'identification PS2.

## Deux pièges de décodage qui ont coûté cher

**La première passe sur la carte ne peut pas être écourtée.** Les types de
*chaque* hunk sont décodés avant que la première longueur soit écrite : s'arrêter
au hunk voulu lit donc des longueurs **au milieu du flux de types** et produit des
décalages qui ont l'air plausibles et ne décompressent rien. Cette erreur a coûté
un après-midi ; la boucle va jusqu'au bout délibérément.

**Le Huffman canonique de MAME a deux détails non devinables**, pris dans
`huffman.cpp` plutôt que reconstruits : le compte de répétition vient d'une
**troisième** lecture du flux, et ce qui est répété est **la longueur qu'on vient
de lire**, pas zéro. Se tromper sur l'un ou l'autre produit quand même un arbre —
simplement pas un dont les longueurs de code somment à 1, ce que le décodeur
vérifie exactement, et refuse le fichier sinon.

## Ce qui est décodé, et ce qui ne l'est pas

- **Sur un CD brut, seuls les secteurs sont rendus** : le codec garde les données
  et le subcode en deux blocs compressés séparément, et le subcode ne porte rien
  qui nomme une console.
- **Le LZMA n'a pas d'en-tête** : MAME compresse avec `lc=3, lp=0, pb=2`, ce qui
  tient dans l'octet de propriétés `0x5D`, avec un dictionnaire normalisé à la
  puissance de deux supérieure. Vérifié contre le vrai fichier PS2, où le bloc de
  secteurs décode à exactement 18 816 octets.
- **Le FLAC n'est reconnu que pour ses sous-trames constantes à zéro.** Les CHD de
  DVD gardent couramment un hunk canonique entièrement nul en FLAC et s'y réfèrent
  partout dans les fichiers creux. Décoder de l'audio arbitraire est inutile pour
  un lecteur d'ELF ; reconnaître ce cas suffit à résoudre ces références, et tout
  le reste **retombe proprement plutôt que d'être deviné**.
- **Le flux de données utilisateur de 2048 octets** masque les en-têtes de CD brut
  et le subcode. Les CD PS2 du banc sont en MODE2 (décalage 24) ; le MODE1 (16) et
  les secteurs déjà cuits sont détectés aussi, depuis la signature du descripteur.
