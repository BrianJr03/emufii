# Direction visuelle : le contrat « HOME MENU », la palette, les accents

Sorti de `ui/theme/` le 2026-08-24 (cf. `docs/STYLE_COMMENTAIRES.md`). Le
système complet est dans `DESIGN.md` ; ce fichier porte **le contrat** et les
raisons chiffrées derrière chaque couleur. Titres = ancres citées depuis le code.

## Le contrat de direction — « HOME MENU »

Posé le 2026-08-22, brief épinglé. **C'est ce texte qui faisait tout le corps de
`ui/theme/Direction.kt`** ; l'objet Kotlin ne sert plus que d'ancre de nommage.

**Thèse.** Emufii est le menu d'accueil d'une console portable, pas une
application avec une liste de jeux. Il refuse le tableau de bord en verre
translucide qu'il a été — panneaux flous, bleu iOS, dégradés dérivants — parce
que le verre est ce que porte une app de téléphone, et que cette chose-là se
tient comme une console.

**Monde propre.** Du plastique moulé sous la lumière d'un plateau. Un sol argent
froid portant une fine grille gravée ; des plaques de plastique blanc à arête
d'un cheveu, biseau supérieur éclairé et vraie ombre portée décalée ; une
typographie à terminaisons arrondies (Rounded M+) ; **une seule couleur
signature**, le cyan du plateau, dépensée uniquement sur le curseur et l'action
principale. La couleur vient sinon des jaquettes. Le sombre est le même plateau
la nuit, **jamais un inverse**.

**Récit.** Le joueur voit ses jeux comme des objets sur un plateau, trouve le
curseur sans le chercher, presse A, et il est parti dans l'émulateur.

**Premier écran.** La bande d'état en haut comme l'écran encastré d'une console —
profil, tunnel, consoles — puis le plateau de plaques carrées qui remplit le
reste, le curseur allumé sur le premier jeu.

**Forme.** Monde épinglé par l'utilisateur (le tirage est battu par un brief
épinglé) ; mode Operate.

**Finition.** Non relu et non documenté vaut non fini ; cette construction
s'achève par la revue de finition, le verdict, et `DESIGN.md`.

## Un seul langage de coins

Le rayon d'un coin moulé, à quatre tailles. **Une superellipse échantillonnée a
été essayée et retirée** : à la taille d'une tuile, 128 segments laissaient une
facette visible dans chaque coin, ce qui se lisait comme sale plutôt que comme
doux. Des rayons simples, donc, généreux et constants — ce que donne réellement
du plastique injecté.

Légèrement plus serrés que les valeurs du monde « verre » : le plastique a une
arête, et un rayon de 28 dp sur un panneau en fait une pastille informe plutôt
qu'une plaque.

**Le rayon du panneau est nommé une fois** (`CardCorner`). Les rangées aux deux
extrémités d'une carte de réglages héritent du coin de la carte, puisque le
curseur trace le contour de la place qu'occupe réellement une rangée. Ce nombre
vivait une seconde fois là-bas, et quand le monde plastique a fait passer le
panneau de 28 dp à 22, la copie est restée en arrière : l'anneau s'arrondissait
plus large que la carte où il se trouvait, et dépassait son coin sur la première
et la dernière rangée de chaque bloc.

## Trois sols, un accent, et rien d'autre n'a de teinte

Le menu d'une portable est un plateau d'objets colorés sur une coque neutre :
toute couleur qui n'est pas le cyan est un gris avec quelques degrés de bleu
dedans, **parce que la jaquette doit être la seule chose qui crie**.

- **Le jour** est un argent froid, pas un blanc : les plaques sont blanches, et
  une coque blanche ne leur laisserait rien sur quoi se poser. Échantillonné
  assez sombre pour que le cheveu d'une plaque soit lisible sans qu'on dessine
  un contour une seconde fois.
- **La nuit** est le même plateau sous une lampe : noir bleuté, jamais gris
  neutre.
- **L'OLED** est exactement éteint. Un pixel noir est un pixel non allumé, et
  `0xFF050505` est allumé.

