# Bibliothèque : les trois mises en page, le curseur, et ce qui a été repris

Le récit qui vivait dans `ui/screens/LibraryScreen.kt`, sorti du code le
2026-08-24 (cf. `docs/STYLE_COMMENTAIRES.md`). Les titres sont des ancres citées
depuis le code : ne pas les renommer à la légère.

La règle mère est dans le `CLAUDE.md` — **une grille paresseuse tient son propre
curseur** — et le détail de l'anneau est dans
[`navigation-manette.md`](navigation-manette.md).

## Trois mises en page, un seul contrat de curseur

Grille, carrousel et liste **gardent chacun leur propre index**. C'est
l'invariant appris à la dure sur la grille, et il vaut tout autant pour un
`LazyRow` ou un `LazyColumn` : une liste paresseuse ne compose que ce qui est à
l'écran, donc la destination d'une direction n'existe souvent pas encore. Ce qui
change d'une mise en page à l'autre, c'est ce que « à droite » veut dire, et rien
d'autre.

Les gestes communs — valider, ouvrir le menu, sortir par le haut, remonter d'un
dossier — sont factorisés : sans ça, une correction manette dans la grille
laissait les deux autres cassées.

Une cellule peut être un jeu **ou un dossier**, dans une seule liste partagée par
les trois mises en page : sinon chacune porterait sa propre branche « suis-je en
mode dossier », soit trois endroits où se tromper sur une seule question.

## Le curseur est un index calculé, jamais un focus deviné

Un `LazyVerticalGrid` ne compose que ce qui est à l'écran : la tuile visée par
une direction n'existe souvent pas, Compose ne trouve alors aucune destination et
se rabat sur le premier élément focalisable — la tuile en haut à gauche.
Symptômes vus en vrai : un curseur qui disparaît, un curseur qui remonte tout en
haut d'une seule pression, un curseur qui saute à gauche en changeant d'écran.

Un index qu'on calcule soi-même **ne peut pas se perdre** : il ne dépend d'aucun
composant vivant.

Corollaire : **la tuile est cliquable mais ne prend jamais le focus.**
`clickable` rend focalisable par défaut, ce qui laissait autant d'arrêts
invisibles que de tuiles ; le curseur étant tenu par la grille, une tuile qui
capte le focus le fait disparaître sans rien montrer à la place.

## Amener la cible, pas seulement la rendre « visible »

Compose fait défiler pour rendre l'élément focalisé *visible*, et « visible » est
tout ce qu'il veut : une tuile à moitié sous le voile du haut compte comme
visible, donc atteindre le début de la liste ne remontait pas jusqu'en haut.

Les marges de la grille disent exactement ce que prennent le haut et le bas : on
s'en sert pour **finir** le mouvement au lieu de s'arrêter au premier pixel
visible.

Dans la liste, la sélection vise le **milieu** de la bande utile. L'amener juste
à l'intérieur était encore faux : en descendant, elle venait buter contre le bord
bas sans rien de visible après elle, donc le joueur ne voyait pas ce vers quoi il
allait. Viser le centre fait défiler d'exactement une rangée par pression, avec
autant de liste devant que derrière — ce que fait tout menu de console. Les deux
extrémités se règlent seules : `animateScrollBy` sature, et le curseur parcourt
librement les premières et dernières rangées.

Deux choses s'ajoutent, et les deux sont nécessaires : une **marge**, parce que
la rangée sélectionnée porte une lueur qui déborde de ses bornes, et **la bande
que repeint le voile du bas**, invisible à la mise en page — Compose considère
une rangée dessous comme parfaitement visible, alors qu'elle ne l'est pas du tout.

## Des rangées entières, ou rien

Un plateau montre des objets, et un demi-objet est un défaut de rendu, pas un
indice qu'il y a une suite. Laissée à elle-même, la grille remplissait le
viewport et coupait la dernière rangée au milieu de la deuxième ligne de ses
titres : « Shadow of the Colossus + Ico » était tranché en plein milieu de ses
lettres **au repos**, sur un écran que personne n'avait fait défiler.

La hauteur restante est donc mesurée et donnée à la **marge haute** plutôt que
laissée en bas : les mêmes rangées sont à l'écran, elles sont toutes entières, et
le mou devient de l'air sous l'en-tête au lieu d'un titre sectionné. La marge
basse ne peut pas faire ce travail : c'est du *voyage*, et le voyage n'existe
qu'une fois qu'on a défilé.

