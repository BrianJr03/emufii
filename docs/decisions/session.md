# Session : l'ordre hôte/invité, les deux panneaux, les cartes par console

Le récit qui vivait dans `ui/screens/SessionScreen.kt`, sorti du code le
2026-08-24 (cf. `docs/STYLE_COMMENTAIRES.md`). Titres = ancres citées depuis le
code.

## L'ordre hôte puis invité n'est pas un détail de confort

Un invité qui s'installe avant l'hôte **ne trouve rien**. Le salon n'existe pas
encore, l'émulateur répond « aucune session », et le joueur conclut que le jeu
est cassé alors qu'il est simplement arrivé trop tôt. Rien à l'écran n'énonçait
cet ordre : les deux joueurs voyaient le même bouton, prêt à être pressé.

L'état par défaut est **vrai** : c'est ce que répondra un coordinator qui ignore
la question, et c'est aussi ce qui vaut pour l'hôte, qui n'attend personne.

**Un salon d'amont ne change rien à cet ordre**, et c'était une erreur de
raisonnement corrigée le 2026-08-10 après une partie à deux. La première version
excluait les sessions à salon d'amont : personne n'y héberge, les deux joueurs
rejoignent le même salon, donc — pensais-je — aucun ordre à respecter. Le
garde-fou ne se montrait jamais, puisque toute session Switch en a un.

Ce que ce raisonnement confondait : **le salon et la partie ne sont pas la même
chose.** Le salon existe dès la création de la session, mais ce que l'invité
cherche dans Eden est la session LDN *du jeu*, qui n'existe qu'une fois que
l'hôte l'a ouverte depuis son jeu. Arriver le premier, c'est fixer une liste
vide — exactement le symptôme d'origine.

L'ordre vaut donc pour **tout backend ayant un salon à rejoindre**, avec ou sans
relais sur le VPS.

### Deux preuves qu'un salon existe, et la seconde est assumée plus faible

L'automatisation ne va au bout **qu'avec le service d'accessibilité** ; un hôte
qui l'a refusé s'installe à la main. Sans second signal, ses invités
attendraient un « Terminé » qui ne vient jamais, et une file sans sortie est pire
que pas de file du tout.

Le second signal est donc le simple fait d'être **revenu dans Emufii** après
avoir ouvert l'émulateur. Il est plus faible, et c'est accepté : au pire un
invité part quelques secondes trop tôt, ce qu'il faisait de toute façon avant.

Le drapeau « l'installation est allée jusqu'au salon » est **verrouillé** plutôt
que lu en direct sur le flux de progression : démarrer la partie désarme le plan,
ce qui remet ce flux au repos, et l'installation cesserait de paraître faite au
moment précis où ça commence à compter. Il est distinct de « l'émulateur a été
ouvert », qui ne dit que ça.

Le retour depuis l'émulateur est aussi le moment de remarquer que
**l'automatisation n'a jamais donné signe de vie** (`NetplayAutomation.neverStarted`) :
ce silence a une cause sur laquelle le joueur peut agir, et ne rien dire se lit
comme « l'app est cassée ».

## Seul un 404 prouve qu'un salon est fermé

Un invité dont l'hôte a fermé le salon restait devant un écran d'apparence vivante
pour toujours. On tolère donc quelques échecs — les réseaux mobiles perdent des
requêtes — puis on le dit et on sort.

Mais **seul un coordinator qui *répond* 404 prouve que le salon a disparu.** Un
coordinator muet ne prouve qu'une chose : qu'on ne l'atteint pas. Sur un hoquet
Wi-Fi, ça annonçait « l'hôte a fermé la session » et démontait un tunnel dont les
deux pairs étaient encore là. Désormais l'app dit ce qu'elle sait et laisse la
session tranquille : WireGuard refait sa poignée de main tout seul dès que le
réseau revient.

De même, un coordinator devenu muet **n'est délibérément pas une erreur** : le
tunnel est un appairage WireGuard direct qui n'a plus besoin de lui une fois
levé, donc une partie qui tourne continue de tourner. Seule la liste de qui est
là cesse d'être digne de foi.

## L'adresse affichée est celle qu'on doit taper, jamais une autre

