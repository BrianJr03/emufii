# Second écran : pourquoi le panneau est fait comme ça

Le récit qui vivait dans `secondscreen/SecondScreenContent.kt`, sorti du code le
2026-08-24 (cf. `docs/STYLE_COMMENTAIRES.md`). Les titres servent d'ancres : le
code les cite, donc **ils ne se renomment pas à la légère**.

Rappel qui domine tout le reste : **le mono-écran reste la mise en page
principale**. Le panneau complète, il ne délègue jamais ; un joueur sans second
écran ne perd pas un mot.

## Le panneau n'a pas de style à lui

Le même plateau que l'écran principal, jamais un second style : sol gravé,
plaques moulées, un seul accent, comme le contrat de direction les fige
(`ui/theme/Direction.kt`). Un panneau avec son propre look se lirait comme une
autre application tournant au dos de la machine.

Il se lit à bout de bras, hors axe, sous les mains du joueur. Donc un objet mène
chaque face et tout le reste l'étiquette ; le panneau ne tient jamais plus que
ce qu'un coup d'œil ramasse.

Il n'avait **ni curseur ni commande** : il rapportait, et rien d'autre. Ça a
changé le 2026-08-25, et seulement en session — voir « Le panneau prend les
étapes, parce qu'il est tactile ». Partout ailleurs la règle tient.

La couleur suit le produit et non le chrome — la jaquette est la seule chose
autorisée à être forte sur la face de survol, et elle prête même sa teinte
extraite à l'ombre qu'elle projette.

## Le panneau prend les etapes, parce qu'il est tactile

Le panneau **est tactile**, et le système le dit : l'écran arrière de la Thor se
déclare `touch EXTERNAL`. Il ne l'était pas pour Emufii, parce que sa fenêtre
portait `FLAG_NOT_TOUCHABLE` et `FLAG_NOT_FOCUSABLE` — posés pour qu'elle ne
vole jamais un appui destiné au jeu, ce qui en faisait un afficheur et rien
d'autre.

Ces deux drapeaux tombent. La Thor arbitre le focus entre ses deux écrans : la
pile de fenêtres porte un focus **par écran**, vérifié sur l'appareil — la
présentation a le sien sur l'écran 4, l'activité garde le sien sur l'écran 0.
L'appui destiné au jeu ne se perd donc pas. `FLAG_NOT_TOUCH_MODAL` reste : ce
qui est pressé *à côté* de la fenêtre continue d'aller à ce qu'il y a derrière.

Ce que ça change en session : **les deux étapes descendent au dos**, sous les
pouces quand la machine est tenue à deux mains, et l'écran de face rend les
130 dp qu'elles prenaient à l'explication — qui en avait besoin, la carte PS2 en
faisant seule plus d'un écran. Dès que le panneau s'éteint, ou qu'il n'y en a
pas, elles remontent de face : le mono-écran reste la mise en page principale,
et un joueur sans second écran ne perd pas un bouton.

Trois précautions :

- **Les libellés voyagent déjà traduits.** La fenêtre du panneau a son propre
  contexte d'affichage, qui a déjà fait parler l'app en français dans une
  interface en anglais une fois.
- **Les actions sont retirées en partant.** Ce sont des lambdas d'une
  composition : l'écran qui les pose les efface à sa mort, sinon le panneau
  garde une session morte sous le doigt.
- **Le libellé et l'état du bouton de lancement ont une seule définition.** Deux
  écrans les dessinent désormais ; le jour où les deux divergent, la moitié des
  joueurs pressent un bouton qui dit autre chose que ce qu'il fait.

**Le panneau est large et court** — 537 dp sur 320 utiles — et la face vit dans
une boîte centrée qui ne défile pas : ce qui dépasse est rogné aux deux bouts,
sans un mot. Deux mises en page ont été essayées et mesurées sur l'appareil
avant la bonne.

1. **Empilée.** Pastille, code, faits, titre, puis deux commandes l'une sur
   l'autre : près de 380 dp demandés pour 320 disponibles. Une session Eden
   n'affichait **qu'un bouton sur deux**, et rien ne disait pourquoi.
