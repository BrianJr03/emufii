# Piloter les émulateurs : le service d'accessibilité, et ce qu'il a coûté

Le récit qui vivait dans `azahar/AzaharNetplayService.kt`, sorti du code le
2026-08-24 (cf. `docs/STYLE_COMMENTAIRES.md`). Titres = ancres citées depuis le
code.

## Pourquoi un service d'accessibilité, et pas autre chose

**Aucun des émulateurs n'expose d'IPC pour le multijoueur.** Le manifeste
d'Azahar n'exporte que `MainActivity` et `EmulationActivity`, et le netplay vit
derrière JNI (`netPlayCreateRoom` / `netPlayJoinRoom`) sans aucun extra d'intent.
Écrire les SharedPreferences demanderait le root ou `run-as`. **Piloter l'interface
est le seul chemin qui marche sur une build non modifiée, installée de côté.**

Le service est **inerte** tant qu'aucun plan n'est armé : il ne fait rien de
lui-même, et ne touche jamais qu'aux paquets visés.

Il est **au mieux-effort par conception.** Azahar est une cible mouvante : quand un
identifiant de ressource bouge, on s'arrête plutôt que de cliquer n'importe où, et
l'interface retombe sur l'affichage de l'adresse à saisir à la main.

## La classe garde son nom d'Azahar, et ce n'est pas de la négligence

Elle sert aussi Eden désormais. **La renommer changerait le `ComponentName`, et
c'est le `ComponentName` qu'Android enregistre quand l'utilisateur active le
service** : un renommage désactiverait donc l'automatisation, en silence, chez tous
ceux qui l'avaient déjà activée. Un nom inexact est le moins cher des deux coûts.

## Trois familles d'écran, trois pilotes, un seul standard

Azahar et Eden partagent une marche fondée sur les identifiants de ressource.

**Dolphin ne peut pas y entrer** : son écran de netplay est en Compose et n'expose
aucun identifiant, donc rien de lui ne s'exprime dans cette marche. Il a son propre
objet, ce qui garantit que les chemins 3DS et Switch ne peuvent pas être atteints —
encore moins modifiés — par quoi que ce soit que fasse Dolphin. Il met aussi en
cache les libellés de l'émulateur, donc il doit survivre à un événement isolé.

**La PS2 est la troisième forme d'écran**, et elle reçoit le moyen de **relire
l'arbre** : saisir une valeur dans ARMSX2 veut dire une douzaine de clics d'affilée
sur son propre clavier, et l'écran se redessine à chaque touche. Les deux autres
pilotes n'en ont pas besoin, écrivant leurs champs d'un seul `ACTION_SET_TEXT`.

Un paquet qu'aucun des trois ne connaît repart sans que rien n'ait été touché.

## Agir sur les événements ne suffit pas : il faut regarder à nouveau

Agir sur les seuls événements suppose que le dernier événement d'un écran arrive
**après** que cet écran est utilisable. La feuille multijoueur d'Azahar prouve le
contraire : elle glisse à l'écran, tous les événements partent pendant que ses
boutons sont encore hors champ, puis plus rien n'arrive — un flux qui venait
d'ouvrir la feuille correctement restait donc à la contempler. Vu sur la Thor, et
**indiscernable de « l'automatisation n'a jamais tourné »**.

Quelques relectures espacées ne coûtent rien quand il n'y a rien à faire, et ce sont
les seules choses qui rattrapent une vue arrivée en retard.

**Une passe qui a progressé récupère son budget de relecture.** Sans ça, le nombre
de regards suivant un événement plafonne la longueur de la route — et la PS2 dépasse
largement ce plafond : menu, réglages, onglet, mode, deux défilements, puis un champ
ouvert, vidé, tapé lettre à lettre, confirmé. Azahar et Dolphin tiennent en trois ou
quatre écrans et n'y avaient jamais buté. Le pilote s'arrêtait donc à mi-chemin,
après un défilement, sans un mot : vu de l'extérieur, « l'installation automatique ne
marche pas ». Renouveler le budget n'ouvre pas de boucle sans fin, chaque pilote
ayant ses propres plafonds.

