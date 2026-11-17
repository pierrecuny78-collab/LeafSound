# Spécification — LeafSound Audio, Studio, Instruments et Apprentissage

**Version :** 0.1.0  
**Statut :** conception fonctionnelle, à développer  
**Priorité :** Android, hors ligne en premier

## 1. Vision
LeafSound réunit un lecteur audio local, une bibliothèque musicale, des outils de création, des instruments virtuels, un parcours d'apprentissage et un environnement algorave. Les services externes restent facultatifs et séparés du moteur audio local.

## 2. Lire de la musique — LeafPlayer

### Fonctions
- Scanner, avec autorisation de l'utilisateur, les dossiers audio choisis.
- Importer des fichiers depuis stockage interne, carte SD et supports USB compatibles.
- Lire les formats pris en charge par le moteur audio et l'appareil : MP3, AAC, FLAC, WAV, OGG/Opus selon prise en charge.
- Afficher titre, artiste, album, durée, pochette et métadonnées disponibles.
- Recherche, tri, filtres, favoris, historique d'écoute et playlists locales.
- File d'attente, lecture aléatoire, répétition, fondu enchaîné si pris en charge.
- Contrôles de notification et écran verrouillé, lecture en arrière-plan et reprise après interruption.
- Sortie haut-parleur, casque filaire et Bluetooth via les mécanismes Android.
- Égaliseur et effets uniquement si techniquement disponibles et sans dégrader la stabilité.
- Lecture entièrement hors ligne des fichiers locaux.

### Limites des services de streaming
LeafSound ne doit pas télécharger, extraire ou réhéberger les flux Spotify. Les titres Spotify sont recherchés et ouverts dans l'application Spotify officielle. Le contrôle distant de lecture ne sera proposé que si le compte, l'accès développeur, les permissions et les règles Spotify le permettent. L'accès API et certaines fonctions de lecture peuvent exiger Premium. Les morceaux Spotify ne seront pas intégrés au mixeur LeafStudio ni à l'algorave.

### Base Android
Étudier Jetpack Media3 / ExoPlayer comme moteur de lecture recommandé pour une nouvelle application Android. Séparer l'interface, la bibliothèque, la file de lecture et le service de lecture.

## 3. LeafStudio — composition
- Projet musical contenant tempo, signature rythmique, tonalité facultative et pistes.
- Séquenceur pas à pas pour créer des rythmes.
- Piano roll pour placer et éditer des notes.
- Pistes audio locales et pistes MIDI/instrument virtuel lorsque pris en charge.
- Métronome, boucle de mesure, quantification et réglage de vélocité.
- Enregistrement au microphone avec permission claire.
- Mixer avec volume, panoramique, mute/solo et effets.
- Sauvegarde automatique et récupération après fermeture inattendue.
- Export des créations en formats audio pris en charge ; export MIDI si le moteur le permet.
- Aucun fichier source de l'utilisateur écrasé sans confirmation.

## 4. Instruments virtuels
Prévoir une première collection :
- Piano et clavier.
- Batterie, boîte à rythmes et percussions.
- Basse.
- Guitare et cordes pincées.
- Synthétiseur soustractif : oscillateurs, filtre, enveloppe et LFO.
- Pads, nappes, leads et effets sonores.

Chaque instrument doit proposer une interface tactile adaptée au téléphone, des presets modifiables, un réglage de volume et une aide expliquant les commandes. Le clavier virtuel doit gérer les octaves, la transposition et les gammes.

## 5. LeafLearn — apprendre en pratiquant
- Parcours débutant, intermédiaire et avancé.
- Leçons courtes : rythme, tempo, mesure, notes, intervalles, gammes, accords et harmonie.
- Exercices de reconnaissance auditive et de reproduction de rythmes.
- Affichage du clavier et des notes jouées.
- Métronome réglable et exercices à vitesse progressive.
- Défis quotidiens facultatifs, objectifs personnels et suivi des progrès.
- Explication des paramètres d'un instrument au moment où l'utilisateur les manipule.
- Exemples modifiables et jouables hors ligne.
- Accessibilité : tailles de commandes tactiles, contraste et explications textuelles.

