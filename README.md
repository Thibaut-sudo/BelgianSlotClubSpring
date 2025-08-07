# 🏁 Belgian Slot Club - Spring Boot Application

Application web pour la gestion des résultats de courses de slot racing en Belgique.

## 📋 Prérequis

### Système d'exploitation
- macOS (testé sur macOS 15.5)
- Linux (Ubuntu/Debian)
- Windows (avec WSL recommandé)

### Logiciels requis
- **Java 21** ou supérieur
- **Maven 3.9** ou supérieur
- **Git** (pour cloner le projet)

## 🚀 Installation

### 1. Installer Java

#### Sur macOS (avec Homebrew)
```bash
# Installer Homebrew si pas déjà fait
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Installer Java
brew install openjdk@21

# Ajouter Java au PATH
echo 'export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

#### Sur Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install openjdk-21-jdk
```

#### Sur Windows
Télécharger et installer depuis [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) ou [OpenJDK](https://adoptium.net/)

### 2. Installer Maven

#### Sur macOS (avec Homebrew)
```bash
brew install maven
```

#### Sur Linux (Ubuntu/Debian)
```bash
sudo apt install maven
```

#### Sur Windows
Télécharger depuis [Apache Maven](https://maven.apache.org/download.cgi) et configurer le PATH

### 3. Vérifier l'installation

```bash
# Vérifier Java
java -version

# Vérifier Maven
mvn -version
```

## 🏗️ Configuration du projet

### 1. Cloner le projet
```bash
git clone <url-du-repo>
cd BelgianSlotClubSpring
```

### 2. Configuration de la base de données

Le projet utilise PostgreSQL. Créez un fichier `application-dev.yml` dans `src/main/resources/` :

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/belgian_slot_club
    username: votre_username
    password: votre_password
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

### 3. Installer PostgreSQL (optionnel pour le développement)

#### Sur macOS
```bash
brew install postgresql
brew services start postgresql
```

#### Sur Linux
```bash
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
```

## 🚀 Lancement de l'application

### Méthode 1 : Script de lancement (recommandé)
```bash
# Mode développement
./start.sh dev

# Mode production
./start.sh prod
```

### Méthode 2 : Maven directement
```bash
# Compiler le projet
mvn clean compile

# Lancer en mode développement
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Ou créer un package et l'exécuter
mvn package -DskipTests
java -jar target/BelgianSlotClubSpring-0.0.1-SNAPSHOT.jar
```

### Méthode 3 : IDE (IntelliJ IDEA, Eclipse, VS Code)
1. Ouvrir le projet dans votre IDE
2. Exécuter la classe `BelgianSlotClubSpringApplication`
3. Configurer les variables d'environnement si nécessaire

## 🌐 Accès à l'application

Une fois lancée, l'application est accessible sur :
- **URL principale** : http://localhost:8080
- **Page d'accueil** : http://localhost:8080/
- **Contact** : http://localhost:8080/contact

## 📁 Structure du projet

```
BelgianSlotClubSpring/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── org/example/belgianslotclubspring/
│   │   │       ├── controllers/     # Contrôleurs REST
│   │   │       ├── entities/        # Entités JPA
│   │   │       ├── services/        # Services métier
│   │   │       ├── repo/           # Repositories
│   │   │       └── configs/        # Configurations
│   │   └── resources/
│   │       ├── static/             # Fichiers statiques (CSS, JS, images)
│   │       ├── templates/          # Templates Thymeleaf
│   │       └── application.yml     # Configuration Spring Boot
│   └── test/                       # Tests unitaires
├── target/                         # Fichiers compilés
├── pom.xml                        # Configuration Maven
├── start.sh                       # Script de lancement
└── README.md                      # Ce fichier
```

## 🛠️ Commandes utiles

### Développement
```bash
# Compiler le projet
mvn clean compile

# Lancer les tests
mvn test

# Créer un package
mvn package -DskipTests

# Nettoyer le projet
mvn clean
```

### Base de données
```bash
# Créer les tables (si ddl-auto: create)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Mettre à jour les tables (si ddl-auto: update)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Déploiement
```bash
# Créer un package pour la production
mvn clean package -DskipTests -Pprod

# Exécuter le package
java -jar target/BelgianSlotClubSpring-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## 🔧 Configuration avancée

### Variables d'environnement
```bash
export SPRING_PROFILES_ACTIVE=dev
export DB_URL=jdbc:postgresql://localhost:5432/belgian_slot_club
export DB_USERNAME=votre_username
export DB_PASSWORD=votre_password
```

### Profils Spring Boot
- **dev** : Mode développement avec logs détaillés
- **prod** : Mode production optimisé
- **test** : Mode test pour les tests unitaires

## 🐛 Dépannage

### Problèmes courants

#### 1. Port 8080 déjà utilisé
```bash
# Vérifier les processus sur le port 8080
lsof -i :8080

# Tuer le processus
kill -9 <PID>
```

#### 2. Erreur de base de données
- Vérifier que PostgreSQL est démarré
- Vérifier les paramètres de connexion
- Vérifier que la base de données existe

#### 3. Erreur de compilation Java
- Vérifier la version de Java (Java 21 requis)
- Vérifier que JAVA_HOME est configuré
- Nettoyer et recompiler : `mvn clean compile`

#### 4. Erreur Maven
- Vérifier que Maven est installé : `mvn -version`
- Nettoyer le cache Maven : `mvn clean`
- Supprimer le dossier target : `rm -rf target`

## 📞 Support

Pour toute question ou problème :
- Créer une issue sur GitHub
- Contacter l'équipe de développement
- Consulter la documentation technique

## 📄 Licence

Ce projet est sous licence MIT. Voir le fichier LICENSE pour plus de détails.

---

*Développé avec ❤️ pour la communauté du slot racing belge*
