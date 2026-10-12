# Vérifications — 12 octobre 2026

Revue effectuée sur la version issue de `c4eaa49bf2676f5007b7b32dc05c549ebeaee1c8`, puis sur les corrections de ce fork. Les valeurs ci-dessous sont issues d'exécutions locales ; elles ne constituent pas une certification de production.

## Résultats exécutés

| Contrôle | Commande / périmètre | Résultat |
|---|---|---|
| Backend d'origine | `mvn -B verify` dans `backend/` | 108 tests, 0 échec, 0 erreur |
| Backend corrigé | `mvn -B verify` dans `backend/` | **111 tests**, 0 échec, 0 erreur, 0 ignoré ; JAR construit |
| Android | `gradlew.bat testDebugUnitTest assembleDebug --no-daemon` dans `frontend/` | **50 tests unitaires**, 0 échec, 0 erreur, 0 ignoré ; APK debug construit |
| Architecture historique | Extraction Git de `fc06dff:SystemDesign.md` | Document restauré sans modification |
| Revue du code | Authentification, rôles, transitions, sérialisation, configuration, mobile | Constats et limites documentés ci-dessous |

Environnement local : Windows, Java 21.0.7, Maven 3.9.11, wrapper Gradle 9.0.0 et SDK Android 34. Les tests backend emploient H2, pas MySQL. Le build Android signale des API Gradle dépréciées et quelques variables Kotlin inutilisées ; la compilation réussit néanmoins.

## Couverture backend après corrections

Mesure issue de `backend/target/site/jacoco/jacoco.xml` :

| Indicateur | Couvert / total | Taux |
|---|---:|---:|
| Instructions | 1 707 / 1 802 | **94,73 %** |
| Lignes | 328 / 334 | **98,20 %** |
| Branches | 82 / 94 | **87,23 %** |
| Méthodes | 95 / 105 | **90,48 %** |

Le `pom.xml` exclut la classe de démarrage, `models/**` et `dto/**`. Ces taux décrivent donc les 14 classes instrumentées, et non l'ensemble du projet. Aucune couverture Android n'a été mesurée.

Les fichiers de résultats sont régénérables : `backend/target/surefire-reports/`, `backend/target/site/jacoco/`, `frontend/build/test-results/testDebugUnitTest/` et `frontend/build/reports/tests/`.

## Corrections apportées

1. **Élévation de privilèges à l'inscription.** Le service acceptait `ADMIN` depuis la requête publique. Il n'accepte désormais que `JOUEUR` et `COACH`. Deux tests vérifient le rejet d'un rôle administrateur ou absent avant toute persistance.
2. **Exposition du hash de mot de passe.** Les entités `User` renvoyées par certains contrôleurs pouvaient sérialiser `password`. Le champ est désormais exclu de JSON ; un test vérifie les réponses directes et imbriquées tout en conservant l'accès interne nécessaire à l'authentification.
3. **Configuration embarquée.** La clé JWT et les paramètres de connexion utilisent des variables d'environnement. `frontend/local.properties`, propre au poste d'un contributeur, est retiré du suivi Git.
4. **Documentation périmée.** La présentation distingue l'API, la démonstration mobile autonome et le document d'intention historique. L'ancien résumé de couverture est remplacé par ces mesures.

## Limites constatées et contrôles non réalisés

- Le mobile utilise `MockRepository` en mémoire, avec mots de passe de démonstration ; il n'est pas connecté à l'API. Ses comptes et ceux de MySQL sont distincts.
- Le paiement est simulé. Le chat utilise HTTP REST, pas WebSocket.
- Les tests de contrôleurs utilisent des dépendances simulées ; le test de contexte H2 n'est pas une validation complète des parcours sur MySQL.
- Aucun test instrumenté sur émulateur, test MySQL réel, test de charge, pentest complet ou déploiement public n'a été exécuté.
- La pagination, la révocation des jetons et les DTO de réponse homogènes restent des améliorations à prévoir.
- Les commits historiques sont conservés. Les anciennes valeurs de configuration restent donc dans l'historique public ; ne pas réutiliser ces valeurs pour un déploiement.

## Reproduire

```bash
cd backend
mvn verify
cd ../frontend
./gradlew testDebugUnitTest assembleDebug
```

Sous Windows, remplacer `./gradlew` par `.\gradlew.bat`. Les workflows GitHub Actions reprennent ces commandes ; leur présence seule ne prouve pas qu'une exécution distante a réussi.