## Un plan armé ne doit pas rendre le jeu inutilisable

Les étapes de navigation — l'entrée du tiroir en jeu, l'onglet des réglages, la
carte Multijoueur — sont les seules qui se déclenchent sur un écran que **le joueur**
a ouvert pour ses propres raisons.

Un plan armé qui n'atteint jamais un formulaire de salon **recliquait donc
Multijoueur à chaque apparition du tiroir en jeu** — c'est-à-dire exactement quand le
joueur essaie d'atteindre Quitter : le tiroir devenait inutilisable. Remonté depuis
la Thor.

Un plafond transforme « pour toujours » en « deux ou trois essais, puis on s'écarte ».
Remplir un formulaire déjà à l'écran reste illimité : celui-là ne se déclenche que là
où le joueur fait ce qu'on lui a demandé.

## La visibilité est le bon filtre pour se situer, le mauvais pour agir

La visibilité distingue « cet écran est devant » de « cet écran existe quelque part
dans la hiérarchie » : elle conditionne donc toute décision sur **où l'on est**.

Mais c'est le mauvais filtre dès qu'il s'agit d'agir sur un formulaire dont on a
déjà décidé qu'il est devant. Deux pannes réelles, la même cause :

- **Le bouton OK est d'ordinaire sous la ligne de flottaison.** Le formulaire est
  une feuille du bas qui défile, et sur la boîte « créer » d'Azahar — un champ de
  plus que « rejoindre » — sur un écran en paysage, OK démarre hors champ. Une
  recherche filtrée sur la visibilité ne trouvait rien, et Emufii retombait sur
  « les champs sont remplis, appuyez sur OK vous-même » : c'est le fameux « le clic
  ne prend pas » que ce projet a poursuivi — **il n'y avait jamais eu de clic**.
- **Le bouton de l'hôte était introuvable.** La feuille empile Lobby / Join /
  Create, et une feuille du bas sur un écran paysage coupe le dernier. Le bouton de
  l'hôte était donc celui que la recherche ne trouvait jamais, pendant que celui de
  l'invité s'affichait confortablement : l'automatisation avait l'air d'avoir une
  idée cassée de qui héberge, alors qu'elle avait simplement la même recherche
  filtrée qui cachait déjà `btn_confirm` et `room_name` sur cet appareil.

**Le remède est le même dans les deux cas : amener le nœud à l'écran, puis
presser.** Et la feuille est « devant » dès que **l'un quelconque** de ses trois
boutons est visible — trois plutôt qu'un, parce que seul celui du haut est
fiablement en vue, et lequel c'est appartient à la mise en page de l'émulateur, pas
à nous.

## Un clic qui ne prend pas n'est pas un succès

Le bouton OK d'Eden se déclare activé et cliquable **et ignore quand même
l'action** — vu sur un appareil, boîte laissée ouverte. Croire notre propre requête
dirait au joueur que tout est fait alors que le salon n'a jamais été créé.

On dit donc ce qui s'est réellement passé : les champs sont remplis, la dernière
pression lui revient.

## L'hôte crée, l'invité rejoint, et c'est journalisé

Un invité qui crée son propre salon ne rejoint rien ; un hôte qui rejoint cherche un
salon que personne n'a ouvert. **Journalisé, parce que les deux échecs sont
identiques vus de l'extérieur** — une boîte qui se remplit puis refuse — et que le
seul moyen de les distinguer est de savoir quel bouton a été pris.

## Le pseudo n'est écrit que sur Eden

Deux joueurs de même pseudo ne peuvent pas partager un salon, et Eden livre le même
à tout le monde par défaut : sans ça, deux joueurs Emufii s'y présentent comme la
même personne et le second est refusé.

**Sur Azahar le plan le laisse nul, et ce n'est pas un oubli.** Emufii y écrivait le
nom de profil, ce qui remplaçait un pseudo valide par un pseudo de deux lettres que
le formulaire refusait — « Invalid address or name is too short! », une faute
imputée à une adresse parfaitement bonne.

