# Scanner la bibliothèque : le cache, la chaîne de décision, les identités

Le récit qui vivait dans `library/RomsRepository.kt`, sorti du code le
2026-08-24 (cf. `docs/STYLE_COMMENTAIRES.md`). Voir aussi
[`identite-disques.md`](identite-disques.md). Titres = ancres citées depuis le code.

## Le cache appartient au processus, pas à l'écran

Le résultat du dernier scan est gardé pour que les appelants qui n'ont qu'une
chose à retrouver ne remarchent pas tout l'arbre : rejoindre depuis le annuaire
faisait exactement ça, une seconde marche complète pour apparier un seul
identifiant de titre.

Il est **délibérément partagé entre instances** et non tenu par dépôt. Un dépôt
est mémorisé par composition, donc tourner l'appareil en fabriquait un nouveau et
rescannait de zéro — avec une ROM 3DS de 2 Go dans le dossier, assez longtemps
pour provoquer un ANR, et plusieurs rotations mettaient plusieurs scans en file.

## Les noms choisis par le joueur sont posés à la sortie, jamais dans le cache

Renommer un jeu ne rescanne rien — ça change une préférence — donc un cache
contenant les titres renommés était un cache que rien n'invalidait : le
renommage n'apparaissait qu'au prochain démarrage à froid, ce qui se lit comme
« renommer ne fait rien ».

Le cache garde donc les titres lus dans les fichiers, et les noms choisis sont
appliqués **à chaque lecture**. Effet de bord bienvenu : effacer un nom rend
immédiatement le titre d'origine, là où avant le nom personnalisé restait en place
jusqu'à un rescan.

**Le tri appartient au même endroit, et pour la même raison** : un jeu renommé
doit aller à sa nouvelle place dans l'alphabet, pas rester où son ancien titre
l'avait mis.

Même logique pour le titre lu dans le fichier : chaque console a son propre
chemin de lecture, et appliquer le nom choisi par chemin aurait donné une
bibliothèque où renommer marche pour la 3DS et pas pour la DS. **Un seul endroit,
à la sortie, pour toutes.**

## La marche de l'arbre

Les gens rangent leurs ROMs dans `3DS/`, `GameCube/`, `Jeux/` — un scan à plat ne
trouvait rien. On marche donc les sous-dossiers, mais pas indéfiniment : au-delà
de quelques niveaux on est presque sûrement là où on ne devrait pas être, et
chaque niveau supplémentaire coûte une requête par répertoire.

**`DocumentsContract` est interrogé directement, pas `DocumentFile`** : ce dernier
émet une requête par entrée pour répondre à `isFile`/`name`, ce qui sur une
bibliothèque de quelques milliers de fichiers fait quelques milliers d'allers-
retours. Ici, une requête par répertoire rend tout ce qu'il faut.

**En largeur d'abord**, pour que les dossiers peu profonds et bien nommés soient
visités avant les profonds : ça compte quand la limite de fichiers coupe la marche.

## Une chaîne de décision, le moins cher d'abord

1. **Le nom du dossier**, quand il nomme une console : le joueur a trié ce fichier
   lui-même.
2. **L'extension**, quand elle appartient à une seule console.
3. **Les octets**, pour les extensions que trois consoles partagent.

Une extension partagée qu'aucune lecture ne cautionne est un disque qu'Emufii ne
sert pas (une PS1, une Dreamcast, un fournisseur illisible) : **il n'est pas listé**
plutôt que pointé vers un émulateur incapable de l'ouvrir.

Exception assumée : les conteneurs Nintendo (`rvz`, `wia`) **gardent la réponse de
l'extension** quand la lecture ne dit rien — les deux se jouent sur Dolphin de
toute façon, et un hoquet de fournisseur ne doit pas vider une bibliothèque.

De même, `.iso` et `.chd` ne disent rien de la console qui les a gravés : la PS2,
la Xbox et une pile de systèmes d'arcade en portent aussi, et ces jeux
atterrissaient dans la grille sous leur nom de fichier alors qu'Emufii ne peut rien
en faire. Pour ces deux conteneurs, le fichier doit **prouver** qu'il est un jeu
PSP — un `PSP_GAME` dans sa table des matières — faute de quoi il n'est pas listé.
`.pbp` et `.cso` restent admis sur leur seule extension : ils n'appartiennent qu'à
la PSP.

## Ce qu'on ouvre, et ce qu'on ne peut pas ouvrir

**3DS et DS sont ouverts**, parce que les deux portent leur vrai titre et leur
icône à l'intérieur — un SMDH pour la 3DS, une bannière pour la DS — et que les
deux sont bon marché à atteindre.

**La PSP aussi** : un UMD porte son icône et son titre dans son système de
fichiers, sous `PSP_GAME` — quelques kilooctets à lire sur un disque qui en pèse un
million. Le titre vient du `PARAM.SFO` et vaut nettement mieux que le nom de
fichier, qui traîne d'ordinaire sa région et sa révision entre parenthèses.

