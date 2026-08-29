# Thème « DUOTONE SHELVES » — contrat de direction

Remplace « HOME MENU » (plastique moulé sur plateau gravé). Décision prise le
2026-08-27 à partir du logo v3 (`emufii_logo_v3.png`), validée par l'utilisateur.
Ce document est le contrat : les écrans s'auditent contre lui.

**Révision du 2026-08-28 — le monde n'est pas plat.** La première passe avait
lu « couches plates » comme « aucun relief » : ni biseau, ni encoche creusée,
un fond en deux halos flous. À l'écran, ça ne tenait pas. Une plaque blanche à
quatre points de luminance au-dessus d'une coquille crème, avec une arête à
24 % et une ombre à 14 %, ne se décolle pas de son fond : chaque écran se lisait
comme une seule feuille. Et deux halos qui se chevauchent à 12 % d'alpha ne sont
plus corail et turquoise, ce sont un lavis rose sale — l'escalier signature
n'était visible nulle part. Deux corrections, décidées par l'utilisateur :
l'escalier devient **littéral** (des tuiles, avec un contour) et le **relief
revient** (moulure éclairée, encoche creusée). Ce qui ne revient pas du monde
HOME MENU : l'ombre portée dure et décalée, le plateau gravé, et l'accent
unique. Les sections ci-dessous portent ces corrections.

## THÈSE

Le logo devient la grammaire de l'interface. Trois tuiles arrondies en escalier
diagonal — corail en haut-gauche, crème au premier plan avec le glyphe, turquoise
en bas-droite. L'app passe d'un monde *monochrome moulé + un accent* à un monde
**bicolore en couches plates** : deux axes de couleur qui se croisent, une tuile
neutre chaude au centre. Refusé : le menu-console gris plastique à relief gravé,
et l'accent unique cyan qui ne distinguait pas « jouer » de « se connecter ».

## LE MONDE

- **Surfaces en couches** qui se chevauchent, comme les tuiles du logo — mais
  des couches qui ont une épaisseur. Une tuile = micro-dégradé vertical (≤3 %
  luminosité), une arête 1 dp, une **moulure** (liseré éclairé sur l'arête
  intérieure haute, ombré en bas), et une ombre ambiante qui grandit avec
  l'élévation logique. Une seule source de lumière pour toute l'app : haute,
  légèrement à gauche. Restent interdits : la grille gravée et l'ombre offset
  dure du monde HOME MENU.
### L'escalier diagonal

Le motif signature : halos en filigrane du fond,
  carte sélectionnée qui monte *et* glisse en diagonale, dialogues qui
  chevauchent un volet en biais derrière eux.
### Deux axes sémantiques

Pas un accent :
  - **Corail** = le social : sessions, amis, rejoindre, présence. Créer un lien.
  - **TURQUOISE** = le jeu et le système : lancer, valider, naviguer, bibliothèque.
  - Le neutre crème chaud = le premier plan (la tuile du glyphe).
### Les creux deviennent des encoches

Des encoches creusées : teinte basse de la plaque *et*
  moulure inversée — ombrée en haut sous la lèvre, éclairée en bas où la
  lumière atteint le fond. C'est la même source, frappant un creux au lieu
  d'une bosse ; une fois la lumière posée, c'est la seule façon honnête de dire
  « enfoncé ».

## PALETTE (contrat chiffré)

### Axes (trois coupes chacun : bright / deep / ink ; soft = bright à 20 %)

| Axe | bright | deep (fond clair, texte blanc) | ink (texte sur fond clair) | dark bright |
|---|---|---|---|---|
| Corail | `#EE6FA3` | `#C24B7E` | `#5A1D3E` | `#F793BC` |
| Turquoise | `#3FCFC0` | `#0E9C8F` | `#0A4A44` | `#5CE0D2` |

Violet de profondeur (liens, sheen, bas de dégradé du logo) : `#6B72E0`
(dark `#8E93EC`). Glyphe/encre chaude : `#221B26`.

### Neutres chauds (light) — la tuile crème étendue

- Coquille : `#F1EFEA` / basse `#E2DFD7` — deux fois déplacée le 2026-08-28, en
  sens contraires, et les deux fois à raison. Partie de `#F5F1E8`, à quatre
  points sous la plaque blanche : une carte n'avait pas de sol. Approfondie à
  `#EDE6D6`, elle l'a eu, mais toute l'app a jauni — dix points de saturation,
  sur un plein écran, ce n'est plus un neutre, c'est une couleur. La valeur
  retenue garde l'écart dont la moulure et l'ombre ont besoin en retirant
  l'essentiel du jaune
- Plaque : `#FFFFFF` / basse `#F7F5F1`
- Encre : `#221B26`, muted `#6E6475`
- Arêtes : noir chaud `#241610` à alpha (remplace le noir-bleu)

