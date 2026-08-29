# Carte de lancement et navigation de l'app

Le récit qui vivait dans `ui/components/GameLaunchDialog.kt` et `ui/EmufiiApp.kt`,
sorti du code le 2026-08-24 (cf. `docs/STYLE_COMMENTAIRES.md`). Titres = ancres
citées depuis le code.

## La carte a remplacé une feuille du bas, et deux fois pour cause

La feuille montrait un titre et deux boutons nus. Elle était fausse deux fois :
elle se lisait comme un menu système boulonné au bord bas — un rectangle ancré à
un écran dont toute la direction est faite de formes flottantes sans
encadrement — et elle ne disait rien de ce que le bouton allait faire, ce qui
pour le jeu en ligne DS est une chose franchement différente de créer une
session.

Une carte flottante, donc, portant la jaquette que le joueur vient de toucher :
l'objet qu'il a choisi est encore l'objet devant lui.

**Elle est couchée quand l'écran l'est.** Empilée, cette carte court du plancher
au plafond sur une portable en paysage — mesuré de 100 à 970 px sur 1080 sur la
Thor — tout en laissant quelque 470 dp de largeur vides de chaque côté. Passer
la pile en deux colonnes dépense cette largeur au lieu de rationner la hauteur,
ce que payait chacune des concessions « compact » d'avant.

Elle est **bornée par l'écran**, jamais par un nombre : en paysage l'appareil a
environ 415 dp de haut, et un jeu dont le titre passe sur deux lignes poussait la
carte sous le bord bas. Les boutons y étaient encore, dessinés hors écran, et le
dernier apparaissait comme une barre sans texte dedans.

## Le curseur doit entrer dans la carte, et ne plus en sortir

La grille tient son propre index et garde le focus : sans demande explicite, la
carte s'ouvrait sans qu'aucun de ses boutons soit atteignable, et les directions
continuaient de déplacer la sélection derrière elle.

**La demande échoue en mode tactile, et c'est correct** : Compose ne rend un
`clickable` focalisable qu'en mode clavier. Cette distinction a coûté cher —
`adb input tap` ouvre la carte en mode tactile, ce qui produit exactement les
symptômes d'un focus cassé.

Mais « une carte ouverte au doigt n'a pas de curseur » était la **mauvaise
conclusion** tirée de là : une portable se touche *et* se tient, et la toute
première chose que fait le joueur après avoir tapé une tuile est d'attraper le
stick. Un nœud `focusable()` simple peut prendre le focus en mode tactile là où
un `clickable` ne peut pas. La carte réclame donc les touches dans les deux cas :
les boutons prennent le curseur quand le mode le permet, sinon la racine le prend
sans rien montrer — et la première direction qui arrive donne le curseur au
bouton principal au lieu de déplacer la grille.

La demande est **réessayée quelques fois** parce que le nœud n'existe pas à la
première composition, puis abandonnée en silence. Et `getOrDefault` plutôt
qu'`isSuccess` : `requestFocus` renvoie `false` sans lever d'exception, donc
`runCatching` réussit avec `false` et tester `isSuccess` se lit comme une victoire
dès le premier tour.

**Le curseur ne quitte pas la carte.** C'est une boîte modale : la grille est
toujours là derrière, focalisable, et une direction vers le haut ramenait la
sélection dedans — la boîte restait ouverte par-dessus mais plus aucune touche ne
l'atteignait. `exit` refuse la traversée dans **toutes** les directions, là où
bloquer une touche précise n'aurait couvert qu'un seul bord.

**À ne pas confondre avec `canFocus = false`**, qui au contraire désactive tout
le sous-arbre — l'erreur d'à côté. Le nœud qui avale les pressions pour qu'un
appui dans la carte n'atteigne pas le fond ne porte donc pas cette propriété : il
avale les pressions, et n'apparaît pas dans la traversée parce qu'aucune direction
ne s'y arrête. Le fond, lui, avale aussi les pressions sans être un arrêt de
curseur : sans ça la traversée s'arrêtait dessus, sur un nœud sans anneau et sans
effet visible.

**B ferme, et depuis la carte** plutôt qu'en s'en remettant au seul
`BackHandler` : mesuré, la première pression ne l'atteignait pas — elle retirait
seulement le curseur du bouton — et il en fallait une seconde. Vu en *preview*,
donc avant que quoi que ce soit d'autre ait une chance de s'en servir.

