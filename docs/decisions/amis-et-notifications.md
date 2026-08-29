# Les amis, la présence, et ce qu'on ose annoncer

Sorti du code le 2026-08-29 (cf. `docs/STYLE_COMMENTAIRES.md`). Les titres sont
des ancres citées depuis le code.

## Ce que la veille en arrière-plan peut promettre, et ce qu'elle ne peut pas

Emufii s'installe hors magasin et n'a aucun service de push derrière elle : rien
sur un serveur ne peut réveiller cette app. Le seul mécanisme honnête qui reste
est de **demander**, de temps en temps, depuis l'appareil lui-même.

Le plancher d'Android pour un travail périodique est de quinze minutes, et Doze
l'étire encore sur un téléphone en poche. Une alerte au sujet d'un ami peut donc
arriver un quart d'heure après son arrivée, parfois plus, et un ami qui joue dix
minutes peut n'être jamais annoncé.

**C'est une limite réelle, et elle est écrite dans le texte des réglages plutôt
que cachée** : une fonctionnalité qui livre discrètement moins que promis apprend
aux gens à se méfier de chaque notification que l'app enverra jamais. Ce qu'elle
livre de façon fiable, c'est la nouvelle lente — une nouvelle version, et un ami
qui s'installe pour une soirée.

`JobScheduler` et non WorkManager : celui-ci apporterait une dépendance, une base
de données et une centaine de kilo-octets pour une tâche périodique sans
chaînage, sans contrainte au-delà du réseau, et sans résultat à observer. Le
planificateur de la plateforme fait exactement ce travail.

## Les règles d'annonce, chacune gagnée en imaginant la notification qu'elle évite

La comparaison de deux sondages est **pure**, et c'est le point : la même
fonction sert l'alerte dans l'app et le travail de fond, donc ce que les deux
annoncent ne peut pas diverger. C'est aussi la seule partie de cette
fonctionnalité qui se teste sans appareil.

- **Un ami jamais vu ne produit rien.** Le premier sondage après en avoir ajouté
  un, ou après que l'app a été tuée pendant une journée, annoncerait sinon toute
  la liste d'un coup comme si tout le monde venait d'arriver.
- **Arriver en ligne s'annonce une fois.** S'il est déjà dans un jeu à cet
  instant, c'est le jeu qui est annoncé, pas les deux.
- **Lancer un jeu s'annonce même pour quelqu'un déjà en ligne** — c'est le cas
  qui compte vraiment : il est là, et il y a maintenant quelque chose à
  rejoindre.
- **Un ami déjà dans ce même jeu ne produit rien**, quel que soit le nombre de
  sondages.
