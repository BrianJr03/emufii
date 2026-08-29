# Le tunnel : WireGuard, le créneau VPN unique, et les mesures

Le récit qui vivait dans `wg/` et `tunnel/TunnelSlot.kt`, sorti du code le
2026-08-24 (cf. `docs/STYLE_COMMENTAIRES.md`). Les pièges réseau côté relais sont
dans le `CLAUDE.md`. Titres = ancres citées depuis le code.

## Pourquoi Emufii a son propre `VpnService`

`GoBackend` en livre déjà un, donc à première vue Emufii n'en a pas besoin. Mais
il le démarre ainsi — lu dans les sources de la bibliothèque avant d'écrire une
ligne :

```java
context.startService(new Intent(context, VpnService.class));
```

`startService`, et **`startForeground` n'est appelé nulle part** dans la
bibliothèque. Le tunnel vivrait donc dans un service d'arrière-plan, qu'Android
est libre de tuer dès qu'Emufii quitte le premier plan — c'est-à-dire
**exactement quand le joueur bascule vers l'émulateur pour jouer**. Cette app a
déjà payé cette facture une fois, sur le service LAN de Dolphin : sans premier
plan, le segment LAN tombait.

`GoBackend` est `final` et ne peut pas être dérivé. `GoBackend.VpnService`, lui,
ne l'est pas, et l'`onCreate()` dont il hérite complète un futur statique que
`GoBackend` consulte avant de démarrer quoi que ce soit. D'où la manœuvre :
**dériver, déclarer la sous-classe au manifeste, et la démarrer nous-mêmes en
premier plan.** `GoBackend` trouve alors le futur déjà complété, saute son propre
`startService`, et travaille à travers notre instance.

Cela tient à un détail interne de la bibliothèque, et c'est pourquoi **le service
possède le cycle de vie du tunnel plutôt que le gestionnaire** : faire le
changement d'état depuis `onStartCommand` rend l'ordre — `onCreate`, puis
`startForeground`, puis l'état — une propriété du code plutôt qu'un espoir sur le
minutage.

Corollaire : **l'`onDestroy` de la bibliothèque ne doit pas être sauté.** Il
éteint le tunnel et remet à zéro le futur statique qui permet à `GoBackend` de
trouver ce service ; le sauter laisserait un futur pointant sur une instance morte,
et le tunnel suivant ne monterait jamais.

Et **balayer Emufii hors des récents doit descendre le tunnel** : un service de
premier plan survit par conception au retrait de sa tâche, donc sans ça le tunnel —
et la clé VPN dans la barre d'état — survivaient à l'app, sans plus rien à l'écran
pour les arrêter.

## Le verrou Wi-Fi n'est pas un détail de confort

Mesuré entre deux Thor distantes, à travers le relais : **25 % de perte à un ping
par seconde, 0 % à trois pings par seconde**, et une gigue de 46 à 369 ms. C'est
la signature de l'économie d'énergie Wi-Fi : un trafic clairsemé laisse la radio
s'assoupir, et les paquets rares paient l'attente ou se perdent.

Or **le LDN de la Switch — que l'amont d'Eden décrit comme « extrêmement sensible
à la latence et à la perte » — fait sa poignée de main avec précisément ces
paquets rares.** Un jeu qui se connecte puis abandonne au bout de sept secondes,
deux fois de suite, est exactement ce que produit une poignée de main perdant un
paquet sur quatre.

`WIFI_MODE_FULL_LOW_LATENCY` fait deux choses de plus que l'ancien `HIGH_PERF` :
il coupe l'économie d'énergie et demande au pilote de privilégier la latence sur
le débit. Il n'agit que l'écran allumé et l'app au premier plan — ce qui, pendant
une partie, désigne l'émulateur et pas nous : le verrou est donc **tenu**, et le
système l'applique quand il le peut.

## Trois nombres mesurés dans la configuration

- **Le keepalive.** Les correspondances NAT des opérateurs expirent bien en
  dessous d'une minute, et le relais ne peut joindre qu'un pair pour lequel il a
  une correspondance. Abaissé depuis 25 s le 2026-08-02 : entre deux bouffées la
  radio Wi-Fi s'endort, et le paquet de réveil payait jusqu'à 369 ms contre 46 ms
  sur un lien tenu éveillé.
- **Le MTU à 1420.** Sans lui le backend retombe sur 1280, le plancher IPv6. 1420
  est le défaut de wg-quick et il est sûr ici : l'en-tête WireGuard coûte 60
  octets sur IPv4, donc le paquet porteur fait 1480 et traverse aussi bien un lien
  1500 qu'un PPPoE 1492. **Mesuré sur la Thor le 2026-08-04 : 1252 octets passent,
  1300 se perdent, rien ne fragmente.** Cette perte silencieuse était le mode de
  défaillance du LDN.
- **La topologie est en étoile**, donc la configuration ne porte **la clé d'aucun
  autre joueur** : le seul pair d'un client est le relais.