Seuls les derniers dp de mou sont dépensés, et en haut : une rangée entière
d'espace vide centrée sous l'en-tête se lirait comme une mise en page ratée.

**La taille de tuile vient de la hauteur, pas seulement de la largeur.**
Dimensionnées sur la largeur seule, six colonnes donnaient une rangée trop haute
pour que deux tiennent. Le nombre de colonnes monte donc — des plaques plus
petites, plus nombreuses — jusqu'à ce que les rangées tiennent entières. Ça
s'arrête dès que c'est le cas, et **jamais au-delà de trois colonnes
supplémentaires** : passé ça les jaquettes ne se reconnaissent plus, ce qui coûte
plus cher que la coupure.

Le portrait garde les trois grandes tuiles de l'ambiance « menu de console ». En
paysage — la façon dont une portable se tient réellement — ça les étirerait au
tiers d'un écran large, donc le nombre de colonnes suit la largeur et les tuiles
gardent leur taille.

**Un seul nombre de colonnes.** Tout ce qui est en aval — les emplacements vides
qui carrent le plateau, et surtout l'arithmétique du curseur, qui se déplace de
±colonnes — lit celui-là et rien d'autre. Une grille qui rend un compte pendant
que le curseur en compte un autre, c'est toute la famille de bugs que cet écran a
été écrit pour finir.

## L'air sous l'en-tête est nommé, plus laissé au hasard

Ça n'était le travail de personne. La grille verse sa hauteur restante dans sa
marge haute, et ce mou faisait accessoirement office d'écart : la grille avait
l'air juste et rien ne disait pourquoi. La liste, elle, n'a pas de mou à verser :
sa première plaque arrivait contre les pastilles de l'en-tête, assez près pour
les toucher, avec leur ombre portée tombant dessus.

D'où une valeur **nommée**, que chaque mise en page garantit à sa façon : la
grille la prend comme **plancher** sur son mou, la liste l'ajoute franchement.

Plancher et non addition : le mou est d'ordinaire plus grand que l'écart n'a
besoin d'être, et additionner les deux poussait le plateau de 14 dp de plus pour
rien, amenant les titres de la dernière rangée dans le voile du bas.

## L'arrivée des tuiles est armée, puis désarmée

L'arrivée est pour l'**ouverture** de la bibliothèque : les tuiles entrent et ça
se lit comme l'étagère qu'on remplit. Mais une grille paresseuse compose une
tuile dès qu'elle approche du viewport, donc chaque rangée atteinte la rejouait.

Or l'arrivée fait apparaître une tuile depuis la transparence, et une couche
translucide laisse l'ombre du curseur se voir **à travers** la tuile qu'elle
entoure. C'est ça, la « lueur creuse » qui a survécu à la correction de timing :
pas un anneau mal dessiné, une tuile pas encore opaque en dessous.

L'animation est donc armée à l'ouverture puis désarmée. Un rescan ou l'entrée
dans un dossier la réarme, ce pour quoi elle a été écrite.

## Une seule horloge pour tout ce qui marque la cellule

La tuile visée **grandit** : c'est le premier signal que donne un menu de
console, et sur une grille de tuiles blanches il porte plus loin qu'un contour.

Sur la même horloge que l'anneau, et repartant au même instant. C'était un
ressort rebondissant, qui se pose en une demi-seconde — quatre fois l'arrivée de
l'anneau, lequel repart désormais instantanément. La tuile qu'on venait de
quitter restait donc agrandie sans anneau autour, un cadre avec rien dedans,
pendant que celle où l'on arrivait était encore au repos avec un anneau en train
d'apparaître : quelques images durant, le curseur avait l'air de s'être coupé en
deux moitiés en laissant un creux derrière lui.

L'anneau n'est **jamais** dessiné sur une tuile encore en train d'apparaître, et
il est dessiné sur la tuile rognée, en dehors du contour de la jaquette, pour que
les deux ne se lisent pas comme une seule bordure épaisse de deux couleurs.

## Sortir par le haut se nomme, et selon la colonne

