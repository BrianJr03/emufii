# La coquille des écrans : chrome flottant, voile, et pastilles

Le récit qui vivait dans `ui/components/EmufiiScaffold.kt`, sorti du code le
2026-08-24 (cf. `docs/STYLE_COMMENTAIRES.md`). Titres = ancres citées depuis le
code.

## Deux métiers : les encoches, et la constance

D'abord les encoches système : le contenu reçoit une marge haute qui dégage déjà
la barre d'état, pour que rien ne finisse sous l'horloge — le défaut que cette
coquille a été écrite pour corriger. Ensuite la constance : le même fond, le même
en-tête flottant, sur chaque écran.

## L'en-tête flotte, et ce que ça coûte

Il flotte au-dessus du contenu au lieu d'être une barre avec un fond. **Une barre
haute délimitée a été essayée et rejetée sur ce projet** : la pièce doit ressembler
à un écran d'accueil de 3DS, où rien n'est mis en boîte.

Flotter a un coût que la première version ne payait pas : la marge haute dégage
l'en-tête **au repos**, mais un écran qui défile envoie son contenu droit sous le
titre, et les deux se dessinent l'un sur l'autre.

D'où **une seconde copie du fond d'écran, dessinée par-dessus le contenu et
effacée partout sauf sur la bande qu'occupe le chrome flottant.** Parce que c'est
le même fond à la même taille, les pixels coïncident exactement avec ceux du
dessous : le contenu se dissout dans le décor au lieu de rencontrer une couture ou
une boîte. Le même dispositif ancré au bord bas sert pour le dock.

**À placer *à l'intérieur* de la source Haze là où il y en a une** : le dock
échantillonne le décor pour le flouter, et échantillonner la grille non voilée
flouterait des tuiles que le voile a déjà cachées.

Le voile et la marge de fondu **n'existent que pour du contenu qui monte sous
l'en-tête**. Un écran qui ne défile pas n'a rien à dissoudre, et les 32 dp réservés
au fondu deviennent une bande vide : sur les 468 dp de la Thor, c'est 7 % de la
hauteur payée pour rien.

La distance de fondu est assez longue pour se lire comme une dissolution plutôt
qu'un bord net, assez courte pour ne pas assombrir la première carte d'un écran au
repos.

## L'en-tête est déclaré avant le contenu, et dessiné par-dessus

La traversée de Compose suit **l'ordre de déclaration**. Avec le contenu déclaré en
premier, « bas » depuis le bouton de retour n'avait rien après lui. L'ordre est donc
remis à l'endroit, et le dessin ne change pas : l'en-tête flotte au-dessus du
contenu qui défile dessous, par son `zIndex`.

**Cela ne suffit pourtant pas à faire descendre le curseur dans la page**, et ça
vaut d'être su avant d'y revenir : trois tentatives ont échoué à franchir la
frontière entre ces deux couches d'un même `Box` —

1. `focusProperties { down = ... }` sur un `focusGroup` ;
2. une demande de focus explicite, qui renvoie `Success(true)` **en donnant le
   focus au groupe lui-même**, pas à l'un de ses enfants ;
3. un `moveFocus(Down)` depuis l'en-tête.

Chaque fois, `uiautomator dump` montrait le focus toujours dans l'en-tête.

**Ce qui marche dans ce dépôt** est la méthode de la bibliothèque : nommer la
destination avec un `FocusRequester` posé sur un contrôle **réellement
focalisable**, jamais sur un conteneur. C'est aussi pourquoi les deux destinations
manette d'un écran voyagent dans un `CompositionLocal` plutôt que dans la signature
du contenu : chaque écran n'a qu'un contrôle à nommer, et le hisser en paramètre
aurait obligé à toucher chaque site d'appel pour une information qu'un seul endroit
utilise.

La touche n'est **consommée que si la destination existe** : un écran dont le
premier contrôle est conditionnel peut n'en avoir aucun, et l'avaler là
emprisonnerait le curseur, alors que la rendre laisse sa chance à la traversée
ordinaire.

