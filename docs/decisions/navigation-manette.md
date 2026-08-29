# Manette : le curseur, l'anneau, et ce qui a été repris

Le récit qui vivait dans `ui/Gamepad.kt`, sorti du code le 2026-08-24
(cf. `docs/STYLE_COMMENTAIRES.md`). Les titres sont des ancres citées depuis le
code : ne pas les renommer à la légère.

Complète la section « Navigation à la manette » du `CLAUDE.md`, qui garde la
règle principale : **une grille paresseuse tient son propre curseur**, la
traversée de focus de Compose ne sait pas viser ce qui n'est pas encore composé.

## Deux métiers séparés exprès

L'appareil visé est une console portable : croix directionnelle, deux sticks,
boutons de façade. Traverser l'écran du pouce pour toucher une tuile est la
mauvaise façon de s'en servir.

Compose déplace déjà le focus sur la croix et traite déjà Entrée et le centre de
la croix comme un clic. Ce qu'il ignore, c'est que `BUTTON_A` veut dire la même
chose : ce code touche appartient à une manette de jeu, et Compose laisse les
manettes à l'application.

D'où deux métiers, séparés à dessein : `gamepadClick` rend une chose pressable à
la manette, `focusRing` rend évident **laquelle**. Un contrôle qui prend le focus
sans le montrer est pire qu'un contrôle inatteignable : le joueur presse A et
quelque chose se produit ailleurs.

`B` est délibérément absent des boutons « valider » : il veut dire retour.

## Le curseur ne s'attarde jamais

L'anneau **ne s'efface pas** en partant. Il s'effaçait sur 70 ms, au motif qu'une
disparition nette scintillerait. Sur une croix maintenue, ce n'est pas ce qui se
passe : le curseur est déjà deux cases plus loin quand la précédente est encore
allumée, et l'œil lit la lueur restante comme une seconde sélection qui traîne
derrière la première. Un curseur est une affirmation sur le **maintenant** ; il
n'a rien à faire là où il n'est plus.

L'arrivée, elle, garde son animation : celle-là, l'œil la suit exprès. Le
ressort par défaut mettait le même temps dans les deux sens, ce qui produisait
exactement les deux sélections simultanées.

Tout ce qui marque la case sélectionnée — l'anneau, sa lueur, la croissance de la
case — doit bouger sur **une seule horloge**, sinon le curseur se décompose en
morceaux qui arrivent séparément.

## Trois choses à la fois, et le souffle qui a été repris

Le curseur est un contour cyan allumé. Le cyan du plateau est dépensé ici et sur
l'action principale, nulle part ailleurs — une couleur, un sens. Il remplace le
vert menthe du monde « verre », qui devait être une troisième couleur justement
parce que le bleu était déjà pris par tous les boutons ; la palette réduite à un
seul accent, le curseur peut simplement l'avoir.

Il lui faut un contour **et** une large lueur colorée, pour que l'œil le trouve à
travers le plateau. À 14 dp sur des plaques claires la couleur se voyait à peine :
le joueur devait chercher où il était, ce que cet anneau existe précisément pour
éviter. Doublée, elle devient une lueur, et le repère se trouve du coin de l'œil.

**Le souffle lent a été essayé puis retiré.** L'intention était juste — un objet
sélectionné sur un menu de console n'est jamais tout à fait immobile — mais
l'élévation d'un `shadow` n'est pas un bouton de luminosité : à chaque valeur
elle recalcule l'ombre de la forme, et une élévation animée sous une surface qui
n'est pas parfaitement opaque se voit **à travers** la surface. La lueur se
glissait à l'intérieur du curseur, dérivait, et laissait un trou mouvant au
milieu de l'élément même qu'elle devait désigner. Un curseur dont l'intérieur
bouge est pire qu'un curseur qui ne respire pas.

## L'anneau entoure, il ne rogne pas

`focusRing` pose son halo avec `Modifier.shadow(elevation, shape)`, et Compose y
fait défaut à **`clip = elevation > 0.dp`**. Le halo étant animé de zéro à sa
pleine valeur, l'anneau se mettait donc à découper le contrôle à sa propre forme
au moment précis où il s'allumait.

Invisible partout où la forme de l'anneau est celle du contrôle — c'est-à-dire
partout sauf un endroit. Sur l'avatar du profil, l'anneau est un cercle et la
pastille crayon est posée au coin d'une boîte carrée, donc **hors** de ce cercle :
elle disparaissait à moitié dès que le curseur arrivait dessus, et c'est
exactement l'élément qui doit ressortir.

`clip = false`, partout et sans condition. Un curseur signale, il ne redécoupe
pas ce qu'il signale — et un contrôle qui déborde de sa forme de focus est une
composition légitime, pas une erreur à rattraper au ciseau.

### Ce qui doit ressortir de l'anneau se déclare après lui

