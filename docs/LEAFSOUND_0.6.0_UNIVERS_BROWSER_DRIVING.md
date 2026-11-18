# LeafSound 0.6.0 — Univers musicaux, LeafBrowser et conduite

## Organisation musicale

LeafSound sépare maintenant deux notions :

- **Univers musicaux** : grandes catégories transversales, créées et nommées librement (ex. Rock, Rap, Chill, Jazz, Voiture, Podcasts, Classique). Chaque univers possède un emoji et peut contenir plusieurs morceaux. Un morceau peut appartenir à plusieurs univers.
- **Playlists** : séquences précises de titres, indépendantes des univers (ex. Trajet du matin, Mes découvertes, À écouter ce week-end).

Les univers peuvent être créés, parcourus, renommés, supprimés et alimentés depuis la bibliothèque. Les playlists gardent leur propre gestion. Les deux systèmes sont sauvegardés dans les préférences et dans les sauvegardes JSON.

## Contrôle rapide depuis la notification

La session Media3 propose une action personnalisée **Univers suivant**. Un appui passe directement à l'univers musical suivant et lance sa file de lecture, sans ouvrir l'application ni afficher de dialogue. C'est volontairement un seul bouton cyclique pour limiter les manipulations en voiture.

Les commandes système standard lecture/pause, précédent et suivant restent disponibles. ExoPlayer gère l'audio focus Android et met en pause lorsque la sortie audio devient « noisy » (par exemple déconnexion d'un casque ou d'un équipement Bluetooth). La lecture en arrière-plan reste portée par MediaSessionService.

**Important sécurité :** configurer les univers et lancer la lecture avant de démarrer. Ne pas manipuler le téléphone en conduisant. La présence et l'emplacement des boutons de notification dépendent de la version Android, du fabricant, du véhicule et du récepteur Bluetooth ; vérifier le comportement à l'arrêt. Cette version n'est pas encore un client Android Auto complet avec catalogue MediaLibrary.

## LeafBrowser

LeafBrowser ouvre les sites HTTPS dans une WebView intégrée à LeafSound :
- navigation interne, retour navigateur et geste de balayage depuis le bord gauche ;
- raccourcis de sites favoris conservés localement ;
- JavaScript et stockage DOM pour les sites web modernes ;
- téléchargement déclenché par un lien de téléchargement du site ;
- choix d'un dossier de destination avec le sélecteur Android, y compris un dossier de carte SD ;
- les fichiers téléchargés sont ajoutés automatiquement à la bibliothèque quand l'écriture réussit.

Les pages restent dans LeafSound ; l'utilisateur n'est pas renvoyé vers un navigateur externe par le bouton de téléchargement. Les sites peuvent toutefois bloquer l'affichage dans une WebView, demander une application native ou limiter le téléchargement. LeafBrowser ne contourne ni DRM ni authentification, et les liens non HTTPS sont refusés.

Le site de téléchargement légal découvert par l'utilisateur n'a pas encore été communiqué : il pourra saisir son adresse dans LeafBrowser puis l'enregistrer en favori.

## LeafColour

Palette commune :
- Oasis #206B4D : en-têtes et navigation
- Noyer #6B4935 : texte et boutons
- Or naturel #C5A46D : accent
- Crème foncée #DCCBB1 : fond
- Brun sable #B69A7B : bordures et surfaces secondaires

## Sauvegarde

Le JSON inclut les titres référencés, favoris, playlists, univers, icônes des univers et sélection active. Les URI de fichiers et les autorisations Android restent liées à l'appareil ; pour un transfert entre téléphones, exporter également les fichiers audio.

## Limites à tester

- Compiler et installer la version debug sur le téléphone réel.
- Vérifier les commandes notification sur Android 16 / One UI et sur le système Bluetooth de la voiture.
- Vérifier les autorisations SD après redémarrage et la sauvegarde de gros téléchargements.
- Certains sites peuvent ne pas être compatibles WebView.
- Un mode Android Auto certifiable nécessitera une intégration MediaLibraryService et des tests en voiture.