2. **Deux colonnes**, l'identité à gauche et les commandes à droite, pour
   récupérer de la hauteur. Pire : chaque colonne tombait à 268 dp, le code se
   cassait en « NRX- » et « 572 », et le port s'écrivait **un chiffre par
   ligne**. Un code coupé en deux n'est plus un code, c'est deux morceaux qu'il
   faut recoller à voix haute.

Ce qui marche : **une colonne, et ce qui se répète se met côte à côte.** Le code
prend toute la largeur sur une seule ligne (`softWrap = false`, jamais
négociable), la console et le titre du jeu partagent une ligne d'étiquettes, les
deux creux de référence en partagent une autre, et les deux commandes se
partagent une rangée à hauteur fixe — 64 dp chacune, la même, pour qu'un
libellé sur deux lignes ne fasse pas grandir sa plaque à côté de sa voisine.

Leçon à garder : **cette boîte rogne en silence.** Toute face qui gagne un objet
se remesure contre ces 320 dp. La capture du panneau est possible mais son
identifiant n'est pas le `displayId` logique : `screencap -d` veut l'identifiant
physique, celui que donne `dumpsys SurfaceFlinger --display-id`. Sans ça on code
au jugé, et ces deux mises en page ratées ont été livrées sans que personne les
voie.

## R tourne la page depuis les deux écrans

La touche R est la seule commande de la face de survol : elle retourne la
plaque vers la fiche du catalogue. Elle était écoutée par la grille de l'écran
de face, et c'était juste tant que le panneau ne pouvait rien recevoir.

Depuis qu'il est tactile, **une pression dessus lui donne le focus de son
écran** — et R n'atteignait plus personne. La commande du panneau cessait de
marcher au moment précis où l'on venait de toucher le panneau, ce qui est le
pire moment possible.

Les deux écoutes coexistent donc, chacune sur son écran, et elles appellent la
même chose. Le focus clavier va à une **fenêtre**, pas à l'appareil : celle qui
l'a répond, l'autre ne voit rien, et il n'y a pas de double déclenchement à
craindre. Sur une machine à un seul écran, rien ne change — la pression ne fait
toujours rien du tout, ce qui reste la règle : l'écran de face ne gagne ni ne
perd rien parce qu'un panneau existe.

L'oreille du panneau est un `focusable` sans destination : rien ne s'y
sélectionne, elle ne sert qu'à recevoir la touche.

## La liste d'amis descend au dos, les deux cartes restent devant

La page des amis porte trois choses : ton code, le champ pour ajouter quelqu'un,
et la liste. Les deux premières **demandent** quelque chose — on les lit, on les
touche. La troisième **rapporte** : qui est là, qui joue à quoi. C'est
exactement la ligne de partage entre les deux écrans, alors la liste passe au
dos et les deux cartes se centrent devant.

Ce que ça donne :

- **Le panneau** montre la liste entière, dans le même ordre que l'écran de
  face — en jeu, puis en ligne, puis les autres par nom. Deux ordres pour une
  même liste, ce serait deux listes. Deux colonnes au-delà de cinq amis, parce
  que la boîte du panneau est courte et rogne en silence.
- **L'écran de face** garde ses deux cartes, centrées, et gagne une ligne qui
  dit **où la liste est passée** et combien d'amis elle contient. Sans elle, un
  joueur dont personne n'est en ligne referme la page en croyant n'avoir aucun
  ami — le panneau est derrière la machine, il ne se remarque pas tout seul.
- **Sans panneau**, rien ne bouge : la page reprend son ordre de document, les
  cartes puis la liste, et se lit du haut.

**Le retrait vit au dos aussi**, et l'asymétrie est fermée. Chaque casse porte
sa croix, à droite ; l'appui ouvre la question **sur le panneau**, pas sur
l'écran de face. C'est le point qui décide : le doigt vient de presser au dos,
et une question posée de l'autre côté de la machine ne se voit pas.

Ce n'est pas un `Dialog` — une fenêtre de dialogue appartient à l'écran qui la
lance, et celle-ci s'ouvrirait devant le joueur. C'est un voile et une plaque,
dans la fenêtre du panneau, avec les mêmes réponses qu'ailleurs : annuler
d'abord, le rouge coque pour ce qui ne se rattrape pas.

