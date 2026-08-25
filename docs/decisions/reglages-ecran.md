# L'écran des réglages : un hub et sept pages

Le récit qui vivait dans `ui/screens/SettingsScreen.kt`, sorti du code le
2026-08-24 (cf. `docs/STYLE_COMMENTAIRES.md`), puis complété le 2026-08-25 quand
l'accordéon est devenu un hub et sept pages. Titres = ancres citées depuis le
code : ne pas les renommer à la légère.

Le code vit désormais dans `ui/screens/settings/` : `SettingsScreen.kt` (l'hôte
et le hub), `SettingsPieces.kt` (les briques communes) et une page par fichier.

## Ce que cet écran remplace

> Les sections d'époque — celle-ci, « Une seule colonne, bornée et centrée »,
> « Une rangée occupe toute la carte », « Deux pièges de focus » et « Le thème
> ouvre un panneau » — décrivent
> l'**accordéon**, remplacé le 2026-08-25 par le hub et ses pages. Elles restent
> parce que chacune enregistre une tentative payée : les rouvrir sans les lire
> coûterait deux fois.


Il s'appelait « Profil » et portait **huit cartes de poids égal** : le pseudo, le
dossier de ROMs, les clés de console, la clé de jaquettes, la langue, le thème, la
boîte À propos, la réinitialisation. Chacune posait un titre, un paragraphe
d'explication et ses boutons — **tout le temps**, qu'on soit venu les voir ou non.

Sur la Thor, la carte Bibliothèque prenait un écran entier pour trois lignes, les
deux colonnes finissaient décalées, et trouver un réglage voulait dire fouiller un
mur de texte.

Ici **chaque réglage est une rangée** : ce que c'est à gauche, où ça en est à
droite. Le texte explicatif et les boutons n'existent qu'une fois la rangée
ouverte, c'est-à-dire au moment précis où on les a demandés. **La valeur affichée à
droite est ce qui remplace le paragraphe** : « Défini », « ROMS », « Français »
répondent déjà à la question qu'on venait poser.

**Une seule rangée ouverte à la fois.** Deux sections dépliées reconstruisent
exactement l'écran qu'on vient de démonter, et sur une portable la page se
remettrait à défiler au premier réglage touché.

Le nom est **enregistré à la frappe** plutôt que derrière un bouton : il n'y a rien
à valider ni à envoyer, le stockage est local, et un bouton qui ne veut dire que
« oui, vraiment » est un bouton dont personne n'a besoin.

## Un hub et sept pages, plus un accordéon

L'écran unique tenait **quatorze rangées dépliantes** réparties en quatre
sections. Chacune était défendable ; l'ensemble ne l'était plus.

Trois défauts, et le troisième est celui qui a décidé :

1. **Une seule rangée ouverte à la fois** était la bonne règle pour un écran de
   huit rangées ; à quatorze, elle veut dire que toucher un réglage pousse tout
   le reste de la page vers le bas, et qu'on referme sans arrêt pour retrouver
   sa place.
2. **Le rangement mentait.** L'auto-configuration de PPSSPP, le profil réseau
   PS2 et le remplissage automatique d'Azahar vivaient dans « Application »,
   entre la langue et le thème, parce qu'il n'existait pas d'endroit où les
   mettre. Ce ne sont pas des réglages de l'application : ce sont des rituels
   hors d'Emufii, sans lesquels une session est refusée.
3. **Un détail déplié n'est pas une page.** Le profil PS2 ouvert, c'est une
   note, deux boutons, un creux d'état à quatre faits et une réserve — dans une
   fente de 18 dp coincée entre deux autres rangées. Le contenu réclamait une
   page depuis longtemps ; la rangée dépliante l'y refusait.

Donc : un **hub** qui ne contient que des entrées, et **sept pages** derrière —
Profil, Bibliothèque, Consoles, Émulateurs, Apparence, Général, À propos. Le hub
est la racine de cet écran ; `B` y revient avant de quitter les réglages, comme
il fermait la rangée dépliée avant.

Ce qui **n'a pas changé** : le contenu des blocs. `DetailNote`, `DetailActions`
et `DetailStatus` sont exactement ceux de l'accordéon, aux mêmes trois règles
(cf. `ui/components/SettingsDetail.kt`). Une page est la même matière, sans la
fente. Ce qui disparaît, c'est la mécanique de dépliage : les coins qui se
morphaient, le `bringIntoView` retardé, l'énumération de la rangée ouverte.