Le geste système ferme aussi, ce qu'un `Dialog` donnait gratuitement et qu'un
calque doit demander. **Toujours actif, y compris pendant le démarrage** : le
désactiver rendait la main à l'écran du dessous, qui n'a nulle part où aller
depuis la bibliothèque — un B pendant le lancement fermait donc l'app. La carte
avale le geste et ne fait rien : l'action est déjà lancée, et annuler ici
laisserait l'appelant à moitié parti.

## Le plateau s'assombrit, il ne se dépolit pas

Flouter la grille derrière était le geste du monde « verre ». Ici la carte est une
plaque soulevée du plateau, et ce qui l'y pose est le plateau qui s'assombrit
dessous — comme une console assombrit son écran d'accueil quand une fiche de titre
monte. L'assombrissement garde aussi les jaquettes honnêtes : les flouter changeait
six pochettes en une seule bavure colorée.

## Ce qui cède, et dans quel ordre

**La colonne des explications cède la première, et elle est la seule à céder.**
Sans poids, elle était mesurée à la hauteur qu'elle voulait, et les actions
au-dessous étaient posées sous le bord bas de la carte puis rognées : le bouton
principal cessait purement et simplement d'exister sur toute carte aux étapes
longues. Deux colonnes ont acheté de la place ; elles n'ont pas acheté une place
infinie, donc la règle qu'avait déjà la carte empilée tient toujours —
**l'explication défile, les actions jamais**.

**La jaquette est décoration : elle cède en premier.** Plus petite quand la carte
est contrainte en hauteur, ce qu'elle est toujours sur une portable en paysage.
Les étapes sont la partie qui enseigne — en mode public elles sont toute
l'instruction — et une jaquette pleine taille les poussait hors de la zone
défilante, laissant une carte qui montrait un titre et trois boutons et
n'expliquait rien.

**Les deux colonnes sont centrées verticalement**, et chacune pour la raison
inverse de l'autre. La colonne de droite décide de la hauteur de la rangée : sur
une carte bavarde — la PSP et ses instructions — la jaquette et le titre restaient
collés en haut avec un grand vide dessous. Sur une carte courte — la DS, trois
étapes et un seul bouton — c'est la droite qui était plus courte que la jaquette
et restait accrochée en haut. Quand les deux colonnes font la même hauteur, le cas
ordinaire, le centrage ne déplace rien : il n'y a **aucune règle à ajouter** pour
distinguer les cas, la géométrie s'en charge.

## Les boutons sont empilés, et c'est un piège évité

Côte à côte a été essayé d'abord et c'est un piège : deux pastilles se partageant
~400 dp ne tiennent pas ces libellés sur une ligne, **et l'échec est silencieux** —
`Text` rogne par défaut, donc « Créer une session » était dessiné « Créer une »
sans ellipse pour l'avouer. Élargir une moitié ne fait que déplacer le rognage sur
l'autre, et aucun partage ne survit à une traduction. La hauteur que coûtent ces
deux boutons est la seule chose que la carte à deux colonnes ait en trop.

## Le choix du monde vient en premier, pas en dernier

C'était un lien texte sous les boutons, et ça ne marchait pas : une phrase bleue
posée après deux pastilles se lit comme une troisième action de la même famille,
alors qu'elle ne déclenche rien — elle réécrit la carte. Sur la PSP, seule console
à l'offrir, on se retrouvait avec trois choses à peser sous les étapes.

En haut et sous forme de sélecteur, il dit ce qu'il est : **la question à laquelle
les étapes du dessous répondent déjà**. Un sélecteur et non deux boutons, parce
que ce n'est pas une action : rien ne part quand on le touche. Chaque moitié porte
l'anneau de son côté — à la manette on traverse un choix, pas un bloc.

**La PSP est la seule console à avoir un côté public aujourd'hui** : celui de la
PS2 a été mis de côté le 2026-08-19 (`docs/PS2_ONLINE_MIS_DE_COTE.md`), donc cette
branche n'a plus deux cas à distinguer. Et ce ne sont **pas** les étapes de la DS :
la DS compose un serveur de renaissance toute seule, là où le joueur PSP a deux
réglages à choisir dans PPSSPP d'abord. Dire « on s'en occupe » ici serait le
mensonge contre lequel le premier scout mettait déjà en garde.

## « Session privée » promet exactement ce que le coordinator livre

Le libellé dit que la session quitte le annuaire, et rien de plus. Écrire
« personne ne peut entrer » serait faux : **le code protège l'entrée d'une session
privée exactement comme celle d'une session publique**, et qui croit le contraire
partagera son code plus légèrement.