**Sur PSP, l'adresse de l'hôte n'est l'adresse de personne à l'écran** : le
joueur ne la tape nulle part. Ce qu'il règle dans PPSSPP est la sentinelle, que
le relais traduit vers l'hôte de sa session. Afficher les deux mettait deux
adresses différentes sur le même écran, présentées comme celles du même hôte — et
l'inutile était celle qui portait le mot « adresse ».

**Avec un salon sur le VPS, l'adresse à composer est celle du salon**, et celle
de l'hôte n'est plus l'adresse de rien : personne n'héberge. L'afficher quand
même mettrait à l'écran, sous le mot « hôte », une adresse que le joueur ne doit
pas saisir — et c'est précisément l'écran qu'il regarde quand l'automatisation
échoue et qu'il tape à la main.

La valeur est calculée en dehors des deux colonnes : calculée dans l'une, elle ne
serait vraie que d'un côté.

## Deux panneaux, parce qu'empilé cet écran ne tient pas

Empilé, cet écran fait huit cartes pleine largeur et trois boutons de 56 dp dans
une colonne défilante : sur les 468 dp de la Thor, le joueur ne voit jamais plus
d'un tiers de sa propre session, et **le code — la seule chose qu'il lit à voix
haute à quelqu'un — quitte l'écran dès qu'il défile**.

À gauche l'état, qui ne bouge pas : le code, qui est là, l'adresse. À droite ce
qu'il reste à faire, boutons épinglés en bas. La règle qui a coûté cher tient
toujours, et gratuitement cette fois : **la réponse à une pression est sous le
bouton qui l'a produite**, dans un panneau qui ne défile pas.

### Le panneau d'état ne défile pas, donc il doit tenir

Il a eu un `verticalScroll`, et une carte s'est retrouvée décalée hors du panneau
sans que rien ne le demande : un panneau d'état capable de cacher son état ne
fait pas son travail. Il tient parce que **le code est monté dans l'en-tête** : à
deux cartes, les deux gardent leur forme pleine, là où en serrer trois finissait
par rogner l'adresse.

**C'est la présence qui cède, jamais l'adresse.** Le panneau ne défile pas et sa
hauteur est celle de l'écran : ce qui ne tient pas est rogné. Sans poids, les
deux cartes étaient mesurées dans l'ordre, la présence prenait ce qu'elle voulait
et l'adresse héritait du reste ; à deux joueurs la liste des arrivées grandit et
le reste a cessé de suffire — les libellés des boutons de copie ont disparu, puis
le bouton de port lui-même a été coupé.

Le poids **inverse l'ordre de mesure** : Compose mesure d'abord les enfants sans
poids, donc l'adresse obtient sa hauteur naturelle, entière, et la présence se
débrouille avec ce qui reste, en défilant à l'intérieur plutôt qu'en étant
coupée. Perdre de vue la troisième ligne d'une liste de joueurs ne coûte rien ;
perdre le bouton qui copie le port empêche d'installer la partie.

Ce qui dépasse du pied du panneau **s'efface** au lieu d'être tranché au milieu
d'un mot : la hauteur du panneau est celle de l'écran et son contenu est un
paragraphe ; tranchée, la dernière ligne se lisait comme un défaut de rendu — la
même plainte que les demi-rangées du plateau.

### Descendre vise le premier bouton qui répond

La destination « bas » est **le premier bouton qui existe et qui répond**. C'était
« Lancer », le seul que tout backend affiche, mais celui-là est désactivé tant que
l'étape précédente n'est pas faite — et un bouton désactivé ne prend pas le
focus : descendre échouait et le curseur partait dans la colonne de gauche.

Le panneau s'efface, puis un vrai espace, puis les boutons. Sans cet espace, la
dernière ligne se dissolvait directement dans le haut de la pastille et les deux
se lisaient comme un seul élément cassé plutôt que comme un texte qui continue
sous la ligne de flottaison.

**Un bouton grisé reste atteignable à la manette.** Un `Button` désactivé cesse
d'être focalisable, et c'est ici le seul arrêt de la colonne : un invité en
attente se retrouvait sur un écran où la croix ne trouve rien du tout, donc figé.
Le focus ne promet pas qu'un clic aboutira, il dit **où l'on est**.

## Ce qui se fait à la main se dit avant le bouton, jamais après

