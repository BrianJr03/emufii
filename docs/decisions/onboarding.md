# L'onboarding : ce que le premier lancement demande, et dans quel ordre

Refonte du 2026-08-29. Les titres sont des ancres citées depuis le code : ne pas
les renommer à la légère.

## Le parcours n'a pas de longueur fixe

Il avait sept pages, les mêmes pour tout le monde : quelqu'un qui ne joue qu'à la
DS traversait une page sur PPSSPP et une sur ARMSX2 qui ne le concernaient pas,
et personne ne lit une consigne qui ne s'adresse pas à lui.

Les pages d'émulateur sont donc tirées de ce que le joueur vient de répondre à la
page des consoles : masquer la PS2 retire la page de la PS2, et le récapitulatif
final ne lui invente pas une dette là-dessus. C'est aussi ce qui rend la page des
consoles réellement utile plutôt que décorative — elle taille la suite.

Le parcours est tenu par sa **valeur** et non par un index : masquer une console
retire une page, et un index désignerait alors la page suivante.

La page du remplissage automatique ne paraît que pour les consoles dont le
multijoueur passe par le pilotage de l'émulateur — 3DS, Switch, GameCube, Wii,
PS2. La PSP passe par le tunnel et la DS par une redirection DNS : proposer un
service d'accessibilité à quelqu'un qui ne joue qu'à ces deux-là, c'est demander
beaucoup pour rien.

## Deux colonnes, et elles ne disent pas la même chose

À gauche le *pourquoi* : une marque, un titre, une phrase, sans plaque — elle
parle par-dessus le plateau, comme un titre d'écran, ce qui laisse la seule
plaque de la page à ce qui se fait. À droite le *quoi faire* : les étapes
numérotées, l'état, le bouton qui travaille.

Le parcours précédent empilait tout au centre d'une carte étroite, sur un écran
de 833 dp de large : deux tiers de la Thor perdus, et le bouton qui finissait
hors champ dès qu'une page avait trois lignes de trop. En portrait, les deux
colonnes redeviennent une pile, dans le même ordre : on lit toujours pourquoi
avant de faire.

## La page des consoles prend toute la largeur

Un troisième mode de mise en page, et une seule page l'utilise. `ConsoleGrid` est
dessinée pour recevoir la largeur entière ; servie dans une colonne de 58 %, elle
retombait à trois colonnes, donc trois rangs, donc une page qu'on parcourait au
défilement pour répondre à une question qui tient en un coup d'œil.

Le *pourquoi* devient alors un bandeau — la marque et le titre sur une ligne, la
phrase dessous — au lieu d'une colonne. Il perd de la hauteur, ce qui est
exactement ce qu'on lui demande là.

Ça ne suffisait pas : même à pleine largeur, sept tuiles pleines font deux rangs,
soit 300 dp de carte dans un écran qui en laisse 322 à toute la page. D'où la
version courte de la tuile — voir `reglages-ecran.md` § La tuile de console a une
version courte. Vérifié sur l'appareil : les sept étiquettes sont à la même
hauteur, de x=221 à x=1638, et il reste 240 px avant les boutons.

## Les rituels d'émulateur sont les blocs des réglages, pas des copies

`PpssppBlock`, `Ps2Block` et `AutofillBlock` viennent de `settings/EmulatorsPage`
tels quels : mêmes étapes, même pastille d'état, mêmes sélecteurs de dossier,
mêmes messages d'erreur. Les redessiner pour l'occasion aurait donné deux
versions d'une même procédure — celle qu'on lit en s'installant et celle qu'on
relit quand ça ne marche pas.

Conséquence voulue : tout se fait **depuis** l'onboarding, y compris importer le
profil réseau PS2 dans la carte mémoire. Les trois blocs sont donc `internal` et
non privés.

## Tout reste passable, sauf le pseudo

Le dossier se choisit plus tard, la permission de notification se refuse de plein
droit, les rituels d'émulateur se font le jour où l'on veut jouer à cette
console-là. Un onboarding qui retient quelqu'un jusqu'à ce qu'il dise oui est un
piège, et celui-ci doit survivre au non.

Le pseudo est la seule exception, et pour une raison mécanique : il part tel quel
dans le formulaire de l'émulateur, qui refuse les trop courts, et un refus
là-bas ressort en connexion qui n'arrive jamais. Le champ arrive pré-rempli d'une
valeur valide, donc le blocage ne concerne que quelqu'un qui l'a activement vidé.

Le retour arrière se fait au bouton système et à la touche B, jamais par un
troisième contrôle en bas de page : trois choses à lire là où il y a une décision
à prendre, c'est trop.

## Le récapitulatif nomme ce qui a été sauté

Une liste de félicitations n'apprend rien. Le relevé final dit ce qui manque et
où le reprendre, une ligne par chose demandée, avec sa pastille. Les lignes qui
ne concernent pas ce joueur ne paraissent pas — même règle que le parcours : un
relevé qui annoncerait « PS2 : à faire » à quelqu'un qui a masqué la PS2 lui
inventerait une dette.

## Où l'on en est, et de quoi il s'agit

Les points seuls disaient la longueur du parcours et rien d'autre. Comme celui-ci
n'a plus de longueur fixe, ils disaient même une longueur qui changeait sous les
yeux du joueur à la page des consoles. Le nom de l'étape répond à la question
qu'ils posaient sans y répondre : où suis-je.

## Le français se relit à voix haute, pas mot à mot

Deux passes de correction ont été nécessaires après la première rédaction, et le
défaut était le même à chaque fois : des phrases *justes* mais construites, pas
des phrases qu'on dit. « Emufii monte le multijoueur à distance entre
émulateurs », « les autres quittent ta bibliothèque, et cette installation te
fera grâce des réglages qui les concernent », « le scan démarre en fond »,
« un profil pointant vers le tunnel ».

La règle qui en sort : **l'anglais se réaligne sur le sens révisé, jamais sur la
phrase française**, sinon il hérite de la même raideur à l'envers. Et une
étiquette de pastille, un titre de page ou une étape numérotée se relisent à voix
haute avant d'être écrites.