## Le curseur arrive avec l'écran

Posé le 2026-08-28, sur une gêne signalée par l'utilisateur : « 90 % du temps le
sélecteur n'est nulle part, donc on doit appuyer au moins une fois pour qu'il
apparaisse ». Une pression de direction sur deux était dépensée à ne rien faire.

`padEntry()` nommait la destination — c'est là que le curseur descend depuis
l'en-tête, et c'est de là qu'il y remonte — mais **personne ne la demandait à
l'ouverture**. `EmufiiScaffold` le fait maintenant, et ça couvre tous les écrans
sauf la bibliothèque, qui n'est pas scaffoldée et tient son propre curseur.

Deux pièges, tous deux découverts en le mesurant sur la Thor, et l'ordre compte :

- **Il faut demander le mode clavier avant de demander le focus.** Compose tient
  deux modes de saisie, et en `InputMode.Touch` aucun élément ne retient le
  focus : `requestFocus` y est un appel qui ne lève rien et ne fait rien. Un
  écran ouvert au doigt laissait la machine en mode tactile et toutes les
  demandes tombaient dans le vide — pendant que le même appel, lancé depuis
  l'en-tête en réponse à une touche, marchait, parce qu'une touche fait passer
  Compose en mode clavier toute seule. C'est aussi la réponse à « le curseur
  n'est nulle part » quand on entre dans une page en la touchant.
- **Il faut redemander sur plusieurs images, sans regarder si ça a marché.** Un
  `LaunchedEffect` part dès la fin de la composition, quand le nœud porteur de
  `padEntry` est composé mais pas encore *placé* ; la demande n'y lève rien et
  ne fait rien non plus. Une première version s'est arrêtée sur ce silence en le
  prenant pour un succès, et le curseur restait introuvable — exactement le
  défaut qu'elle devait corriger. Six images, soit une centaine de
  millisecondes : redemander sur un nœud qui a déjà le focus ne coûte rien, donc
  rien ne sert de tester, et la fenêtre est trop courte pour arracher le curseur
  à quelqu'un qui aurait déjà appuyé.

La boucle est bornée parce qu'un écran a le droit de n'avoir aucun premier
contrôle, et qu'une boucle qui l'attendrait ne s'arrêterait jamais. Le paramètre
`autoFocus` existe pour qu'un écran qui placerait son curseur lui-même puisse
refuser sans qu'on retire l'arrivée du curseur à tous les autres ; personne ne
s'en sert aujourd'hui.

**Aucun `padEntry` ne tombe sur un champ de saisie** — vérifié sur les sept
écrans qui en posent un, ce sont tous des boutons ou des zones cliquables. C'est
la condition pour que l'arrivée du curseur n'ouvre pas un clavier au visage du
joueur, et elle rejoint la règle de « Un champ de texte ne doit pas être un arrêt
du curseur ».

## L'anneau entoure la pastille, il ne mord pas dedans

Posé sur la pastille elle-même, son trait mordait dans le fond teinté et écrasait
le libellé : ça se lisait comme une bordure mal dimensionnée sur le bouton plutôt
que comme une sélection posée par-dessus. Le bouton rond de l'en-tête n'a jamais eu
ce défaut parce que son halo déborde de son fond blanc ; ici, c'est l'écart qui
joue ce rôle.

**L'écart existe en permanence, focalisé ou non** : le faire apparaître à la
sélection décalerait le bouton d'autant, et une rangée de pastilles sauterait à
chaque passage du curseur. Et il reprend **la forme déclarée juste au-dessus** : le
curseur trace son contour, il ne le déduit pas.

## La pastille fait la taille de sa cible tactile

`Surface(onClick)` réserve d'office les 48 dp que Material impose à une cible
tactile, puis dessine son fond à la taille du libellé, centré dedans. Le cadre — et
donc l'anneau — suivait **la réservation**, pas la pastille : cinq pixels de blanc
entre le trait et le bord, mesurés en haut comme en bas.