La configuration est rendue **en texte** plutôt que par les constructeurs de la
bibliothèque : une seule forme à réussir, journalisable quand un tunnel refuse de
monter, et c'est le format qu'emploie la documentation WireGuard.

## La seconde adresse de l'hôte, sans quoi ses paquets se perdent

L'hôte a une seconde adresse en `.254`, nulle chez un invité. **Le relais y
réécrit la connexion de l'hôte vers lui-même**, et c'est celle que distribue le
serveur ad hoc. Sans elle ici, les paquets envoyés à l'hôte arrivent par le tunnel
et sont jetés.

## Le DNS n'est annoncé que pour la PS2

Nul partout ailleurs, **délibérément : un VPN qui annonce un DNS prend la main sur
la résolution de tout l'appareil.** Les autres consoles composent des adresses, pas
des noms.

La PS2 fait exception parce que **le clavier d'ARMSX2 n'a pas de touche point** :
aucune adresse IPv4 ne peut y être saisie. Local Link résout les noms, et une seule
étiquette suffit — le relais répond à ce nom par la sentinelle.

## L'identité WireGuard doit persister

La raison est côté serveur : **le coordinator est idempotent sur la clé publique**,
donc la même clé obtient toujours la même adresse. Une clé régénérée à chaque
session prendrait une adresse neuve à chaque fois et laisserait le relais tenir une
route vers un pair derrière lequel il n'y a personne — ce que l'autre joueur voit
comme une partie qui se connecte puis devient muette.

Gardée dans les préférences privées de l'app, à côté du profil et de la liste
d'amis. **Pas dans le keystore** : WireGuard a besoin de la clé privée en clair en
espace utilisateur pour faire sa poignée de main, donc une clé adossée au matériel
qu'il ne pourrait jamais extraire serait inutile ici. Le stockage privé de l'app
est la frontière honnête, et c'est déjà celle sur laquelle repose le code d'ami.

L'effacer va avec la suppression du profil : la clé publique est un identifiant
stable que le coordinator voit, donc la laisser derrière survivrait au profil dont
elle venait.

## « En ligne » veut dire moins qu'on ne croit

`Tunnel.State` ne distingue que haut et bas, donc « en ligne » signifie **que
l'interface existe** — pas qu'un autre joueur a rejoint, ni même qu'une poignée de
main a abouti. L'app confirme la joignabilité réelle en pinguant le relais, ce
pour quoi son adresse est rendue.

## Android n'a qu'un créneau VPN, et Emufii a deux tunnels

Le tunnel de session, et le tunnel DNS qui envoie la DS vers Kaeru. **Celui qui
appelle `establish()` en second gagne, et l'autre est révoqué sans que l'app ni le
joueur soient consultés.**

Ce n'est pas une course théorique : quitter l'écran WFC par le geste de retour
système **laisse son tunnel debout**, et créer une session ensuite coupe la partie
DS en plein jeu. Ça marche dans l'autre sens aussi — le service de session est
`START_STICKY` et en premier plan, donc il survit à l'activité.

Qui tient le créneau est **dérivé des états que les services publient déjà**,
plutôt que suivi séparément. Trois règles :

- **`Starting` compte comme tenu.** `establish()` a peut-être déjà eu lieu, et le
  traiter comme libre est exactement la fenêtre où deux tunnels se percutent.
  `Stopping` et `Error` ne comptent pas : le descripteur est en train de partir, ou
  n'a jamais été ouvert.
- **La session gagne les égalités** : un chevauchement veut dire que l'un des deux
  est un reliquat en cours de démontage, et la session est celui dont la perte
  coûte quelque chose au joueur.
- **Demander le créneau qu'on tient déjà est gratuit** : déplacer le tunnel de
  session vers un autre jeu est un redémarrage, pas un conflit.

## Balayer l'app hors des récents coupe le tunnel

Sans ça, le tunnel survivait à l'app indéfiniment. `START_STICKY` rendait la
chose pire qu'un simple oubli : tuer le processus et Android ramène le service,
tunnel compris, sans aucune Emufii à l'écran pour l'arrêter. L'icône de clé reste
dans la barre d'état et la seule issue passe par les réglages VPN d'Android.

`onDestroy` ne suffisait pas seul : balayer la tâche ne détruit pas un service de
premier plan démarré, ce qui est tout l'intérêt d'en avoir un. `onTaskRemoved`
est le seul signal qu'Android donne pour « l'utilisateur en a fini avec cette
app », donc c'est là que la décision appartient.

**Délibérément inconditionnel.** Balayer l'app pendant que melonDS est encore en
session coupera la résolution de noms WFC sous lui, mais un tunnel que rien ne
peut atteindre est la pire des deux pannes — et l'émulateur garde sa propre tâche
dans les récents, donc le geste vise Emufii précisément.

`stopSelf` compte autant que couper le tunnel : il efface le redémarrage
collant, donc le service reste à terre au lieu d'être ressuscité.