La carte PSP passe avant tout le reste, juste après le code : c'est une chose à
faire **dans un autre programme**, une fois. La laisser au bas de la colonne, sous
le bouton qui démarre la partie, revenait à la montrer après le moment où elle
servait. Les autres consoles gardent leur carte en fin d'écran : elles n'ont rien
à régler avant de jouer.

Même raisonnement pour le pseudo d'Azahar : **avant les boutons, pas après.**
C'est la seule chose que le joueur doit faire à la main dans l'émulateur, et
Azahar refuse le salon pour cette raison **en accusant l'adresse** — un
prérequis imprimé sous le bouton auquel il s'applique est lu après la faute, si
tant est qu'il le soit.

Et la réponse à une pression s'affiche **directement sous le bouton qui la
produit**, pas au bout de la colonne : cette colonne défile, et sur une portable
en paysage son bas est hors écran. Rendue en dernier, la réponse atterrissait là
où l'utilisateur ne pouvait pas la voir, et un lancement refusé pour une bonne
raison — un émulateur sans interface multijoueur, par exemple — était
indiscernable d'un bouton mort.

## Les cartes par console, et ce que chacune doit empêcher

- **PPSSPP** n'a aucun netplay à piloter, aucun service d'accessibilité ne peut
  le faire, mais il a des réglages que le joueur doit saisir lui-même et qui ne
  se devinent pas. Le bouton **ne les applique pas** : il ouvre l'émulateur, ce
  qui est tout ce qu'Emufii peut faire, et le dit franchement dans son libellé
  plutôt que de laisser croire à une installation automatique comme celle
  d'Azahar. Les quatre réglages sont affichés **avec l'adresse déjà dans le
  presse-papier** : copier à l'affichage plutôt qu'au clic, parce que le joueur
  s'apprête à quitter Emufii pour PPSSPP, et devoir revenir presser un bouton
  qu'il n'a pas vu avant de partir est exactement l'aller-retour que cette carte
  existe pour éviter. Le bouton reste, pour qui revient plus tard.
- **Eden** : son multijoueur n'est pas dans un tiroir de jeu mais dans les
  réglages de l'app. La carte dit donc où aller et, quand le remplissage
  automatique est actif, qu'il n'y a plus rien à taper une fois arrivé. **L'hôte
  est invité à Créer et l'invité à Rejoindre** : contrairement à Azahar, cet
  écran peut s'ouvrir avant qu'un jeu tourne, et dire la même chose aux deux
  mettrait les deux joueurs du même côté du salon.
- **Dolphin** n'a pas d'étape 2. Le jeu n'est pas lancé ici puis rejoint : il est
  choisi dans le salon, par l'hôte, une fois celui-ci levé — et Dolphin ne peut
  de toute façon pas se voir remettre un jeu de l'extérieur. La carte ne dit donc
  jamais « démarre ta partie », et le dire franchement vaut mieux que de retomber
  sur « pas encore pris en charge », qui était faux et décourageant.
- **PS2** : ARMSX2 sait faire deux multijoueurs sans rapport. Le mode local
  (Local Link) est celui qu'Emufii sert, pour la soixantaine de jeux livrés avec
  un mode LAN ou System Link. Le mode en ligne passe par un serveur de
  renaissance, en DNS clair, sans session ni tunnel : Emufii n'y sert à rien, et
  laisser croire le contraire produirait exactement la mauvaise attente. D'où
  l'avertissement en tête de carte, avant toute autre chose.

### Le prérequis Dolphin que personne ne vérifie

Les deux côtés ont besoin du **même dump, octet pour octet** : le netplay le
hache et refuse silencieusement sinon. Ça, l'émulateur ne le mentionne qu'une
fois trop tard.

Mais la **sauvegarde** est aussi bruyante et plus traîtresse : le dump, Dolphin
le vérifie et le refuse ; la sauvegarde, personne ne la vérifie. Deux joueurs
partant d'états différents rejoignent le salon, démarrent la partie, et voient
chacun un match qui n'existe pas chez l'autre, **sans message nulle part**.
Mesuré sur Brawl le 2026-08-16 : l'un était au menu « créer une sauvegarde »,
l'autre l'avait déjà passé, et il a fallu une soirée pour en arriver là.