## 6. LeafAlgoRave — live coding / algorave
- Éditeur de motifs rythmiques et mélodiques.
- Lecture en boucle et modification en direct, avec transitions sûres au prochain cycle.
- Pistes indépendantes : kick, snare, hi-hat, basse, mélodie et textures.
- Contrôle BPM, densité, probabilité, répétition, décalage et variations.
- Presets de styles et générateur de motifs guidé.
- Visualisation des événements et indication de la mesure en cours.
- Enregistrement de la performance et sauvegarde du code/projet.
- Mode débutant visuel par blocs ou paramètres, puis mode code.
- Mode hors ligne pour les moteurs et banques de sons installés localement.

### Technologie à étudier
Strudel est une référence de live coding musical en JavaScript basée sur les motifs Tidal. Il est intéressant pour apprendre et expérimenter. Avant de l'intégrer ou de le distribuer dans LeafSound, vérifier précisément la licence AGPL-3.0, les licences des banques de sons, les obligations de publication du code source, l'intégration dans une application Android et les performances sur appareils modestes. Une alternative consiste à développer un moteur de motifs LeafAlgoRave natif et interopérable MIDI/OSC.

## 7. Architecture indicative
- `LeafMediaService` : service Android de lecture locale.
- `LeafAudioLibrary` : index local des fichiers et métadonnées.
- `LeafQueueEngine` : file, répétition et aléatoire.
- `LeafAudioEngine` : moteur audio pour les créations.
- `LeafProjectStore` : projets, pistes et sauvegardes.
- `LeafInstrumentHost` : instruments et presets.
- `LeafSequencer` : tempo, motifs et événements.
- `LeafMixerEngine` : routage, volume et effets.
- `LeafLearnEngine` : leçons, exercices et progression.
- `LeafAlgoRuntime` : interprétation des motifs algorithmiques.
- `LeafSync` : module séparé pour les métadonnées et playlists Shazam/Spotify.

Le lecteur local ne doit pas dépendre de LeafSync, Spotify, Shazam ou d'une connexion Internet.

## 8. Étapes de réalisation
1. Prototype LeafPlayer : indexer et lire des fichiers locaux, y compris depuis carte SD.
2. Ajouter bibliothèque, playlists, favoris, notifications et Bluetooth.
3. Créer LeafStudio : métronome, séquenceur simple, piano roll et sauvegarde.
4. Ajouter piano, batterie et synthétiseur virtuels.
5. Ajouter LeafLearn avec leçons et exercices hors ligne.
6. Développer LeafAlgoRave : motifs, boucles et modifications live.
7. Intégrer les effets, enregistrement multipiste et export.
8. Finaliser LeafSync et LeafConnect selon les accès officiels disponibles.
9. Tests de performance, latence audio, autonomie, permissions, sauvegarde et compatibilité Android.

## 9. Critères d'acceptation initiaux
- Lire des fichiers locaux sans réseau.
- Lire en arrière-plan avec contrôles système.
- Ne pas altérer les fichiers audio source.
- Enregistrer et rouvrir un projet musical sans perte.
- Jouer un instrument virtuel avec une latence utilisable sur les appareils ciblés.
- Modifier un motif algorave pendant la lecture sans bloquer l'application.
- Suivre au moins un parcours pédagogique hors ligne.
- Garder les fonctions locales disponibles sans connexion Spotify ou Shazam.

## 10. Références à étudier
- Android Media3 : https://developer.android.com/media/media3
- Strudel — démarrage : https://strudel.cc/workshop/getting-started/
- Strudel — REPL : https://strudel.cc/
- Strudel — FAQ MIDI/OSC : https://live.strudel.cc/learn/faq/
- Algorave : https://algorave.org/