Depuis la première rangée, on quitte la grille par le haut. La destination est
**nommée** : les deux couches sont sœurs dans un même `Box`, et la traversée
automatique n'y voit aucun chemin. Le retour vers le bas est nommé de la même
façon, et posé sur la rangée entière plutôt que sur le seul groupe de droite —
maintenant que le coin gauche porte des boutons, on doit pouvoir en redescendre.

Et la destination dépend de **la colonne**. Chaque mouvement vers le haut visait
les sessions, à droite : depuis une tuile du bord gauche, le curseur traversait
tout l'écran pour un geste qui ne demandait qu'à monter. Il rejoint désormais le
groupe de son propre côté. Les mises en page sans colonnes — le carrousel et sa
carte centrée, la liste et ses rangées pleine largeur — n'ont pas de côté à
déduire : elles gardent la droite, là où l'app mène.

## Le carrousel doit suivre le doigt sans se retourner contre la manette

La carte visée vient au **centre**, pas « quelque part à l'écran » : un carrousel
dont l'élément actif finit collé à un bord ne se lit plus comme un carrousel.

La carte active est **la plus proche du milieu du viewport, quoi qu'elle y ait
amenée**. C'est ce qui le fait marcher sous un doigt : l'active était auparavant
celle que la croix avait désignée en dernier, donc un défilement tactile
déplaçait la rangée pendant que la carte agrandie restait en arrière, et le
carrousel s'immobilisait entre deux cartes avec la mauvaise allumée.

Mais le suivi doit être **coupé pendant nos propres défilements** : un défilement
programmé balaie toutes les cartes entre ici et la cible, et si le curseur suivait
le centre pendant ce temps, une double pression rapide calculerait son second pas
depuis la carte survolée au passage.

De même, le calage ne doit répondre **qu'au doigt**. Indexé sur
`isScrollInProgress` seul, il se déclenchait aussi à la fin de nos propres
animations et recalait depuis là où ce défilement se déclarait à cet instant : une
tape sur la carte voisine atterrissait deux cartes plus loin. Une interaction de
*drag* est le seul signal honnête qu'une personne, et pas ce fichier, a bougé la
rangée.

**Une tape sur une carte qui n'est pas au milieu l'amène au milieu ; seule celle
du milieu ouvre.** Lancer directement depuis une carte latérale était l'autre
moitié du carrousel réservé à la manette : le centre voulait dire quelque chose et
le tactile pouvait l'ignorer. Ça faisait aussi des voisines — dessinées petites et
estompées précisément pour dire « pas celle-ci » — les choses les plus faciles à
lancer par accident.

### Trois mesures du carrousel, toutes corrigées sur capture

- **La carte se dimensionne sur la hauteur réellement libre**, pas sur celle de
  l'écran. Une fraction de la plus petite dimension de l'écran donnait des cartes
  qui débordaient : la bande du haut, la bannière et la barre de navigation
  prennent chacune leur part, et le titre sous la carte en veut une quarantaine de
  dp de plus.
- **Les marges latérales valent la moitié de ce qui reste autour d'une carte**, et
  c'est ce qui permet à la première et à la dernière d'atteindre le centre. Avec
  une marge fixe, une liste ne peut pas défiler avant son début : la carte active
  restait collée au bord gauche jusqu'à deux crans, donc le carrousel s'ouvrait
  toujours de travers.
- **C'est la carte qu'on centre, pas la colonne.** Un élément est une carte
  au-dessus de son titre ; centrer l'ensemble met le milieu de la colonne au milieu
  de l'écran, donc la jaquette — sa partie haute, la seule regardée — finit trop
  haut. La place du titre est donc **déplacée** du bas vers le haut plutôt
  qu'ajoutée en haut : la somme des deux marges ne change pas, donc la carte
  descend sans rien perdre de sa taille. La première tentative ajoutait seulement
  en haut, et la carte perdait un cinquième d'elle-même pour un défaut purement
  positionnel. Et **la moitié** de la place du titre, pas la totalité : déplacer x
  d'un côté à l'autre décale le contenu de x, et il faut décaler d'un demi-titre.
  Mesuré sur une capture, pas jugé à l'œil.