Publique par défaut, parce que c'est ce qui garde le annuaire vivant : une app
dont chaque partie est invisible n'a plus de liste à montrer, et personne ne
trouve plus personne. Le choix est **offert, pas imposé**.

Toute la rangée est cliquable : viser un interrupteur au pouce, sur une carte déjà
serrée en hauteur, est le genre de cible qu'on manque.

L'interrupteur est **au-dessus du bouton**, pas parmi les étapes qui défilent :
c'est une décision qu'on prend en pressant, et une décision qu'on ne voit pas n'est
pas offerte. Il est **absent partout où aucune session n'est créée** — la DS
compose son serveur seule, l'ad hoc public de la PSP se choisit dans PPSSPP :
cacher du annuaire une chose qui n'y paraît jamais n'a rien à offrir.

## Ce qui remplace les boutons quand un prérequis manque

Une session **PS2** sans configuration réseau sur la carte mémoire ne peut pas se
jouer, quoi que fasse le tunnel : le menu local du jeu ne s'ouvre jamais. La carte
se prépare une fois, dans les Réglages, et tant que le joueur n'a pas dit qu'elle
est dans ARMSX2, il n'y a rien qui vaille la peine d'être démarré ici — les actions
sont donc remplacées par quoi faire, plutôt que laissées échouer vingt minutes plus
tard.

Une session **PSP** s'appuie sur l'INI par jeu qu'Emufii écrit sur le memory stick :
sans le dossier accordé une fois dans les Réglages, PPSSPP n'entend jamais l'adresse
de la session et le salon ad hoc du jeu reste vide. Même refus, même endroit pour le
régler. Le mode public en ligne n'est **pas** bloqué : il passe par les serveurs de
PPSSPP et ne demande aucune autorisation.

Ces écrans de remplacement énoncent le prérequis et où le régler, **et rien
d'autre** : il n'y a pas de raccourci vers les Réglages depuis ici, parce que cette
carte vit dans l'arbre de la bibliothèque et que câbler une navigation à travers lui
pour un message coûterait plus que la phrase n'économise.

## Le verdict de compatibilité, là où la décision se prend

La tuile porte déjà la perle, mais **la tuile se balaie et cette carte se lit** :
ici il y a la place de dire ce que la marque veut dire, et c'est le dernier moment
avant qu'un joueur dépense un code de session et la soirée de quelqu'un d'autre sur
un jeu qui ne marche pas. Rien du tout pour un jeu que personne n'a noté, exactement
comme sur la tuile.

La perle est **répétée** plutôt que remplacée par du texte seul, pour que la marque
vue sur la tuile soit le même objet ici, et la phrase enseigne ce que la couleur
veut dire pour la prochaine fois.

**La note du testeur n'est délibérément pas affichée.** Elle l'a été, le temps
d'une version, et elle se lisait comme une seconde voix discutant le verdict à
l'endroit même où le joueur décide : les quatre mots du verdict sont tout le
message que cette carte doit porter. Le champ reste dans la base et dans l'outil,
pour là où il y a la place d'argumenter.

## Le bouton garde sa couleur pendant qu'il travaille

Material grise un bouton désactivé, ce qui dit « tu ne peux pas presser ça » —
mais la raison pour laquelle on ne peut pas le presser est qu'il **travaille
déjà**, et un bouton gris sous un indicateur se lit comme une panne.

Le délai avant de passer la main est juste assez long pour que la pression
s'inscrive sur le bouton visé, et pas plus. Il a été de deux pleines secondes,
dépensées à couvrir une attente dont le joueur n'avait aucun autre signe ; l'écran
qui suit nomme désormais sa propre progression, donc ce rembourrage n'a plus rien à
cacher et n'est que du retard.

Les étapes sont des **points numérotés** plutôt que des puces : les trois lignes
sont une séquence, et une puce ne le dirait pas.

---

# Navigation de l'app

## Une seule destination nommée par écran

`padEntry()` pose **le** `FocusRequester` de l'écran, celui que la coquille vise
quand le curseur descend de l'en-tête, et celui qui renvoie vers l'en-tête quand
il remonte. Il y en a un par écran, et c'est tout le contrat.

Le chercheur de sessions en portait **deux** : la barre de recherche, et *chaque*
carte de session. Il l'avait d'abord sur la carte, du temps où elle était le
premier contrôle ; la barre est arrivée après, avec le sien, et personne n'a
retiré l'autre. Un `FocusRequester` partagé entre douze nœuds ne désigne plus
rien, et les deux symptômes sont exactement ceux d'une destination ambiguë :