Le Dolphin de bureau a « Sync Save Data », qui pousse la sauvegarde de l'hôte et
rend tout ça invisible. Cette version Android ne l'expose pas, et Emufii ne peut
pas s'en charger : les sauvegardes vivent dans le stockage privé de Dolphin. On
avertit donc, faute de pouvoir agir.

## Ce que chaque backend reçoit au lancement

**Azahar : les deux rôles pointent vers l'adresse tunnel de l'hôte** — l'invité
pour l'atteindre, l'hôte parce que `netPlayCreateRoom` se lie et se rejoint
lui-même sur la même adresse (voir `PHASE0_AZAHAR.md`). Sa propre IP de tunnel est
la seule valeur qui marche pour les deux.

**Le pseudo, sur Eden seulement, et pour les deux rôles** : deux joueurs de même
pseudo ne peuvent pas partager un salon, et Eden livre le même à tout le monde
par défaut — deux joueurs Emufii s'y présenteraient donc comme la même personne.
Azahar garde le sien : Emufii y écrivait le nom de profil, ce qui remplaçait un
pseudo valide par un pseudo de deux lettres que le formulaire refusait, avec un
message accusant l'adresse.

**Le code de session sert aussi de code de salon sur PS2** : ARMSX2 en exige un,
identique des deux côtés, et ne négocie rien. C'est le secret que les deux joueurs
partagent déjà. Inutile ailleurs — les autres émulateurs n'ont pas de champ où le
mettre — sauf pour les salons VPS, qui portent le leur.

**Dolphin : un retour, pas un lancement.** Le jeu est choisi et démarré dans le
salon Dolphin. Ce bouton doit ramener le joueur là où la partie l'attend, après
un aller-retour par Emufii. L'intent de lancement **reprend la tâche existante**
au lieu d'en ouvrir une neuve : le salon est encore à l'écran derrière, et on
retombe droit dessus. Si Dolphin a été tué entre-temps on atterrit sur sa grille
de jeux, ce qui est le mieux possible — `NetplayActivity` n'est pas exportée et ne
peut pas être visée. Et surtout : **aucun plan armé.** Le salon est déjà ouvert ;
ré-armer enverrait le pilote remplir le formulaire par-dessus une partie qui
tourne.

**PS2 : un vrai lancement**, contrairement à Dolphin — la `MainActivity` d'ARMSX2
est exportée avec un filtre VIEW sur `content`, donc la ROM SAF voyage avec
l'intent. Pas de plan armé pour autant : le réseau a été posé à l'étape un, et
ré-armer enverrait là aussi le pilote par-dessus une partie qui tourne.

Enfin, une image PS2 **dont l'ELF de démarrage est illisible** prend une branche à
part : sa forme réseau peut toujours passer par le pilote d'accessibilité établi,
mais la carte préparée réclame d'abord l'unique affectation globale héritée que
les fichiers par jeu évitent. On garde cette navigation supplémentaire hors de
tous les ISO/CHD pris en charge, et on la rend explicite ici plutôt que de lancer
en silence une partie sans profil réseau.

## Le retour ferme la session, et il le dit

Il y avait **deux contrôles pour un seul geste** : le bouton de retour de
l'en-tête, qui quittait sur-le-champ, et « Fermer la session » à l'autre bout de
la même barre. Le premier promettait de remonter d'un écran ; il coupait le
tunnel.

Un seul reste, et c'est celui qu'on trouve sans le chercher parce qu'il est là
sur tous les autres écrans. Il change de marque — **une croix, au rouge coque**
— et il demande avant d'agir. Le rouge n'apparaît que deux fois dans toute
l'app ; ceci en est une, et c'est ce qui le rend lisible.

La question n'est pas la même des deux côtés : l'hôte ferme la session **pour
tout le monde**, l'invité s'en retire. Et les deux formulations disent ce que la
fermeture ne fait pas — le jeu déjà lancé continue de tourner.

En portrait, le bouton du bas reste : la page y est une colonne qu'on parcourt,
et finir sur l'action de sortie est l'ordre naturel. Il passe par la même
question.

## Le jeu s'affiche dans le vide que le panneau a laissé

Quand le panneau arrière porte l'adresse et les étapes, la colonne d'état de
l'écran de face se termine par trois cents pixels de vide — sur un écran où
l'on attend quelqu'un, et où il n'y a donc rien d'autre à regarder.