**Les images de disque prennent leur *titre* dans le nom de fichier**, et c'est
accepté : un `.rvz` est compressé, donc aucune bannière ne se trouve à un décalage
fixe, et il faudrait décompresser un jeu entier pour aller chercher son
`opening.bnr`. Leur **identité** est une autre affaire : une image non compressée
livre pour presque rien son identifiant de disque de six caractères, et c'est ce
que le garde-fou de session compare. Sans lui, la session ne publie rien et
l'invité s'entend dire qu'il n'a pas le jeu qu'il a sous les yeux — **exactement le
défaut corrigé pour la PSP en versionCode 12**.

Les icônes décodées atterrissent dans le répertoire de cache sous le code du jeu,
pour qu'un rescan ne redécode pas chaque bannière.

## `productCode` et `titleIdHex` ne jouent pas le même rôle

L'identifiant de disque sert de clé de cache, **mais pas d'identité de session** :
il va dans `productCode`, comme pour la DS, et non dans `titleIdHex`, qui décide si
deux joueurs ont vraiment le même jeu.

C'est délibéré et ça vaut pour la PSP comme pour le GameCube/Wii : **deux dumps
régionaux du même titre portent deux identifiants**, et rien ne dit encore qu'ils
refusent de jouer ensemble. `sessionId` sait retrouver un jeu par l'un ou l'autre,
mais le garde-fou « ce n'est pas le même jeu » ne refuse que sur un identifiant de
**titre**. Refuser sur un identifiant de disque reviendrait à interdire une partie
sur une supposition.

La PS2 suit le chemin des disques Nintendo pour la même raison : le titre vient du
nom de fichier, mais le numéro est lu dans le disque — `SLES-50877` sur
TimeSplitters 2, exactement ce qu'ARMSX2 affiche.

## Les clés Switch : ramassées, jamais fournies

Un dump Switch ne dit rien de lui-même sans elles : ni icône, ni titre. **Emufii
n'embarque aucune clé et n'en télécharge aucune** : il ramasse un `prod.keys` que
le joueur a déjà mis dans son propre dossier de ROMs, ce que tout émulateur Switch
lui demande de faire de toute façon. Gardées en mémoire seulement.

Absentes, les tuiles Switch gardent leurs initiales, exactement comme un fichier
non reconnu — et c'est ici le cas **courant**, pas l'exception.

## Ce que le joueur voit du dossier choisi

SAF nous rend un identifiant de document de la forme `primary:Roms/3DS` : le
préfixe de volume ne veut rien dire pour quiconque hors du framework, donc seule
la partie qui suit est montrée. Repli sur le dernier segment de l'URI pour les
fournisseurs dont le format d'identifiant nous est inconnu.

## Un nom que le fichier ne donne pas se demande à l'index

Une ROM dit toujours **quel jeu elle est** — l'identifiant de titre, le code de
jeu, l'identifiant de disque, le serial vivent dans des en-têtes qu'aucun
chiffrement ne touche — mais pas toujours son **titre** : pas de clés console
signifie pas de NACP sur un dump Switch, pas de SMDH sur un 3DS, et les formats
de disque ne portent aucune bannière une fois sur deux. La grille retombe alors
sur le nom de fichier, étiquettes de release comprises.

C'est donc le seul endroit où un vrai nom est *demandé*, par identifiant, aux
mêmes index publics contre lesquels l'outil de compatibilité résout les siens.
Toutes les consoles sont couvertes : une tuile qui nomme son jeu sur une console
et son fichier sur la suivante est le désordre que ça existe pour éviter.

Servi et mis en cache comme tout autre document public (`/compat`, `/meta`) : les
portables sont hors ligne la moitié du temps, et un vrai titre qui disparaît sans
Wi-Fi renverrait la grille au jargon de scène, ce qui se lit comme une panne.

La surcouche ne remplace **jamais** qu'un nom dérivé du fichier, et perd contre
les deux choses qui la dominent : un titre lu dans le fichier lui-même (la
cartouche parle sa propre langue) et le nom choisi par le joueur.

## La langue d'une cartouche est celle de l'app

Chaque format qu'Emufii lit porte son titre plusieurs fois — une bannière DS en
six langues, un SMDH 3DS en douze, un control Switch en seize — et chaque lecteur
choisissait dans une liste figée à « français, puis anglais, puis japonais ». Une
app en anglais affichait donc « Pokémon Version Blanche 2 », sans moyen de
demander autre chose. La cartouche connaît les deux noms ; la seule question est
lequel lire, et la réponse est la langue que l'app elle-même parle.

L'app est bilingue, donc chaque ordre nomme sa langue d'abord et l'autre ensuite,
puis garde l'ancienne queue : une cartouche japonaise qui ne porte ni français ni
anglais doit quand même produire *quelque chose*, et un titre japonais vaut mieux
qu'un nom de fichier.

Le marqueur de langue existe parce que les titres sont mis en cache sur disque.
Deux langues de la même cartouche sont deux chaînes différentes sous le même code
de jeu : la clé de cache doit donc porter la langue, sans quoi changer la langue
de l'app afficherait la précédente jusqu'au prochain scan.