- **descendre depuis l'en-tête sautait la recherche** et tombait sur une carte ;
- **remonter depuis n'importe quelle carte partait droit au bouton retour**, au
  lieu de passer à la carte du dessus — parce que chaque carte portait aussi le
  `onPreviewKeyEvent` de `padEntry`, qui renvoie « haut » vers l'en-tête.

La règle qui manquait, et qui vaut pour tout écran : **un seul `padEntry` par
écran, sur le premier contrôle, jamais sur un élément de liste.** Une liste se
traverse toute seule ; ce qu'il faut nommer, c'est la porte d'entrée.

## La recherche ouvre le clavier de l'app

La barre du chercheur était un `PadTextField`, donc un vrai champ de saisie,
donc l'IME du système : il recouvre la moitié de l'écran, il ne se pilote pas à
la manette, et il n'a rien à voir avec le reste de l'app.

La bibliothèque n'a jamais eu de champ. Elle affiche la requête et pose **son
propre clavier** — quatre rangées, des touches qui sont des plaques, et un
panneau qui avale les appuis à côté pour qu'un raté n'ouvre pas un jeu en plein
mot. Le chercheur fait pareil désormais : une alvéole qui montre la requête,
pressable, focalisable, et qui n'est pas éditable — c'est précisément ce qui
empêche l'IME de s'ouvrir.

Deux façons de le refermer, et les deux sont celles de la bibliothèque : `B`,
puisque c'est un sous-niveau, et **une tape à côté**. La tape passe par un voile
invisible sur tout l'écran, déclaré *avant* le panneau pour que les touches
restent au-dessus de lui. Sans ce voile, `B` était la seule sortie, et une tape
sur une carte rejoignait une session en plein mot.

La loupe, elle, a été hissée dans `TrayIcons`. Elle vivait en deux exemplaires,
dessinée à ses propres proportions dans la barre de la bibliothèque, et le
chercheur en aurait fait un troisième. Un glyphe est le même partout où il
apparaît, sinon ce n'est plus le même glyphe.

## Un écran vide se centre sur l'écran, pas sous l'en-tête

L'état vide du chercheur recevait la marge haute de la coquille — la bande que
l'en-tête flottant occupe — et rien en bas. Centré dans ce qui reste **sous**
l'en-tête, son milieu tombait une cinquantaine de dp sous le milieu de l'écran.
Sur un écran qui n'a rien d'autre à regarder, ce décalage est la seule chose
qu'on regarde. La même marge en haut et en bas, et le bloc se centre.

**L'alvéole porte sa marque.** Elle avait été laissée nue exprès : « personne
pour l'instant » est un emplacement sans jeu, et le plateau a déjà un mot pour
ça — le creux que la grille laisse dans son dernier rang. Deux tentatives de
croissant de lune avaient prouvé qu'une métaphore empruntée ne le dirait pas
mieux.

Vu sur l'appareil, ça ne se lit pas comme une métaphore : **ça se lit comme une
icône qui n'a pas chargé.** Un cadre vide au milieu d'un écran vide est un
défaut, pas une figure de style. Le creux reste — c'est le bon mot pour une
absence — et il reçoit la silhouette que l'app dessine déjà pour un joueur.
L'absence est dite deux fois, par le trou et par ce qui manque dedans.

## Ce que « retour » veut dire, écran par écran

**C'est la faille qui fermait l'app.** Rien n'interceptait le retour au-dessus de
la bibliothèque : sur les amis, les sessions, les réglages, la saisie de code, le
jeu en ligne DS ou PSP, une pression sur B — que la console livre comme un retour
système — remontait jusqu'à l'activité, qui n'avait rien d'autre à faire que se
terminer. Mesuré : depuis l'écran Sessions, `BUTTON_B` rendait la main au lanceur.

**Nul sur la bibliothèque**, qui est la racine : là, partir *est* la bonne réponse,
et retenir le geste enfermerait le joueur dans l'app.

**Nul aussi pendant la préparation et en session, mais le retour y est consommé
quand même**, et cette distinction est tout le sujet. Une préparation n'a pas
d'état stable où revenir, le tunnel étant à moitié levé ; quitter une session veut
dire prévenir le coordinator et couper le tunnel, ce que le bouton « Quitter » de
l'écran fait déjà. Laisser le geste remonter au système, en revanche, fermait l'app
en pleine partie et laissait derrière une session que personne ne referme. **Ne
rien faire est le seul comportement sûr à ce moment-là.**