Trois détails de forme, tous demandés par l'usage :

- **Le titre est en haut à gauche, en gras.** Centré au-dessus d'une liste, il
  se lisait comme la légende d'un objet ; c'est le nom de ce que la face porte.
- **Une casse ne dépasse jamais la moitié de l'écran.** À pleine largeur, un nom
  et deux mots s'étalent sur 500 dp et la rangée se lit comme une barre. Deux
  colonnes toujours, même avec un seul ami : la colonne de droite reste vide
  plutôt que de laisser la casse grandir.
- **Le point de présence touche le pseudo**, il n'est pas à l'autre bout de la
  casse : c'est une propriété de la personne, pas une colonne.

Les libellés d'état (« Hors ligne », « En ligne », le titre du jeu) sont résolus
côté écran de face et voyagent déjà traduits, comme pour les étapes de session :
la fenêtre du panneau a son propre contexte d'affichage.

## Chaque face se centre pour elle-même

Les faces sont centrées dans la bande qui reste entre l'en-tête et la légende.
Cette bande n'est pas la même pour toutes : la face de session a une légende
**vide** — ses commandes se pressent au doigt, il n'y a pas de touche à nommer —
alors que les faces du menu en ont une. Son centre géométrique tombe donc plus
bas que le leur, et elle paraissait posée trop bas quand les autres étaient
justes.

Le creux qui la remonte appartient donc **à elle seule**. Posé d'abord sur la
boîte commune, il a remonté les faces du menu, qui n'avaient rien demandé —
c'est la correction d'une face qui en a déréglé trois.

La règle : une face qui a besoin d'être décalée porte son décalage. La boîte
commune ne fait que centrer.

## Le fondu entre deux faces n'est pas une décoration

Sans lui le panneau **coupe** : un curseur qui longe une étagère remplace une
face entière par pression, et un texte qui apparaît à pleine intensité en une
image se lit comme un flash du coin de l'œil — précisément là où cet écran se
trouve. Le fondu donne aussi à une image pas encore arrivée les deux cents
millisecondes qu'il lui faut, si bien qu'un passage rapide sur la grille cesse
de ressembler à un chargement répété.

Il est indexé sur **l'identité du jeu**, pas sur le modèle : la pastille de
compatibilité et la fiche du catalogue sont publiées un instant après, contre la
même ROM. Fondre sur le modèle entier dissoudrait une face dans une face presque
identique à chaque fois, ce qui se voit comme un bégaiement.

## La console se lit en direct, les autres faces sont gelées

Le modèle est mémorisé **sur la clé de face** pour qu'une face en train de
disparaître ne se redessine pas avec le contenu de celle qui arrive.

La branche console est la seule qui doive échapper à ce gel. Toutes les consoles
partagent la clé `"console"`, donc la valeur mémorisée resterait sur la première
console affichée et la fiche ne changerait jamais de texte — **c'est arrivé, et
ça s'est vu comme « le second écran ne marche plus »**. Comme la clé ne change
pas entre deux consoles, aucun fondu ne tourne à ce moment-là : il n'y a pas de
face sortante à protéger. La valeur gelée reste le repli, pour le départ vers
une autre face, quand le modèle est déjà devenu autre chose.

## La fiche console est une plaque qui grandit, pas une plaque qu'on remplace

Trois tentatives ont chacune cassé quelque chose : une carte à la taille de son
texte sautait d'une console à l'autre ; une carte étirée sur toute la hauteur
était aux deux tiers vide ; une carte accrochée en haut se retrouvait sous
l'en-tête. Les trois cherchaient à empêcher une **coupure** — le panneau
remplaçant une carte par une autre de taille différente entre deux images.

Ce qui manquait, c'est que le changement lui-même peut être montré. Le cadre
n'est jamais remplacé : c'est la même plaque du début à la fin, centrée, dont la
taille est animée vers ce que réclame la console suivante pendant que les mots
se fondent à l'intérieur. Rien ne claque, rien n'est rembourré à une taille
commune, et la carte n'est jamais plus grande qu'elle n'a besoin.