Donner cette hauteur à la pastille fait coïncider le dessin et la cible : l'anneau
serre juste, et le bouton devient plus facile à toucher au doigt par la même
occasion.

## Le libellé est centré dans les deux sens, et les deux sont nécessaires

`textAlign` seul gère l'horizontale. Il ne gère pas la verticale : quand la
pastille est étirée pour s'aligner sur une voisine à deux lignes, un libellé d'une
ligne reste collé en haut de la hauteur qu'on vient de lui donner. La `Box` est ce
qui le remet au milieu — `Surface` propage ses contraintes minimales à son contenu,
donc la `Box` remplit bien toute la pastille, étirée ou non.

**Aucun `fillMaxWidth` ici.** Il y en a eu un, et il cassait toute rangée de deux
pastilles sans poids : la première prenait toute la largeur, la seconde tombait à
zéro et repliait son libellé sur autant de lignes qu'il a de lettres — la carte
Bibliothèque des réglages mesurait 390 dp de haut pour trois lignes de texte.

Sans lui, la `Box` s'ajuste à son contenu ; et quand l'appelant étire la pastille
(un poids, un `fillMaxWidth`), `Surface` propage ses contraintes minimales et la
`Box` remplit quand même. Le centrage tient dans les deux cas, ce qui est tout ce
qu'on lui a jamais demandé.

Pour la même raison, « cette pastille est seule et prend la largeur de sa carte »
est **explicite** et non déduit d'un `fillMaxWidth` posé par l'appelant : depuis que
l'anneau entoure la pastille, c'est le **cadre** qui reçoit le modificateur de
l'appelant, et laisser la pastille s'étirer toute seule rejouerait le défaut
ci-dessus.

## Le titre de groupe parle la voix de l'app

C'était une micro-étiquette en capitales espacées — le « sourcil » que livre tout
tableau de bord, et le seul procédé que la charte bannit franchement : une ligne en
petites capitales au-dessus d'un titre est un costume d'importance, et elle fait
lire l'app comme un écran de réglages venu d'ailleurs.

La casse de phrase au poids du corps dit la même chose, dans la voix que parle le
reste de l'app, et cesse de concurrencer le contenu qu'elle introduit.

## Le bouton rond est un disque moulé

Rond, moulé, flottant sur le plateau : le bouton qu'une console met dans le coin de
son écran — un disque de plastique à l'arête supérieure éclairée, avec un glyphe
**dessiné** à l'intérieur, jamais un caractère tapé.

---

# Saisir du texte à la manette

Sorti de `ui/components/PadTextField.kt` et `ui/screens/JoinScreen.kt`.

## Un champ de texte ne doit pas être un arrêt du curseur

Le défaut est celui de Compose, pas le nôtre : **un `OutlinedTextField` qui prend
le focus ouvre le clavier logiciel.** À la manette, où le focus se déplace en
traversant l'écran, **le simple fait de *passer* sur un champ** suffisait à faire
surgir le clavier, couvrir la page et capturer les directions : on ne traversait
plus un écran de réglages, on y tombait.

Ici **le champ n'est pas une étape de la traversée : son cadre l'est.** Le cadre
s'annonce avec l'anneau habituel, et A — ou un doigt — fait entrer dans le champ.
B en ressort et rend le focus au cadre, pour qu'on reprenne là où on était plutôt
que de retomber au début de l'écran.

`canFocus` est refusé au champ hors édition, et **c'est ça qui le tient vraiment
hors de la traversée** : le rendre simplement non cliquable l'aurait laissé
attraper le focus depuis une direction.

## C'est la disparition du clavier qui termine l'édition, pas la touche

**Le clavier avale le premier B et le gestionnaire de retour ne le voit jamais** —
mesuré sur la Thor : une pression fermait le clavier en laissant le champ ouvert et
sans anneau, et il en fallait une seconde pour sortir.

