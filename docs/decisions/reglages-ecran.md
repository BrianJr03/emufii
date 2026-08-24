# L'écran des réglages : des rangées qui se déplient

Le récit qui vivait dans `ui/screens/SettingsScreen.kt`, sorti du code le
2026-08-24 (cf. `docs/STYLE_COMMENTAIRES.md`). Titres = ancres citées depuis le
code.

## Ce que cet écran remplace

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