## Ce que le second écran reçoit

Publié depuis le seul endroit qui sait, et **dérivé** de l'écran courant plutôt que
poussé à chaque appel : une session se termine de plusieurs façons, dont certaines
sont des échecs, et un panneau mis à jour à la main finirait par montrer le code
d'une session qui n'existe plus. Dériver garantit que le panneau ne peut pas
contredire l'app, et ça coûte un effet.

Publié dans un porteur à portée de processus et non descendu dans la composition,
pour que l'hôte de service qui survivra à cette activité lise la même chose.

Un événement d'ami est mis **en une phrase** pour le panneau arrière : celui-ci n'a
ni colonne de nom ni avatar, il reçoit une ligne au milieu d'autre chose, donc la
phrase doit porter le nom elle-même. Ce sont **délibérément** les chaînes que la
notification Android utilise déjà : le même événement ne doit pas se lire
différemment selon la surface qui l'a attrapé en premier.

## Le logo, une fois par processus et jamais au premier lancement

**Pas au premier lancement**, parce qu'il n'y a rien à charger alors : le dossier de
ROMs n'a pas encore été choisi, et l'accueil est l'onboarding. Faire attendre
quelqu'un devant un logo pour un scan qui ne trouvera rien serait du temps volé au
tout premier contact avec l'app.

**Une fois par processus, et non une fois par composition** : changer la langue ou
le thème recrée l'activité, et un logo qui revient à chaque réglage touché se lit
comme un plantage. Un `rememberSaveable` n'aurait pas suffi — l'état sauvegardé est
restauré avec l'activité, donc le logo serait revenu. Ce qu'on veut retenir
n'appartient pas à l'écran mais au lancement de l'app : ça vit donc où l'app vit.

Pour la même raison, l'écran courant **survit à la recréation d'activité** que
provoque un changement de langue : sans ça le joueur retombait sur la bibliothèque
juste après avoir touché un réglage.

## Les refus se disent avant l'invite VPN

**Le profil réseau PS2 conditionne aussi le fait de rejoindre**, pas seulement
d'héberger. La carte de lancement refusait d'ouvrir une session sans lui, mais le
annuaire, la liste d'amis et un code tapé passaient tous à côté de ce contrôle, et
l'invité atterrissait dans un tunnel dont le jeu n'ouvre jamais son menu local. Même
refus, mêmes mots, **dit avant l'invite VPN** — et avant la prise du créneau de
tunnel aussi, pour qu'un refus ne coûte jamais une session en cours.

Décidable seulement quand la ROM est à nous : une session pour un jeu qu'on n'a pas
ne porte aucune console, et ce cas a sa propre réponse plus bas.

De même, **rejoindre une session pour un autre jeu** ouvre un tunnel qui ne pourra
jamais rien porter : les deux émulateurs ne se trouveraient jamais. Dit avant
l'invite VPN plutôt qu'après un échec silencieux en jeu. Seuls des **titres**
différents sont attrapés : deux dumps régionaux du même jeu partagent un identifiant
de titre et sont indiscernables ici.

## Le créneau VPN unique d'Android

Créneau libre, ou déjà tenu par nous : ça part tout de suite — demander à chaque
fois mettrait une boîte de dialogue devant un démarrage de session ordinaire. Tenu
par l'autre tunnel : on attend une réponse, parce que le prendre met fin à une
partie en cours.

**Rien ici ne s'appuie sur la révocation du système** : elle fonctionne, mais elle
est silencieuse, et le perdant l'apprend en se faisant retirer son descripteur.

L'attente du tunnel a une fin : elle rend l'état en ligne, ou `null` s'il a échoué
ou trop tardé. Les deux arrivent en pratique — une autre app VPN peut nous
préempter, et une poignée de main sur un mauvais réseau n'aboutit simplement pas.
Auparavant, l'un ou l'autre laissait l'écran de chargement en place indéfiniment.
Il n'y a **pas d'adresse à attendre** : le coordinator l'assigne avant que le tunnel
démarre, donc elle est connue depuis le début.

Démonter un tunnel est du travail local — fermer un descripteur, joindre un fil —
donc c'est rapide ou c'est bloqué. Attendre plus longtemps ne ferait que retarder le
moment où l'on dit à l'utilisateur que quelque chose ne va pas.