## Une entrée du hub est une plaque, pas une rangée

Chaque entrée est sa **propre `SoftCard`**, et non une rangée dans une carte
commune. Ça a supprimé d'un coup toute la machinerie que la carte partagée
imposait : les coins qui s'animaient pour dire quelle rangée hérite de ceux de
la carte, et le remplissage opaque tranché dans le dégradé de la carte pour que
la lueur du curseur reste dehors. Une plaque est déjà opaque, et `SoftCard` pose
déjà l'anneau dans sa forme, avant son propre `clip`.

Le remplissage tranché **reste** pour ce qui vit encore à l'intérieur d'une
carte — les choix d'une liste (langue), qui sont des rangées dans un bloc.

Et ce ne sont **pas des tuiles**. La grille de tuiles est la grammaire de la
bibliothèque, où le contenu est la jaquette et où l'œil vise une image. Une page
de réglages n'a rien à montrer, elle a des noms à lire ; un nom se lit dans une
rangée, à la vitesse où on cherche un mot dans une liste. La variante en grille
de tuiles a été écrite puis retirée le 2026-08-25 : elle reproduisait le menu
principal pour un contenu qui n'est pas du contenu.

## Le hub tient sur un écran, sans intitulés de groupe

Le hub a d'abord porté quatre intitulés de groupe, puis les a perdus, puis les a
retrouvés — et les trois états ont été vus sur l'appareil, ce qui vaut d'être
raconté dans l'ordre.

**Ils coûtaient deux fois.** Le deuxième répétait mot pour mot le nom de
l'entrée qu'il coiffait — « Bibliothèque » au-dessus de « Bibliothèque » — et
les quatre ensemble prenaient assez de hauteur pour qu'il ne reste que **deux
entrées et demie** visibles sur les 1080 px de la Thor. Retirés, quatre et demie
tenaient, et les familles se lisaient à l'écart.

**Ce n'était pourtant pas le principe qui clochait, c'était le mot.** Un menu de
réglages sans rayons est une liste, et une liste de sept entrées se parcourt au
lieu de se viser. Un intitulé doit nommer **la famille**, jamais son premier
membre : « Bibliothèque et consoles » coiffe ses deux entrées sans répéter ni
l'une ni l'autre, et le problème de répétition disparaît sans que le hub perde
ses rayons.

Les quatre noms retenus : « Toi », « Bibliothèque et consoles », « Avant de
jouer », « L'application ». Aucun n'est le titre d'une page.

L'écart est serré entre deux entrées d'une même famille (10 dp) et ouvert entre
deux familles, intitulé compris (18 dp) : c'est le groupement qui porte la
lecture, l'intitulé ne fait que le nommer.

## La pastille du hub reprend la perle, elle n'en invente pas une seconde

Une entrée qui mène à quelque chose à préparer porte une pastille : la **perle
d'état de `DetailStatus`**, les mêmes quatre tons et les mêmes quatre glyphes,
plus le mot que la perle seule ne porte pas. Une application n'a le droit de
dire « c'est bon » que d'une seule façon, et un écran de réglages qui aurait
inventé une deuxième pastille l'aurait enseignée deux fois.

Deux entrées n'en portent **pas**, et c'est délibéré :

- **Consoles** : masquer une console est un goût, pas un état à rattraper. Une
  pastille verte y dirait « rien à faire » sur une page où il n'y a jamais rien
  à faire. Le compte (« 7 consoles sur 7 ») tient dans le résumé, où il se lit
  comme un fait et non comme un verdict.
- **Apparence**, **Général**, **À propos** : leur résumé *est* leur état.

**Émulateurs** ne passe au vert qu'à `3 / 3`. C'est la seule page qui demande
quelque chose au joueur, et « 2 / 3 » en vert se lirait comme « rien à faire ».

## Une icône par page, et pas une de plus

Sept marques dessinées (`TrayIcons.kt`), une par page. Dans un menu où toutes
les rangées ont la même forme, l'œil retrouve une page à sa silhouette avant
d'en lire le nom — c'est la seule raison qu'elles ont d'exister, et c'est
pourquoi il n'y en a pas une huitième pour décorer.

Elles suivent les trois règles du système : boîte de 24 unités, bouts et
jointures ronds, une seule graisse de trait. Deux détails de dessin :

- La marque des **consoles** ne dessine que **trois** tuiles sur quatre. Le trou
  est la console masquée, et il dit ce que la page fait mieux qu'une quatrième
  tuile ne le dirait.
