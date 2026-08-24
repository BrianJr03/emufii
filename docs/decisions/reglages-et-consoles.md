# Réglages et consoles : les défauts, et la table d'extensions

Le récit qui vivait dans `settings/AppSettings.kt` et `library/Console.kt`, sorti
du code le 2026-08-24 (cf. `docs/STYLE_COMMENTAIRES.md`). Titres = ancres citées
depuis le code.

## Suivre le téléphone est le bon défaut, sauf pour l'accent

Langue et thème valent « ce que dit le téléphone » par défaut, pour la même
raison : **une app qui ignore le réglage du système est une app qui contredit son
utilisateur.**

Les autres valeurs existent parce que le réglage du téléphone est souvent une
plage horaire, et que quelqu'un qui lit au lit ne devrait pas avoir à le changer
pour tout l'appareil pour obtenir une bibliothèque sombre.

**L'accent est la seule exception, et c'est délibéré.** Il *peut* suivre la
couleur qu'Android extrait du fond d'écran, mais ce n'est pas son défaut : le
cyan du curseur est la signature de l'app, et **un menu de console qui change
d'identité avec le fond d'écran n'a pas d'identité**.

Ce réglage ne touche d'ailleurs pas à la règle du monde — il y a exactement *un*
accent, dépensé sur le curseur et l'action principale, tout le reste venant des
jaquettes. Il dit **quelle teinte joue le rôle**, pas combien sont à l'écran.

## L'OLED est un sombre, pas un troisième univers

Les écrans de portables visés sont OLED : un pixel noir y est un pixel éteint,
donc le fond bleuté du thème sombre consomme là où il pourrait ne rien coûter, et
laisse un halo gris dans le noir.

Il ne change **que** le fond et le remplissage des cartes ; tout ce qui lit « est-ce
sombre ? » continue de voir sombre. Ça évite de rejuger 44 composants pour un
réglage qui ne parle que de luminosité.

## La langue passe par la plateforme, le thème ne peut pas

La langue est posée par l'API de langue par-app plutôt qu'en jonglant nous-mêmes
avec une `Configuration`. minSdk vaut 33, donc elle est simplement là : Android
retient le choix d'un lancement à l'autre, l'affiche dans les réglages système à
côté de toutes les autres apps, et recrée l'activité pour que les nouvelles
chaînes prennent effet.

**Aucune API de plateforme ne possède le thème** : Android n'a pas de mode sombre
par-app sous `setApplicationNightMode` (API 31), et même celui-là ne couvre que
les deux valeurs forcées. Le choix vit donc ici et le thème le lit — ce qui a aussi
l'avantage de rendre le basculement **instantané** au lieu de recréer l'activité
comme le fait un changement de langue.

## Ce qui est stocké, c'est ce qui est refusé

Les consoles masquées sont enregistrées comme **ce qui est caché**, jamais comme
ce qui est montré, et c'est le choix porteur.

Sous l'autre forme, une bibliothèque ne contenant que des dumps 3DS aurait cinq
consoles cochées à l'installation, et **masquerait silencieusement une console
ajoutée par une version ultérieure** : l'ensemble stocké ne la mentionnerait
simplement pas. Enregistrer des refus fait que tout ce qui est nouveau arrive
**visible**, le seul défaut qui ne peut pas perdre un jeu.

Un nom auquel aucune énumération ne répond est **abandonné à la lecture**. Ça
arrive après une rétrogradation, ou si une console est un jour retirée — et le jeu
qui réapparaît est un bien meilleur échec qu'une grille amputée d'une machine en
silence.

Même principe pour la mise en page de la bibliothèque : une valeur inconnue,
écrite par une version plus récente puis rétrogradée, retombe sur le défaut au lieu
de faire tomber le lancement. Elle est gardée ici plutôt que dans l'écran, parce
que c'est un choix qu'on fait une fois et qu'on s'attend à retrouver : le perdre à
chaque retour d'un écran de session passerait pour un bug.