## Une liste qui recycle ne contient pas ce qu'on n'a pas encore vu

Les rangées du hub de réglages portent toutes les mêmes identifiants, donc la rangée
se trouve **par son texte** — celui de l'émulateur, lu dans ses propres ressources,
pour que ça marche quelle que soit sa langue.

Deux choses à survivre, vues toutes deux sur la Thor avec Eden :

- la rangée peut être défilée sous le bas de la liste, auquel cas elle est dans
  l'arbre mais pas visible — ou bien **pas dans l'arbre du tout**, une liste qui
  recycle ne tenant que ce qu'elle a dessiné. D'où un défilement par passe, compté
  comme un clic pour qu'il ne puisse pas boucler ;
- le libellé peut être **l'une de deux chaînes**, parce que le hub montre un titre
  *et* une description, et qu'une seule des deux est ce que l'amont appelle
  « multiplayer » dans une build donnée.

Échouer d'une façon ou de l'autre était identique vu de l'extérieur : l'émulateur
s'ouvrait sur sa grille de jeux et rien d'autre ne se passait.

## On ramène le joueur chez lui

Il a demandé une étape d'installation, pas un voyage dans une autre app : il a tapé
un bouton dans Emufii et la chose suivante dont il a besoin est le bouton juste en
dessous. Le laisser dans les réglages de l'émulateur l'obligeait à retrouver son
chemin avant de pouvoir lancer la partie.

**Retardé**, parce que l'émulateur est encore en train d'agir sur le clic qu'on vient
de faire : revenir instantanément courrait contre son propre message « rejoint ». Au
mieux-effort : si la plateforme refuse le lancement, le flux est terminé de toute
façon et le joueur revient à la main.

## `typeText` et non `setText` : un an de test vert qui ne prouvait rien

`AccessibilityNodeInfo` a **déjà** un membre nommé `setText`, et en Kotlin un membre
bat toujours une extension. L'extension n'était donc **jamais appelée** : chaque
remplissage partait vers le setter de la plateforme, lequel lève `Cannot perform this
action on a sealed instance` sur tout nœud issu d'une requête — c'est-à-dire tous.

**L'automatisation échouait sur son tout premier champ depuis qu'elle avait été
écrite.** Personne ne l'a vu parce que le menu en jeu d'Azahar n'avait jamais tourné
sur un appareil (M16), et parce que l'émulateur pré-remplit sa propre adresse — ce
qui, pour un hôte, se trouve être la bonne réponse. Un test vert qui ne prouvait
rien.

---

# Le pilote PS2 (ARMSX2)

Sorti de `ps2/Ps2NetplayDriver.kt`.

## Deux singularités qui n'existent nulle part ailleurs

1. **Il n'y a aucun champ de texte.** Ouvrir une rangée fait monter le clavier
   propre d'ARMSX2, et la saisie se fait touche par touche. `ACTION_SET_TEXT` n'a
   rien à viser, et les événements clavier injectés sont ignorés (mesuré).
2. **Ce clavier n'a pas de touche point**, donc l'invité ne peut pas écrire une
   adresse IPv4. Il écrit un **nom**, `emufii`, que le DNS du tunnel résout vers la
   sentinelle du relais (`relay/dns.js`).

L'écran est lu par **rangées** — libellé à gauche, valeur à droite — et c'est le
conteneur qui prend le clic.

## L'ordre des réglages n'est pas cosmétique

Changer le mode redessine la moitié basse de l'écran, et les champs de l'hôte ne
sont pas ceux de l'invité : une valeur écrite avant serait perdue. Un réglage par
passe, dans l'ordre où ils dépendent les uns des autres.

**On descend l'écran et on ne remonte jamais**, d'où une mémoire des étapes déjà
posées : une fois l'interrupteur DEV9 dépassé il n'est plus dans l'arbre, et sans
mémoire le pilote conclurait qu'il reste à le poser. Pour la même raison, un
libellé absent ne veut pas dire « pas d'interrupteur » mais « on l'a déjà
dépassé ».

