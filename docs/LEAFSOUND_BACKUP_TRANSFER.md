# LeafSound — sauvegarde, transferts et téléchargements

## Fonctions livrées dans 0.5.0

### Sauvegarde JSON
Depuis l'accueil, **Sauvegarde JSON** ouvre le sélecteur Android pour choisir où créer `LeafSound-sauvegarde.json`.
Le fichier contient le schéma/version, la date d'export, les URI et noms des titres, les favoris, les playlists et la playlist active.

### Import JSON
**Importer JSON** lit une sauvegarde LeafSound et fusionne les éléments accessibles dans la bibliothèque actuelle. Les playlists et favoris ne sont réappliqués que pour les URI de fichiers accessibles.

**Limite Android importante :** les URI SAF et autorisations d'accès sont propres à l'appareil et au fournisseur de fichiers. Un JSON seul ne transporte pas les fichiers audio et ne peut pas rétablir automatiquement une autorisation révoquée ou une URI d'un autre téléphone. Sur un nouvel appareil, importer d'abord les fichiers audio, puis les ajouter à la bibliothèque ; une restauration portable complète avec correspondance par empreinte de fichier est une évolution prévue.

### Export des fichiers audio
**Exporter les audios** demande un dossier de destination (stockage interne, carte SD ou support USB proposé par Android) puis copie les titres de la bibliothèque. Les originaux ne sont jamais supprimés. Les fichiers non lisibles ou non copiables sont comptabilisés comme échecs.

### Téléchargement
**Télécharger** accepte un lien HTTPS direct vers un fichier que l'utilisateur est autorisé à télécharger. Android DownloadManager gère le téléchargement et sa notification. Destination : dossier public `Musique/LeafSound`.

Les flux protégés, les contenus soumis à DRM et le téléchargement de musique depuis Spotify/Deezer ne sont pas pris en charge. Pour ces services, LeafSound doit utiliser leurs applications et intégrations officielles, sans extraire ni retransmettre leur audio.

## Import audio
**Ajouter des musiques** utilise le sélecteur Android et accepte plusieurs fichiers audio. Les fichiers peuvent provenir de la mémoire, carte SD ou USB lorsque le fournisseur Android les expose.

## Sécurité et non-destruction
- Les exports copient les fichiers ; ils ne déplacent ni ne suppriment les originaux.
- L'import JSON fusionne au lieu d'effacer la bibliothèque existante.
- Les téléchargements sont HTTPS uniquement dans cette première version.
- Les autorisations de stockage sont accordées via les sélecteurs Android, pas par accès global au stockage.

## Évolutions prévues
- Archive portable ZIP incluant les fichiers audio et un manifeste JSON.
- Restauration avec empreintes SHA-256 pour retrouver les morceaux déplacés.
- Export/import M3U et playlists par dossier.
- Sources de téléchargement légales intégrées (podcasts RSS avec téléchargement explicite, catalogues libres et domaines autorisés).