- Chaque marque est posée **dans une alvéole** ronde. À nu sur la plaque, une
  icône flotte, et sept icônes flottantes alignées se lisent comme de la
  décoration ; dans un creux, chacune est un objet moulé de plus.

Elles sont à l'**encre atténuée, jamais à l'accent** : l'accent ne veut dire
qu'une chose, « c'est ici », et sept marques cyan le lui retireraient
(cf. `direction-visuelle.md` § La règle du seul accent). La seule couleur du hub
est l'avatar du joueur, qui vient du contenu.

## Les émulateurs ne sont pas un réglage de l'application

La page **Émulateurs** rassemble les trois préparations qui se font hors
d'Emufii : le paramétrage de PPSSPP, le profil réseau PS2 à importer dans
ARMSX2, et le service d'accessibilité qu'Azahar utilise pour le remplissage
automatique. Elles n'ont rien en commun avec la langue ou le thème, et tout en
commun entre elles : ce sont les trois choses qu'Emufii **demande** au joueur,
et une session est refusée tant qu'elles ne sont pas faites.

C'est aussi la seule page dont l'état se compte, d'où le `3 / 3` du hub.

## La remise à zéro vit sur la page qu'elle efface

Elle avait sa propre section, « Zone rouge », pour une seule rangée. Une section
entière pour un geste qu'on fait une fois dans la vie de l'app était déjà
généreux sur un écran unique ; sur un hub de sept entrées, ça aurait fait une
huitième entrée dont la seule fonction est d'être dangereuse.

Elle est donc en bas de la page **Profil**, sous l'identité qu'elle efface — au
rouge coque, sans chevron qui tourne, et la confirmation qui suit porte
l'avertissement. C'est le seul rouge de cet écran.

## Un réglage qui n'a que deux états est un interrupteur

Les notifications et le second écran se pilotaient par des **boutons dont le
libellé change** : « Amis coupés » devenait « Amis activés » quand on le
pressait. Un tel bouton pose une question à chaque lecture — est-ce qu'il décrit
l'état, ou l'action qu'il déclenche ? On se la pose une demi-seconde avant de le
presser, à chaque fois, et un interrupteur ne la pose jamais.

L'interrupteur est **moulé, pas Material** : la piste est la même alvéole que
les champs de saisie et les emplacements vides de la grille, le bouton est la
même plaque que tout le reste. Le `Switch` de Material dessine une piste teintée
et une pastille plate, qui sur une plaque moulée se lit comme un autocollant, et
il peint un voile de focus — éteint partout dans cette app — qui se lit comme
« désactivé » sur une console où le curseur est en permanence quelque part.

Trois détails payés :

- **Le bouton est toujours la plaque claire**, quel que soit le thème. En plaque
  sombre sur thème sombre, il se lisait comme un trou de plus dans l'alvéole au
  lieu du bouton qui coulisse dedans.
- **Allumé, c'est le creux qui prend l'accent**, pas le bouton : l'accent y veut
  dire « en marche », et le curseur garde son anneau pour dire « ici ».
- **Toute la rangée est la cible**, pas la pastille de 52 dp au bout de la
  ligne : la viser à la manette est un travail, et à deux mains sur une console
  c'est le mauvais geste. L'interrupteur ne porte donc pas son propre anneau.

Il n'y en a **qu'un dans l'app**. La carte de lancement avait le sien, un
`Switch` de Material, dernier contrôle Material resté à l'écran ; il a pris
celui-ci.

## Apparence se compare d'un seul coup d'oeil

Les quatre plateaux et les huit perles tenaient dans un seul bloc, l'un au-dessus
de l'autre, et la dernière rangée d'accents passait sous la ligne de flottaison
de la Thor. C'est le seul écran de l'app où **tout doit être visible ensemble**,
puisque comparer est exactement ce qu'on y fait : un choix qu'il faut faire
défiler pour voir n'est plus une comparaison, c'est une liste.

Deux blocs côte à côte, le thème et l'accent, et l'ensemble tient. Les perles
passent de deux rangées de quatre à une rangée de huit quand la colonne est
assez large — la grille garde ses colonnes, elle change de nombre.

## La carte du dossier montre le dossier

Le dossier de ROMs est ce que ce bloc contient de plus important, et il tenait
sur une ligne étiquette-valeur perdue en travers d'une demi-colonne vide : la
carte était, mot pour mot, « ROM folder / Folder ROMS » et deux boutons.