**Le mode courant ne se lit pas sur le bouton** : mesuré, aucun des trois ne porte
`selected` ni `checked` dans l'arbre. On le déduit des champs présents — comme le
pilote Dolphin distingue ses onglets par l'absence du champ d'adresse.

**Un clic, jamais deux.** Le marqueur qui confirme le mode est plus bas que le
bouton : tant qu'on n'a pas défilé il est absent de l'arbre, et le pilote concluait
qu'il n'avait pas cliqué. Vu en vrai le 2026-08-17 : **huit clics d'affilée** sur
« Host local game » avant que l'écran bouge assez pour le détromper.

L'écran Réseau est reconnu par **n'importe lequel de ses marqueurs**, et non par le
seul interrupteur DEV9 : il est plus haut que l'appareil, il faut le faire défiler,
et un arbre d'accessibilité ne contient que ce qui est réellement dessiné.
S'accrocher au premier libellé revenait à perdre l'écran de vue au tout premier
défilement.

## Un écran en cours d'animation est un écran inconnu

Juste après un clic, l'arbre d'ARMSX2 tombe à une poignée de nœuds pendant que la
page suivante se dessine. Revenir en arrière à cet instant **défait le clic qu'on
vient de faire**, et le pilote tourne en rond : menu, réglages, retour, menu… jusqu'au
plafond. Vu en vrai le 2026-08-17, sur un arbre de 18 nœuds.

On n'insiste donc qu'après plusieurs passes perdues d'affilée, ce qui laisse tout
le temps à une transition, et le compteur retombe à zéro dès qu'on reconnaît
quelque chose.

## Deux plafonds, deux pannes évitées

**Les saisies** sont plafonnées : un écran qui ne relit pas la valeur qu'on vient
d'y écrire ferait recommencer le pilote sans fin. C'est arrivé — sans ce compteur
il aurait réécrit le code de salon jusqu'à ce que le joueur ferme l'app.

**Les défilements** aussi : si défiler ne fait jamais apparaître ce qu'on cherche,
on rend la main en disant quoi régler, plutôt que de faire défiler l'écran
indéfiniment sous le pouce du joueur. C'est le même piège que le bouton OK
d'Azahar en paysage, sous une autre forme : là il fallait chercher **sans** le
filtre de visibilité, ici il faut **amener** la rangée à l'écran.

**La route PS2 est plus longue que les autres** — bibliothèque, menu, réglages,
onglet — donc son plafond de navigation est plus haut : quatre suffisaient pour
Dolphin, pas ici, et un plafond trop bas se lit comme « l'installation ne marche
pas ».

## La saisie se fait en une passe

Vider, taper, confirmer, avec l'arbre relu entre chaque touche. Découper en une
passe par caractère **aurait eu l'air plus sûr** : ça l'aurait rendu dépendant d'un
événement d'accessibilité par touche, alors que rien ne garantit qu'ARMSX2 en émet
un pour chacune.

Le code de salon est coupé aux bornes d'ARMSX2. Le code de session est déjà le
secret partagé des deux joueurs, et il est alphanumérique donc tapable. **Trop
court, on n'en invente pas** : mieux vaut laisser celui d'ARMSX2 — identique des
deux côtés seulement si les joueurs se le recopient — que d'en poser un que l'autre
n'aura pas.

Le geste « retour » du système sert à sortir d'un écran qu'on ne sait pas lire : un
ARMSX2 déjà ouvert revient au premier plan là où le joueur l'a laissé, et il n'y a
pas d'autre chemin d'un écran inconnu vers les réglages.

---

# Le pilote Dolphin

Sorti de `dolphin/DolphinNetplayDriver.kt` et `dolphin/DolphinScreen.kt`.

## Lire un formulaire Compose sans un seul identifiant