C'est donc la disparition du clavier qui met fin à l'édition. Le gestionnaire de
retour reste pour le cas où il n'y a pas de clavier (une manette avec un clavier
physique, un IME masqué). Et un drapeau « déjà ouvert » est nécessaire parce que
**le clavier n'est pas encore visible à l'instant où l'on entre dans le champ** :
sans lui, l'édition se refermerait aussitôt ouverte.

## L'anneau *est* le contour du champ, et c'est le seul arrangement qui tienne

Deux tentatives ont échoué sur la Thor, et c'est la mesure qui a tranché.

Dessiner l'anneau sur les mêmes bornes posait son trait **par-dessus** le contour
propre du champ : deux lignes légèrement décalées. Rentrer le champ et élargir le
rayon de l'anneau pour les rendre concentriques n'a pas marché non plus : **mesuré
au grossissement 4, l'écart était de 4 dp sur les côtés et de 11 dp en haut**, parce
qu'`OutlinedTextField` ne remplit pas le cadre qu'on lui donne. **Aucun rayon ne
rend deux courbes parallèles quand l'espace entre elles n'est pas régulier au
départ.**

Donc : la bordure du champ devient transparente sous le curseur et l'anneau prend
sa place, sur les bornes et la forme exactes du champ. **Un seul contour à la
fois** — il n'y a plus rien à aligner.

Et **avant le `focusable`, l'ordre est tout** : l'anneau lit le focus par
`onFocusEvent`, qui ne voit que les nœuds en dessous de lui dans la chaîne. Placé
après, il ne voyait jamais le focus du cadre et restait éteint alors que le curseur
était bien là — le champ défilant vers le centre de l'écran sans rien afficher, ce
qui se lit comme un curseur disparu.

### L'étiquette se pose au-dessus du cadre, jamais dedans

`OutlinedTextField` réserve en haut la place où son étiquette ira flotter, même
quand elle est encore au repos : le texte s'assied nettement sous le milieu,
beaucoup d'air au-dessus, peu en dessous. Dans un cadre dont l'anneau *est* le
contour, cette asymétrie se lit comme un anneau mal dimensionné — c'est ce qui a
été signalé sur le pseudo du profil.

La réserve ne servait d'ailleurs à rien ici : une étiquette qui flotte se pose
dans l'encoche du contour de Material, contour que ce champ efface au profit de
l'anneau. Elle serait donc allée flotter **sur l'anneau lui-même** dès le premier
caractère tapé.

`PadTextField` rend donc l'étiquette lui-même, au-dessus du cadre, et passe
`label = null` à Material. Le texte retrouve son centre, et l'étiquette reste
lisible en permanence — y compris une fois le champ rempli, où la version
flottante se serait perdue dans le trait.

### Les quatre contours s'effacent, pas trois

Le champ éteint son propre contour quand le cadre porte le curseur, pour qu'il
n'y en ait jamais deux à la fois. Trois couleurs le faisaient — `unfocused`,
`disabled`, `focused` — et la quatrième manquait : **`errorBorderColor`**, que
Material fait passer devant les autres dès que `isError` est vrai.

Un champ en erreur gardait donc son trait rouge sous l'anneau : deux contours de
tailles différentes l'un dans l'autre, ce qui se lit comme un anneau mal
dimensionné. Ça se voyait à chaque ouverture du profil, où le pseudo est vide
donc en erreur dès l'arrivée — et l'arrivée automatique du curseur l'a rendu
visible en permanence.

`framed` est faux pendant l'édition, le curseur étant alors *dans* le champ et
non sur son cadre : le rouge revient donc exactement quand l'anneau s'éteint,
règle que les trois autres suivaient déjà.

## Le doigt n'atteignait pas le cadre

Signalé sur l'écran d'accueil, vrai partout où ce champ sert.

