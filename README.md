# Belgian Slot Club

**Prod :** [belgianslotclub.com](https://belgianslotclub.com)

Plateforme web des clubs de slot racing belges. Les pilotes y retrouvent leurs résultats, le championnat et le calendrier. Les organisateurs y publient les courses, les rallyes et les dates.

Trois clubs sont en ligne :

- **Slot 4000** (Liège, vendredi) — courses circuit, classements, règlement
- **SRCS** (Seraing, mardi) — même socle, calendrier et stats propres au club
- **Rallyes Slot** (Hobby 2000) — spéciales, grilles, scan des temps, classement cumulé

Autour des courses : un **calendrier commun** (Google / Apple / `.ics`), un **forum**, une **marketplace** d’occasions, et un **contact**.

Les résultats circuit partent d’un export Excel DS. Les rallyes se saisissent le jour même (pilotes, boucles, ES, temps). Un mot de passe organisateur protège les ajouts au calendrier et les actions d’admin.

| | |
|---|---|
| Stack | Spring Boot 3.5 · Java 25 · Thymeleaf |
| Données | H2 fichier (`./data/`) en `dev` et `prod` |
| Clubs | Slot 4000 · SRCS · Rallyes Slot |

---

## Démarrer

Prérequis : **JDK 25**. Maven est déjà dans le repo (`./mvnw`).

```bash
brew install openjdk@25          # une fois, macOS
./start.sh                       # compile, lance, ouvre http://localhost:8080
```

macOS : double-clic sur `Démarrer.command` / `Arrêter.command`.

```bash
./start.sh --fg          # logs dans le terminal
./start.sh --restart     # relance
./start.sh status
./stop.sh
tail -f app.log
```

Le profil par défaut est `dev` (H2 + logs verbeux). Pas besoin de PostgreSQL.

---

## Commandes

```bash
./mvnw test                      # tests
./mvnw -DskipTests package       # JAR → target/BelgianSlotClubSpring-0.0.1-SNAPSHOT.jar
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Port 8080 déjà pris : `./stop.sh` ou `lsof -tiTCP:8080 -sTCP:LISTEN | xargs kill`.

Java introuvable : `export JAVA_HOME=$(/usr/libexec/java_home -v 25)` puis relancer.

---

## Parcours

| URL | Rôle |
|---|---|
| `/` | Accueil, choix du club |
| `/calendrier` | Calendrier commun + ICS Google / Apple |
| `/selectRace/{club}` | Courses du club (`slot4000`, `srcs`) |
| `/championnat` `/statistiques` | Classements |
| `/rallye` `/prochain-evenement` | Rallyes et calendrier club |
| `/forum` `/marketplace` `/contact` | Communauté |
| `/reglement` `/reglement/srcs` `/reglement/sco` | Règlements |

Les organisateurs ajoutent une date depuis le calendrier (mot de passe). Elle s’inscrit sur les trois clubs.

---

## Données

| Profil | Base | Usage |
|---|---|---|
| `dev` (défaut via `start.sh`) | `./data/belgianslotclub` (H2) | Local |
| `prod` | même fichier H2, côté Pi | [belgianslotclub.com](https://belgianslotclub.com) |
| défaut (`application.yml`) | PostgreSQL `localhost` | Uniquement si tu lances sans profil |

Ne jamais copier la H2 locale vers le Pi : ça écrase les rallyes et résultats de prod.

---

## Déploiement

Le JAR est construit sur le Mac, copié sur le Raspberry Pi, Java 25 + nginx + systemd sont alignés. La base distante est sauvegardée avant.

```bash
SSHPASS='…' DEPLOY_HOST='raspserver@192.168.0.104' ./deploy/deploy-to-pi.sh
```

Forcer une synchro `data/` (dangereux) : `DEPLOY_SYNC_DATA=1`.

Après un restart, Spring met ~70 s à répondre : un 502 nginx au tout début est normal.

---

## Dépannage

| Symptôme | Cause probable |
|---|---|
| Popup IDE « older Java runtime » | Extension Java, pas le projet. On est déjà en 25. |
| `UnsupportedClassVersionError` | JAR 25 lancé avec un JRE 21. |
| 502 après deploy | Boot encore en cours, attendre. |
| Calendrier Google en retard | Google poll le flux ICS, parfois plusieurs heures. |