### Neutres violacés (dark) — le bas du dégradé turquoise

- Coquille : `#120F1D` / basse `#090711`
- Plaque : `#272238` / basse `#1C1929`
- Encre : `#F0EAF5`, muted `#9B93AC`

### OLED

Coquille `#000000`, plaque `#16131F` / basse `#0F0D17` ; halos du fond à alpha
réduit pour rester noir.

### Sémantique (centralisée)

Plus jamais dupliquée en dur.

- Bon/vert tiré vers le turquoise : `#1FA98B` (dark `#3BC4A6`)
- Attention/ambre : `#C98A12` (dark `#E3A83C`)
- Erreur tirée vers le corail : `#E5604F` (dark `#F0796A`)
- Info/bleu : `#5A8FD8` (dark `#82AFE6`)

### Stratégie de couleur

Committed duo : les deux axes portent la structure (rings, actions primaires,
domaines), le fond et les surfaces restent neutres chauds. Le coloris n'est
jamais décoratif : il encode *jeu* ou *lien*.

## MATIÈRE (remplace Plastic.kt)

- `plate()` : micro-dégradé + arête 1 dp + moulure + ombre ambiante. La lèvre
  s'élargit avec l'élévation (1,5 dp, 2 dp au-delà de 10 dp de lift) — un
  dialogue ne porte pas le même liseré qu'une pastille. `pressed` : la tuile
  s'enfonce (scale 0.98, ombre au tiers) **et retourne sa lumière**.
- `socket()` : teinte basse de la plaque + moulure inversée. Le nom reste,
  vingt appelants le disent encore.
- `engravedGrid` reste vide : le fond porte déjà son relief, et deux motifs sur
  le même sol se battraient.
- Le fond (TrayBackdrop) : neutre chaud + **deux tuiles squircle énormes** —
  corail haut-gauche, turquoise bas-droite — chacune avec un dégradé de corps
  (bright en haut, deep en bas) et **un contour de 2 dp**. C'est le coin et
  l'arête qui disent « tuile », pas la teinte : sans contour on retombe sur le
  lavis. Assez grandes pour déborder de deux côtés chacune. La troisième tuile,
  la crème du premier plan, ce sont les plaques de l'app elles-mêmes — le fond
  ne dessine que les deux qui sont derrière. Puis sheen diagonal violet et
  vignette, gelés si les animations sont désactivées.
- Chevauchement : un élément au premier plan (dialogue, tuile sélectionnée)
  peut déborder en diagonale sur ce qui est derrière.

## FOCUS MANETTE