La détection était sur le cadre, **sous** le champ. Mais **Compose teste les enfants
d'abord**, et `BasicTextField` installe son propre gestionnaire de pointeur pour
placer le curseur d'insertion : il consommait la tape, puis demandait un focus que
`canFocus = false` refusait. Le geste s'évanouissait donc entre les deux, sans rien
bouger à l'écran.

La surface de détection est donc **dessinée après le champ, donc touchée avant lui**,
et elle n'existe qu'hors édition : une fois dedans, le champ doit récupérer les tapes
pour placer son curseur.

## Six cases plutôt qu'un champ

L'écran de saisie de code était un `OutlinedTextField` pleine largeur avec son
étiquette et son texte d'aide, centré dans une colonne — **un formulaire là où il n'y
a qu'une chose à taper**, et dont le champ occupait les 784 dp de l'écran pour six
caractères.

Six cases à la place. **On sait d'avance combien il en faut, autant le montrer** : la
progression se voit sans lire, la case courante porte l'accent, et le code s'affiche
à la taille où on le lit à bout de bras. Le champ de saisie existe toujours,
invisible, sous les cases — c'est lui qui apporte le clavier, la sélection et le
collage sans qu'on ait à les réécrire.

Chaque case est un **creux** plutôt qu'une plaque : un code se tape *dans* quelque
chose. Celle qui est allumée porte l'anneau du curseur, le même objet que les tuiles,
pour que « où suis-je » ait une seule réponse partout dans l'app.

**Pas d'autocorrection**, et ce n'est pas une affaire d'orthographe : c'est ce qui
empêche le clavier d'ouvrir une **région de composition** sur le champ. Le curseur et
le texte du champ étant tous deux transparents, le bloc pâle assis dans le creux
allumé était le surlignage de composition du clavier, dessiné sur un code qui n'a
rien à corriger.

## La touche du clavier ferme le clavier, et s'arrête là

`ImeAction` ne fait que **dessiner** la touche ; sans action pour y répondre, la
presser ne faisait rien du tout — l'IME restait par-dessus l'écran et le bouton
retour était la seule sortie, sur un écran dont tout le travail est de prendre six
caractères.

**Ce qu'elle ne doit surtout pas faire, c'est démarrer la session** : l'écran a déjà
un bouton pour ça, et une touche de clavier qui lance depuis le dernier caractère
retire la décision des mains du joueur, sans qu'il puisse relire le code.

Fermer compte aussi pour le bouton lui-même : le code peut être complet alors que
l'IME est encore levé, et une session démarrerait alors **sous un clavier que
personne n'a fermé**.

## Pas de clavier automatique, et le bloc est centré sur l'écran

En paysage sur cette machine, **l'IME s'ouvre en plein écran (mode extract) et couvre
tout** : on arrivait sur un éditeur de texte nu, sans avoir jamais vu les six cases ni
le nom du jeu. Le clavier vient quand on touche les cases, c'est-à-dire une fois
qu'on a décidé de taper. Le plein écran lui-même ne nous appartient pas — l'IME en
décide sur un écran court — mais le subir sans avoir vu l'écran, si.

Le bloc est centré **sur l'écran, pas sous l'en-tête** : réserver la marge haute le
centrait dans ce qui restait sous le titre, soit 90 px trop bas. Rien ici n'atteint
l'en-tête, le bloc faisant 212 dp des 468 de l'appareil, donc il n'y a pas de place à
lui réserver.

## Le clavier de l'app est une dalle gravée, pas une planche à boutons

L'IME du système est fait pour un téléphone tenu droit : sur un portable en
paysage, son mode plein écran prend toute la dalle, et la bibliothèque qu'on
cherche disparaît exactement au moment où il faut la voir. Celui-ci ne monte
jamais au-delà de la moitié de l'écran.

