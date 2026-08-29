# Les deux sons de l'interface

Ajoutés le 2026-08-29, à la demande de l'utilisateur, qui a fourni les deux
fichiers. Les titres sont des ancres citées depuis le code.

## Deux sons, une seule famille

`hoversoundemufii` (96 ms) part quand le curseur se pose sur autre chose,
`clicksoundemufii` (144 ms) quand quelque chose est pressé. Ils vivent dans
`res/raw/` sous `sfx_hover` et `sfx_click`.

**`SoundPool` et non `MediaPlayer`.** Ces sons durent moins de deux dixièmes de
seconde, partent plusieurs fois par seconde quand on balaie une grille, et
doivent se déclencher sans latence. `MediaPlayer` prépare une source à chaque
lecture et ne sait pas en superposer deux ; `SoundPool` décode une fois en
mémoire et joue depuis là. Quatre flux simultanés : le curseur peut glisser
pendant qu'un appui résonne encore.

Ils sont décodés au démarrage de l'activité et non à la première lecture, sinon
le tout premier survol serait muet — celui qui arrive avant que le joueur ait
compris que l'app fait du son.

**Le survol est plus bas que l'appui** (0,45 contre 0,85). Il part à chaque case
traversée ; un déplacement aussi fort qu'une action ferait croire qu'il s'est
passé quelque chose.

## Le réglage d'Android fait autorité

L'app ne crée pas son propre interrupteur de sons : elle lit
`Settings.System.SOUND_EFFECTS_ENABLED`. Quelqu'un qui a coupé les sons
d'interface les a coupés pour toutes les applications, et la nôtre n'a aucune
raison de faire exception.

Les `AudioAttributes` les rangent dans la famille des sons d'interface
(`USAGE_ASSISTANCE_SONIFICATION`) : ils suivent le volume système, se taisent
pendant un appel, et ne coupent pas la musique de quelqu'un.

## Le survol se déclenche là où le curseur se dessine

`focusRing` est le point de passage **unique** de tout ce qui porte le curseur :
les contrôles ordinaires y arrivent par `controlRing`, et les tuiles de la grille
l'appellent directement avec leur propre index calculé. Un son posé là couvre
donc les deux familles, y compris la grille qui ne se sert pas du focus de
Compose.

C'est aussi ce qui garantit qu'on ne peut pas ajouter un curseur silencieux : la
règle de l'app était déjà « tout ce qui prend le focus doit le montrer », elle
devient « et le dire ».

## Le son et le clic sont un seul appel

`Modifier.tap` et `Modifier.tapOrHold` remplacent `clickable` et
`combinedClickable` dans toute l'app — 32 endroits. Ce n'est pas un modificateur
de son posé à côté du clic : c'est **le même appel**, parce qu'un `clickable`
ajouté plus tard sans son serait une chose qui répond en silence au milieu d'une
interface qui parle, et rien ne le signalerait.

Trois exceptions, toutes voulues :

- **Les avaleurs d'appui** (`onClick = {}`) restent des `clickable` nus. Ils
  n'existent que pour empêcher un appui d'atteindre ce qui est derrière — la
  dalle du clavier, la carte d'un dialogue — et un son y annoncerait une action
  qui n'a pas lieu.
- **Les voiles de renvoi** sonnent, eux : toucher à côté pour fermer le clavier
  ou un dialogue *est* une action, avec une conséquence visible.
- **Le chemin manette passe par `gamepadClick`**, pas par `tap` : `Key.ButtonA`
  n'est pas une des touches que Compose reconnaît lui-même. Le son y est donc
  posé aussi. Les deux chemins ne se croisent pas — `gamepadClick` avale
  l'appui correspondant pour qu'une pression ne compte pas double.

## Cinq gestionnaires de touche court-circuitent `tap`

Le premier essai a laissé l'ouverture d'un jeu muette : on entendait le survol de
la carte qui s'ouvrait, jamais le clic qui l'avait ouverte. La cause est
structurelle et vaut d'être nommée, parce qu'elle se reproduira.

`Modifier.tap` ne couvre que ce que `clickable` traite : le doigt, et les touches
que Compose reconnaît lui-même (Entrée, centre de la croix). Mais **cette app lit
souvent la touche de confirmation elle-même**, parce qu'elle tient ses propres
curseurs. Cinq endroits le font, et aucun ne passait par `tap` :

- `gamepadClick` — le cas général de `Key.ButtonA` ;
- `entryKeys`, la grille — appui court, maintien, et `Y` qui ouvre le menu ;
- l'écran de session, pour l'étape désignée sur le panneau arrière ;
- la dalle du clavier, dont les touches ne sont pas focalisables ;
- `PadTextField`, où confirmer ouvre le clavier.

Le son y est donc posé à la main. **La règle à retenir :** un écran qui tient son
propre curseur tient aussi son propre son. Chercher `CONFIRM_KEYS` donne la liste
complète des endroits concernés.

C'est aussi ce qui a fait tomber le `Context` : `entryKeys` est une lambda
ordinaire, pas un composable. Plutôt que de faire descendre un `Context` jusqu'à
elle, `Sfx` retient le contexte applicatif à sa préparation — et `click()` comme
`hover()` ne demandent plus rien à l'appelant.

## Couper ceux d'Android se fait vue par vue

Les sons d'interface d'Android continuaient de se superposer aux nôtres. Ce
n'est pas un réglage d'application ni un attribut de thème : `playSoundEffect`
est **gaté par le drapeau de la vue qui l'appelle**, et il n'existe pas de
commutateur global côté app.

`SilenceSystemSfx()` éteint donc ce drapeau sur `LocalView.current` **et sur
toute sa chaîne de parents** — la vue de Compose n'est pas toujours l'émettrice,
la vue de décor d'un `Dialog` en est une autre.

Il faut le poser **une fois par fenêtre**, et une fenêtre n'est pas un écran :

- l'activité (couvre tous les écrans, et les couches posées dedans — la carte de
  lancement n'est pas une fenêtre à part) ;
- chaque `Dialog` de Compose, qui ouvre la sienne : `PadDialog` et
  `IconPickerDialog` ;
- le panneau arrière, qui est une `Presentation`.

Un `Dialog` ajouté plus tard sans cet appel ramènera les deux sons ensemble.