Centrée, pour qu'une carte qui grandit s'ouvre depuis son milieu dans les deux
sens : grandir vers le bas seulement tirerait l'œil, et ce panneau se lit du coin
de celui-ci.

Le cadre met plus longtemps que le texte, exprès : les mots ont disparu avant que
la plaque ait fini de voyager, donc rien ne se lit pendant qu'elle bouge.

## Le panneau ne crie pas

L'avertissement d'une console est une barre, pas un triangle avec un point
d'exclamation. Le panneau dessine ses propres symboles, et l'une des deux choses
qu'il ne doit jamais faire est crier : c'est une chose à savoir avant de
commencer, pas une erreur qui vient de se produire.

## La version s'affiche sur la face au repos

C'est le seul écran où elle a sa place sans qu'on l'ait demandée : c'est la
réponse à la question qu'on pose réellement au panneau quand rien ne tourne —
« sur quelle version tourne cette machine ? » — et y répondre ici épargne une
visite aux réglages. Un cran plus petite et un cran plus estompée que le nom,
pour qu'elle se lise comme une note de bas de page et non comme une seconde
ligne de poids égal.

## La fiche console : ce qu'elle dit, et ce qu'elle ne dit pas

Une plaque et rien d'autre à l'écran. C'est la seule face du panneau qui se
**lit** au lieu de se survoler, donc elle prend la forme qu'a ici une chose à
lire — une plaque en relief avec de l'air autour — plutôt que la mise en page de
la face de survol, dont le travail est de poser une jaquette et une pastille
côte à côte.

Le nom de la machine mène, parce que le joueur regarde une étagère de dossiers et
que la première chose que le panneau lui doit est de dire lequel est sous le
curseur. Puis deux lignes, puis un avertissement si cette console en a un. Rien
d'autre ne tient, et rien d'autre n'y a sa place : l'écran principal garde toutes
ses explications.

## La face de survol : deux pages, la seconde vraiment optionnelle

La page une est ce qu'on regarde en déplaçant un curseur : la jaquette, la
machine, le titre, si ça se joue ensemble, quel dump c'est. La page deux est ce
que veut quelqu'un qui s'est **arrêté** sur un jeu — de quoi ça parle, quand
c'est sorti, à quoi ça ressemble — et on l'atteint par un bouton de l'écran
**principal**, puisque celui-ci n'a pas de curseur.

Elle glisse au lieu de se fondre. Le bouton dit « plus bas », et une page qui
arrive par le bas est le geste que le joueur vient de faire ; un fondu dirait
« remplacé », ce qui n'est pas ce qui s'est passé.

La jaquette est l'objet et prend le poids : c'est la seule chose que le joueur
reconnaît avant de lire quoi que ce soit. La colonne à côté répond à ce qu'une
jaquette ne peut pas dire : quelle machine, si celui-ci se joue vraiment
ensemble, et quel dump est dans le lecteur — deux copies du même jeu ne sont pas
le même fichier, et c'est le joueur qui doit le savoir.

**Rien n'est deviné.** Les moitiés absentes ne sont simplement pas imprimées : un
panneau qui déduirait « USA » d'un silence se tromperait pour tout joueur
européen dont le dumpeur a sauté l'étiquette. Idem page deux, entièrement
éditoriale et entièrement faillible : un synopsis dans la mauvaise langue est
présenté comme étant dans cette langue, et un jeu inconnu du catalogue reçoit une
phrase honnête plutôt qu'une mise en page vide.

## Rien ne défile, donc tout doit tenir

Cette fenêtre ne défile pas — pas de curseur, pas de tactile — donc ce qui passe
sous la ligne de flottaison est perdu, et une page coupée en plein milieu d'une
image a l'air cassée plutôt que longue. C'est le paragraphe qui cède ses lignes
en premier : un synopsis se lit très bien tronqué, une image non.

## La langue vient de la fenêtre, pas du processus

Elle se lit sur la configuration de **cette** fenêtre : le panneau est un second
affichage avec sa propre configuration, et le choix de langue du joueur
s'applique par configuration. Prendre `Locale.getDefault()` serait juste par
accident et faux le jour où les deux divergent.

