# 🚀 Guide de démarrage rapide

## Lancer l'application (méthode la plus simple)

### Option 1 : Script simple (recommandé)
```bash
./start.sh
```

### Option 2 : En arrière-plan
```bash
./start.sh dev --background
# ou
./start.sh -b
```

### Option 3 : Mode production
```bash
./start.sh prod
```

## Arrêter l'application

```bash
./stop.sh
```

## Autres méthodes

### Avec Maven directement
```bash
./mvnw spring-boot:run
```

### Avec le JAR compilé
```bash
./mvnw package -DskipTests
java -jar target/BelgianSlotClubSpring-0.0.1-SNAPSHOT.jar
```

## Accès à l'application

Une fois lancée, l'application est accessible sur :
- **URL principale** : http://localhost:8080
- **Page d'accueil** : http://localhost:8080/
- **Contact** : http://localhost:8080/contact

## Vérifier que l'application tourne

```bash
curl http://localhost:8080
# ou
lsof -i :8080
```

## Voir les logs (si lancé en arrière-plan)

```bash
tail -f app.log
```


