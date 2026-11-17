# LeafSound

**LeafSound** est le projet audio de l'écosystème LeafVibe : une application Android et multiplateforme orientée musique, podcasts, apprentissage musical et création audio, avec priorité au fonctionnement hors ligne.

## Objectifs
- Lire et organiser les fichiers audio locaux, sans publicité.
- Gérer bibliothèque, favoris, files d'attente et playlists.
- Connecter les bibliothèques Shazam et Spotify via LeafSync, dans les limites des API officielles.
- Découvrir et suivre des podcasts.
- Apprendre la musique, jouer d'instruments virtuels et composer.
- Enregistrer, mixer et exporter de l'audio.
- Créer des performances algorithmiques / algorave en direct.
- Fonctionner hors ligne pour toutes les fonctions qui ne nécessitent pas un service externe.

## Modules prévus

| Module | Rôle |
|---|---|
| LeafPlayer | Lecteur de fichiers audio locaux |
| LeafLibrary | Bibliothèque locale et métadonnées |
| LeafFavorites | Favoris |
| LeafPlaylists | Playlists locales et listes intelligentes |
| LeafPodcast | Podcasts |
| LeafSync | Synchronisation Shazam ↔ Spotify |
| LeafConnect | Ouverture et connexion aux services externes |
| LeafStudio | Studio multipiste, mixage et export |
| LeafSynth | Synthétiseurs et instruments virtuels |
| LeafKeys | Piano, clavier et apprentissage des notes |
| LeafDrums | Batterie, percussions et séquenceur |
| LeafGuitar | Guitare et instruments à cordes virtuels |
| LeafRecorder | Enregistrement audio |
| LeafEffects | Effets et traitement |
| LeafMixer | Mixage et pistes |
| LeafConverter | Conversion de formats pris en charge |
| LeafVisualizer | Visualisations audio |
| LeafLearn | Cours, exercices et progression |
| LeafAlgoRave | Composition algorithmique et performances live |

## Écouter la musique
LeafPlayer lit les fichiers audio locaux compatibles depuis le téléphone, une carte SD ou un support USB accessible. Il gère la recherche, les pochettes, les files d'attente, les playlists, les favoris, la lecture en arrière-plan et les sorties audio compatibles, dont Bluetooth.

Les morceaux Spotify ne sont pas téléchargés ni réémis par LeafSound. LeafConnect ouvre le contenu dans l'application Spotify officielle et LeafSync gère les métadonnées et les playlists avec les autorisations prévues par Spotify. Les fonctions Spotify dépendantes du compte ou de l'accès développeur seront clairement signalées.

## LeafSync — Shazam ↔ Spotify
- Importer un export ou des données partagées depuis Shazam.
- Rechercher les titres correspondants dans Spotify.
- Afficher les correspondances fortes, ambiguës et introuvables.
- Prévisualiser les ajouts et demander confirmation.
- Ajouter les titres validés à une playlist choisie, par défaut « Mes titres Shazam ».
- Détecter les doublons, conserver un historique et permettre la reprise.
- Ne jamais modifier la bibliothèque Shazam source ni supprimer automatiquement des titres Spotify.

Shazam ne doit pas être considéré comme disposant d'une API publique exploitable par défaut. Aucun scraping ou contournement de connexion. Spotify utilise son API officielle et OAuth 2.0 ; les fonctions disponibles dépendent des conditions et accès accordés à l'application.

## Composition et apprentissage
LeafStudio doit proposer un séquenceur multipiste, piano roll, pistes audio et MIDI lorsque pris en charge, métronome, tempo, gamme, accords, effets, enregistrement et export.

LeafLearn accompagne chaque instrument avec des leçons progressives, schémas de notes, exercices rythmiques, accords, gammes, entraînement à l'oreille, métronome, objectifs et suivi de progression. Les exercices doivent fonctionner hors ligne.

LeafAlgoRave permet de créer et modifier des motifs rythmiques et mélodiques pendant leur lecture. Une intégration ou adaptation de Strudel/TidalCycles pourra être étudiée après vérification des licences, compatibilité Android, performances et exigences de publication du code source.

## Principes techniques
- Android en priorité, architecture modulaire.
- Lecture locale hors ligne, moteur audio indépendant des services externes.
- Jetons OAuth stockés de façon sécurisée, jamais dans Git.
- Aucune publicité intégrée et aucun abonnement LeafSound.
- Respect des licences open source, API et conditions des services tiers.

## État du projet
Ce dépôt constitue le point de départ documentaire. Les modules et fonctions ci-dessus sont des objectifs de conception, pas des fonctions déclarées implémentées.

## Documentation
- [Spécification LeafSync — Shazam ↔ Spotify](docs/LEAFSYNC_SHAZAM_SPOTIFY.md)
- [Lecteur, studio, instruments, apprentissage et algorave](docs/LEAFSOUND_AUDIO_STUDIO.md)
