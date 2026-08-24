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