`clip = false` a rendu la pastille entière, mais pas visible : `Modifier.border`
dessine **par-dessus** le contenu du nœud qui le porte, donc le trait de l'anneau
lui passait toujours au travers. Un enfant de la boîte annelée est sous
l'anneau, quoi qu'on fasse.

Sur l'avatar du profil, l'anneau est donc posé sur la boîte de la photo seule, et
la pastille crayon est déclarée **après** — sœur, pas fille — donc dessinée
au-dessus. Le focus reste sur la boîte extérieure, qui contient les deux : un seul
arrêt de curseur, et le doigt atteint aussi la pastille. `controlRing` y est
gardé mais silencieux (`enabled = false`) pour son seul `bringIntoView` ; c'est
`focusRing` qui trace, en dessous.

Règle générale : un élément qui doit franchir le contour du focus n'est pas dedans.

## L'anneau garde le même poids partout

`controlRing` est exactement `focusRing`, celui des tuiles, aux mêmes bornes et
avec la forme propre du contrôle — pas une forme reconstruite depuis un rayon,
pas un cadre plus grand, aucune réserve entre les deux.

Les tuiles ont toujours rendu correctement. Tout le reste de ce qui a été tenté —
réserver quelques dp et dessiner dedans, recalculer un rayon extérieur, repeindre
la lueur à la main — revenait à réinventer ce qui marchait déjà, et chaque
variante ratait le bord à un endroit différent.

Le trait et la lueur ont été **réduits un temps**, au motif qu'un bouton est plus
petit qu'une tuile. C'était une erreur : ce dessin, à ces valeurs, est ce qui a
été approuvé sur les tuiles et sur le bouton de retour, et l'affaiblir donnait un
curseur terne qu'il faut chercher. Un anneau doit avoir le même poids partout,
sinon il cesse de se lire comme le même objet.

La nuance qui reste vraie : les valeurs par défaut sont celles des tuiles, larges
de 150 dp — trait de 4, lueur de 28. Appliquées telles quelles à un bouton de
46 dp, elles débordent visiblement et se lisent comme un contour mal posé, ce
qu'on a vu sur le bouton de retour de l'en-tête. Les petits contrôles passent donc
des valeurs réduites : c'est le même anneau, à leur taille.

## L'anneau lit le focus lui-même, et l'ordre compte

`onFocusEvent` voit le focus des nœuds **en dessous** de lui dans la chaîne, donc
celui du contrôle : plus besoin de faire descendre un `MutableInteractionSource`
jusqu'à un `Button` Material qui n'en expose pas. C'est ce qui permet d'équiper un
écran sans le réécrire.

**À placer avant le `clickable` ou le `focusable`, jamais après.** Placé après, il
ne voit rien et reste éteint alors que le curseur est bel et bien là.

## Rien ne doit s'arrêter sous l'en-tête

Le contenu défile **sous** l'en-tête flottant, qui n'est pas une barre mais une
couche posée par-dessus. Pour Compose, un contrôle glissé dessous est « visible » :
le défilement s'arrêtait donc dès qu'il arrivait à son niveau, et remonter au
premier élément d'une page ne ramenait jamais le haut de cette page.

En demandant la bande entière, la requête dépasse le début du contenu et le
défilement se pose à zéro. D'où la marge haute au moins égale à la hauteur de
l'en-tête, publiée en `CompositionLocal` plutôt qu'en paramètre : tous les
contrôles en auraient besoin, et aucun n'a à connaître cette valeur.

## Un rayon nommé une fois

Le rayon des grands boutons d'action est nommé à part et partagé : deviné à
chaque appel, il finissait par ne plus coïncider avec le bouton qu'il entoure.
L'anneau a besoin du **nombre**, pas de la forme — il trace son propre contour,
plus large, et doit pouvoir y ajouter l'écart qui les sépare.

## Un seul rectangle arrondi par chemin, jamais deux

La bande de l'anneau a d'abord été un anneau **rempli** : deux rectangles
arrondis concentriques en `EVEN_ODD`, le grand moins le petit. La forme était
juste et le coût invisible à la lecture.

Skia ne reconnaît un chemin comme rectangle arrondi que s'il n'en contient
**qu'un**, et rastérise tout le reste sur le processeur, dans un masque qu'il
téléverse ensuite en texture. Or l'épaisseur s'anime à l'arrivée du curseur :
chaque image donnait une forme inédite, donc un masque neuf. Mesuré sur la Thor
le 2026-08-29, en descendant vite dans la grille : le cache de masques logiciels
montait à **27 Mo en 372 entrées** et continuait de grimper, pour un curseur.

La même surface se trace en un trait — la ligne médiane de la bande, épaisse de
`band`. Couverture identique au pixel, les bords interne et externe tombent
exactement où ils tombaient, et le GPU la trace sans repasser par le processeur.
Après correction : **0,4 Mo en 5 entrées**.

C'est la même leçon que le halo, passé du flou gaussien aux traits empilés,
appliquée cette fois à la bande elle-même. **Ne pas réintroduire un chemin à
plusieurs sous-formes dans ce fichier.**