Chaque touche a d'abord été un creux à elle : son contour, son coin arrondi,
sept points d'écart avec sa voisine. Trente-huit objets posés côte à côte, donc
trente-huit contours à suivre pour l'œil, et un objet ne se distinguait du
suivant que par la rainure qui l'en séparait — la même rainure partout. Le
panneau se lisait comme un tas.

Il n'y a plus qu'une pièce. Les touches sont des cases découpées **dedans**,
séparées par une gravure d'un point — un trait sombre puis un trait clair,
exactement ce que le plastique moulé fait déjà partout ailleurs. Rien ne flotte,
rien n'a de coin propre, et le contour du panneau est le seul contour de
l'ensemble. C'est la sérigraphie d'une coque de console, qui est l'objet que ce
clavier imite depuis le début.

Ce qui allume une case, c'est l'**état**, plus jamais la forme : le curseur posé
dessus, le doigt qui appuie, ou la majuscule verrouillée. Une case au repos n'a
aucun dessin à elle, donc une case allumée est la seule chose que l'œil trouve.

La majuscule verrouille comme un caps lock au lieu de ne tirer qu'un coup : une
majuscule qui se défait après une lettre est un comportement que personne ne peut
prévoir sans le regarder. La recherche ignore la casse de toute façon — la touche
sert à ce que le joueur se regarde taper, pas à ce qu'il trouve.

## La dalle tient son propre curseur

Première tentative : rendre chaque touche focalisable et laisser la traversée
bidimensionnelle faire le reste. Elle ne le fait pas. Les rangées sont des `Row`
distinctes, et un `focusGroup` n'y a rien changé — vérifié deux fois sur la
Thor : « A » puis haut sortait du clavier au lieu de monter sur « Q ». Le
déplacement échouait, la touche remontait non consommée, et la sortie de secours
prévue pour la première rangée s'appliquait à toutes.

C'est exactement la leçon de la bibliothèque, écrite noir sur blanc dans
`CLAUDE.md` : sur une grille, on ne confie pas la navigation à la traversée de
focus. Un seul nœud focalisable — la dalle — un index (rangée, colonne) qu'elle
calcule elle-même, et des touches à qui l'on **dit** si elles portent le curseur.

Trois conséquences, toutes voulues :

- **Les touches ne sont plus focalisables.** Trente-huit arrêts invisibles en
  moins, et plus aucun moyen pour le curseur de se perdre entre deux.
- **Les bords rendent la main.** Haut depuis la première rangée, bas depuis la
  dernière : la touche n'est pas consommée et l'écran qui héberge la dalle en
  fait ce qu'il veut. Une dalle qui avalerait tout serait un piège.
- **La colonne se souvient.** En changeant de rangée, on garde l'index de
  colonne, ramené dans les bornes de la rangée d'arrivée ; sans ça, passer de la
  rangée des dix lettres à celle des quatre touches de service ramenait le
  curseur au bord à chaque aller-retour.

## Une rangée dépliée est faite de trois choses, et de rien d'autre

Fermée, la liste des réglages est une pile de plaques moulées et se lit à bout de
bras. Ouverte, chaque section avait pris ses habitudes : un paragraphe, puis deux
boutons de poids égal, puis trois ou quatre phrases en quatre couleurs
différentes disant ce qui s'était passé. Chaque ligne était défendable et le
résultat était un mur — le profil PS2 finissait avec onze textes empilés, le plus
important en dernier.

1. **La note** — au plus un paragraphe, et seulement tant qu'elle apprend encore
   quelque chose. Une fois la chose faite, l'explication cède la place à l'état.
2. **Les actions** — la première remplie, les autres en fantômes. Deux pilules
   côte à côte à poids égal disaient « ce sont deux choses de même nature », ce
   qui n'a jamais été vrai.
3. **L'état** — ce que l'app sait, dans un creux : une perle moulée, une phrase,
   et les faits en rangées alignées plutôt qu'en prose à points médians.

