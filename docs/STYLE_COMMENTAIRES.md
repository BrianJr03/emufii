# Ce qui reste dans le code, et ce qui va dans `docs/`

Constat du 2026-08-24, remonté par **@22sh** et mesuré : **11 132 lignes de
commentaire pour 22 172 lignes de code**, soit **50 %**, sur 159 fichiers.
Plusieurs fichiers portent plus de commentaire que de code — `DolphinScreen.kt`
148 %, `Gamepad.kt` 132 %, `Console.kt` 106 %, `DiscImage.kt` 100 %. Un lecteur
qui ouvre un de ces fichiers ne lit pas du code, il lit un récit dans lequel du
code est enchâssé.

## La règle

**Le code dit ce qui est fait. `docs/decisions/` dit pourquoi, et ce qui a été
essayé avant.**

Trois choses seulement ont le droit de rester en commentaire :

1. **Le KDoc d'une déclaration publique** : une à trois lignes, ce que la chose
   est et ce qu'elle garantit. Pas d'historique, pas de justification.
2. **Un piège non déductible de la ligne**, en **une ou deux lignes**, quand
   l'ignorer casse quelque chose — un ordre d'appel obligatoire, une API qui ment,
   une valeur mesurée. S'il faut plus de deux lignes pour l'expliquer, la ligne
   reste et l'explication part dans `docs/decisions/`, avec un renvoi.
3. **Un renvoi**, une ligne : `// pourquoi : docs/decisions/<fichier>.md#<ancre>`.

Tout le reste — le récit des tentatives ratées, l'argumentaire de conception,
l'anecdote de terrain, la comparaison entre deux mondes visuels — part dans
`docs/decisions/`.

## Ce qui n'est pas un gain

Supprimer une raison **n'est pas** nettoyer. Ces raisons ont été payées : elles
disent ce qui a déjà été essayé et pourquoi ça a échoué, et c'est ce qui empêche
de le réessayer. Le but est de les **déplacer et de les indexer**, pas de les
perdre. Un commentaire retiré sans que son contenu se retrouve dans
`docs/decisions/` est une régression, pas une amélioration.

## La forme d'une entrée

Un fichier par domaine (`ui-bibliotheque.md`, `second-ecran.md`,
`identite-disques.md`…). Dans chacun, une section par décision, avec un titre
stable qui sert d'ancre — c'est ce que le code cite. Chaque section dit : **la
décision**, **ce qui a été essayé avant et pourquoi ça n'allait pas**, et **ce
qui casserait si on revenait dessus**.

Un titre ne se renomme pas à la légère : il est cité depuis le code.