Il est maintenant **l'objet du bloc** : la marque de dossier, le nom que le
joueur a choisi, et sous lui ce qui est vrai de ce dossier — les sous-dossiers
sont explorés aussi, ou bien, quand il n'y en a pas encore, ce qu'on attend de
lui. Le compte de jeux vit dans la pastille de l'en-tête, où il est un état.

## Renoncer à Cocoon demande un nouveau parcours

`CocoonMedia.forget()` ne vide que l'**index** de Cocoon. Les vignettes écrites
pendant le scan, elles, restent sur le disque : après avoir renoncé à Cocoon, la
bande d'aperçu et la grille continuaient d'afficher ses images, et le réglage
avait l'air inerte alors qu'il avait bien pris.

Changer de source d'images — la choisir comme y renoncer — déclenche donc un
nouveau parcours. C'est la seule façon que la source change vraiment, et c'est
aussi ce qui fait bouger l'aperçu sous les yeux du joueur.

## Les deux liens sortants, et leur ordre

Ce sont les deux seuls liens sortants de toute l'application, et ils vivent sur
« À propos » — jamais dans un dialogue, jamais au lancement.

Le Discord est **rempli**, le Ko-fi est un fantôme, et l'ordre n'est pas
alphabétique : le Discord est le seul des deux qui rende quelque chose au
joueur — quelqu'un avec qui jouer, un endroit où signaler ce qui casse. Une app
qui demande de l'argent plus fort qu'elle n'offre de l'aide se lit comme un
guichet.

Chacun porte **la marque de sa destination**, avant son libellé. Celle de
Discord est la marque officielle, à son bleu ; celle de Ko-fi est une tasse
redessinée dans la langue d'icônes de l'app — boîte de 24 unités, un seul trait
— et ne prétend pas être le logo du service, dont le nom est écrit en toutes
lettres juste à côté. Ces marques ne sont pas teintées par l'accent : elles
désignent un ailleurs, donc c'est du contenu, comme l'icône d'une console.

Deux choses ont été écrites puis **retirées** de cette page :

- **Le paragraphe au-dessus des boutons.** Il disait à quoi sert le Discord et
  où va l'argent. C'était vrai, et personne ne le lit avant de presser le bouton
  qu'il coiffe : deux boutons dont le libellé porte déjà leur destination n'ont
  rien à faire expliquer.
- **La carte des sept consoles.** On visite « À propos » pour connaître une
  version ou trouver un lien ; la liste des consoles est déjà dans la grille,
  dans la page Consoles et sur chaque tuile. Une image n'a sa place que si elle
  répond à la question que sa page pose.

Il reste donc deux cartes, côte à côte, du même haut et du même pied.

## Aligner deux colonnes demande de mesurer, pas d'intrinsèque

Sur « À propos », la colonne de gauche porte deux blocs et celle de droite un
seul. Laissé à lui-même, le bloc solitaire s'arrête à mi-hauteur et laisse un
trou de trois cents pixels au pied de la page.

`Modifier.height(IntrinsicSize.Min)` est la réponse évidente, et elle est
**fausse ici** : la hauteur intrinsèque minimale d'un paragraphe est celle qu'il
ferait à la largeur de son mot le plus long, donc énorme. Essayé, mesuré sur
l'appareil — la rangée prenait deux écrans et demi de haut et le bloc solitaire
s'étirait dedans.

Ce qui marche est bête et solide : mesurer les deux colonnes (`onSizeChanged`)
et imposer **la plus grande des deux comme minimum** aux deux. La première image
les voit à zéro, la suivante les a, et rien ne saute parce qu'une carte ne fait
que grandir.

Le mot *minimum* est le second piège, payé après le premier : imposer la hauteur
mesurée à gauche comme **taille** à droite a écrasé le dernier bouton de la
carte de droite en un trait de trois pixels. Une hauteur imposée découpe ; un
minimum laisse grandir.

Et le bloc étiré ne répartit pas tout son contenu : seul son **pied** descend.
Une répartition uniforme écartait aussi le texte de son titre, et le bloc avait
un trou au milieu. D'où le slot `footer`, et l'écart qui ne va qu'entre les deux.

## Une console éteinte est un trou dans le plateau

Les sept tuiles étaient des plaques dans les deux cas, avec une barrette
d'accent dessous pour dire laquelle était allumée. Deux tuiles voisines se
ressemblaient à quatre dp près, et sur les thèmes sombres la barrette était la
seule chose à lire sur une grille entière.