Le ring (contour 4 dp + halo, entrée 140 ms, sortie 0, bring-into-view) est
conservé tel quel mais devient **turquoise** par défaut et **corail** sur les
zones sociales (chips session/amis, listes d'amis, join). La couleur du curseur
dit la zone. L'ombre de la tuile sélectionnée prend la teinte de l'axe.

## TYPOGRAPHIE

M PLUS Rounded 1c conservée (ses rondes épousent les squircles du logo).
Contraste de graisse assumé : titres Black/ExtraBold, corps Regular. Pas de
texte en dégradé, pas de bicolore sur les titres.

## FORMES

Squircles du logo : tuile bibliothèque 16 → **20 dp**, artwork 13 → 16 dp,
cartes 22 → **28 dp** (le premier plan est plus rond), inset 14 dp, actions
18 dp, pilules 50 %. « Un seul langage de coins » conservé.

## DÉCLINAISON PAR ÉCRAN

- **Splash** — LED corail → turquoise (rééchantillonnées du logo v3). Option :
  assemblage des trois tuiles en diagonale puis apparition du glyphe.
- **Bibliothèque** — tuiles crème squircle sur coquille crème plus sombre ;
  focus : scale + ring turquoise + ombre teintée. Badge console pilule ;
  compat garde le code sémantique. En-tête flottant conservé (jamais une
  barre) : pilules plates sur encoches, chips Sessions/Amis/Profil **corail**
  quand actives, contrôles bibliothèque neutres/turquoise.
### Fiche de jeu (dialogue)

 grande carte au premier plan, et le liseré des
  deux axes **sur son contour**, dont la couleur dérive d'un axe à l'autre
  (7 s, aller-retour, figée à mi-course si les animations système sont coupées).
  C'est le seul écran où les deux sont vrais à la fois — la carte propose de
  créer une session (corail) et de lancer (turquoise) — et une teinte figée y
  prendrait un parti que l'écran ne prend pas. Lancer = pilule turquoise pleine ;
  privé/session = corail.

  **Une tuile posée derrière a été essayée trois fois, puis retirée.** C'était la
  troisième tuile du logo prise au pied de la lettre : une plaque turquoise
  débordant de la carte, d'abord en `matchParentSize` (donc de la taille de
  l'écran entier), puis à la taille de la carte et penchée de 3°, puis droite
  avec une marge égale de 6 dp. Les trois avaient le même défaut de fond : pour
  dire « il y a une couche en dessous », elles ajoutaient un objet de plus à un
  écran qui en a déjà deux — la carte, et la bibliothèque assombrie derrière. Le
  contour dit la même chose sans rien ajouter, et il porte la dérive aussi bien.

  Piège de forme, payé deux fois : le liseré est un `drawWithContent`, donc il
  prend la taille de **ce qu'il enveloppe**. Placé en tête de chaîne il
  enveloppait aussi le padding extérieur et traçait un contour 48 dp plus large
  que la carte, flottant autour d'elle. Il va sous les bornes de taille, et sous
  `scale`/`alpha` pour arriver avec la carte au lieu de rester fixe pendant
  qu'elle grandit.

### Session / Join — domaine corail

Session, Join, Friends, Finder, Wfc, PspOnline : domaine corail :
  codes, présence, rejoindre, alertes. Slots de code en encoches crème, caret
  corail. Quitter = erreur.
### Réglages

Hub neutre, cartes crème, icônes en encoches teintées par
  domaine (turquoise système, corail profil/social). L'accent configurable est
  retiré : deux couleurs sémantiques le remplacent (SYSTEM/Material You peut
  rester en option qui teinte les deux axes).
### Onboarding / Preparing

 cartes neutres avec le liseré corail→turquoise
  **sur tout le contour**, pour les flux d'attente et de connexion. Il vit dans
  `ui/components/WaitTrim.kt`.

  Repris trois fois le 2026-08-28. Ce qui était écrit n'était pas un liseré sur
  une arête mais une **corde** : un segment droit joignant un point du bord
  gauche à un point du bord haut, donc une barre en biais posée en travers de la
  face. Premier défaut, elle n'était pas découpée à la silhouette, donc ses deux
  bouts sortaient de la plaque par le coin *arrondi* et flottaient sur le fond.
  Découpée, le second est apparu : elle restait une balafre de 6 dp barrant le
  quart supérieur gauche, à pleine saturation, et sur un écran qui arrive en
  fondu la carte est encore transparente quand la bande est déjà pleine —
  pendant un instant on ne voit qu'elle. Ramenée sur l'arête, elle n'en tenait
  plus qu'un coin, ce qui se lisait comme un accident de tracé.

  Le tracé est le **contour de la carte lui-même, sur tout son tour** : 3 dp, le
  dégradé court sur la diagonale du logo, donc chaque côté porte la couleur de
  son coin. Il borde au lieu de barrer.

- **Second écran** (SecondScreenHost) — même thème, mêmes tokens.

### Avatars

L'avatar prend la teinte de l'axe social — corail — et jamais l'accent choisi :
c'est une personne, pas un réglage. Sa pastille de crayon est déclarée après
l'anneau du curseur, sinon le trait de l'anneau lui passe au travers.

## GARDÉ TEL QUEL

Navigation manette (Gamepad.kt comportement), en-tête flottant + WallpaperVeil,
structure des écrans (sealed Screen), grille 2–4 colonnes, clavier maison,
PadDialog/PadTextField, densité « menu console » : peu d'éléments, gros,
espacés.

## CONTRAINTES (aucun hex en dur)

- Contraste : turquoise plein sur crème est limite en texte → réservé aux fonds
  de boutons (encre foncée ou blanche selon coupe) et aux rings, jamais au
  corps de texte. Muted se teinte de l'hue de sa surface, jamais gris neutre.
- Toute couleur vit dans le thème (Color.kt / objet sémantique) ; plus aucun
  hex en dur dans les écrans (badges, statuts, avatars, splash, voiles).
- Avatars : dégradés remixés depuis les deux axes du logo.

## ANTI-RÉFÉRENCES

L'accent cyan unique, la grille gravée et l'ombre offset chaude du monde HOME
MENU — remplacés, pas adoucis. Le « Liquid Glass » (flou, translucide, halos
iOS) reste interdit comme avant. **Le biseau et l'encoche creusée ne sont plus
des anti-références** depuis le 2026-08-28 : c'était le seul point du contrat
qui, appliqué, vidait l'interface de sa matière.

## Le lustre est parti

La large bande de lumière qui traversait le plateau en dix-neuf secondes a été
retirée le 2026-08-29. Elle se lisait comme un voile semi-transparent balayant
l'écran, ce qu'un fond ne doit pas faire.

Et sur le panneau arrière, où le plateau est figé, elle était peinte immobile :
sa boîte de découpe laissait alors une arête franche en bas à gauche, un grand
carré que rien n'expliquait. Le même défaut des deux côtés, invisible d'un côté
et flagrant de l'autre.

Restent les deux étagères, leurs ondes et la vignette. Ne pas la réintroduire :
c'est la seule chose du fond dont le mouvement se remarquait.

## MATIÈRE (fond)

### Deux étagères, et un budget de mouvement

Les deux étagères sont l'escalier du logo à l'échelle de l'écran — corail en haut
à gauche, turquoise en bas à droite. Ce sont des carrés arrondis assez grands
pour sortir par deux bords : seul le coin qui regarde le milieu reste visible, et
ce coin est tout le motif. **C'est le coin et l'arête qui disent « tuile », pas
la teinte** ; deux halos flous à 12 % ne donnaient qu'un lavis rose sale, sans
bord, sans escalier, sans logo.

La couleur n'encode que les deux axes. Avec des jaquettes par-dessus, deux
palettes se battent : les étagères restent donc sous le contenu et jamais sous un
texte.

**Le budget de mouvement est le sujet de ce fond.** Il a tenu la moitié d'un
processeur à repeindre à 120 Hz sans que rien ne se passe à l'écran, et il l'a
refait une seconde fois : 85 % d'un cœur, mesuré sur une bibliothèque immobile,
contre 3,7 % pour le lanceur d'à côté. Deux causes, toutes deux corrigées, toutes
deux à ne pas réintroduire :

1. **Tout était redessiné à chaque image**, y compris ce qui ne bouge pas — une
   dizaine de dégradés reconstruits trente fois par seconde pour un résultat
   identique. Ce qui est immobile est cuit dans un bitmap à demi résolution,
   enregistré une fois ; seules les ondes se redessinent.
2. **Le flou gaussien retombait en rendu logiciel.** `BlurMaskFilter` n'a pas
   d'équivalent GPU : Android dessinait ces chemins sur le processeur, dans une
   image intermédiaire, douze fois par image. La lueur des ondes se fait en
   traits empilés, que le GPU trace sans y penser. Même leçon que le curseur —
   voir `navigation-manette.md` § Les quatre couches du curseur néon.

## Le liseré des deux axes borde, il ne barre pas

Le contrat disait « un liseré diagonal sur une arête », et ce qui a été écrit
était une **corde** : un segment droit joignant un point du bord gauche à un
point du bord haut, donc une barre en biais posée en travers de la face. Deux
défauts, dont le second n'est apparu qu'une fois le premier corrigé :

1. Elle n'était pas découpée à la silhouette, donc ses deux bouts sortaient de la
   plaque par le coin *arrondi* et flottaient sur le fond.
2. Découpée, elle restait une balafre de 6 dp barrant le quart supérieur gauche,
   à pleine saturation. Sur un écran qui arrive en fondu, la carte est encore
   transparente quand la bande est déjà pleine : pendant un instant, on ne voit
   qu'elle.

Le tracé est le **contour de la carte lui-même**, et il en fait le tour : le
dégradé court sur la diagonale du logo, corail en haut à gauche, turquoise en bas
à droite, donc chaque côté porte la couleur de son coin. Il borde au lieu de
barrer.

La phase fait dériver le dégradé le long de cette diagonale. À zéro il ne bouge
pas — c'est ce que veulent les écrans d'attente, où la seule chose qui doit
tourner est le disque de progression. La carte de session, elle, le fait respirer
d'un axe à l'autre : c'est le seul écran où les deux axes sont vrais à la fois,
et une teinte figée y prendrait un parti que l'écran ne prend pas.

## La pastille de compatibilité est l'exception documentée à l'accent unique

Trois marques, une par verdict, et un jeu que personne n'a noté n'en montre
aucune. **Cette dernière distinction est celle qui compte** : un jeu non testé et
un jeu connu comme fonctionnel ne doivent pas se ressembler, sinon la pastille
cesse d'être une information et devient une décoration. Une coche veut dire que
quelqu'un a vérifié.

Ça a été construit dans l'autre sens d'abord — rien de dessiné pour un jeu qui
marche, au motif qu'une bibliothèque est surtout faite de jeux qui marchent et
que tous les marquer poserait une marque sur presque chaque tuile. Ce raisonnement
tient pour la *densité* et se trompait sur le *sens* : le silence veut déjà dire
« inconnu » ici, donc le dépenser aussi pour « vérifié » rendait les deux
indiscernables.

Sur les couleurs : le chrome reste achromatique et le seul accent est réservé au
curseur. Ceci est l'exception documentée, et elle est étroite — une marque qui ne
paraît que sur les jeux que quelqu'un a réellement jugés, en trois couleurs
fixes, dont aucune n'est l'accent. **Fixes plutôt que suivant l'accent choisi**,
à dessein : un verdict est le même fait pour chaque joueur, et une pastille qui
changerait de couleur avec un réglage personnel dirait quelque chose du réglage
au lieu de dire quelque chose du jeu.