**Pas de changement d'écran pendant l'étape de préparation**, exprès : la carte de
lancement est encore levée et tourne encore, donc elle porte cette étape elle-même.
Basculer vers un plein écran juste pour montrer un second indicateur donnait
l'impression que l'animation de la carte avait été coupée. L'étape du tunnel, elle,
en reçoit un, parce que c'est celle qui peut réellement durer.

**Le port publié est celui de l'émulateur visé**, jamais un défaut partagé :
Dolphin écoute sur 2626 là où les autres écoutent sur 24872. C'est ce que le
coordinator publie, donc ce que l'invité composera ; un défaut unique l'envoyait sur
un port muet, avec une adresse parfaitement bonne.

Un **premier battement de cœur avant d'entrer** annonce l'arrivée et ramène le
jeton qui permettra de se retirer. Sans lui, quitter la session reposerait sur la
seule connaissance d'un identifiant, ce que le coordinator n'accepte plus.

Enfin, la boucle qui attend l'adresse de l'hôte utilisait `return@repeat`, qui ne
termine que l'itération courante : chaque tentative de rejoindre subissait donc les
dix secondes complètes même quand l'adresse était là au premier essai.

## Ce qui est hissé au niveau de l'app, et pourquoi

- **La présence** (« mes amis me voient en ligne ») : muette pendant une session,
  où le battement de membre la rapporte déjà et dit à quel jeu on joue. Quitter une
  session la rallume, et son premier appel est ce qui efface « en jeu » pour tous
  ceux qui regardent. Hors session, la cadence tolère quelques échecs avant qu'un
  ami nous voie clignoter hors ligne, les entrées de présence du coordinator durant
  deux minutes.
- **Qui est là**, demandé une fois pour toute l'app : sorti de l'écran des amis
  parce que la présence n'est pas l'affaire privée de cet écran — l'arrivée d'un ami
  vaut d'être sue depuis la bibliothèque, et c'est ce qui alimente à la fois la carte
  qui glisse ici et la notification qui part quand personne ne regarde.
- **La veille qui continue app fermée** : resynchronisée dès que bouge ce dont elle
  dépend — les deux réglages, et le fait qu'il y ait quelqu'un à surveiller. La
  planification est idempotente, et une app sans amis ni alerte de version ne
  planifie rien.
- **L'entretien de la bibliothèque** : piloté depuis les réglages mais observé par
  la bibliothèque, donc possédé ici, le seul endroit d'où les deux écrans pendent.
  La révision est ce qui fait reconstruire la grille : la bibliothèque ne lit jamais
  que le cache partagé du dépôt, et incrémenter ce nombre lui dit que le cache a
  bougé.
- **Les verdicts de compatibilité**, lus du cache **synchroniquement** pour que les
  perles soient là à la première image : un avertissement qui arrive une seconde
  après que la grille est dessinée est un avertissement devant lequel le joueur a
  déjà défilé. L'appel réseau ne fait jamais que le remplacer, et jamais par moins
  qu'il n'avait. Le catalogue éditorial suit le même patron pour les mêmes raisons,
  mais il est lu par une page de panneau et non par chaque tuile, donc rien ne
  l'attend.

## Deux routes qui ne sont pas des sessions

La route Kaeru (DS) et l'ad hoc public PSP portent une ROM et **pas** une session :
rien n'est créé, rien n'est rejoint, aucun autre joueur n'est impliqué. La PSP a un
écran plutôt qu'une carte parce qu'on le quitte pour aller configurer PPSSPP et
qu'il faut retrouver sa place au retour.

## L'écran d'ouverture tient le logo entre deux durées

Sans lui, l'app s'ouvrait sur une grille vide surmontée d'un indicateur de
chargement — la bibliothèque étant parcourue au moment même où l'écran d'accueil
se compose, et un dossier de ROMs raisonnablement rempli prend quelques secondes
à lire. Le premier écran de l'app était donc son plus laid. Ici le parcours se
fait *derrière* le logo, et l'accueil ne paraît qu'une fois rempli.

Deux durées, et elles tirent en sens contraires :

- Un **minimum** retient le logo même quand le cache est déjà chaud. Une
  animation qui dure trois images ne se lit pas comme une ouverture mais comme un
  clignotement — le même défaut déjà corrigé sur l'écran de préparation, sauf
  qu'ici l'écran est *toujours* traversé.
- Un **maximum** le fait céder quand le parcours s'éternise. Un premier scan sur
  une grande carte SD peut dépasser dix secondes, et retenir le joueur devant un
  logo aussi longtemps serait pire que de le laisser regarder la bibliothèque se
  remplir : elle a son propre indicateur pour ça.