## `Modifier.alpha` rogne, et c'est ce qui rendait le curseur carré

`Modifier.alpha()` n'est pas qu'une opacité : sous 1, il pose `clip = true`, un
découpage **rectangulaire** aux bornes de l'élément. L'anneau entoure la tuile
par l'extérieur ; pendant l'arrivée des tuiles, il se faisait donc trancher à
l'équerre, et le curseur paraissait carré.

Ça ne se voyait qu'au retour vers la bibliothèque, la seule occasion où une tuile
**déjà visée** rejoue son arrivée. Et l'écart dure le dernier centième du
ressort : l'anneau s'allume dès que l'arrivée dépasse 0,99, le découpage ne lâche
qu'à 1,00 pile. Deux seuils différents pour deux effets qu'on croyait liés.

Remède : `graphicsLayer { alpha = … }`, dont `clip` vaut faux par défaut. Ce qui
doit être découpé à la forme de la tuile l'est plus bas, par le `clip(TileShape)`
qui suit.

Deux autres endroits portent la même construction et sont laissés tels quels : le
carrousel, où l'opacité ne descend que sur une carte non visée donc sans curseur,
et le dialogue de lancement, où ce découpage est aujourd'hui la seule chose qui
retient le contenu pendant l'apparition.

## Le contrôle visé passe devant ses voisins

L'anneau déborde de ses bornes de mise en page — c'est sa définition. Entre
frères, c'est le dernier dessiné qui gagne : une rangée de tuiles recouvrait la
moitié droite de l'anneau de chacune, et la dernière était la seule à montrer le
sien en entier. Vu sur la grille des consoles, dans les réglages comme dans
l'onboarding.

La tuile de bibliothèque avait déjà son `zIndex` posé à la main pour cette raison
exacte. La règle étant la même partout, elle vit désormais dans `controlRing`.

Elle ne vaut qu'**entre frères** : une grille sur plusieurs rangs doit encore
lever le *rang* qui porte le curseur, ce que fait `ConsoleGrid`. Les deux
ensemble dégagent l'anneau dans les quatre directions.

## Le curseur ne peut pas passer devant la barre du haut

Essayé le 2026-08-29, en deux variantes, et abandonné les deux fois.

La barre du haut de la bibliothèque flotte au-dessus de la grille : elle passe
donc devant tout ce que la grille dessine, et l'anneau appartient à la tuile,
donc à la grille. Un `zIndex` n'y peut rien — en Compose, un enfant ne passe
jamais devant la sœur de son parent.

La tentative : sortir l'anneau de la tuile, publier le rectangle de celle-ci en
coordonnées de la racine, et le redessiner dans une couche posée après la barre.
Il passait bien devant — et **traînait d'une image derrière sa tuile** dès qu'on
défilait. Deux variantes, deux fois le même retard : replacement par
recomposition d'abord, puis lecture différée en phase de mise en page. Une couche
qui court après des coordonnées les apprend toujours *après* la mise en page qui
les a produites.

L'anneau se dessine avec sa tuile, dans la même passe : c'est ce qui le rend
exact, et c'est ce qui l'enferme dans la grille. **L'exactitude du suivi et le
passage devant la barre s'excluent.** Le passage derrière l'étagère se règle donc
par la place — voir `bibliotheque.md` § L'air sous la barre est celui du curseur.

## Les quatre couches du curseur néon

Ce que l'ancien anneau faisait en deux traits — un `border` de 4 dp plus une
`shadow` détournée en halo — se fait ici en quatre couches, et c'est l'empilement
qui produit l'effet, pas la couleur :

1. **La lueur** : trois traits concentriques du plus large et pâle au plus fin et
   dense, posés sur la ligne médiane de la bande. C'est le profil d'un flou,
   échantillonné en trois points. Ça **a été** un vrai `BlurMaskFilter`, et c'est
   ce qui le rendait juste ; il a été retiré parce qu'il n'a pas d'équivalent
   GPU, donc Android dessinait le chemin sur le processeur à chaque image, en
   permanence, puisque le curseur est toujours à l'écran.
2. **La bande** : un trait à la ligne médiane, épais de `band`, portant le
   dégradé qui coule. Ça a été un anneau *rempli* en `EVEN_ODD` — voir § Un seul
   rectangle arrondi par chemin.
3. **Le liseré extérieur** : blanc, en dégradé vertical de 50 % à 30 %.
4. **Le liseré intérieur** : blanc plat à 40 %.

Les deux liserés sont ce qui fait lire la bande comme un objet de verre posé sur
l'écran plutôt que comme un aplat. Ils sont fixes, jamais teintés : leur rôle est
de capter la lumière, pas de dire une couleur.

Toutes les mesures sont des fractions de la largeur de bande, elle-même une
fraction de la taille du contrôle : le curseur grossit avec ce qu'il entoure, au
lieu de garder une épaisseur qui écrase les petits contrôles et disparaît sur les
grands.