## Les défauts « activé », et pourquoi ce sont quand même des interrupteurs

- **Le panneau du second écran** : activé, parce qu'un joueur dont la portable a
  un écran arrière l'a acheté pour s'en servir, et **une fonction que personne ne
  trouve dans une page de réglages est une fonction que personne n'a**. C'est un
  interrupteur et non un comportement silencieux parce que le panneau reste allumé
  toute la session, et que quelqu'un qui joue dans le noir ou compte sa batterie a
  le droit de l'éteindre. Le réglage est stocké **sur tous les appareils**, même
  sans second écran : ça coûte un booléen, et un joueur qui déplace son profil vers
  une portable qui en a un arrive avec son choix intact.
- **Les notifications d'amis** : activées, parce qu'une liste d'amis dont personne
  n'est prévenu est un carnet d'adresses — l'intérêt d'ajouter quelqu'un est de
  savoir quand une partie est possible. Ça reste un interrupteur parce que la même
  fonction, vue de l'autre côté, est une app qui dit qui surveille qui.
- **Les annonces de version** : activées, et c'en est à peine une préférence.
  Emufii est installé de côté, aucun magasin ne parle pour lui, et **un correctif
  qui n'atteint jamais les joueurs ne corrige rien**. Interrupteur seulement parce
  que quelqu'un qui met à jour à la main a droit au silence.

## Chaque joueur apporte sa propre clé

La clé SteamGridDB est vide tant que le joueur n'en a pas donné une — la
bibliothèque garde alors les icônes des ROMs, de 32 ou 48 pixels de côté.

**Une clé figée dans l'APK serait la même pour tout le monde** : extractible en
ouvrant le paquet, et ce serait le compte de l'auteur qui porterait le quota et
les abus de tout le parc installé.

Le dossier Cocoon suit la même logique inverse : Cocoon Shell a déjà téléchargé
des jaquettes pour ces fichiers-là, et le joueur en a souvent recadré certaines.
Y pointer Emufii fait que sa bibliothèque a ici exactement l'aspect qu'elle a
là-bas — sans clé, sans réseau, sans attente.

## Un seul magasin pour le processus

Chaque flux ici est tenu en mémoire, et une seconde instance ne verrait pas les
écritures de la première.

Construit par écran jusqu'au 2026-08-19, et le bug qui en est né est du genre
discret : **les consoles éteintes pendant l'onboarding revenaient dès que la
bibliothèque apparaissait.** Celle-ci avait construit son propre magasin pendant
que l'onboarding était encore affiché, l'avait amorcé depuis le disque avant que
quoi que ce soit ne soit écrit, et rien ne l'a jamais détrompée. Sur le disque le
choix était bon depuis le début, donc il survivait à un redémarrage — ce qui fait
lire ce genre de chose comme un hasard.

`SharedPreferences` est déjà à portée de processus et sûr entre fils ; ce qui ne
l'est pas, c'est le `StateFlow` posé devant. Il n'y en a donc qu'un.

---

# La table des consoles

## La grille reste une grille

L'utilisateur dépose un dossier et tout ce qu'il possède apparaît ensemble. Quel
émulateur est lancé, et ce qui doit se passer sur le réseau d'abord, **c'est notre
problème, pas le sien**.

## La table d'extensions est une carte : un propriétaire par clé

C'est la contrainte qui explique deux « oublis » apparents.

**Le GameCube et la Wii sont listés sans `.iso`**, alors que c'est le nom le plus
courant d'une image de disque. Ajouter `.iso` ici ne le **partagerait** pas avec la
PSP, il le lui **prendrait** — le dernier inscrit gagne — et tout rip UMD de la
bibliothèque pointerait silencieusement vers Dolphin.

