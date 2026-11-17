# LeafSound

**LeafSound** est le projet audio de l'écosystème LeafVibe : une application Android et multiplateforme orientée musique, podcasts, création audio et fonctionnement hors ligne.

## Objectifs
- Lire et organiser les fichiers audio locaux, sans publicité.
- Gérer bibliothèque, favoris, files d'attente et playlists.
- Découvrir et suivre des podcasts.
- Enregistrer et créer de l'audio avec des outils de studio.
- Proposer instruments, effets, visualisations et création algorithmique / algorave.
- Fonctionner hors ligne pour toutes les fonctions qui ne nécessitent pas un service externe.
- Garder les comptes externes séparés, avec OAuth officiel lorsque disponible.

## Modules prévus

| Module | Rôle |
|---|---|
| LeafPlayer | Lecture audio et file d'attente |
| LeafLibrary | Bibliothèque locale et métadonnées |
| LeafFavorites | Favoris |
| LeafPlaylists | Playlists locales et listes intelligentes |
| LeafPodcast | Podcasts |
| LeafStudio | Création audio |
| LeafSynth | Synthétiseurs et instruments |
| LeafRecorder | Enregistrement |
| LeafEffects | Effets et traitement |
| LeafConverter | Conversion de formats pris en charge |
| LeafVisualizer | Visualisations audio |
| LeafAlgoRave | Création musicale algorithmique et performances live |
| **LeafSync** | Synchronisation Shazam ↔ Spotify |

## LeafSync — Shazam ↔ Spotify

LeafSync permettra d'importer les titres reconnus dans Shazam, de retrouver leurs équivalents dans Spotify et de les organiser dans une playlist cible.

Fonctions prévues :
- Import d'un export utilisateur ou de données partagées depuis Shazam.
- Analyse du titre, de l'artiste, de l'album et de la date si disponibles.
- Recherche Spotify et affichage du degré de confiance des correspondances.
- Aperçu avant modification et validation explicite.
- Ajout dans une playlist choisie (nom par défaut : « Mes titres Shazam »).
- Détection des doublons et prévention des ajouts répétés.
- Signalement des titres introuvables et des versions ambiguës (live, remix, remaster, etc.).
- Classement multi-critères : genres, ambiances, artistes, années.
- Historique local et reprise après interruption.
- Aucune suppression ou modification de la bibliothèque Shazam d'origine ; aucune suppression Spotify automatique.

### Limites et sécurité
Ne pas supposer l'existence d'une API publique Shazam exploitable. La première version privilégiera l'import d'un export fourni par l'utilisateur ou le partage de données depuis Shazam. Aucun scraping ni contournement de connexion.

Spotify sera intégré via son API officielle et OAuth 2.0. Les autorisations seront limitées au nécessaire. Les jetons et secrets ne seront jamais enregistrés dans GitHub ni dans les journaux. La lecture locale restera utilisable sans compte Spotify.

## Principes techniques
- Android en priorité, architecture modulaire.
- Stockage local et mode hors ligne en priorité.
- Synchronisation réseau facultative, explicite et contrôlée.
- Pas d'abonnement LeafSound ni de publicité intégrée.
- Respect des API, conditions d'utilisation et droits des services tiers.
- Aucun identifiant personnel ni secret dans le dépôt.

## État du projet
Ce dépôt constitue le point de départ documentaire. Les fonctions ci-dessus sont des objectifs de conception, pas des fonctions déclarées implémentées.

## Documentation
- [Spécification LeafSync — Shazam ↔ Spotify](docs/LEAFSYNC_SHAZAM_SPOTIFY.md)