Corollaire attrapé le 2026-08-24, et il coûtait cher : la fenêtre est une
`Presentation`, dont le contexte est fabriqué par `createDisplayContext()`. Ce
contexte repart de la configuration du **display** et **perd la locale par-app**
— l'app réglée en anglais gardait un panneau en français, la langue du système.
Le piège de langage qui l'a caché est décrit dans
[`SecondScreenHost`](#la-fenetre-le-contexte-nest-pas-celui-quon-croit).

## La lumière de service a sa propre couleur

Un point allumé et deux mots, rien d'autre : c'est l'unique morceau de chrome du
panneau et il doit être lisible sans être lu — la couleur répond à travers la
pièce, les mots ne font que confirmer.

Sa couleur n'est **pas** l'accent de l'app. L'accent veut dire « c'est ici que tu
es » partout ailleurs dans Emufii, et un voyant qui l'emprunterait ferait dire
deux choses au curseur. Le vert et le rouge sont ce que disent déjà une prise, un
routeur et un chargeur de console.

## Les nouvelles arrivent d'en haut et repartent seules

Un ami en ligne, une version publiée : l'écran principal dit toujours les deux, et
un joueur à un écran ne perd rien. Ce que ceci ajoute est le cas que l'écran
principal ne peut pas servir — l'émulateur le possède — où l'alternative est un
volet de notification tiré par-dessus un jeu qui tourne.

Ça repart tout seul parce que **personne ne peut le renvoyer** : cette fenêtre ne
prend aucun tactile, par conception. Tout ce qui demanderait un acquittement
resterait pour toujours.

## Le code de session ne porte pas d'étiquette

Les six caractères dans l'accent de l'app, sur l'unique plaque soulevée du
plateau, sont déjà la seule chose du panneau qu'on pourrait lire à voix haute à
quelqu'un. Les nommer serait un costume d'importance, que cette app ne porte pas.

Les deux nombres que réclame la boîte de dialogue de l'émulateur sont **gravés**
dans le plateau plutôt que plaqués : ce sont des références à relever, pas des
objets à saisir. Côte à côte en creux parce qu'on les tape ensemble, et qu'un
joueur qui en copie un à la fois doit revenir.

## La légende, et pourquoi les symboles sont dessinés

À gauche on quitte, à droite on agit : la disposition de toutes les coques de
console que le joueur possède déjà. Un côté vide ne prend pas de place, donc une
face sans rien à dire à gauche ne laisse pas de trou.

Un bouton est une **plaque**, pas un creux : c'est l'image d'une chose qui
dépasse et qu'on peut presser, et les creux du plateau sont pour les trous. Posé
à plat, 2 dp de relief, parce qu'une légende est un schéma et ne doit pas
concurrencer ce qu'elle étiquette. Une consigne de maintien montre un bouton
**tenu** : la plaque perd son relief et son arête éclairée et prend la teinte de
l'ombre.

La croix directionnelle et les flèches sont **dessinées, jamais tapées**. Un
caractère arriverait de la première police qui le porte, et un cap fait 26 dp :
à cette taille la graisse et la ligne de base d'une police de repli se voient
toutes les deux. Le système dit que les icônes se dessinent.

## Une lettre est centrée sur son encre, pas sur sa boîte

Trois choses distinctes la poussaient hors du centre, et mettre le texte en page
ne pouvait en corriger qu'une.

- **À gauche.** `labelLarge` porte `letterSpacing = 0.1.sp`, et Compose ajoute
  cet espace *après* le dernier caractère comme entre les caractères. Sur une
  chaîne d'une lettre, la largeur mesurée est le glyphe plus un écart de queue :
  centrer la mesure laisse l'encre à gauche.
- **En bas.** `labelLarge` pose `lineHeight` 18 sp sur `fontSize` 14 sp. Rogner
  cet interligne laisse encore une boîte allant de l'ascendante à la descendante,
  alors qu'une capitale sans descendante ne remplit que de la ligne de base à la
  hauteur de capitale. Centrer cette boîte n'est pas centrer la lettre, et aucun
  rognage ne rend les deux identiques.
- **À droite, une fois les deux premières corrigées.** Centrer sur la chasse
  n'est toujours pas centrer l'encre : les approches latérales d'un glyphe
  diffèrent, et le B de cette police est mesurablement à droite du centre de sa
  propre chasse. Mesuré hors ligne contre `rounded_bold.ttf` à cette taille
  exacte, **puisqu'un second écran ne se capture pas**.

D'où : le glyphe est dessiné et placé depuis
`android.graphics.Paint.getTextBounds`, le plus petit rectangle enfermant
l'encre. En posant la plume à `w/2 - (left + right)/2`, le centre de l'encre
tombe sur le centre du cap par construction, sur les deux axes, pour n'importe
quel glyphe et n'importe quelle échelle typographique.

## La jaquette est moulée dans le plateau, et son ombre est de sa couleur

L'ombre est teintée de la couleur que la jaquette elle-même a livrée
(`Rom.accentArgb`, déjà extraite pour l'écran principal). C'est la règle de
couleur-contenu prise au mot : la teinte est celle du jeu, pas celle de l'app, et
elle arrive comme de la profondeur — une vraie ombre décalée — plutôt que comme
un lavis passé sur le chrome. Un jeu sans teinte extraite projette simplement
l'ombre du plateau, et rien de la mise en page ne change.

**Un liseré seul ne suffisait pas ici, et l'arithmétique dit pourquoi.** Le
contour est un trait de 1,5 dp centré sur le tracé, donc le rognage en mange la
moitié extérieure et il survit 1,73 px, à 24 % d'opacité, sur une jaquette de
452 px — 0,38 % de la largeur, la moitié de la présence qu'il a sur une tuile de
grille. C'est pour ça qu'il se lit sur l'écran principal et disparaît sur
celui-ci. Mettre le trait à l'échelle aurait été une seconde règle pour un seul
endroit. Une plaque avec l'image en retrait est le cadre que ce monde possède
déjà : face, arête et biseau éclairé, à une taille que l'œil trouve depuis
l'autre bout d'une pièce.

## Le contrôle appartient à ce sur quoi il agit

La jaquette, et le chemin vers son autre page directement dessous. Placé au
milieu du panneau, ce contrôle était un quatrième objet flottant entre deux
colonnes et se lisait comme une légende pour l'écran entier plutôt que pour le
jeu.

Il est dessiné comme une chose **pressable** — une plaque, comme les caps de la
légende — parce que c'est ce qu'il est : la gâchette à l'avant de la machine fait
ça. Rien sur cette fenêtre n'est tactile, donc un contrôle qui aurait l'air d'une
cible à toucher serait un mensonge.

---

## La fenêtre : le contexte n'est pas celui qu'on croit

Vit dans `secondscreen/SecondScreenHost.kt`.

Dans `EmufiiPresentation(context: Context, display: Display)`, le paramètre
`context` n'est **pas** déclaré `val`. Un paramètre de constructeur non-`val`
n'est pas visible depuis une fonction membre, donc le `context` écrit dans
`onCreate` ne désigne pas celui qu'on passe : le nom se résout silencieusement
vers la propriété héritée `Dialog.getContext()`, le contexte d'affichage que
`Presentation` se fabrique. **Ça compile sans un mot**, ce qui explique que ce
soit passé inaperçu.

Ce contexte d'affichage est le bon pour tout **sauf la langue** : il est bâti par
`createDisplayContext`, qui repart de la configuration du display et laisse donc
tomber la locale par-app. D'où la correction : garder la configuration du display
— c'est elle qui donne à la fenêtre sa taille et son thème — et n'y réinjecter
que les locales, lues depuis `LocaleManager` pour qu'un changement fait dans les
réglages d'Android compte aussi. Locale vide veut dire « le joueur n'a jamais
choisi » : le contexte est alors rendu tel quel plutôt que figé sur la langue du
jour.

---

## L'état du panneau vit à portée de processus, pas dans la composition

C'est tout le dessin, et ce n'est pas de la propreté : **la raison d'être du
second écran est le moment où l'émulateur possède l'écran principal et où Emufii
n'est nulle part.** Un modèle tenu dans une composition meurt avec elle, donc le
seul hôte qui comptera plus tard — un service de premier plan survivant à
l'activité — ne pourrait jamais le lire. Publier ici ne coûte rien aujourd'hui et
fait de cet hôte **un nouvel abonné plutôt qu'une réécriture**.

Les deux hôtes rendent le même contenu depuis ce flux : il y a donc exactement
**une** description de l'écran, qui que soit le porteur de la fenêtre.

La page courante y vit aussi, pour deux raisons : **le bouton qui la tourne est
sur l'écran avant** — le panneau n'a ni curseur ni tactile — et le panneau est
redessiné de zéro chaque fois que la fenêtre est refaite. Elle **revient à la
première page dès que le curseur change de jeu** : une seconde page laissée
ouverte montrerait le synopsis d'un jeu sous la jaquette du suivant, le temps que
le joueur s'en aperçoive.

D'où aussi la distinction entre « même jeu » et « modèles égaux » : la pastille de
compatibilité et les métadonnées arrivent **après** la ROM, et republier le même
jeu avec sa pastille remplie ne doit pas refermer d'un coup une seconde page
ouverte sous les mains du joueur.

## Ce qui voyage jusqu'au panneau

**La [Rom] entière**, pas une poignée de champs extraits : le panneau résout sa
propre jaquette depuis elle, par le même chemin qu'une tuile de l'écran avant.
Les deux écrans répondent donc depuis un seul cache et un seul jeu de règles — le
jour où un joueur choisit une jaquette à la main, le panneau arrière n'est pas un
second endroit à prévenir.

**Sauf la région et la révision, qui sont passées et non calculées ici** : elles
sont lues une fois, sur la ROM que l'écran avant tient déjà. Un panneau qui
analyserait des noms de fichier le referait à chaque déplacement du curseur.

Les faces sont **délibérément peu nombreuses** : un second écran qui essaie d'être
une seconde app est une seconde app à maintenir.

La face « dossier de console » existe parce qu'un dossier est **le seul endroit de
l'app où le joueur pense à la machine** — et chaque machine joue ensemble
différemment ici : l'une passe par un salon sur notre serveur, l'autre par une
redirection vers un service associatif, une autre veut une adresse tapée dans le
jeu lui-même. Il n'y avait jamais eu de moment pour le dire : la tuile de l'écran
avant est une image avec un décompte, et un paragraphe posé dessus serait un mur
au milieu d'une grille.

Enfin, **les deux nombres que réclame la boîte de l'émulateur sont aussi sur
l'écran avant, et c'est exactement le problème qu'ils résolvent ici** : le moment
où on en a besoin est celui où on est *dans* ARMSX2 ou Azahar en train de les
taper, et l'écran avant a disparu. Le presse-papier n'en porte qu'un à la fois, et
la boîte en veut deux.

## Le panneau ne s'allume que s'il a une raison

L'hôte est une `Presentation` — une boîte de dialogue liée à l'activité, qui ne
coûte aucune permission et meurt avec elle.

Mais **être lié à la *durée de vie* d'une activité n'est pas la même chose
qu'être devant**, et cette différence était un vrai défaut : quitter Emufii pour
l'écran d'accueil laissait le panneau arrière allumé sur un processus qui se
trouvait simplement encore vivant. Ce qui décide désormais est une règle à deux
réponses :

- **Emufii devant** : le panneau reflète ce que fait le joueur, donc il suit
  l'app et s'éteint quand elle est quittée. Un panneau qui brille encore au dos
  d'une portable dont le propriétaire est parti sur son écran d'accueil est
  exactement le genre de chose qui fait désactiver une fonction pour de bon.
- **Une session qui tourne** : il reste allumé même si Emufii est derrière
  l'émulateur, **parce que c'est là qu'il gagne sa place**. Le code au dos de la
  console est ce que lit l'autre joueur, et on en a besoin précisément quand
  l'écran avant a été cédé au jeu.

La règle est **pure**, donc lisible et testable sans second écran.