Enfin, la marge de tête est **déjà comptée** dans le repère
d'`animateScrollToItem` (`viewportStartOffset` vaut `-beforeContentPadding`) :
repasser ce même décalage l'appliquait deux fois, la liste ne bougeait pas d'un
cran sur les premières cartes, et le curseur avançait pendant que la carte visée
restait à droite du centre.

## La liste existe pour distinguer deux dumps du même jeu

Une icône ne sépare pas deux dumps du même jeu, ni deux épisodes d'une série
partageant une jaquette. La liste existe pour ce moment-là : le nom complet sur
une ligne, la console à droite, une vignette assez grande pour reconnaître sans
dominer.

**L'anneau d'abord, avant tout ce qui rogne — la règle de la maison — et surtout
avant un remplissage opaque.** La rangée était un film translucide (un blanc à 8 %
sur le plateau) avec l'anneau appliqué après. Deux fautes en une : la lueur est
une ombre, et une ombre sous une couche transparente est dessinée *à travers*,
donc la lumière du curseur se répandait dans la rangée en un lavis plat à bouts
carrés — la « lueur qui se décompose et devient creuse », sa seconde et dernière
source. Et un film n'est pas la matière de cette app : toute surface sélectionnable
y est une plaque moulée.

## Les dossiers de console

Un dossier emprunte la forme des tuiles (carré, mêmes coins, même lueur de focus)
et s'en écarte en substance : une plaque de couleur portant le nom de la console,
pas une jaquette. La distinction doit tenir à la vitesse où l'on balaie une
grille, sans lire.

La place du titre est **réservée mais laissée vide** : la plaque porte déjà le nom
en grand, et le répéter dessous donnait le même mot deux fois à dix dp d'écart.
L'espace doit rester, sans quoi un dossier soulèverait toute sa rangée par rapport
aux emplacements vides qui la complètent.

**Deux fichiers d'illustration par console, clair et sombre**, parce que ce sont
des illustrations avec leur propre fond et non des glyphes à teinter : recolorer
l'une abîmerait le dessin, et montrer la claire sur le thème sombre pose un carré
blanc au milieu d'une grille noire. Le retour est **nullable** et c'en est tout
l'intérêt : une console ajoutée demain montre son nom en typographie jusqu'à ce
que ses deux fichiers existent, au lieu d'emprunter l'illustration d'une autre
machine.

À défaut, la plaque prend une couleur tirée de la palette déjà utilisée pour les
jaquettes manquantes, indexée sur le nom de la console : deux consoles ne tombent
pas sur la même couleur, et la couleur d'une console ne bouge pas d'un lancement à
l'autre.

Le fil d'Ariane vit **dans la rangée des réglages** plutôt que sur une ligne à lui :
une bande pleine largeur pour trois mots poussait la grille, la liste et le
carrousel d'autant, alors que la barre du haut a la place — et « où suis-je »
appartient à la même famille de questions que « comment je regarde ».

## La barre du haut : deux étagères, jamais une barre

À gauche ce que je regarde, à droite qui je suis. Le logo tenait le coin gauche et
n'y faisait rien : une marque qu'on lit une fois, sur l'écran qu'on ouvre le plus
souvent. Les deux réglages d'affichage l'ont remplacé, étant ce qu'on vient
réellement toucher.

Rien sur le tunnel ici non plus : il est mené par la session, donc ce n'est pas une
plomberie que le joueur démarre ou arrête, et un indicateur qui en rendrait compte
rendrait compte de quelque chose sur quoi il ne peut pas agir.

Chaque groupe est dans **son propre creux** — l'écran encastré où une console met
ses voyants. Deux étagères et non une barre : un rectangle pleine largeur en haut a
été rejeté sur ce projet à répétition, et il écraserait le plateau sous un en-tête.
En creux, les disques cessent de se lire comme cinq boutons épars sur neuf cents
pixels de rien et deviennent un panneau avec des commandes dedans.

Le dock ne garde qu'une destination par pastille. Il portait Dossier et Rescan à
côté : deux entretiens qu'on touche une fois et plus jamais, et les poser en
permanence sur l'écran d'accueil donnait trois pastilles d'aspect égal dont une
seule menait quelque part.

Les emplacements vides sont **à peine là**, exprès : ils existent pour garder la
grille carrée, comme le fait un menu de console, pas pour ressembler à du contenu
qui n'a pas chargé. Un creux plutôt qu'une plaque pâle — une place vide sur un
plateau est un logement sans rien dedans, et il est éclairé par en dessous là où
une plaque l'est par au-dessus.

