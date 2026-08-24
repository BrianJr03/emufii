# L'identité du joueur, et ce qu'un dump dit de lui-même

Le récit qui vivait dans `profile/Profile.kt` et `library/RomTags.kt`, sorti du
code le 2026-08-24 (cf. `docs/STYLE_COMMENTAIRES.md`). Titres = ancres citées
depuis le code.

## Le code d'ami *est* l'identité, et il est public par conception

L'identifiant est un aléatoire stable, **non dérivé de quoi que ce soit de
l'appareil** : c'est ce sur quoi le coordinator compte la présence, donc il doit
survivre à un changement de pseudo sans identifier la personne au-delà de cette
app.

Il sert **aussi** de code d'ami, ce qui est pourquoi il est assez court pour être
lu à voix haute. C'est délibéré : **le code portant l'identité, ajouter un ami ne
demande aucun annuaire côté serveur.** Cela veut dire que l'identifiant est public
par construction — il l'a toujours été en pratique, puisqu'il voyage avec chaque
session comme identifiant d'hôte ou de membre.

**Il n'y a ni compte ni profil côté serveur** : le pseudo voyage avec chaque
session comme une simple chaîne, et l'image ne quitte jamais l'appareil. Les autres
joueurs sont donc dessinés en initiales sur une couleur dérivée de leur nom, plutôt
qu'avec une image qu'il faudrait héberger, modérer et payer. **Téléverser de vrais
avatars est une décision produit, pas une fonction manquante.**

L'identité est durable mais **liée à l'appareil** : elle vit ici et nulle part
ailleurs, donc une réinstallation fait de vous une nouvelle personne pour vos amis.
La restaurer d'un appareil à l'autre demanderait un secret de récupération et un
endroit où le mettre — exactement le compte hébergé que ce dessin évite.

Effacer l'identité en produit une sans rapport avec l'ancienne : **quiconque avait
gardé la précédente ne vous voit plus**, ce qui est le but, et la seule issue si un
code se retrouve quelque part qu'on n'avait pas prévu. Ça vous coupe aussi de votre
propre liste d'amis, donc l'appelant doit la vider et demander d'abord.

## Le pseudo est contraint là où il est saisi

Le formulaire de netplay d'Azahar **refuse un pseudo trop court** — « Invalid
address or name is too short! » — et Emufii y envoie le nom de profil tel quel.

La contrainte est appliquée **là où le nom est entré**, pour que la valeur sur le
disque soit toujours utilisable, plutôt que rustinée au point d'usage. Un garde-fou
au stockage reste en place pour les appelants qui ne passent pas par un formulaire :
**rien en aval ne doit avoir à se demander si le pseudo stocké est acceptable par
l'émulateur.**

La longueur minimale a été **observée sur l'appareil, pas lue dans une constante** :
le validateur vit dans le DEX d'Azahar et son message ne porte pas le nombre.

Le pseudo par défaut est **une sentinelle fixe, stockée telle quelle et envoyée sur
le réseau**, plutôt qu'une ressource : c'est ce à quoi « a-t-il un nom ? » se
compare, et c'est déjà persisté sur des appareils. La traduction se fait **à
l'affichage** — ce qui vous donne aussi le nom par défaut de l'autre joueur *dans
votre* langue.

## L'avatar est recopié, jamais référencé

Deux raisons de ne pas garder l'original. **L'autorisation SAF du sélecteur n'est
pas persistée**, donc garder l'URI laisserait un avatar cassé après un redémarrage.
Et **une photo de téléphone moderne fait 50 mégapixels** : en décoder une entière
pour dessiner un cercle de 40 dp est la façon dont une app se fait tuer pour
mémoire.

`inSampleSize` fait que l'image entière **n'est jamais décodée** : le décodeur
sous-échantillonne à la lecture.

---

# Ce qu'un dump dit de lui-même

## Rien n'appelle le réseau

**Un appareil portable dans un train doit pouvoir répondre à cette question**, et
un fait qui a besoin du réseau pour être lu est un fait qui disparaît exactement
quand le joueur a le temps de le lire.

Deux sources, dans cet ordre :

1. **Le serial ou l'identifiant de titre**, quand la console y estampe une région.
   C'est la parole du dump lui-même, prise sur le disque ou la cartouche, et **elle
   survit au renommage d'un fichier** — ce que les joueurs font constamment.
2. **Les étiquettes du nom de fichier**, la convention No-Intro/Redump dont tous
   les sets du monde portent le nom. Plus faible, parce que ce n'est qu'un nom,
   mais c'est tout ce que donnent un dump PS2 ou PSP, leurs serials portant des
   numéros sans rapport d'une région à l'autre.

**Inconnu s'imprime comme rien du tout.** Un panneau qui déduirait « USA » d'un
silence se tromperait pour tout joueur européen d'un jeu dont le dumpeur a sauté
l'étiquette.

Et seules **les orthographes que les deux grandes conventions emploient réellement**
sont acceptées : un appariement plus lâche — n'importe quelle parenthèse contenant
un nom de pays — transformerait `(Disney's Aladdin)` en région, et **un fait faux
imprimé en gras sur un panneau est pire qu'un fait manquant**.

## Le préfixe Sony se lit lettre par lettre

La première lettre dit le support (`S` un disque, `U` un UMD), la deuxième
l'éditeur (`L` sous licence, `C` Sony), et **la troisième est la région** — `U`
Amérique, `E` Europe, `P`/`J` Japon, `K` Corée, `A` Asie.

Lu ainsi plutôt que comme une liste de préfixes entiers : c'est comme ça que
`ULUS-10041` — **tous les jeux PSP du monde** — n'obtenait aucune région pendant que
la PS2 en avait une.

## Les positions de région sont répétées, pas partagées

Elles sont les mêmes que celles dont dépendent les clés de compatibilité, et elles
sont **délibérément répétées** parce que les deux fonctions répondent à des
questions différentes : l'une *retire* la région pour fabriquer une clé qui lui
survit, l'autre la *garde* pour l'afficher. Les lier voudrait dire que l'une change
le sens de l'autre le jour où une console est ajoutée.

La lecture est aussi exposée **sans passer par une `Rom`**, pour que les règles
puissent être figées par un test unitaire : une `Rom` porte une `Uri`, et
`android.net.Uri` est un bouchon sur la JVM de bureau — un test qui devrait en
construire une ne pourrait pas tourner là où le reste de ces règles est vérifié.

## La révision a été retirée, et pourquoi

Retirée le 2026-08-24 après lecture sur une vraie bibliothèque.

Ce qu'un nom de fichier peut livrer, c'est `Rev 1`, `Rev 2` et le `v0` de la
Switch — **et aucun des trois ne dit au joueur quoi que ce soit sur quoi agir** :
`v0` est ce qu'est toute cartouche, et un numéro de révision sans l'autre révision
pour le comparer est un fait sur une usine de pressage.

Une vraie version de titre — le `1.0.2` qu'installe une mise à jour — **n'est pas
dans le nom de fichier du tout** ; elle est dans les métadonnées du NSP, que rien
ici ne lit encore. Imprimer la version faible parce qu'elle était bon marché était
l'erreur.