La jaquette y va, encadrée comme sur sa tuile : même plaque, même contour, même
lueur empruntée à ses propres couleurs. C'est ce que la session **est**, et
c'est la seule couleur de l'écran — elle vient du contenu, jamais du chrome.

Deux points de plomberie :

- **La session ne porte qu'une référence de ROM**, sans icône ni couleur
  extraite. La jaquette se retrouve dans la bibliothèque par son URI, dans le
  cache déjà chaud, hors du fil principal, et sans jamais déclencher un scan à
  elle seule. Rien ne s'affiche si elle n'est pas trouvée.
- **La présence garde la priorité.** Les deux blocs sont en `weight(fill =
  false)` : la carte des joueurs prend ce qu'il lui faut, l'image prend ce qui
  reste, et une liste plus longue que sa moitié défile dans sa carte comme
  avant. L'image disparaît plutôt que de tasser quoi que ce soit sous 96 dp.

**Et le fondu de la colonne de droite ne vit qu'en mono-écran**, aux deux bouts.
Il est là pour qu'un texte trop long se dissolve au lieu d'être coupé au milieu
d'un mot, ce qui arrive quand les deux commandes vivent sous lui et lui prennent
sa hauteur. Panneau allumé, elles sont au dos, la colonne a l'écran entier — et
un dégradé qui éteint le bas d'une carte pleine se lit alors comme un défaut
d'affichage.

Même règle, même raison, que la jaquette : **ce que le panneau prend, la face le
récupère ; ce que le panneau rend, la face le remet.** Un état intermédiaire où
la face garde les habitudes des deux ne marche dans aucun des deux cas — la
première version affichait la jaquette dans les deux, et en mono-écran elle
plafonnait la carte de présence à la moitié d'une colonne qui portait déjà
l'adresse.

## Copier l'adresse n'a plus de sens depuis qu'Emufii la remplit

La carte de connexion portait deux pastilles, « copier l'adresse » et « copier le
port ». Elles viennent d'une époque où le joueur remplissait le formulaire de
l'émulateur à la main.

Trois raisons de les retirer, et la troisième suffit :

- **Emufii remplit le formulaire.** C'est la fonction que le service
  d'accessibilité existe pour rendre.
- **Quand elle ne peut pas** — Android ayant coupé le service — la carte
  d'explication au-dessus dit quoi taper, et la valeur est affichée juste à
  côté. Le presse-papier, lui, ne tient **qu'une valeur à la fois** alors que la
  boîte de dialogue en demande deux : il n'a jamais été la bonne réponse à ce
  problème.
- **Elles coûtaient 62 dp**, et c'est exactement ce qui manquait pour que le
  panneau tienne sans défiler.

Le **code**, lui, se copie toujours : c'est ce qu'on envoie à un ami dans une
autre application, et rien d'autre ne le porte.

## Ce que le panneau arrière porte, l'écran de face ne le redit pas

Le panneau affiche déjà, en session, le code en grand, l'adresse et le port dans
leurs alvéoles, et le nom du jeu. Tant que l'écran de face les redisait, le
joueur lisait deux fois la même chose et la carte de connexion mangeait 150 dp.

Quand le panneau est **réellement allumé** — le réglage activé *et* un second
écran présent — la carte de connexion quitte l'écran de face. Le réglage seul ne
suffit pas à décider : un appareil peut n'avoir qu'un écran.

**Les boutons, eux, ne bougent pas, et ne peuvent pas.** La fenêtre du panneau
porte `FLAG_NOT_TOUCHABLE` : rien dessus n'est pressable, par construction, pour
qu'elle ne vole jamais un appui destiné au jeu. Le panneau rapporte, il ne
commande pas — c'est écrit dans `second-ecran.md`, et c'est aussi ce que le
matériel impose. Déplacer les étapes 1 et 2 au dos donnerait deux boutons que
personne ne peut presser.

Ce qui bouge à la place, et qui rend la place demandée : la colonne d'état
passe de 272 à 220 dp quand le panneau est allumé, puisqu'il n'y reste que la
présence. Les 52 dp rendus vont à la colonne de droite, où l'explication tient
alors sur moins de lignes.