Le plateau sait déjà dire deux choses : « posé dessus » (`plate`) et « creusé
dedans » (`socket`). C'est exactement la distinction que cette page fait, alors
elle l'emprunte : **une console affichée est une plaque, une console masquée est
un trou.** La barrette disparaît — deux marques pour un seul état sont du bruit
— et l'atténuation du contenu reste, pour que la tuile éteinte nomme quand même
sa machine.

## La grille des consoles n'a pas le droit à une orpheline

Prendre le maximum de colonnes qui tient était le réflexe, et il donne le pire
résultat du lot : sept consoles dans une carte qui en porte six, ça fait six
tuiles puis **une seule** sur la ligne suivante. Une orpheline se lit comme un
oubli, pas comme une grille.

On choisit donc le nombre de colonnes qui remplit le mieux le dernier rang —
sept dans six colonnes devient **quatre plus trois** — et le rang incomplet se
termine en alvéole vide, comme la bibliothèque le fait déjà. Quand tout tient
sur une ligne, ça reste une ligne : c'est la mise en page pour laquelle la page
d'accueil a été dessinée.

Deux conséquences de forme, toutes deux mesurées sur l'appareil :

- **La tuile a une hauteur fixe.** Les alvéoles du dernier rang doivent faire la
  même, et une hauteur intrinsèque ne se partage pas entre frères sans mesurer.
  La valeur est celle du contenu — une icône et trois lignes ; huit dp de moins
  et le numéro de version se faisait couper.
- **La phrase d'explication est bornée, et il n'y en a plus qu'une.** Elle
  courait sur toute la largeur de la carte, près de 1700 px, où l'œil perd la
  ligne avant d'en trouver la fin. La seconde — « éprouvé aux versions affichées
  ici » — disait ce que les numéros sous chaque tuile disent déjà, et c'est elle
  qui faisait passer la page sous la ligne de flottaison.

Et le bloc a **un titre**, qui n'est pas celui de la page : sans lui, la
pastille d'état se retrouvait seule au bout d'une ligne vide, où elle se lisait
comme un accident.

## Sur une page, l'état passe devant l'explication

L'accordéon présentait un réglage dans cet ordre : un paragraphe, puis les
boutons, puis l'état tout en bas dans son creux. C'était juste **pour une
rangée dépliée** — on venait de demander à ouvrir, donc on venait apprendre.

Sur une page, tout est déjà ouvert, et ce que le joueur vient vérifier c'est
**où ça en est**. Mesuré sur l'appareil : le bloc PPSSPP ouvrait sur quatre
lignes pleine largeur de prose technique avant que le mot « prêt » n'apparaisse,
et le bloc à lui seul remplissait les 1080 px de la Thor.

L'ordre est donc renversé, et il tient en une phrase : **le nom et l'état en
en-tête, ce qu'on peut faire ensuite, l'explication en dernier — et seulement
tant qu'elle apprend quelque chose.** Une fois le dossier choisi, les étapes
disparaissent ; il reste le fait (quel dossier) et la réserve (quitter le jeu
avant de changer de mode).

Deux conséquences de forme :

- **Une explication de méthode se donne en étapes numérotées, jamais en
  paragraphe.** Quatre phrases techniques d'affilée ne se lisent pas, elles se
  sautent ; les mêmes faits numérotés se parcourent, et le format force à les
  écrire courtes.
- **Le creux d'état disparaît du cas ordinaire.** `DetailStatus` posait un
  creux par bloc ; l'état étant désormais dit par la pastille de l'en-tête, il
  ne restait qu'un conteneur de plus dans un empilement qui en comptait déjà
  trois. Les faits sont des lignes `BlockFact` — étiquette à gauche, valeur à
  droite — et le creux est réservé à ce qui est vraiment un aparté.

## Deux colonnes, une fois l'accordéon parti

La colonne unique était la bonne réponse **tant que les rangées se dépliaient** :
une hauteur qui change rouvre un trou entre deux colonnes à chaque geste, et
c'est ce que dit la section « Une seule colonne, bornée et centrée » plus bas.
Une page ne change plus de hauteur.

Ce qui restait, alors, c'était une colonne bornée à 620 dp au milieu des 850 dp
que la Thor offre en paysage : un quart de l'écran vide à droite de chaque bloc,
et trois écrans à faire défiler là où il en faut un.