Tout y est **géométrie et texte**. La lecture est exprimée sur un nœud maison
plutôt que sur `AccessibilityNodeInfo`, parce que ces règles sont tout le risque de
ce backend et que le type de la plateforme **ne peut pas être construit dans un
test unitaire**.

Des bornes maison plutôt que `android.graphics.Rect`, pour la même raison : un test
JVM reçoit l'`android.jar` bouchonné, où chaque méthode de `Rect` rend zéro en
silence. La règle de contenance aurait été fausse partout et le test serait resté
vert **en ne prouvant rien** — une forme que ce projet a déjà payée une fois, sur
le setter d'accessibilité qui ne tournait jamais.

## La contenance, pas la parenté, pas la position

**Un libellé appartient au champ dont les bornes le contiennent.** L'`OutlinedTextField`
de Compose dessine son libellé *à l'intérieur* de la bordure du champ : la légende
« Port » se trouve au coin haut-gauche de la boîte qui contient « 2626 ». Les deux ne
sont donc pas des frères à compter dans l'ordre, ils sont **imbriqués dans l'espace**.

C'est délibérément l'ancre. Apparier par position dans le formulaire casserait le
jour où l'amont ajoute un champ — et l'amont bouge encore : trois PR netplay étaient
ouvertes le jour où ceci a été écrit. La contenance survit à une réorganisation, à
une rangée insérée et à une rotation d'écran, et c'est le même test quelle que soit
la langue du libellé.

**La parenté était la première règle, et elle est fausse sur le vrai arbre.** Relevé
sur la Thor : le bouton de validation et sa propre légende sortent **frères, à la
même profondeur** — un `Button` en `[1698,859][1883,988]` sans texte, à côté d'un
`TextView` « Host » en `[1756,900][1826,947]`. Remonter depuis le texte ne trouvait
donc aucun bouton : le pilote remplissait tout le formulaire puis s'arrêtait **à une
pression d'ouvrir le salon**. La boîte contient toujours la légende, donc la
contenance répond là où la parenté ne pouvait pas — et le cas ancêtre est conservé
au cas où une build future les imbriquerait.

**L'onglet et le bouton portent le même texte.** Le bouton est enveloppé dans un
`android.widget.Button`, l'onglet est une rangée nue en haut de l'écran. Cliquer le
mauvais n'est pas anodin : presser « Host » alors que l'onglet Connect est affiché
lancerait un hébergement quand Emufii voulait rejoindre.

## Le bouton de débordement se trouve par sa forme

La première tentative demandait à appcompat sa propre description
(`abc_action_menu_overflow_description`). **Elle ne résout nulle part** : ni dans les
ressources de Dolphin, ni dans les nôtres, Emufii étant tout-Compose et n'embarquant
pas appcompat. Mesuré deux fois sur la Thor ; le pilote restait sur la grille de jeux
en disant `desc=0`.

Ce que le nœud a en revanche, c'est une **forme que rien d'autre dans cette barre ne
partage**. Les boutons propres à Dolphin portent tous un identifiant de ressource,
venant de sa ressource de menu ; le débordement est ajouté par le framework et n'en
porte aucun, tout en restant cliquable et en se décrivant pour les lecteurs d'écran.
Donc : dans la bande du haut, le nœud cliquable **sans identifiant mais avec une
description, le plus à droite**. Indépendant de toute langue — c'est le but — et du
fait que le menu gagne ou perde des entrées.

De même, la rangée Netplay du menu se trouve **par son texte, pas par son
identifiant** : la ressource nomme l'entrée `menu_netplay`, mais appcompat rend le
titre de chaque rangée dans une vue portant `id/title`, donc l'identifiant de
l'entrée n'atteint jamais l'arbre d'accessibilité et la recherche ne trouvait rien,
en silence. Exactement comme les cartes de réglages d'Azahar.

## L'ordre des écrans, et les deux pièges qu'il évite

**Le salon d'abord**, avant tout le reste, parce que c'est le dernier écran : le
formulaire est derrière nous et ne doit plus être touché.