## La recherche prend l'étagère, et les deux états ne se croisent pas

Un champ et les pastilles de mise en page en même temps promettraient deux choses
à la fois, et sur une portable l'étagère n'a de toute façon pas la largeur. Ce que
le joueur regardait revient intact à la fermeture.

**Les deux états se relaient, ils ne partagent pas l'étagère.** Les croiser est ce
qui faisait clignoter la fermeture : le champ et chacune des trois pastilles
dessinent leur propre creux, et un creux est un encastrement translucide.
Superposés à alpha partiel, ils s'empilaient en une dalle plus claire pendant deux
images — ce qui se lit comme un clignement, pas comme un échange. Le sortant est
parti avant que l'entrant commence.

La **taille** est laissée à l'`animateContentSize` de l'étagère, qui a aussi le fil
d'Ariane à porter : `AnimatedContent` anime la taille lui aussi par défaut, et les
deux tirant sur le même nœud faisaient le reste du tremblement.

**Le panneau de saisie est un état à lui, pas une seconde lecture de « la recherche
est ouverte ».** Les lier voulait dire que ranger le clavier jetait la requête : le
joueur posait le panneau pour voir les résultats qu'il venait d'épeler, et
c'étaient les résultats qui disparaissaient. Taper et lire sont deux moitiés d'une
seule recherche : seule la fermeture de la recherche vide le champ.

Le clavier flotte sur le bas de la grille et **jamais au-delà de sa moitié** : ce
que le joueur cherche reste à l'écran pendant qu'il l'épelle. Il monte depuis le
bord bas plutôt que d'apparaître — un clavier qui surgit est une boîte de dialogue
système, un clavier qui glisse fait partie du plateau.

Sa sortie est un **glissement seul, sans fondu, et jusqu'au-delà du bord**. Le
panneau est dépoli par Haze, et le fondre revenait à animer l'alpha d'une couche
floutée : le flou décroche pendant ce temps, donc pendant une image ou deux le
remplissage plat en dessous se voit. En prime, l'ancienne sortie ne parcourait
qu'un tiers de la hauteur, donc le panneau atteignait l'alpha zéro alors qu'il
était encore aux deux tiers à l'écran et disparaissait sur place au lieu de partir.

Une **tape hors du panneau** le range. Rien ne le disait avant : le clavier n'avait
d'autre sortie que le retour système, et une tape sur la grille traversait jusqu'à
une tuile et ouvrait un jeu en plein mot. La zone est invisible et déclarée
**avant** la barre du haut, pour que le champ de recherche reste au-dessus et
réponde encore : c'est la seule chose hors du panneau qui ne doit pas le ranger,
puisque le toucher est la façon de le faire revenir.

## Les voiles, et pourquoi la carte de lancement est là où elle est

Les deux voiles sont **dans la source Haze et après la grille**. Cet écran était le
seul à porter du chrome flottant sans eux : la pastille du nom et les puces de
profil se retrouvaient nues sur les jaquettes dès que la grille défilait, et le dock
cachait en permanence deux titres de tuiles. Le contenu qui monte doit aller
quelque part. `EmufiiScaffold` résolvait déjà exactement ça partout ailleurs : c'est
sa technique, pas une seconde.

Ils **rognent** la grille plutôt que de lui prendre de la place : la bande du haut
et le dock flottent toujours au-dessus, et les tuiles se dissolvent dedans au lieu
de les traverser.

La **carte de lancement** est dans ce `Box`, et en dernier, exprès. Elle doit être
sœur de la source Haze plutôt qu'une fenêtre `Dialog` pour pouvoir flouter la
grille derrière elle, et venir après la barre du haut et le dock pour qu'un modal
couvre le chrome au lieu de le laisser flotter par-dessus.

Elle est **délibérément laissée levée** : le travail qu'elle vient de lancer ne
publie aucun écran à lui avant l'étape du tunnel, donc son propre indicateur est ce
qui couvre l'attente. Elle s'en va avec la bibliothèque quand le flux navigue enfin,
et son état est retenu dans cet écran, si bien que revenir en donne une propre
plutôt que la carte encore ouverte.