Délibérément non focalisable et sans contrôle : rien à viser à la manette, donc
rien à signaler. Le curseur reprend sa place sur la grille.

## Le logo est centré seul, et la barre d'état n'existe pas

Empilés dans une colonne centrée, c'est la **paire** qui se centrait : la barre
poussait le logo vers le haut de la moitié de ce qu'elle occupait. Mesuré sur la
Thor, les anneaux tombaient 30 px au-dessus du milieu de l'écran. La barre est
donc positionnée par rapport au centre sans entrer dans le calcul de mise en page
du logo.

L'autre moitié du décalage venait de l'image elle-même : le PNG portait 113 px de
vide à gauche et aucun à droite, 92 en haut contre 142 en bas. Recadré sur son
contenu, ce qui est ce qui fait enfin que « centré » veut dire ce qu'on lit à
l'écran.

**L'heure et la batterie n'ont rien à faire sur la première image.** Le splash
est un noir plein tenu quelques secondes, et la barre d'état d'Android s'y
détachait comme une ligne de texte étrangère posée sur le logo — c'est la seule
seconde de l'app où l'on ne peut rien faire, donc la seule où l'on regarde
vraiment ce qu'il y a autour. On les cache le temps du logo, et on les rend en
partant : le reste de l'app en a besoin, un joueur qui cherche un jeu veut savoir
combien il lui reste de batterie.

## PSP en ligne : deux volets, et centré sur l'écran

Cet écran était le dernier construit en portrait : deux cartes pleine largeur et
deux boutons de 56 dp empilés dans une colonne défilante, dont la Thor ne montrait
qu'un tiers — et les instructions pour lesquelles on vient étaient précisément
dans les deux tiers invisibles. À gauche le jeu et ce qu'est ce mode, à droite ce
qu'il y a à faire et les deux boutons qui le font.

Centré sur l'**écran**, pas sous l'en-tête. Réserver la marge du haut centrait la
carte dans ce qui restait *sous* le titre, donc 87 px trop bas. Un plafond de
hauteur a été essayé pour pouvoir centrer sans risquer de passer derrière
l'en-tête : il **rognait** le contenu au lieu de le comprimer, la colonne de
gauche n'étant pas défilante, donc ce qui déborde disparaît. Retiré.

Ce qui rend le centrage simple possible est que la carte a maigri : 310 dp sur
les 468 de l'appareil, son bord haut tombant à 79 dp là où l'en-tête s'arrête à
68. Le `heightIn` qui reste n'est qu'une butée d'écran, et il ne se déclenche pas
ici.

## Le préchargement tourne, et l'app se compose derrière lui

Le logo était un écran **à la place** de l'app : quand il s'effaçait, la
bibliothèque commençait seulement à se composer — mesurer la grille, demander ses
jaquettes, poser son curseur. D'où les quelques centièmes de seconde où les tuiles
n'étaient pas là, malgré des caches déjà chauds : ce qui manquait n'était plus les
données, c'était le travail de rendu.

Le logo est donc une **couche par-dessus** : tout ce qu'on voit ensuite est
composé, mesuré et peint pendant qu'il tient l'écran. Quand il s'en va, il ne
reste rien à faire — il découvre une image déjà finie.

Le budget accordé aux préchauffages suit la même logique : les quatre premières
secondes sont gratuites, c'est le plancher pendant lequel le logo reste de toute
façon. Les deux suivantes, seul un démarrage à froid les dépense — index de
dossiers à construire, jaquettes à décoder pour la première fois. Le plafond de
l'écran de chargement reste au-dessus : un préchauffage qui traîne rend la main de
lui-même, il ne peut pas retenir l'app.

**La règle « une fois par processus » est partie.** Android garde le processus
vivant plusieurs minutes après le départ du joueur, donc rouvrir depuis le
lanceur sautait le logo entièrement et tombait droit sur la grille — lu comme un
splash cassé plutôt que comme un cache chaud. Le jeton est réarmé à chaque
démarrage réel, sauf pendant qu'une session vit : revenir de l'émulateur doit
ramener dans l'écran du jeu, pas devant un logo.

## Une tentative en vol ne doit pas téléporter quelqu'un qui a renoncé

L'écran d'attente n'avait aucune sortie : ni bouton, ni légende, ni changement
d'état. Un tunnel qui ne monte pas ou un VPS qui ne répond pas laissaient le
joueur devant un rond qui tourne, avec la touche Home pour seule issue.