**Une divergence trouvée en chemin, et corrigée.** L'écran de face calculait
`room?.host ?: hostIp` et le panneau recevait `hostIp` brut : une session Eden
avec salon aurait affiché au dos une adresse que l'émulateur n'attend pas. Tant
que les deux s'affichaient côte à côte, ça se voyait à peine ; le jour où la
face cesse de le redire, ça devient l'unique valeur affichée. La règle vit
désormais **une seule fois**, sur `Session.shownAddress` / `shownPort`, avec le
cas PSP dedans — son serveur ad hoc a un nom fixe et pas de port.

## Les partis pris de dessin de cet écran

**Le code est monté dans l'en-tête** : c'est ce qu'on lit à voix haute à
quelqu'un, donc il doit rester visible en permanence. La pastille est celle des
puces de la bibliothèque — même hauteur, même rayon, même ombre — pour que
l'en-tête reste une rangée d'objets flottants. Une pression copie : c'est le geste
qu'on a envie de faire devant un code.

**Le bouton destructeur est une pastille moulée**, comme tout ce qui se presse. Il
était un texte rouge nu flottant sur le plateau — le seul contrôle de l'écran fait
de rien, et c'était justement le destructeur. De l'encre rouge sur une plaque dit
la même chose sans prétendre être un lien.

**La coche est dessinée** plutôt qu'importée : deux traits coûtent moins que de
tirer tout l'artefact material-icons pour un glyphe, et contrairement à un
caractère « ✓ » elle se pose exactement où on la met — les glyphes de texte sont
centrés sur leur boîte de ligne, pas sur leur encre.

**La liste de présence** est tout l'intérêt de la boucle de présence : héberger
était un écran avec un code dessus et aucun moyen de savoir si quelqu'un était
venu. Son défilement interne n'est actif **que dans le panneau**, dont la hauteur
est fixe. Ailleurs c'est faux, et ce n'est pas une question de goût : la page à
une colonne défile déjà, donc elle mesure ses enfants à hauteur infinie, et
Compose refuse — en levant une exception — de mesurer du contenu défilant sous une
contrainte non bornée.

**Le dégradé d'effacement se déclare avant le défilement**, et l'ordre est tout le
sujet. Placé après, il travaille dans les coordonnées du contenu *déroulé* :
`size.height` y vaut la hauteur totale du texte et le dégradé atterrirait sous la
ligne de flottaison, invisible. Placé avant, il enveloppe le nœud défilant, donc
il mesure la fenêtre et l'effacement reste collé au bas de la carte. Il devient
**opaque avant d'atteindre le bord**, et pas seulement au bord : un dégradé
linéaire courant jusqu'en bas laissait le haut de la dernière ligne sous 40 % de
couverture — donc lisible et tranchée, mesuré. Sa hauteur doit **effacer une ligne
entière**, interligne compris : à 28 dp la ligne coupée restait à moitié lisible.

**Le nom du jeu n'est pas affiché dans le panneau**, où ses quarante dp étaient
exactement ce qui rognait les boutons de copie. Le nom du jeu n'est pas un état
sur lequel on agit — le joueur vient de le lancer — et l'autre panneau parle déjà
de son émulateur, alors que l'adresse, elle, se copie.

**La note importante est un creux, et sa marque est dessinée.** Deux tours pour en
arriver là. Elle a commencé en `errorContainer` rempli à 55 % sous un « IMPORTANT »
en capitales espacées : un sourcil, que la charte bannit, sur un champ rouge
saturé de la taille d'un paragraphe qui détournait l'attention du panneau au
détriment des boutons qui sont le vrai travail. C'est devenu un creux avec une
barre rouge de 3 dp le long du bord de lecture — mieux, et encore deux choses de
travers : une bordure latérale colorée au-delà du cheveu est refusée sur les
encarts, et le rouge est la couleur de danger de ce produit, dépensée exactement
deux fois dans toute l'app ; deux avertissements sur un écran la dépensaient
quatre fois, ce qui est ce qui lui fait perdre tout sens quand quelque chose ne va
vraiment pas. Donc : le creux du plateau, l'encre ordinaire, et la même perle
d'avertissement que la bibliothèque porte déjà sur un jeu qui marche à moitié.
Dessinée, ni tapée ni remplie : elle dit « lis celle-ci » sans prétendre que quoi
que ce soit est cassé.

