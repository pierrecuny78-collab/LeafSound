# LeafSound — feuille de route fonctionnelle 2026

## Direction produit

LeafSound devient un environnement musical modulaire, hors ligne en priorité, sans publicité ni abonnement LeafSound. Les inspirations sont des modèles de fonctions — pas une copie d'interface ou de marque :
- **Shazam** : reconnaissance rapide, historique d'identifications, recherche par extrait, suivi des découvertes.
- **Spotify** : recommandations, files d'attente, playlists, dossiers, listes intelligentes et découverte personnalisée.
- **Deezer** : Flow, SongCatcher, paroles, quiz musical, personnalisation de la bibliothèque.
- **Musicolet / Poweramp / VLC** : lecture locale robuste, formats, tags, contrôle audio.
- **BandLab / FL Studio Mobile / Koala Sampler** : enregistrement, pistes, séquenceur, effets et création mobile.
- **Sonic Pi / Strudel / TidalCycles** : livecoding, motifs algorithmiques et performances.

Les références servent à définir des attentes de qualité. LeafSound garde son identité visuelle, son architecture et ses fonctions propres.

## Espaces de l'application

### 1. ÉCOUTER — LeafPlayer
- Lecture de fichiers locaux depuis stockage interne, carte SD et supports USB autorisés par Android.
- Formats et métadonnées : MP3, AAC, OGG, FLAC, WAV, M4A, tags ID3/Vorbis, pochette, artiste, album, année, genre, durée.
- File d'attente réordonnable, lecture sans coupure lorsque possible, reprise à la dernière position.
- Favoris, historique, récemment ajoutés, plus écoutés, ignorés et statistiques locales.
- Égaliseur, préampli, balance, fondu, minuterie d'arrêt, vitesse de lecture et normalisation facultative.
- Miniplayer, écran de lecture complet, paroles synchronisées si source autorisée.
- Commandes Bluetooth, casque, écran verrouillé, notification Android et Android Auto si les conditions d'intégration sont remplies.

### 2. BIBLIOTHÈQUE — LeafLibrary / LeafPlaylists
- Playlists nommées : créer, renommer, dupliquer, supprimer, réordonner.
- Ajouter/retirer un morceau, ajouter plusieurs titres, déplacer/copier entre playlists.
- Dossiers de playlists par genre, ambiance, activité ou projet.
- Listes intelligentes : favoris, Shazam, nouveautés locales, jamais écoutés, récemment joués, durée, genre, décennie.
- Règles anti-doublons, import/export M3U et sauvegarde/restauration.
- Couvertures personnalisées et couleurs par playlist.

### 3. IDENTIFIER — LeafShazam / LeafIdentify
- Bouton d'identification immédiate et écoute courte, explicite et consentie du microphone.
- Historique horodaté, lieu facultatif, extrait de reconnaissance si légalement autorisé, confiance de correspondance.
- Détection des correspondances ambiguës, versions live/remix et rééditions.
- Ajouter le résultat aux favoris ou à une playlist, ouvrir dans Spotify/Deezer/Apple Music.
- Import de l'historique Shazam par export ou partage de fichier.
- Mode hors ligne : mise en attente des demandes / empreintes pour traitement ultérieur ; identification universelle hors ligne non promise sans base musicale locale.
- Important : Shazam ne fournit pas une API publique générale garantie pour une application tierce. Évaluer un fournisseur de reconnaissance sous contrat et ses conditions, sans scraping ni clé secrète embarquée.

### 4. DÉCOUVRIR — LeafDiscover
- Flow local : radio personnalisée construite à partir des titres locaux, favoris et historiques.
- Découvertes par genre, humeur, tempo, énergie, décennie, instrument et pays.
- Nouveautés, sorties d'albums, artistes émergents, concerts et festivals.
- Actualités musicales via flux RSS/API éditoriaux identifiés, avec source, date et lien d'origine.
- Fiches artiste, discographie, événements, recommandations similaires et radios thématiques.
- Cache hors ligne des articles et métadonnées déjà consultés.
- Quiz musical, blind test local, défis de reconnaissance et calendrier de sorties.

