# Le coordinator, et la mise à jour de l'app

Le récit qui vivait dans `network/CoordinatorClient.kt` et
`update/UpdateInstaller.kt`, sorti du code le 2026-08-24
(cf. `docs/STYLE_COMMENTAIRES.md`). Titres = ancres citées depuis le code.

## Un jeton, parce que le code de session est public

Le annuaire publie le code : il ne peut donc pas servir d'autorisation. **Sans
jeton, les routes qui modifient une session n'exigeaient rien** — n'importe qui
pouvait réécrire l'adresse d'hôte d'un jeu inconnu, la fermer, ou éjecter un
joueur.

Le jeton est rendu **à la création seulement**, et ne quitte jamais l'appareil.
C'est lui qui autorise, par exemple, à déclarer que le salon de l'hôte existe ou
n'existe plus — et seul l'hôte a la réponse, le coordinator ne pouvant pas voir
dans l'émulateur.

Même raisonnement un cran plus bas pour les membres : le coordinator **ne publie
plus les codes d'ami** dans la liste des membres, les lire suffisant à suivre
n'importe qui. L'identifiant qu'on y trouve n'est donc plus le nôtre, et c'est le
« handle » rendu par le battement de cœur qu'il faut comparer pour se reconnaître.

Le jeton de membre, lui, n'arrive **qu'au premier battement**, celui qui nous
enregistre ; ensuite le champ est absent, délibérément. Le rendre à quiconque le
redemande revenait à le donner à quiconque connaît un identifiant.

## Distinguer « ça n'existe pas » de « je n'ai pas pu demander »

La distinction compte pour le joueur, pas seulement pour le journal : **un code
qui n'existe pas est son erreur à corriger, un coordinator injoignable est la
nôtre.** Les confondre — ce que faisait un `getOrNull()` nu — disait à quelqu'un
dont le réseau était coupé que la session de son ami n'existait pas.

## Les défauts d'un champ absent sont choisis dans un sens précis

**« L'hôte a-t-il ouvert son salon ? » vaut vrai quand le champ manque**, et la
direction est tout le sujet : l'absence vient d'un coordinator plus ancien, qui
ne connaît pas la question. Le défaut inverse aurait bloqué **tous** les invités
jusqu'au déploiement — et un réglage de séquencement qui empêche de jouer est pire
que le désordre qu'il corrige.

Inversement, **la console est envoyée explicitement** : le coordinator ne la
devine pas, il ne voit qu'un titre et un titleId que la 3DS et la Switch écrivent
pareil. Ne rien dire veut dire « pas de salon ».

Et **« privée » n'est envoyé que quand c'est vrai** : le coordinator traite
l'absence comme « publique », donc taire le défaut évite de faire dépendre un
comportement d'un champ que l'app pourrait un jour oublier.

Un ami absent d'une réponse est hors ligne : ce type **n'a pas de drapeau
« en ligne »**, sa présence *est* le signal. Et seuls les codes qu'on envoie
peuvent revenir — il n'y a **aucune route de listage** et aucun annuaire derrière.

## Le salon Eden sur le VPS change la forme d'une partie Switch

Au lieu qu'un joueur héberge sur son téléphone et que l'autre le rejoigne à travers
le tunnel, **les deux rejoignent le même salon public**. Le maillon « un joueur doit
être joignable » — le plus fragile de la chaîne, et le seul qui dépende de
l'appareil de l'hôte — disparaît. Prouvé le 2026-08-05 avant d'être écrit.

`null` pour toute autre console, et `null` aussi quand le coordinator n'a pas de
salon à offrir : l'app retombe alors sur l'hébergement par un joueur, qui n'a pas
disparu. **Un salon incomplet compte comme pas de salon** : les trois champs sont
nécessaires pour composer, et retomber sur l'hébergement par un joueur vaut mieux
que viser un port deviné.

## Réclamer une adresse est idempotent sur la clé

Idempotent côté serveur, pour qu'un réessai après une réponse perdue **retombe sur
la même adresse** plutôt que d'en brûler une seconde et de laisser le relais router
vers un pair derrière lequel il n'y a personne.

L'identifiant de profil permet au coordinator de reconnaître l'hôte réclamant sa
propre adresse et de publier `host_ip` lui-même, si bien que l'app n'a jamais à le
lui rapporter.

Un champ absent chez un invité — et absent aussi d'un coordinator antérieur au
2026-08-03 — veut dire dans les deux cas que l'interface n'a qu'une adresse, ce qui
est exactement l'ancien comportement. Tester la nullité couvre les deux d'un coup
**et évite le piège d'`optString`, qui rend la chaîne `"null"` sur un `null` JSON**.

## La présence hors session, et pourquoi elle s'éteint dedans

Le battement de session rapporte déjà la présence, et dit à quel jeu on joue.
Passer « hors session » en partant efface ça tout de suite, plutôt que de laisser
les amis regarder une partie terminée.

---

# La mise à jour

## Pourquoi ceci est acceptable alors que la revue S5 l'avait exclu

`docs/SECURITY_REVIEW.md` (S5) avait tranché : l'app ne télécharge ni n'installe.
Le raisonnement reposait sur une chose — **mettre à jour depuis une URL lue sur le
réseau est un chemin d'exécution de code**, et la revue suppose le réseau non
fiable.

Ce chemin est rouvert, et refermé par **trois verrous**, dans cet ordre :

1. **L'URL n'est pas suivie telle quelle.** Seul l'hôte du coordinator est
   accepté, en HTTPS. Une `url` pointant ailleurs dans `latest.json` est ignorée :
   compromettre le JSON ne suffit donc pas à faire télécharger un binaire
   arbitraire, il faudrait déjà tenir le serveur.