**L'arête moulée** est un cheveu d'un ton plus sombre que la plaque en clair, un
ton plus clair en sombre. C'est ce qui fait d'une plaque un objet plutôt qu'un
remplissage, et c'est **le seul séparateur restant en OLED**, où une ombre ne
dessine rien. Approfondie le 2026-08-22 : à `0x1F` le contour disparaissait à
distance de coup d'œil et une plaque blanche se lisait comme une carte plate
portée par sa seule ombre. Un moulage a une arête qu'on voit sans la chercher.

## Le cyan est dépensé sur le curseur, et sur rien d'autre

Le curseur, l'action principale, la sélection courante. Rien d'autre. Le dépenser
en décoration est ce qui referait de « où suis-je ? » une question — et sur une
portable, le curseur est en permanence quelque part.

**Il y a deux cyans, et c'est mesuré.** Le cyan clair est une *lumière* : à 4,7:1
contre le noir il est parfait pour un curseur qui brille sur un plateau sombre,
et sans espoir comme fond de texte blanc — **2,2:1**, ce qui est illisible et non
discutable. Le thème clair remplit donc son bouton principal d'une coupe plus
profonde (4,6:1 sous du blanc) et garde le vif pour le curseur. Le thème sombre
n'a pas ce problème : le cyan vif y porte l'encre profonde, à 8:1.

Le **rouge de coque** est emprunté à la console dont ce monde vient : erreurs et
confirmations destructrices seulement. Il apparaît peut-être deux fois dans toute
l'app, et c'est pour ça qu'il se lit quand il apparaît.

Le **vert** est un alias hérité : l'anneau vert a disparu — le curseur est cyan,
un accent pour un sens — mais plusieurs écrans nomment encore cette couleur pour
un état « prêt ».

## Un accent, mais toujours en trois coupes

Une seule teinte ne peut pas faire les trois métiers de l'accent : le **vif** doit
se voir d'un coup d'œil sur un plateau noir, le **profond** doit porter du texte
blanc, et l'**encre** doit se lire *sur* le vif. Le cyan livré était déjà bâti
ainsi — la coupe profonde existe parce que du blanc sur le cyan clair est à
2,2:1 — donc faire de l'accent un choix veut dire porter les trois coupes pour
chaque couleur, pas échanger un hexadécimal.

Les pastilles secondaires ne prennent **jamais** un aplat : elles prennent
l'accent au cinquième.

**Les couleurs fixes n'ont pas été choisies à l'œil** : chaque coupe profonde est
sa base assombrie en HSL jusqu'à passer 4,6:1 sous du blanc, et chaque encre
jusqu'à passer 5:1 sur sa propre base — exactement les ratios que mesure le cyan
livré, pour qu'une couleur choisie soit aussi lisible que celle qu'elle remplace.
Assombrir **en luminosité** plutôt qu'en mettant les canaux à l'échelle est ce qui
garde la teinte : la mise à l'échelle vide une couleur claire jusqu'au quasi-noir
avant même d'atteindre le ratio.

**Le vert est délibérément absent** : il tombe sur le vert « connecté », et un
accent qui se lit comme un sens réservé retire ce sens aux deux.

**Le rouge était absent pour la même raison et est proposé quand même**, sur
décision de l'utilisateur (2026-08-23). Ce que ça coûte, il faut le savoir : le
curseur porte alors presque exactement la couleur que la coque dépense pour les
erreurs et les confirmations destructrices, donc sur cet accent « c'est ici que
tu es » et « ceci va supprimer quelque chose » cessent d'être distingués par la
teinte seule. Le rouge retenu ici est à une teinte plus froide que le rouge de
coque, pour garder un peu de jour entre eux.

**L'encre du blanc est l'encre sombre de l'app**, et non le gris où la règle de
ratio s'arrêterait. La règle pose un plancher, pas une cible : un gris moyen sur
blanc passe 5:1 et se lit encore comme une étiquette désactivée, là où l'encre
utilisée partout ailleurs passe 14:1 et se lit comme de l'écriture.

## L'accent système est pris à la plateforme, pas dérivé

Il est pris aux deux schémas d'Android plutôt que recalculé ici, **parce qu'ils
portent déjà les garanties de contraste voulues** : le `primary` du schéma sombre
est un ton clair, fait pour être lu sur un fond sombre — le métier du curseur ;
le `primary` du schéma clair est un ton sombre, fait pour porter du blanc — le
métier de l'action remplie ; et le `onPrimary` du schéma sombre est, par
construction, ce qui est lisible sur le ton pris pour le vif.