Donc deux colonnes dès que la largeur les porte (700 dp), une seule en dessous.
Deux règles :

- **Un bloc appartient à une colonne entière**, il n'est jamais coupé au
  milieu : sinon son état et ses boutons finissent de part et d'autre de la
  gouttière.
- **La page se borne à la largeur des deux colonnes**, pas à celle d'une seule.
  Le piège, payé une fois : la mesure qui décide du nombre de colonnes se fait
  *à l'intérieur* de la coquille, donc une coquille qui serre déjà à 620 dp lui
  fait toujours répondre « une seule ». Ce qui veut rester étroit — le hub — se
  borne lui-même.

## Un avertissement n'est pas une erreur, et ne porte pas le rouge

Le rouge coque n'apparaît que deux fois dans toute l'app, et c'est pour ça qu'il
se lit quand il apparaît. La note de carte dossier de la PS2 le portait : six
lignes de rouge sous une pastille verte « Prêt », vues sur l'appareil, et on
relit la pastille pour savoir laquelle des deux ment.

Or cette note n'est pas un échec — c'est la raison pour laquelle aucune
sauvegarde du joueur n'a été clonée, donc la seule chose qu'il doit absolument
lire. Deux objets, plutôt qu'un :

- `BlockCaveat`, rouge et court : quelque chose a échoué.
- `BlockNotice`, un creux avec la perle d'avertissement et l'encre ordinaire :
  quelque chose est à savoir pendant que tout va bien.

Le creux dit « voilà ce qui est », la perle dit de quel poids, et le texte ne
crie pas.

## Les images des pages viennent de l'appareil, pas d'une banque

Ces pages parlaient d'images sans jamais en montrer. Quatre endroits où ça
coûtait quelque chose, et quatre images qui existaient déjà :

- **Les icônes de jeu** montrent une bande de cinq jaquettes **de la
  bibliothèque du joueur**, prises dans le cache que l'app a déjà chauffé au
  démarrage. Le bloc annonçait « Cocoon est en vigueur » et il fallait aller
  vérifier dans la grille ; ici on voit ce que la grille affiche, à l'endroit
  où on en change la source. C'est aussi la seule couleur de tout cet écran, et
  elle vient du contenu, comme le veut la direction.
- **Les blocs d'émulateur** portent l'icône de l'application installée. « PPSSPP »
  en titre ne dit pas si PPSSPP est là ; son icône, oui — et l'alvéole vide, quand
  elle manque, le dit aussi bien.
- **« À propos »** montre les sept consoles servies, par les mêmes fichiers que
  la bibliothèque utilise. La table qui les associe a été hissée hors de
  `LibraryScreen` : deux copies auraient divergé au premier ajout.
- **« Ce que les autres voient »** montre la rangée que les autres joueurs
  reçoivent — et **sans la photo**. La première version affichait l'avatar
  local, ce qui donnait exactement le contraire de la phrase en dessous : la
  photo ne quitte pas l'appareil, les autres reçoivent les initiales et la
  couleur. Un aperçu qui contredit sa légende est pire que pas d'aperçu.

Rien n'est téléchargé, rien n'est dessiné pour l'occasion, et aucune de ces
images n'est décorative : chacune répond à la question que son bloc pose.

## Une seule colonne, bornée et centrée

Deux colonnes avaient l'air d'être la bonne réponse à un écran large, et ne le sont
pas : **quatre sections de longueurs différentes ne se partagent jamais également**,
donc l'une finit plus courte que l'autre et laisse un trou de trois cents pixels que
rien ne remplit. Le problème n'est pas l'appariement, il est structurel — et une
rangée qui se déplie change de hauteur, ce qui **rouvre le trou à chaque ouverture**
même quand l'équilibre était bon au repos.

**Bornée**, parce qu'une colonne unique étirée sur 1920 px met l'étiquette et sa
valeur aux deux bords opposés de l'écran, et la paire cesse d'être lisible.
**Centrée**, parce qu'un bloc borné collé à gauche laisserait le vide qu'on vient
justement de supprimer, simplement déplacé.

## Les trois constantes de forme d'une rangée

- **La largeur maximale** : au-delà, l'étiquette à gauche et la valeur à droite se
  retrouvent aux extrémités et l'œil ne les apparie plus.
- **Le rayon de coin**, petit : à 52 dp de haut, un grand rayon donne une gélule
  posée dans une carte aux coins bien plus francs. **C'est aussi le rayon de
  l'anneau**, puisque le curseur trace le contour de la rangée.
