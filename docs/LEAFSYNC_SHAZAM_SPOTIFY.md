# Spécification fonctionnelle — LeafSync (Shazam ↔ Spotify)

**Projet :** LeafSound  
**Module :** LeafSync  
**Version documentaire :** 0.1.0  
**Statut :** à développer

## 1. But
Importer les titres reconnus avec Shazam, retrouver leurs équivalents Spotify, puis les ajouter de manière contrôlée à une ou plusieurs playlists. Le module compare aussi un import Shazam à une playlist Spotify pour identifier les titres présents, absents et ambigus.

## 2. Parcours utilisateur
1. Ouvrir LeafSound > LeafSync.
2. Importer un export ou partager une liste depuis Shazam.
3. Normaliser les données disponibles.
4. Se connecter à Spotify via OAuth officiel.
5. Rechercher les correspondances et afficher les résultats.
6. Choisir une playlist cible (par défaut « Mes titres Shazam »).
7. Examiner l'aperçu : ajouts, doublons, ambiguïtés et titres introuvables.
8. Valider explicitement les ajouts.
9. Consulter le rapport et l'historique local.

## 3. Exigences fonctionnelles

### LS-01 — Import Shazam
- Accepter les formats d'export réellement disponibles et validés pendant le développement.
- Accepter le partage manuel d'une liste ou d'un titre si aucun export structuré n'est disponible.
- Ne pas supposer l'existence d'une API Shazam publique.
- Conserver le fichier source intact.
- Détecter les entrées incomplètes et les doublons de l'import.

### LS-02 — Normalisation et correspondance
- Normaliser espaces, casse et ponctuation sans altérer le texte original affiché.
- Utiliser au minimum titre et artiste ; album et durée peuvent aider si disponibles.
- Classer les résultats : correspondance forte, à vérifier, introuvable.
- Ne jamais choisir automatiquement entre plusieurs versions ambiguës (remix, live, remaster, explicit/clean, réédition).

### LS-03 — Connexion Spotify
- Utiliser OAuth 2.0 et les flux officiellement pris en charge.
- Demander uniquement les permissions nécessaires à la lecture des informations et à la modification des playlists choisies.
- Ne jamais demander ni stocker le mot de passe Spotify.
- Stocker les jetons de façon sécurisée sur l'appareil ; ne jamais les committer dans Git.

### LS-04 — Gestion des playlists
- Permettre de choisir une playlist existante ou d'en créer une.
- Proposer « Mes titres Shazam » comme nom par défaut, sans imposer d'identifiant.
- Ajouter uniquement les titres validés par l'utilisateur.
- Vérifier le contenu actuel de la playlist avant l'ajout.
- Dédupliquer par identifiant Spotify, et en secours par titre/artiste normalisés.
- Respecter les limites de lots documentées par Spotify.
- Ne jamais remplacer intégralement une playlist pour une simple synchronisation incrémentale.

### LS-05 — Rapport et reprise
- Afficher : importés, trouvés, à confirmer, introuvables, déjà présents, ajoutés et en erreur.
- Conserver un historique local horodaté.
- Rendre la synchronisation idempotente : relancer le même import ne doit pas ajouter de doublons.
- Gérer erreurs réseau, expiration OAuth et limitation de débit avec reprise contrôlée.

### LS-06 — Classement
- Autoriser plusieurs catégories par titre : genre, ambiance, activité, époque, artiste, etc.
- Distinguer les métadonnées des services des catégories définies par l'utilisateur.
- Ne pas inventer de genre ou d'ambiance si les données sont absentes ; permettre le classement manuel.

## 4. Sécurité et données
- Import local par défaut ; aucun envoi de l'export Shazam vers un serveur LeafSound.
- Aucune suppression dans Shazam ou Spotify.
- Toute écriture Spotify est précédée d'un aperçu et d'une validation explicite.
- Ne pas journaliser jetons, secrets ou données personnelles inutiles.
- Fournir déconnexion et effacement des jetons locaux.
- Aucun secret dans le dépôt Git.

## 5. Architecture indicative
- `ShazamImportAdapter` : lecture des exports et données partagées.
- `TrackNormalizer` : normalisation et dédoublonnage.
- `SpotifyAuthAdapter` : OAuth et gestion sécurisée des jetons.
- `SpotifyCatalogAdapter` : recherche de morceaux.
- `MatchResolver` : score et validation des correspondances.
- `PlaylistSyncEngine` : comparaison, planification et ajouts incrémentaux.
- `SyncHistoryStore` : historique local et reprise.
- `LeafSyncReviewUI` : aperçu, sélection et validation.

Les adaptateurs externes sont indépendants du lecteur local : LeafPlayer doit continuer à fonctionner hors ligne si Shazam ou Spotify est indisponible.

## 6. Critères d'acceptation
- Un export Shazam pris en charge est importé sans modification de la source.
- Les résultats ambigus nécessitent une décision humaine.
- Un titre déjà présent n'est pas ajouté une seconde fois.
- Relancer le même import ne crée aucun doublon.
- Aucune playlist n'est modifiée avant confirmation.
- Une interruption réseau ne provoque pas d'ajouts répétés.
- La lecture locale fonctionne sans connexion Spotify.
- Aucun secret ou jeton n'apparaît dans Git ou les journaux.

## 7. Points à vérifier au développement
- Confirmer les formats d'export Shazam réellement disponibles sur les appareils ciblés. Sans export structuré officiel, utiliser partage/import manuel ; ne pas scraper.
- Vérifier à chaque version la documentation Spotify, les scopes OAuth, les endpoints et les limites d'accès.
- L'accès aux API Spotify dépend des règles du compte développeur et des autorisations accordées.

## 8. Références
- Spotify Web API : https://developer.spotify.com/documentation/web-api
- Création de playlist : https://developer.spotify.com/documentation/web-api/reference/create-playlist
- Ajout d'éléments : https://developer.spotify.com/documentation/web-api/reference/add-items-to-playlist
- Changements Spotify février 2026 : https://developer.spotify.com/documentation/web-api/references/changes/february-2026
