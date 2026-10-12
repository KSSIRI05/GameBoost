# GameBoost — API REST

API Spring Boot 3.4.4 / Java 21 pour une plateforme de coaching gaming. Les contrôleurs exposent les parcours joueurs, coachs et administrateurs ; la persistance utilise MySQL et Spring Data JPA.

## Lancement

Depuis ce dossier, avec Java 21 et Maven 3.9+ :

```bash
mvn verify
```

Les tests utilisent H2 et leur propre clé JWT de test. Pour lancer l'application avec MySQL, définir `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` et `JWT_SECRET` dans l'environnement du processus, puis :

```bash
mvn spring-boot:run
```

La clé JWT doit être aléatoire et longue d'au moins 32 octets. `JWT_EXPIRATION_MS` vaut 86 400 000 par défaut. Le port est 8080 ; `SERVER_PORT` permet de le changer. Les fichiers `.env` ne sont pas chargés automatiquement.

## Routes implémentées

| Méthode | Route | Fonction |
|---|---|---|
| POST | `/auth/register` | Inscription JOUEUR ou COACH |
| POST | `/auth/login` | Authentification JWT |
| GET / PUT | `/users/me` | Consulter / modifier son profil |
| GET | `/coaches` | Coachs approuvés, filtres `game` et `rank` |
| GET | `/coaches/{id}` | Détail d'un coach |
| POST | `/coaches/profile` | Candidature coach |
| PUT | `/coaches/me/profile` | Modifier son profil coach |
| GET | `/coaches/me/earnings` | Total des séances marquées payées |
| POST | `/sessions/request` | Demander une séance |
| GET | `/sessions` | Séances de l'utilisateur |
| PATCH | `/sessions/{id}/accept` | Accepter une demande |
| PATCH | `/sessions/{id}/complete` | Déclarer la fin |
| PATCH | `/sessions/{id}/confirm` | Confirmer la prestation |
| POST | `/sessions/{id}/pay` | Simuler le paiement |
| POST | `/sessions/{id}/review` | Noter une séance payée |
| GET / POST | `/sessions/{id}/messages` | Lire / envoyer des messages |
| GET | `/admin/coaches/pending` | Coachs en attente |
| PATCH | `/admin/coaches/{id}/approve` | Approuver un coach |
| PATCH | `/admin/coaches/{id}/reject` | Rejeter un coach |
| GET | `/admin/dashboard` | Statistiques |
| GET | `/admin/users` | Liste des utilisateurs |

Toutes les routes sauf `/auth/**` exigent `Authorization: Bearer <token>`. Les routes administrateur nécessitent le rôle `ADMIN`. Le rôle administrateur ne peut pas être demandé lors de l'inscription publique.

### Exemple d'inscription (bash)

```bash
curl -X POST http://localhost:8080/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"player@example.test","username":"player_demo","password":"DemoPassword123","role":"JOUEUR"}'
```

Cet exemple utilise un mot de passe de démonstration. La réponse est une enveloppe `ApiResponse` et le jeton se trouve dans `data.token`. Les champs des DTO Java constituent le contrat actuel ; les exemples du document historique ne sont pas tous à jour.

## Vérification et couverture

Le bilan reproductible est dans [docs/VERIFICATION.md](../docs/VERIFICATION.md). Le rapport JaCoCo local se trouve dans `target/site/jacoco/index.html`.

Le plugin exclut les entités, les DTO et la classe de démarrage. Les taux affichés concernent ce périmètre, pas toutes les lignes du dépôt. Les tests incluent des tests unitaires, des tests de contrôleurs MockMvc et un chargement du contexte Spring sur H2.

## Points corrigés dans ce fork

- Refus du rôle ADMIN à l'inscription, avec tests de régression.
- Exclusion des mots de passe hachés dans les réponses JSON, y compris imbriquées.
- Clé JWT et connexion MySQL configurées via l'environnement.

## Limites

Paiement simulé, messages REST sans WebSocket, absence de pagination et de révocation des JWT. Le frontend mobile utilise un dépôt de données en mémoire et n'appelle pas cette API. Le démarrage sur une instance MySQL réelle et les tests de charge restent à vérifier.