- **Le retrait latéral** : c'est la largeur d'une rangée de réglages, les
  séparateurs la dessinent et l'anneau doit s'y poser. **Une constante pour les
  deux**, sinon ils divergent au premier ajustement.

## Une rangée occupe toute la carte, bord à bord

**Aucune marge autour.** En laisser ne serait-ce que quelques dp produisait une
bande blanche entre le curseur et le bord, moment où le curseur n'entourait plus
rien. En échange, chaque rangée prend la forme exacte de la place qu'elle occupe,
**coins de carte compris** ; le trait de l'anneau est dessiné à l'intérieur de ses
bornes, donc rogner la carte ne l'entame pas.

Corollaire, appris à la dure : **pas de retrait sur l'anneau non plus.** Le rétrécir
pour aligner le contour sur les séparateurs enfermait une boîte plus petite que la
rangée, et le trait passait au milieu de l'étiquette. **Le retrait appartient au
texte**, il est donc appliqué plus bas, à l'intérieur.

La forme suit la place : une rangée du milieu est un rectangle franc, celles des
extrémités héritent des coins de la carte. **Ouverte, elle s'arrondit partout** :
elle se détache de la pile, elle devient l'en-tête de ce qu'elle vient de révéler,
et un coin franc s'y lit comme une coupure — en haut contre la rangée précédente
autant qu'en bas contre son propre détail. Les deux extrémités suivent donc la même
règle et **se transforment progressivement plutôt que de claquer**, sinon la forme
saute au moment précis où le contenu se déroule.

**Le séparateur est dessiné au-dessus, pas en dessous** : une rangée dépliée pousse
son détail vers le bas, et une ligne posée après elle finirait par séparer le détail
de la rangée suivante au lieu de séparer deux rangées.

## Le remplissage opaque existe pour le curseur, pas pour le look

La lueur est une ombre portée du contour du contrôle, et **une ombre projetée par
une couche non opaque est dessinée *à travers* elle** : une rangée focalisée se
retrouvait remplie d'un lavis d'accent, vive sur ses bords arrondis et **creuse au
milieu**. Rien ne découpe une ombre hors de son propre contour ; la seule chose qui
la cache est du contenu opaque par-dessus.

Une couleur plate aurait fait ce travail et en aurait cassé un autre : **chaque
rangée figerait le dégradé à son propre sommet**, et une carte de cinq rangées
deviendrait cinq bandes. Découper le dégradé de la carte coûte pareil et ne se voit
pas.

D'où la nécessité de connaître **la carte dans laquelle l'appelant dessine**, en
coordonnées racine et non celles d'un parent : les choses qui en ont besoin sont à
des profondeurs différentes — une rangée est fille directe de la carte, un choix
dans un détail déplié est trois niveaux plus bas.

## Deux pièges de focus, et leurs contournements

**Le drapeau « première rangée » est un drapeau, pas un modificateur passé de
l'extérieur.** Le `Modifier` de cette rangée s'applique à la colonne qui contient la
rangée *et* son détail, et un `FocusRequester` posé là vise un nœud qui n'est pas
focalisable : la demande échoue **en silence**. C'est la rangée cliquable qui doit le
porter, et elle est privée.

**Déplier n'amène pas le contenu à l'écran tout seul.** Le curseur reste sur la
rangée : rien ne bouge du point de vue du focus, donc le défilement automatique n'a
aucune raison de se déclencher, et le contenu qu'on vient de demander s'ouvre sous
la ligne de flottaison. On le demande donc explicitement, **et sur la colonne
entière — rangée *et* détail** — faute de quoi on ne ramène que la rangée, qui était
déjà visible. Et **après l'animation d'ouverture, pas pendant** : la colonne mesure
encore sa hauteur précédente au moment où l'état change.

L'identité d'une rangée dépliable est **une énumération, pas un index** : l'ordre des
sections se réorganise entre portrait et paysage, et un index aurait ouvert la
mauvaise rangée en tournant l'appareil.

**Un chevron qui ne tourne pas dit « c'est ailleurs ».** Le thème ouvre son propre
panneau plutôt que de se déplier — la façon d'un écran de dire la différence entre
« c'est en dessous » et « c'est ailleurs ».

## Les lignes d'état, et ce que personne ne devinerait