**La PS2 n'a pas une seule extension à elle**, et ce n'est pas un oubli non plus.
Sur la Thor, les six jeux PS2 et les six jeux PSP sont tous des `.iso`, dans deux
dossiers voisins — exactement la collision que le GameCube avait déjà rencontrée.

Ces consoles ne réclament donc que ce que rien d'autre n'utilise, et l'extension
partagée est tranchée **en lisant le fichier**. Un jeu PS2 arrive par son dossier
(`ps2/`), ou par la lecture des octets qui ne promeut que ce qu'elle a
positivement reconnu — un `BOOT2` dans `SYSTEM.CNF`, qui est aussi ce qui le
distingue d'un disque PS1.

## Le nom du dossier est la réponse la moins chère et la plus vraie

**Le joueur a trié le fichier lui-même**, là où une extension peut entrer en
collision et où renifler le contenu coûte une lecture.

Indexé sur le nom **normalisé** — minuscules, séparateurs retirés — pour que `PS2`,
`ps_2` et `PlayStation 2` soient une seule clé. Et sur **le dossier direct du
fichier seulement**, pas ses ancêtres : `ROMS/ps2/dumps/game.iso` est une PS2,
`ROMS/dumps/ps2-something.iso` n'est pas décidé par `ROMS`.

## Le nom réseau est un contrat, jamais un libellé

Le coordinator ne peut pas déduire la console de ce qu'il stocke — un titre et un
titleId, que la 3DS et la Switch écrivent pareil. Le nom est donc écrit ici en
minuscules stables, **jamais dérivé du libellé** : un libellé se retouche pour
l'écran, et ce nom-là décide s'il faut lever un salon sur le VPS.

Même prudence pour le nom de l'émulateur affiché au joueur : **ce n'est pas une
chaîne traduite**, ce sont des noms de produits, identiques dans toutes les
langues, et c'est ce qui est écrit sur l'icône qu'il s'apprête à voir. Coder
« Azahar » en dur dans un libellé est **comment une session Switch a fini par
annoncer « installation automatique d'Azahar » alors qu'elle pilotait Eden**.

## Le port fait partie du plan

Azahar et Eden partagent 24872, hérité de Citra ; Dolphin écoute sur 2626. Le plan
doit porter le bon, faute de quoi l'invité compose une adresse valide sur un port
où personne ne répond — **une panne qui se lit comme un tunnel cassé**.

## Les quatre familles de multijoueur

- **Salons sur le réseau de session** (3DS/Azahar, Switch/Eden, GameCube-Wii/
  Dolphin, PS2/ARMSX2 en Local Link) : il faut rejoindre le salon **avant** que le
  jeu démarre.
- **PSP** : l'ad hoc de PPSSPP n'a ni salon à créer ni boîte à remplir — la console
  cherche « le serveur ad hoc » à une adresse posée une fois pour toutes, et le
  relais la traduit vers l'hôte de la session courante.
- **WFC (DS)** : du jeu en ligne atteint **en déplaçant le DNS**, pas en
  construisant un réseau. Aucun code de session, aucun tunnel entre joueurs :
  chaque console parle au serveur de renaissance. Un second produit dans la même
  app. Il est hors de la règle « rejoindre un salon d'abord » parce qu'**il n'y a
  aucun salon, seulement un résolveur**.
- **Reconnu, mais sans chemin multijoueur construit** : ces ROMs restent dans la
  grille. Les en sortir ferait paraître la bibliothèque cassée à quelqu'un qui les
  possède.

Et trois formes d'écran, trois pilotes : Azahar et Eden par identifiants de vue,
Dolphin par imbrication de textes Compose, ARMSX2 par rangées — avec, pour ce
dernier, de vraies vues Android mais **aucune chaîne traduisible dans l'APK**, d'où
des libellés anglais en dur.

Le jeu en ligne PS2 ne passe **pas** par là : il se joue sur un serveur de
renaissance, en DNS, sans session ni tunnel. Les deux ne doivent pas être confondus
à l'écran.