Le creux est le vocabulaire du plateau — le même trou que la grille utilise pour
une alvéole vide — donc un écran de réglages fait de plaques n'a **qu'une** sorte
de creux, et elle veut dire « voilà ce qui est, pas ce que tu peux faire ».

## Rejoindre : le clavier de l'app plutôt qu'un champ invisible

Il y avait six encoches, un caret, un exemple en petit, et sous le tout un champ
de saisie invisible qui attendait le clavier système. Sur une console à manette,
ce clavier ne s'ouvrait jamais : rien à l'écran ne disait comment produire un
caractère, et c'était le seul écran de l'app à en demander un. Le champ invisible
apportait le collage et la sélection — deux gestes qui n'existent pas sans écran
tactile ni curseur — au prix du seul geste qui compte ici.

Il est remplacé par la dalle de la recherche. Le code se saisit donc de la même
façon qu'on cherche un jeu, avec les mêmes touches et le même curseur, et l'écran
n'a plus rien de spécial.

**En deux colonnes, parce que la machine est couchée.** Empilé, le clavier aurait
poussé les encoches sous l'en-tête. À gauche ce qu'on lit — le jeu, le gabarit,
les six cases, l'action ; à droite ce avec quoi on écrit. C'est l'ordre de la
main droite sur une console tenue à deux mains.

## Le clavier de code n'est pas le clavier de recherche

L'écran « Rejoindre par code » demandait six caractères à la manette et ne disait
nulle part comment les produire. La réponse était déjà dans le dépôt : la même
dalle, réglée autrement.

Trois différences, et une seule raison à chacune — **un code n'est pas une
phrase**. Pas de casse, donc pas de majuscule à verrouiller ; pas de mots, donc
pas d'espace ; pas de bascule lettres/chiffres, parce qu'un code mélange les deux
et qu'aller les chercher dans deux pages doublerait le nombre de gestes.
L'alphabet entier et les dix chiffres tiennent en quatre rangées, ce qui est
exactement la hauteur de l'autre.

**L'alphabet, pas l'AZERTY.** On ne tape pas un code de mémoire musculaire mais
caractère par caractère, en le lisant sur un écran ou dans un message. Sur une
disposition de machine à écrire il faut chasser chaque lettre ; sur l'alphabet on
sait où elle est avant de la chercher.

## Une case de la dalle ne dessine rien au repos

Trois états seulement l'allument, et ils se cumulent proprement parce qu'ils sont
dans l'ordre de la certitude : la majuscule verrouillée (état durable, le plus
discret), le curseur (où l'on est), l'appui (ce que l'on fait). La plus forte
gagne. Le contour n'apparaît qu'avec le curseur : c'est lui qui doit se retrouver
d'un coup d'œil sur trente-huit cases, l'appui se voit déjà sous le doigt.

**Le curseur avait été oublié.** Une touche était `clickable`, donc focalisable,
donc un arrêt du curseur — et rien ne le montrait. Sur une app pilotée à la
manette, le clavier était le seul endroit où l'on tapait à l'aveugle. Le
`focusable` est désormais explicite, avec le même `interactionSource` que le clic.

Corollaire payé cher par la bibliothèque : **cliquable au doigt, jamais
focalisable**. `clickable` rend focalisable par défaut, ce qui laisserait autant
d'arrêts invisibles que de touches, en concurrence avec le curseur que la dalle
tient. L'écran tactile garde son chemin, la manette a le sien, et les deux
désignent la même case.

Et la dalle n'affiche son curseur **que tant qu'elle l'a** : la case allumée est
désignée par un index, pas par le focus, donc elle restait allumée après que le
haut avait rendu la main au champ de recherche. L'index se souvient — c'est ce
qu'on veut au retour — mais il ne se dessine que quand la dalle a le focus.

La gravure qui sépare deux cases est un trait sombre puis un trait clair d'un
point sous lui : la même paire que le biseau des plaques, à l'échelle d'un sillon.
Un seul trait gris aurait fait un tableau ; la paire fait un sillon.