**Le second écran** : sans sa ligne d'état, la rangée est une promesse que le joueur
ne peut pas vérifier — il l'active, rien ne se passe, et il ne peut pas savoir si la
fonction est cassée ou si son appareil n'a qu'un écran. Nommer le panneau trouvé, ou
dire qu'il n'y en a aucun, répond avant qu'il aille chercher.

**Les notifications** : Emufii est installé de côté, donc **aucun service de push ne
peut le réveiller**. Hors de l'app, c'est Android qui décide quand laisser la veille
regarder, et c'est un quart d'heure au mieux. Le dire ici coûte une phrase et achète
la confiance du joueur dans chaque alerte qui arrive ; l'omettre ferait paraître la
fonction cassée la première fois qu'une alerte est en retard.

**Le service d'accessibilité est relu tant que cet écran est affiché**, pas une seule
fois : partir vers les réglages d'Android est un voyage hors de l'app, pas une boîte
de dialogue avec un résultat — **la réponse n'existe qu'au retour**. Interrogé
périodiquement, ce qui coûte moins qu'un observateur de cycle de vie pour un booléen,
et c'est ce qui fait passer la rangée au vert sous les yeux du joueur.

## Le remplissage automatique a sa rangée parce qu'Android peut l'éteindre

Un service d'accessibilité **n'est pas une permission que l'app détient** : c'est un
réglage système que le joueur a accordé, et que le système retire de lui-même — une
mise à jour, une restauration sur un nouvel appareil, un optimiseur de batterie.

L'écran de session portait auparavant le chemin du retour, sous forme d'un bouton
**qui n'apparaissait qu'une fois l'automatisation déjà éteinte**, au bas d'une carte
que personne ne fait défiler. Sa place est ici : un interrupteur qu'on bascule une
fois dans la vie de l'app est de la plomberie, et la plomberie vit dans les réglages.
**La rangée affiche l'état qu'il soit allumé ou éteint**, ce qui la rend trouvable
*avant* que quelque chose n'aille mal plutôt qu'après.

## Ce que les réglages disent de ce qu'Emufii ne fait pas

La rangée des clés de console **dit délibérément ce qu'Emufii ne fait pas** — fournir
des clés, en télécharger, envoyer le fichier où que ce soit — parce que **demander un
fichier de clés sans explication est la façon dont une app se fait désinstaller**.

La clé SteamGridDB est **en clair et non masquée** : ce n'est pas un mot de passe,
elle n'ouvre qu'un catalogue d'images publiques, en lecture seule. La masquer
gênerait surtout le repérage d'une faute de frappe, **le seul incident probable** —
une mauvaise clé ne dit rien, elle ne ramène simplement rien.

Le dossier de ROMs et son bouton de rescan étaient des pastilles du dock de la
bibliothèque, **en permanence devant quelqu'un qui avait choisi son dossier des mois
plus tôt**. Une plomberie qu'on règle une fois appartient aux réglages.

## Le thème ouvre un panneau, il ne se déplie pas

L'aspect de l'app n'est pas un détail de ligne de réglages. C'étaient **neuf choix
étiquetés empilés dans la carte**, poussant tout ce qui suivait hors de l'écran, et
demandant au lecteur d'imaginer à quoi chaque nom ressemblait. Sa valeur nomme
toujours les deux moitiés du choix, donc la rangée dit où en sont les choses **sans
être ouverte**.

## Un bouton est nommé pour ce qu'il fait

De deux actions, **une seule est remplie** : celle qui fait le travail. En pastilles
de même poids, elles se lisaient comme un choix entre égales, ce qui envoyait les
gens vers le sélecteur de dossier qu'ils venaient de traverser.

Une fois le travail fait, **l'accent s'en va plutôt que l'étiquette ne se transforme
en vantardise**. Un bouton disant « profil installé » qui relance quand même toute la
préparation ment, et un bouton qui ne fait rien n'est pas un bouton. Le travail se
rétrograde donc en course ordinaire — le refaire — et c'est le creux en dessous qui
dit qu'il est installé. **La section cesse de demander quelque chose**, ce qui est le
changement que l'œil cherche.

## Restaurer les jeux retirés est tout ou rien

Une liste des jeux retirés aurait besoin de leurs titres et de leurs icônes, lus dans
des fichiers que cet écran ne scanne jamais : elle montrerait donc **des chemins**. Et
un joueur qui a retiré trois copies régionales d'un même jeu ne gagne rien à choisir
entre trois lignes identiques.

Tout ramener coûte une suppression de plus à refaire, **et ça marche toujours**.