Sous Android 12 il n'y a aucune couleur extraite, et le cyan du plateau prend le
relais. **Le réglage reste affiché** : ce n'est pas un mensonge, il dit « suivre
le système », et sur un téléphone qui n'a rien à suivre, c'est la couleur de
l'app.

## Les deux seuls endroits qui lisent l'accent à la main

L'anneau du curseur, dessiné à la main, et l'action remplie, qui a besoin de la
coupe profonde pour laquelle Material n'a pas d'emplacement. Tout le reste passe
par `MaterialTheme.colorScheme.primary`, que le thème remplit depuis la même
source — donc un écran neuf reçoit l'accent choisi sans savoir que ceci existe.

---

# Le plastique moulé, et les pastilles de la barre du haut

Sorti de `ui/theme/Plastic.kt` et `ui/components/ProfileChip.kt`.

## Une plaque moulée est quatre choses, dans cet ordre

L'ordre est tout le tour de main :

1. **une ombre avec un vrai décalage vertical** — la lumière vient d'au-dessus du
   plateau, donc une plaque projette *en dessous* d'elle-même. Un halo sans
   décalage est une décoration, pas de la profondeur, et c'est ce que dessinait le
   monde « verre » ;
2. **un remplissage plus clair en haut qu'en bas**, ce que fait une face de
   plastique courbe sous cette lumière ;
3. **une arête d'un cheveu**, le contour propre du moulage ;
4. **un biseau éclairé** un cheveu à l'intérieur, le long du haut.

**Retirez-en une et la plaque s'aplatit en rectangle de couleur.**

Le dégradé est à peine un dégradé sur le thème clair : du plastique blanc sous une
lumière diffuse a une décroissance très courte, et davantage se lit comme une
salissure grise.

Le **relief** dit à quelle hauteur la plaque est posée — 0 dp pour du plat, 10 dp
pour une tuile que le curseur a soulevée — et **le décalage de l'ombre le suit** :
une plaque qui monte sans que son ombre bouge se lit comme un grossissement, pas
comme une élévation.

**Pressée, la plaque perd son relief et son arête éclairée** et prend une teinte de
l'ombre du plateau : les trois parties qui la faisaient saillir, retirées — c'est
de ça qu'est fait « enfoncé ». Un bouton moulé qui ne voyage jamais est l'image
d'un bouton.

## Le biseau ne fait que le tiers supérieur, et sa profondeur est fixe

Un reflet qui fait tout le tour est un trait, et le trait, c'est déjà le contour.
Il est dessiné **par-dessus** le contenu, exprès : c'est un cheveu, et il appartient
à la surface, pas au-dessous.

**Une profondeur fixe, jamais une fraction de la hauteur.** Proportionnel, un grand
panneau de réglages voyait la moitié de sa face délavée : le biseau est une
propriété de l'arête, et une arête ne s'épaissit pas parce que l'objet grandit.

Les deux couleurs de la plaque sont exposées parce qu'un appelant doit parfois
construire le dégradé lui-même — une rangée de réglages se remplit avec la tranche
de la face de sa carte qui lui revient, soit les mêmes couleurs sur des bornes
décalées. **Prendre la liste plutôt que recopier les valeurs** est ce qui empêche
les deux de diverger.

Sur une tuile dont la jaquette couvre toute la face, le contour est dessiné
**après** le contenu — sans quoi il passerait dessous — pour qu'une image
photographique garde une arête moulée qui attrape la lumière du plateau.

## Le plateau est gravé, et un creux s'éclaire à l'envers

Le sol d'un menu de console n'est jamais un aplat : il porte une fine texture
répétée qui donne à l'œil une référence d'échelle, si bien que les plaques se
lisent comme des objets **d'une certaine taille** plutôt que comme des formes sur
un plan. Deux familles de cheveux, une sombre une claire, à un millimètre — **c'est
une gravure, pas un damier**.

Un logement vide est un **creux**, pas une plaque : l'éclairage inverse de tout le
reste — sombre en haut là où une plaque est éclairée — et c'est toute la raison
pour laquelle un trou se lit comme un trou.

## Les pastilles de la barre du haut sont une famille