La bannière de mise à jour **pousse** la grille vers le bas au lieu de s'asseoir
dessus : couvrir la première rangée ferait payer l'annonce par des jeux devenus
intouchables.

Le jeu en ligne a son propre bouton pour la seule console qui en a à côté d'une
session — l'ad hoc public de la PSP. C'est un second genre de multijoueur, d'où un
bouton à lui plutôt qu'un carrefour de plus avant de créer une session. La PS2 en
avait un aussi, ses serveurs de renaissance, mis de côté le 2026-08-19 (voir
`docs/PS2_ONLINE_MIS_DE_COTE.md`) ; son Local Link est intact et passe toujours par
une session.

## Le maintien de A, et le titre qui s'efface

`combinedClickable` de Compose ne donne l'appui long qu'aux doigts : le A d'une
manette arrive en événement clavier, et un événement clavier n'a pas de durée —
c'est à l'app de le chronométrer. D'où un minuteur armé à l'enfoncement et désarmé
au relâchement, dont la seule garantie à tenir est qu'une pression fasse
**exactement une chose** : ouvrir le menu au maintien, ou lancer au relâchement,
jamais les deux.

L'état du maintien est un état Compose et non un simple champ : la tuile sous le
curseur le lit pour s'enfoncer, donc la grille doit se recomposer quand il change.
Un bouton tenu sans réponse à l'écran se lit comme une pression ratée.

Le menu est composé **à l'intérieur** de la tuile : c'est ce qui donne au `Popup`
les bornes de la tuile comme ancre, sans lire ni transporter de coordonnées à la
main. Et il est **toujours composé, jamais conditionné** : c'est ce qui lui laisse
le temps de se fermer. Il n'ouvre sa fenêtre que s'il a quelque chose à montrer.

Le titre s'**efface** en fin de ligne quand il déborde, au lieu de points de
suspension. Les points coupent net et mangent trois caractères pour dire qu'il
manque quelque chose : sur « The Legend of Zelda: A Link Between Worlds » on
perdait le sous-titre *et* la place de l'annoncer. Le fondu laisse lire tout ce qui
tient et s'estompe, donc le lecteur comprend qu'il y a une suite sans que ça coûte
de la place.

**Deux lignes toujours réservées**, même pour un titre d'un mot : sinon une tuile
au nom court soulèverait toute sa rangée et la grille perdrait son alignement.

## Ce qui est publié au second écran

Un seul endroit, appelé par les trois porteurs de curseur : la grille, le carrousel
et la liste gardent chacun leur index, et dupliquer la correspondance est la façon
dont les deux écrans finiraient par ne plus être d'accord sur ce qui est
sélectionné. Ce sont des alternatives : exactement une est composée à la fois.

En partant, le panneau est **rendu à sa face de repos** plutôt que de laisser le
dernier jeu briller au dos de la machine après que le joueur est allé ailleurs.
L'écran suivant publie son propre état juste après, donc la paire converge sans que
l'un connaisse l'autre.

**La publication attend un dixième de seconde d'immobilité.** Une direction
maintenue parcourt une étagère à quelque dix tuiles par seconde, et chacune était
publiée : le panneau passait son temps à démarrer du travail — une recherche de
jaquette, un listage de dossier — pour un jeu que le curseur avait déjà quitté, et
le résultat visible était une face qui semblait charger plusieurs fois. L'effet est
annulé et relancé à chaque déplacement, donc l'attente n'échoit jamais tant que le
joueur bouge. Un dixième de seconde : sous le seuil où une pression volontaire
paraît retardée, au-dessus du temps de traversée d'une tuile en maintien.

## Les consoles masquées le sont ici, pas dans le scan

Le filtre est appliqué au moment où la grille est construite, pas au scan : le cache
du dépôt est partagé avec le flux de session, qui doit continuer à trouver une ROM
par son identifiant de titre même pour une console masquée. **Masquer une console
est une affirmation sur cet écran, pas sur ce que l'app possède** ; le code d'un ami
ouvre toujours ce qu'il ouvre.

Le dossier ouvert est remis à zéro dès que le mode de classement change : garder un
dossier ouvert en repassant en A-Z laisserait la bibliothèque silencieusement
amputée d'une console, sans rien à l'écran pour l'expliquer.
