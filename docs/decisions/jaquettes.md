# Les images des jeux : d'où elles viennent, et ce qui passe avant quoi

Sorti du code le 2026-08-29 (cf. `docs/STYLE_COMMENTAIRES.md`). Les titres sont
des ancres citées depuis le code : ne pas les renommer à la légère.

## L'icône, jamais la jaquette verticale

Une ROM ne porte qu'une icône minuscule — 32×32 sur DS, 48×48 sur 3DS — et
agrandie à la taille d'une tuile, c'est le premier défaut qu'on voit en ouvrant
l'app. SteamGridDB en publie des versions haute résolution, faites et notées par
une communauté.

On prend les **icônes**, pas les jaquettes, bien que le même service serve les
deux. Une jaquette de boîte est en 2:3 : l'adopter voudrait dire passer toute la
grille en tuiles verticales, c'est-à-dire abandonner la cible « menu 3DS », qui a
des tuiles carrées. L'icône tombe dans la tuile existante sans rien changer
d'autre, et le gain visé — la netteté — est le même.

## Rien n'est empaqueté dans l'APK

Ces images appartiennent à leurs éditeurs : l'app les télécharge à l'exécution,
sur l'appareil du joueur, et les garde dans son cache local. C'est ce que fait
tout lanceur, et c'est la différence entre **afficher** une image et la
**redistribuer**.

## Chaque joueur apporte sa propre clé

Une clé figée dans l'APK serait la même pour tout le monde : extractible en
ouvrant le paquet, et ce serait le compte de l'auteur qui porterait le quota et
les abus de tout le parc installé.

Sans clé, la fonctionnalité n'existe pas : aucune requête ne part et les tuiles
gardent leur icône embarquée. Ce n'est pas un échec, juste une bibliothèque sans
icônes distantes.

## L'ordre des sources, et ce qui l'emporte

Du plus fort au plus faible :

1. **L'image choisie par le joueur.** Quand quelqu'un a pris la peine de
   corriger, le corriger en retour serait le pire des comportements.
2. **Cocoon**, quand son dossier est lié : ces images sont sur l'appareil, ont
   été téléchargées pour ces fichiers-là, et par endroits recadrées à la main.
   Préférer une supposition fraîche d'un catalogue à une image que quelqu'un a
   déjà choisie serait prendre le problème à l'envers.
3. **Le catalogue** (SteamGridDB), si une clé est donnée.
4. **L'icône de la ROM**, qui ne disparaît jamais.

## La grille s'ouvre complète, ou elle se remplit sous les yeux du joueur

L'écran de chargement attendait le parcours des ROMs puis rendait la main : la
grille paraissait alors, et se remplissait sous les yeux du joueur. Trois choses
arrivaient en retard, et aucune des trois n'était le parcours.

1. **L'index des images locales**, construit console par console à la première
   tuile qui la demande. C'est une énumération de dossier en SAF, la chose la
   plus lente de l'app : la première tuile 3DS payait tout l'index 3DS, la
   première tuile PS2 tout l'index PS2, et ça se voyait rangée par rangée.
2. **L'adresse de chaque jaquette**, résolue par tuile à sa composition.
3. **Le décodage des images**, fait au moment de peindre.

Les trois se font pendant que le logo tient l'écran de toute façon. Ensuite les
tuiles trouvent tout en cache et se peignent sur la première image.

**Rien de ce préchauffage n'est indispensable.** Chaque étape est enveloppée : un
dossier illisible, un réseau absent ou un format inattendu doivent laisser l'app
s'ouvrir exactement comme avant, images en retard. Un préchauffage qui
empêcherait d'entrer serait pire que pas de préchauffage du tout.