Il peut renoncer — mais renoncer ne suffit pas à arrêter une coroutine déjà
partie, qui aboutirait plus tard et téléporterait dans une session quittée
quelqu'un revenu tranquillement à sa bibliothèque.

Chaque tentative retient donc le numéro qu'elle portait au départ, et n'a le droit
d'ouvrir une session que si c'est toujours le sien. Renoncer incrémente le
compteur, ce qui suffit à rendre orpheline toute tentative en vol sans avoir à
savoir où elle en est.

## Les icônes de l'app sont dessinées, pas tapées

Elles étaient du texte : `‹` pour le retour, `✕` pour retirer un ami, un emoji
dans chaque état vide. Trois problèmes, et le troisième a décidé — un caractère
est positionné par les métriques de sa police, donc il ne tombe jamais tout à fait
au centre du bouton qui le porte ; un caractère hérite de la police système d'un
appareil dont le jeu d'emoji n'est pas le vôtre ; et un emoji est l'illustration
de quelqu'un d'autre, à la graisse de quelqu'un d'autre, au milieu d'un monde
entièrement moulé.

Toutes sont construites pareil : un carré de 24 unités, bouts ronds, jonctions
rondes, une seule graisse de trait. **C'est tout le système d'icônes, et ce qu'on
ajoutera plus tard se dessine aux trois mêmes règles.**

Deux conséquences de dessin qui reviennent :

- **Le point est un trait sans longueur.** Un bout rond le rend comme un disque
  exactement à la graisse de l'icône, donc il reste du même dessin au lieu d'être
  une forme pleine passée en fraude dans un système qui n'a que des traits.
- **Pas de forme dans une forme.** Le triangle d'avertissement a été retiré : la
  pastille est déjà une perle ronde à liseré blanc, un contour dans un contour se
  lit à l'étroit, et il laissait la marque elle-même trop petite. Ses deux
  voisines sont des figures d'un seul trait qui remplissent la perle — une coche,
  une croix — et celle-ci est la troisième de ce jeu.

Un glyphe est le même partout où il apparaît, sinon ce n'est plus le même glyphe :
la loupe vivait en deux exemplaires à des proportions différentes, et le chercheur
en aurait fait un troisième.

## Le chercheur de sessions sonde, il n'écoute pas

Les sessions durent une heure au plus et la liste est courte : une socket serait
beaucoup de machinerie pour un écran sur lequel on reste vingt secondes.

Le coordinator ne connaît qu'un **titre** : il n'a ni jaquette ni console à
offrir, et il n'a pas à en avoir — ce sont des ROMs, qui vivent sur l'appareil. On
apparie donc le titre annoncé contre ce qu'on a localement, et quand ça tombe la
carte montre la vraie icône du jeu. Sinon elle montre l'hôte : une session reste
identifiable par qui l'ouvre.

Les faits d'une carte sont des **pastilles**, pas une phrase à points. Une phrase
doit se lire en entier pour en extraire un détail ; des pastilles se balaient, ce
qui est ce qu'on fait devant une liste de sessions.

## Une seule destination nommée par écran

La carte de session était la destination nommée de la manette, du temps où elle
était le premier contrôle de l'écran. Depuis qu'une barre de recherche la précède,
elles la portaient **toutes les deux** — et un `FocusRequester` partagé entre
douze nœuds ne désigne plus rien : le curseur descendait de l'en-tête vers une
carte au hasard en sautant la recherche, et « haut » depuis n'importe quelle carte
remontait droit au bouton retour au lieu de passer à la carte du dessus.

## Un écran vide se centre sur l'écran, pas sous l'en-tête

Il n'y avait que la marge du haut — la bande de l'en-tête — donc le bloc était
centré dans ce qui reste **sous** l'en-tête, et son milieu tombait une
cinquantaine de dp sous le milieu de l'écran. Un écran vide n'a rien d'autre à
regarder : le décalage se voit.

La marque tient sur un objet moulé, le même que le bouton rond de l'en-tête : un
état vide fait encore partie du plateau, il n'y est pas un trou. Sauf quand il
parle justement d'une absence, et là c'est une alvéole — mais **avec sa marque
dedans**. L'alvéole a d'abord été laissée nue, au motif qu'un emplacement sans jeu
est déjà ce que la grille dessine ; vue en vrai, elle ne se lit pas comme une
métaphore, elle se lit comme une icône qui n'a pas chargé.