### 5. APPRENDRE — LeafLearn
- Solfège progressif : portée, clés, notes, silences, rythmes, mesures, intervalles, tonalités.
- Lecture à vue, dictée rythmique, dictée mélodique, reconnaissance d'intervalles et entraînement de l'oreille.
- Métronome, accordeur, générateur d'accords et cercle des quintes.
- Leçons par instrument et parcours débutant → intermédiaire → avancé.
- Exercices hors ligne, correction immédiate et progression sauvegardée.

### 6. COMPOSER — LeafStudio
- Piano roll avec notes MIDI éditables, vélocité, durée, quantification, swing et automation.
- Partition lisible en notation musicale, affichage simultané piano roll / portée / clavier.
- Pistes MIDI et audio, boucle, marqueurs, tempo, tonalité, mesure et métronome.
- Instruments : piano, synthétiseur, basse, batterie, percussions, guitare et échantillonneur.
- Enregistrement microphone, overdub, découpe, déplacement, fades et édition non destructive.
- Mixer multipiste, volume, panoramique, EQ, compresseur, reverb, delay et effets.
- Export WAV/FLAC/MP3 selon codecs disponibles, export MIDI et MusicXML lorsque pertinent.
- Transcription audio vers notes/MIDI comme fonction expérimentale, avec limites explicites et traitement adapté au matériel.

### 7. LIVE — LeafAlgoRave
- Éditeur de motifs rythmiques et mélodiques, séquenceur pas-à-pas et générateurs.
- Livecoding en blocs et en texte, motifs répétitifs, probabilités, variations, polymètres et changements à chaud.
- Synchronisation BPM, quantification, scènes, clips et transitions.
- Contrôle MIDI/USB/Bluetooth MIDI si pris en charge, OSC et contrôleurs externes.
- Mode performance plein écran, gros contrôles tactiles, sauvegarde de set et export audio.
- Vérifier licences, dépendances et compatibilité Android avant d'intégrer Strudel ou tout moteur tiers.

### 8. SYNCHRONISER — LeafSync
- Importer l'historique Shazam par export officiel ou partage.
- Faire correspondre titre, artiste, album, version et durée ; signaler les ambiguïtés.
- Prévisualiser les changements avant écriture dans Spotify.
- Détecter les doublons et conserver un journal réversible.
- Spotify via OAuth et API officielle uniquement ; fonctions disponibles selon les autorisations et restrictions du compte et de l'API.
- Ne jamais télécharger ni retransmettre le flux audio Spotify.

## Priorités de livraison

| Version cible | Contenu |
|---|---|
| 0.4.x | Playlists nommées, répétitions bornées, notification de lecture et commandes système |
| 0.5.x | File d'attente, métadonnées, pochettes, historique, dossiers et listes intelligentes |
| 0.6.x | LeafIdentify : interface micro, historique, intégration fournisseur de reconnaissance après validation des conditions |
| 0.7.x | LeafDiscover : actualités RSS, sorties, artistes, cache hors ligne |
| 0.8.x | LeafLearn : solfège, métronome, exercices et accordeur |
| 0.9.x | LeafStudio : piano roll, pistes MIDI, instruments et enregistrement de base |
| 1.0.x | LeafAlgoRave, performances live, synchronisation Shazam/Spotify et stabilisation générale |

## Qualité, sécurité et performances
- Lecture et bibliothèque locales disponibles sans compte ni réseau.
- Aucun enregistrement micro en arrière-plan silencieux : indicateur visible, permission Android et action volontaire.
- Pas de publicité, pas de profil LeafSound obligatoire, pas d'abonnement LeafSound.
- Clés API et jetons OAuth jamais stockés en clair dans le dépôt ou l'APK.
- Fonctionnalités distantes facultatives et remplaçables ; affichage de la source et de la date pour les actualités.
- Tests sur Android réel, batterie, Bluetooth, verrouillage écran, interruptions audio, carte SD et reprise après fermeture.
- Optimiser pour appareils Android modestes : modules à la demande, cache limité, rendu léger et traitement audio progressif.
- Respect des licences des moteurs, banques de sons, paroles, images et catalogues musicaux.

## État d'implémentation
Cette feuille de route décrit la cible. Une fonction ne doit être présentée comme disponible qu'après intégration, compilation réussie et test fonctionnel sur appareil. La compilation CI seule ne prouve pas le bon fonctionnement sur téléphone.