Le conseil de confort de la carte PSP est **en fin de carte**, pas au milieu :
c'est le plus long paragraphe de l'écran, et glissé entre l'adresse et son bouton
« Copier », il séparait le geste de ce sur quoi il agit. Il n'est délibérément
**pas** une note importante — ce bloc est réservé à ce qui empêche de jouer — mais
pas non plus dans la voix grise des notes du dessus, où il se lirait comme du
remplissage. D'où un titre, et un corps à pleine force.

**La liste des présents est mise en phrase par ICU** (« Toi, Bibi et Théo » /
« You, Bibi and Théo ») : l'écrire à la main allait tant que l'app parlait une
seule langue, mais la conjonction et la place des virgules changent avec la
locale.

## Un seul `focusRequester` par nœud, et c'est celui de la coquille

Il y en avait deux empilés sur le même nœud — celui du pilote et celui de la
coquille — et la trace disait que le nœud ne prenait **jamais** le focus : ni
`focus=true`, ni même `hasFocus=true`, pas une fois. Conséquence, la demande que
l'en-tête envoie sur Bas tombait dans le vide, la touche était consommée quand
même, et il en fallait une seconde pour revenir au panneau — pendant que Haut,
qui ne demande rien à personne, marchait du premier coup.

Le pilote *reçoit* les touches sans avoir le focus, parce qu'il est près de la
racine et qu'un événement clavier remonte la chaîne. Mais recevoir n'est pas être
désigné : pour qu'on puisse lui **rendre** le curseur, il faut qu'il soit une
destination, et une destination n'a qu'une adresse.

Le pilote réclame ensuite le curseur image par image. Une seule demande après
150 ms tenait avant : le focus initial de Compose désigne l'en-tête, déclaré avant
le contenu, et il gagnait contre elle. Le délai était un pari sur le calendrier ;
la boucle est la même réponse que celle de la coquille — redemander tant que le
nœud n'est pas placé, sans regarder si ça a marché, et borner.

## Le retour ferme la session, donc il porte une croix et il demande

Il y avait deux contrôles pour un seul geste : le bouton de retour, qui quittait
sur-le-champ, et « Fermer la session » à l'autre bout de l'en-tête. Un seul reste
— celui qu'on trouve sans le chercher, parce qu'il est là sur tous les autres
écrans — et il cesse de promettre un retour en arrière : une croix rouge, et une
question avant de couper le tunnel.

## Ce que le panneau porte, l'écran de face le rend en place

Trois endroits appliquent la même règle, et chacun rend sa place à autre chose :

- **La pastille du code** ne paraît de face que si le panneau ne la porte pas :
  il l'affiche en 64 sp au dos, la redire en 19 sp ne sert personne.
- **Le volet d'état** est plus étroit panneau allumé — il ne reste que la
  présence — et les 52 dp rendus vont à la colonne de droite, où l'explication
  passe alors sur moins de lignes.
- **Le jeu encadré** ne s'affiche **que** dans le vide que le panneau a laissé.
  Rendu dans les deux cas, il cassait la carte de présence en mono-écran : deux
  enfants pondérés se partagent l'espace libre, donc la carte des joueurs se
  retrouvait plafonnée à la moitié d'une colonne. Sans panneau, il n'y a pas de
  vide à remplir — c'est le panneau qui le crée en prenant l'adresse.

Et le fondu du bas **n'existe qu'en mono-écran**. Il est là pour qu'un texte trop
long se dissolve au lieu d'être coupé au milieu d'un mot, ce qui est le cas quand
les deux commandes vivent sous lui et lui prennent sa hauteur. Panneau allumé,
elles sont au dos, la colonne a l'écran entier — et un dégradé qui éteint le bas
d'une carte pleine se lit alors comme un défaut d'affichage.

## Copier l'adresse n'a plus de sens depuis qu'Emufii la remplit

Les boutons « copier » venaient d'une époque où le joueur remplissait le
formulaire de l'émulateur à la main. Emufii le remplit pour lui, et quand elle ne
peut pas — Android ayant coupé le service — la carte au-dessus dit quoi taper.

Le presse-papier ne saurait de toute façon porter la chose qu'à moitié : il ne
tient qu'une valeur à la fois et la boîte de dialogue en veut deux.

Ils coûtaient 62 dp, et c'est exactement ce qui manquait pour que ce panneau
tienne sans défiler.
