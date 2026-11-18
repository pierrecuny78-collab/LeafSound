# LeafSound 0.7.0 — LeafSound Connect et minuteur

## LeafSound Connect
LeafSound ajoute un catalogue personnalisable de services musicaux dans une fenêtre intégrée à l'application.

Services préconfigurés :
- Pixabay Music et Pixabay Sound Effects
- Shazam (site web ; la reconnaissance dépend des possibilités du site ou de l'application officielle)
- Spotify, Deezer, SoundCloud, Bandcamp
- Jamendo et Freesound

Actions disponibles :
- Ajouter un raccourci HTTPS personnalisé.
- Ouvrir un service dans LeafBrowser intégré.
- Ajouter ou retirer des favoris.
- Masquer ou réafficher un service.
- Réorganiser les raccourcis.
- Supprimer un raccourci sans désinstaller une application Android et sans supprimer un compte externe.
- Filtrer le catalogue sur les favoris.

Les raccourcis personnalisés et leur ordre sont conservés localement. La configuration du catalogue est incluse dans les sauvegardes JSON LeafSound.

### Limites d'intégration
LeafSound peut afficher les sites compatibles dans sa WebView. Un site peut toutefois bloquer les WebView, demander une application native ou imposer une authentification. Une application Android tierce ne peut pas être intégrée arbitrairement dans une WebView. Les fonctions Shazam, Spotify et Deezer qui nécessitent une API ou une application officielle restent soumises à leurs interfaces, autorisations et conditions. Aucun DRM ou mécanisme de téléchargement protégé n'est contourné.

## Minuteur de sommeil
Un minuteur de 5, 10, 15, 30, 45, 60 ou 90 minutes met la lecture en pause automatiquement. Il peut être désactivé avant son échéance. Le minuteur est annulé si l'activité est détruite ; il ne s'agit pas encore d'une alarme persistante après fermeture forcée ou redémarrage.

## Version
- Version Android : 0.7.0
- Version code : 8
- APK : build debug signé par la clé de débogage CI.
- Palette : LeafColour (Oasis, Noyer, Or naturel, Crème foncée, Brun sable).

## Tests à réaliser sur appareil
- Ouvrir chaque service et vérifier la compatibilité WebView.
- Ajouter, masquer, réafficher, déplacer et supprimer un raccourci.
- Exporter puis réimporter le catalogue avec la sauvegarde JSON.
- Vérifier le minuteur pendant la lecture et son annulation.
- Vérifier les commandes Bluetooth et notification en conditions réelles, téléphone immobile et véhicule à l'arrêt.