2. **C'est la signature qui décide, pas la provenance.** L'APK téléchargée est
   ouverte ici et son certificat comparé à celui de l'application qui tourne. Un
   binaire signé d'une autre clé est jeté **sans jamais être montré à Android**.
   C'est le verrou qui tient encore « le jour où le serveur n'est plus le nôtre »,
   l'hypothèse que la revue posait explicitement.
3. **Rien ne démarre sans une pression.** Le téléchargement commence quand le
   joueur presse « Installer », jamais tout seul.

Sur ce troisième point, **mesuré sur la Thor et contre toute attente : Android ne
montre aucune boîte de confirmation.** Depuis Android 12, une app qui se met à jour
*elle-même* avec la même signature est installée sans rien demander, et la session
va droit à `INSTALL_SUCCEEDED`. Le relais de résultat reste nécessaire pour autant :
rien ne garantit ce raccourci d'une version ou d'un fabricant à l'autre, et là où
il n'existe pas, le bouton ne ferait visiblement rien sans lui.

**Une conséquence à assumer plutôt qu'à cacher : la pression sur « Installer » est
le seul consentement recueilli.** C'est le verrou 2 qui porte la sécurité, pas un
écran système — et il est plus strict que ce qu'un navigateur offrirait sur le même
lien, puisque le refus tombe avant qu'Android ouvre le fichier, avec un message
disant ce qui s'est passé plutôt que « échec d'analyse ».

## Le verrou central : deux questions, et les deux doivent tenir

Le certificat est le nôtre, **et** la version est celle annoncée. La seconde ferme
le retour en arrière — servir une vieille version signée, donc authentique, pour
ramener un défaut déjà corrigé.

**On compare des certificats, pas des paires de clés.** `hasMultipleSigners`
sépare deux mondes à ne pas mélanger : une app à plusieurs signataires n'a pas
d'historique de rotation, et lire le mauvais tableau rend une liste vide — qui se
comparerait « égale » à une autre liste vide. D'où le refus explicite quand il n'y
a rien à comparer.

Et **intersection, pas égalité** : après une rotation de clé, l'app installée
connaît son historique et la nouvelle APK n'en porte qu'une partie. Exiger
l'égalité ferait échouer la seule mise à jour qu'on aurait vraiment besoin de voir
réussir ce jour-là.

## Trois issues, pas deux

La distinction a été payée : un booléen faisait rapporter à un transfert **qui
avait parfaitement démarré puis calé en route** un « cette version n'est pas encore
téléchargeable ici ». Le joueur partait chercher un binaire absent d'un serveur qui
le servait très bien.

**60 s de délai** : une APK de 32 Mo sur un réseau domestique n'est pas un appel
d'API. À 30 s, un transfert calé un instant était abandonné — mesuré en vrai sur la
Thor, `broken pipe` côté serveur à la seconde près.

Le fichier va **dans le cache** : si l'installation réussit il ne sert plus à rien,
et si elle échoue Android le récupère tout seul quand la place manque. Une APK
oubliée dans les documents du joueur serait la seule trace durable de cette
fonction.

Le plafond de taille laisse de la marge au-dessus des 32 Mo actuels et **empêche un
serveur bavard de remplir le cache de l'appareil pendant qu'on regarde ailleurs**.

## Deux refus qui ne sont pas des erreurs

**« Android ne laisse pas encore Emufii installer des applications »** n'est pas
une erreur : c'est une permission à accorder une fois, et l'app ouvre l'écran
exact pour le faire.

**Un lien qui pointe ailleurs n'est pas traité comme une attaque** : le champ sert
aussi à publier une page à lire, et le bouton « Voir » l'ouvre dans le navigateur,
où le joueur juge.

Sans `url` publiée, on retombe sur le `/download` du coordinator : **le serveur qui
a annoncé la version la sert aussi**, ce qui évite d'avoir à garder deux champs
cohérents pour publier.

`PackageInstaller` plutôt qu'`ACTION_INSTALL_PACKAGE` : ce dernier est déprécié
depuis Oreo et réclame un `FileProvider` plus des permissions d'URI juste pour
nommer un fichier qu'on possède déjà.

## La signature du client change le coût, pas l'identité

L'adresse du coordinator voyage en clair dans l'APK — un `strings` sur le dex
suffit à la lire — et l'API n'exigeait rien de ses appelants. Une session pouvait
donc être créée en production avec un simple `curl`, mesuré le 2026-08-09.
N'importe qui tenant l'APK publique pouvait faire tourner ses parties sur un VPS
qu'il ne paie pas.

**Ce n'est pas une preuve d'identité et ça ne peut pas l'être.** Le client est
entre les mains de la personne même qu'on veut tenir dehors : la clé est dans le
binaire, donc extractible, et prétendre l'inverse serait un mensonge. Ce que la
signature change est le **coût** : lire une URL ne suffit plus, il faut démonter
l'APK, y trouver la clé, et réimplémenter ce calcul. Et comme la clé change à
chaque version, l'exercice est à refaire à chaque fois.

Le reste de la défense est côté serveur, où elle se trouve vraiment : le
coordinator journalise la version qui l'appelle, ce qui rend un client périmé ou
étranger visible, donc blocable.

La forme : `HMAC-SHA256(secret, méthode + "\n" + chemin + "\n" + horodatage +
"\n" + SHA-256(corps))`, en hexadécimal minuscule. Le corps entre dans le calcul,
sans quoi une signature valide pour une requête le serait pour n'importe quelle
autre au même chemin. L'horodatage borne à quelques minutes la rejouabilité d'une
signature interceptée.