**La liste de jeux du salon se distingue de la grille de démarrage par
`lobbyClicks > 0`**, et ce n'est pas un détail : sans ça, l'étape se déclenchait sur
la grille de démarrage de Dolphin, qui n'a pas plus de champ de texte que la liste du
salon et montre les mêmes titres. Le pilote **lançait donc le jeu dès la première
passe** au lieu d'ouvrir le netplay.

**Le type de connexion se règle avant toute saisie.** En changer reconstruit le
formulaire — le champ de port apparaît et disparaît avec lui — et une valeur écrite
dans un champ sur le point d'être recréé est perdue. C'est aussi un réglage unique
partagé par les deux onglets, donc posé une fois pour les deux rôles.

**Connexion directe, jamais la traversée** : celle-ci ferait passer la session par le
serveur STUN de Dolphin, ce que cette app existe précisément pour rendre inutile —
les deux joueurs sont déjà sur le même réseau WireGuard et l'hôte répond à une
adresse simple. Elle retirerait aussi le champ de port, qui n'existe qu'en mode
direct : il ne resterait plus rien à pointer où que ce soit.

**Et surtout, pas de `Done` à la validation du formulaire.** Un formulaire validé
n'est plus la fin de la route : le salon s'ouvre derrière, et le jeu doit encore y
être choisi. Or `report(Done)` efface le plan — c'est tout son objet — donc crier
victoire là désarmerait le pilote juste avant l'écran qu'il lui reste à traiter, et
le sélecteur de jeu resterait sur le dernier choix de l'appareil.

**On ne clique jamais « Start ».** Démarrer la partie est la décision de l'hôte, pas
la nôtre : l'invité peut ne pas être prêt, et une partie lancée sous le pouce du
joueur est exactement le genre d'initiative que ce pilote s'interdit partout
ailleurs.

## Apparier un jeu quand les deux côtés ne le nomment pas pareil

L'égalité stricte ne peut pas marcher : Emufii part du nom de fichier et coupe à la
première parenthèse, ce qui donne « Super Smash Bros. Brawl » ; Dolphin lit le titre
estampé dans l'en-tête du disque et affiche « Smash Bros. Brawl ». **Aucun des deux
n'a tort**, et aucun ne peut changer — le premier est notre bibliothèque, le second
est le disque.

La règle est donc la **contenance sur chaînes normalisées, dans les deux sens** : le
titre du disque est souvent plus court que le nôtre, parfois l'inverse quand notre
nom de fichier est abrégé. La ponctuation est retirée parce que c'est précisément là
que les deux divergent (« Smash Bros. Brawl » contre « Smash Bros Brawl »).

**Le plus long gagne, et une égalité annule tout.** Deux entrées qui correspondent
aussi bien, c'est une bibliothèque contenant « Mario Kart Wii » et « Mario Kart Wii
(disc 2) » : choisir au hasard lancerait le mauvais jeu, ce qui est pire que de ne
rien faire. On rend `null`, et le joueur choisit lui-même.

## Les libellés sont résolus une fois

Résoudre les libellés coûte une trentaine de recherches de chaînes : la même
ressource est résolue dans **toutes** les langues où Dolphin pourrait tourner, parce
qu'il n'y a aucun moyen de demander laquelle il utilise réellement. Six libellés,
relus à chacune des six re-lectures par écran, feraient un millier de recherches pour
remplir un formulaire. Ils ne peuvent pas changer pendant que l'émulateur tourne.

## Le plafond de navigation est le moment à photographier

Sous la barre où le pilote emmène le joueur vers un écran qu'il n'a pas demandé,
tout est plafonné : un plan armé qui n'arrive jamais ne doit pas rouvrir le menu de
débordement sous le pouce du joueur.

**Atteindre le plafond est *le* moment où ce pilote abandonne en silence** : il a
cliqué, l'écran n'a pas changé comme prévu, et il rend la main sans rien dire au
joueur. C'est exactement l'instant à photographier ; plus tôt, on capturerait la
grille avant l'ouverture du menu, ce qui ne prouve rien.
