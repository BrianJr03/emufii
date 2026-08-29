# Ce qui coûte à l'affichage, mesuré plutôt que supposé

Campagne du 2026-08-29, partie d'un constat de l'utilisateur : la grille n'était
pas fluide quand on change vite de jeu. Les titres sont des ancres citées depuis
le code.

Le compagnon de cette page est `docs/STYLE_COMMENTAIRES.md` pour la forme, et la
section « Une build debug ne se juge pas sur sa fluidité » du `CLAUDE.md` pour la
condition préalable — sans laquelle aucune de ces mesures n'a de sens.

## Le point de comparaison : Cocoon

`rip.moth.cocoonshell` 3.04, installé sur la Thor, mesuré dans les mêmes
conditions (25 descentes rapides, `dumpsys gfxinfo`).

| | Cocoon | Emufii (avant) |
|---|---|---|
| médiane / 90ᵉ | 5 ms / 6 ms | 9 ms / 31-53 ms |
| images en retard | 0 % | 12-23 % |
| état ART | `speed-profile` | `run-from-apk` |
| masques logiciels | 75 Ko / 1 | 1,1 Mo / 14 |
| cibles hors écran | 4,9 Mo / 16 | 34 Mo / 23 |
| mémoire GPU | 17,6 Mo | 61,6 Mo |
| images par pas de curseur | ~1,1 | ~2,3 |

Ce qu'on lui a pris : le profil de compilation, et la chasse aux calques
intermédiaires. Ce qu'on ne lui prend pas : ses tuiles sont légères — une image,
pas de plaque moulée, pas d'ombre portée colorée, pas de liseré — et sa sélection
ne déclenche aucune animation. Troquer notre direction visuelle contre la sienne
serait payer la fluidité en identité.

Détail d'analyse : Cocoon n'utilise **pas** les listes paresseuses de Compose
(aucune trace de `androidx.compose.foundation.lazy` ni de `LazyListState` dans
ses six fichiers dex, alors que `androidx.compose.foundation` y est). Il a son
propre défilement.

## Le profil de compilation

`app/src/main/baseline-prof.txt`, avec `androidx.profileinstaller`. Android
n'installe pas une application compilée : ART l'interprète, compile à la volée ce
qui revient souvent, et ne compile sérieusement qu'après des heures
d'inactivité — précisément après les premières sessions, celles où l'on juge si
l'app est fluide.

Mesuré, même code, même appareil, seule la compilation change :

| build | médiane | 90ᵉ | 99ᵉ |
|---|---|---|---|
| debug | 12 ms | 48 ms | 113 ms |
| release, pas encore compilée | 9 ms | 53 ms | 97 ms |
| release compilée | 9 ms | 31 ms | **46 ms** |

C'est la queue qui bouge, et c'est elle qu'on sent. Le profil est écrit à la main
avec des jokers sur les paquets chauds : l'outil officiel demande un module de
macrobenchmark, et un profil approximatif vaut infiniment mieux qu'aucun. Ce
qu'il rate est simplement compilé plus tard, comme avant.

## La source du flou ne se branche que quand quelque chose floute

`hazeSource` enregistre tout ce qu'il porte dans un calque hors écran, pour que
la dalle du clavier puisse le flouter au travers. Il était posé sur la
bibliothèque entière, **en permanence**, alors que le seul `hazeEffect` de
l'écran vit dans la dalle de recherche, qui n'est composée que clavier ouvert.
Toute la grille passait donc par une cible de rendu plein écran à chaque image,
pour personne.

Branché sur `searchOpen` et non sur `keyboardOpen` : une longueur d'avance sur
l'ouverture de la dalle, pour que le flou soit déjà enregistré quand elle arrive.

## Un calque hors écran n'est pas un réglage de dessin

Le titre de chaque tuile portait un `CompositingStrategy.Offscreen` pour un
dégradé de fondu qui ne se dessine que si le titre déborde — donc, la plupart du
temps, jamais. Quatorze cibles de rendu allouées, effacées et recomposées par
image, pour rien.

La règle : un `Offscreen` se pose **quand l'effet qui l'exige est actif**, pas en
prévision de son éventualité.

## Ce qui a été mesuré et n'a rien donné

- Le seuil de publication au second écran est déjà débattu (110 ms à l'époque) :
  ce n'était pas la cause, mais il a quand même été porté à 200 ms — voir
  `bibliotheque.md`.
- `RomTagReader.read`, `CompatDb.ratingFor` et `GameMetaDb.metaFor` s'exécutent
  sur le fil d'UI dans `PublishHovered` : suspecté, écarté. Ce sont des recherches
  dans des tables en mémoire et des opérations de chaînes.
- `rememberTileArt` ouvre trois collectes de flux par tuile : suspecté, écarté.
  `SettingsStore` est un singleton dont les flux sont adossés aux
  `SharedPreferences`, donc s'y abonner est presque gratuit.

## Ce qui reste ouvert

Des images isolées à 100-150 ms subsistent, probablement un décodage de jaquette
ou la composition d'une rangée entrante. Le pilotage par `adb` envoie un
événement toutes les ~200 ms et ne reproduit pas un pouce : les départager
demande une trace Perfetto prise pendant que quelqu'un descend réellement dans la
grille.

## Une seule horloge pour tout ce qui bouge en permanence

Deux choses tournent sans arrêt dans l'app — le fond et le curseur — et chacune
avait la sienne. Rien ne les alignait : à douze et quinze pas par seconde, elles
écrivaient à des instants différents, donc l'app redessinait **vingt-sept fois
par seconde au lieu de douze**.

Or ce qui coûte n'est pas *combien* on dessine mais *combien de fois* : chaque
repeint force la fenêtre entière — quatorze tuiles avec leurs plaques, leurs
moulages et leurs ombres — soit une douzaine de millisecondes de processeur,
quelle que soit la raison du repeint.

Les deux battent donc ensemble, et l'app ne se redessine qu'une fois par
battement. Mesuré sur la Thor, bibliothèque immobile : 85 % d'un cœur au départ,
0 % quand rien ne bouge.

**Ne pas en créer une seconde.** Tout ce qui doit avancer tout seul se dérive de
celle-ci : c'est la seule garantie que le nombre de repeints ne remonte pas en
douce au fil des ajouts. Elle est immobile si le système a coupé les animations —
ce réglage existe pour les personnes que le mouvement gêne, et c'est aussi celui
que prennent ceux qui ménagent leur batterie.

## Le dégradé du curseur n'est pas un balayage angulaire

Un `SweepGradient` tourne autour d'un centre : sur un carré ses bandes s'écartent
aux coins, et sur une rangée large elles s'écrasent aux extrémités — la couleur
n'avance plus à vitesse constante le long du trait, ce qui est précisément ce
qu'on veut voir.

Ici chaque pixel est ramené à sa **position en abscisse curviligne sur le
périmètre** du rectangle arrondi, et la couleur ne dépend que de cette distance
parcourue. Elle avance donc à la même vitesse sur un bord droit et dans un coin,
quelle que soit la forme.

```
u   = frac(t / périmètre − phase)
mix = 0.5 − 0.5·cos(2π·u)
```

Le cosinus est ce qui rend le cycle **sans couture** : il vaut 0 en 0 et en 1,
donc la couleur revient d'elle-même à son point de départ après un tour complet.
Une interpolation linéaire y aurait laissé une cassure nette qui aurait tourné
avec l'anneau.

Le rendu est un petit bitmap étiré, pas un shader par pixel : le calcul ne se
refait qu'aux changements de pas de phase, et le résultat est mis en cache.