Les trois — profil, amis, sessions — ont la **même forme, la même taille et le même
relief**, parce que les trois sont de la navigation. Le bouton des sessions était
auparavant une pastille bleue pleine flottant seule en bas de l'écran : elle disait
bien qu'elle menait quelque part, mais **dans une langue que rien d'autre ne
parlait**, et elle défilait par-dessus les jaquettes.

Les amis n'étaient atteignables que par la page de profil, deux touches plus loin et
rangés sous les réglages — **la mauvaise étagère** : voir qui est en ligne et le
rejoindre est une chose qu'on fait *au lieu* de parcourir la bibliothèque, pas une
préférence qu'on ajuste.

Le profil, lui, ne porte plus que l'avatar. Il portait une invitation à choisir un
pseudo tant que le profil n'en avait pas, ce qui **faisait changer la pastille de
largeur selon l'état** et posait une corvée permanente sur l'écran d'accueil pour
une chose dont une session se passe très bien.

## Le remplissage sombre est plus clair qu'il ne devrait, et c'est mesuré

La valeur précédente était à un cheveu de la couleur du fond d'écran, **et une ombre
ne fait rien sur un fond sombre** : la pastille était invisible. Ça n'a pas été vu
tant que l'avatar du profil était seul à en occuper une — un disque clair qui
remplit la pastille n'a pas besoin de pastille. Ça saute aux yeux dès qu'il y a un
glyphe.

## Pas d'indication Material : une animation de pression

`Surface(onClick)` amène une ondulation dont la couche d'état couvre aussi le
**focus** — et Android donne le focus à la première vue focalisable dès qu'un
clavier ou une manette est branché, **toujours**, sur une machine comme la Thor.

Le résultat était un lavis plat à 10 % posé en permanence sur cette pastille, ce
qui se lit « désactivé » plutôt que « sélectionné ». Le profil l'avait aussi,
caché depuis toujours sous son avatar.

L'agrandissement à la pression est le retour qu'emploient déjà le dock et les
tuiles, et les pastilles **restent focalisables** : la croix les atteint toujours.

## Les glyphes disent « d'autres joueurs » comme le reste de l'app

La pastille des amis a commencé en emoji 👥, **qui arrivait avec la palette de la
police système** à côté d'une pastille dont le seul autre occupant est une photo ou
deux initiales aux couleurs de l'app — la moitié d'une barre de deux boutons venant
d'ailleurs, et redessinée par chaque version d'Android en prime. Le redessiner à la
main corrigeait la palette et gardait le problème : **une petite figure de personne
reste un symbole collé sur une barre qui n'en contient aucun autre**.

Cette app dit déjà « les autres joueurs » d'une façon précise — des disques qui se
chevauchent avec un anneau découpé entre eux, sur l'écran de session et la carte de
présence. Le redire ici ne coûte rien et fait lire la paire comme **une seule
famille de formes** : toi à droite, les autres à gauche.

Les deux disques sont **blancs exprès**. Les remplir avec les deux premiers amis a
été envisagé puis abandonné : l'état que tout le monde voit sur une installation
neuve est l'état vide, donc l'icône serait de toute façon celle-là la plupart du
temps — avec une branche à maintenir et une apparence qui change sous l'utilisateur.
L'anneau est le remplissage de la pastille plutôt que du blanc franc, pour que
l'écart entre les disques reste un écart dans les deux thèmes.

Ils sont placés **par décalage depuis le centre**, pas par alignement de coins :
alignés en haut-droite et bas-gauche, ils ne se touchaient qu'en diagonale — deux
disques qui s'effleurent, la paire décentrée dans une pastille ronde. **Le
chevauchement est tout l'intérêt de la forme** : c'est lui qui fait lire deux cercles
comme deux personnes plutôt que comme un schéma.

Pour les sessions, c'est **deux écrans reliés, pas deux personnes** : une session est
deux consoles qui se parlent. La distinction est portée par la forme — des disques
contre des rectangles — pas par un détail décoratif.

Et le glyphe de devant prend **l'accent en vigueur**. Il disait « l'accent de l'app »
et était un bleu iOS codé en dur — un reliquat du monde « verre » qui n'avait jamais
suivi le cyan non plus, et donc la dernière chose du plateau dont la couleur ne
répondait à rien.
