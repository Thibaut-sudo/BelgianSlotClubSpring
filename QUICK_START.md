# Démarrage rapide — Belgian Slot Club

## Le plus simple (macOS)

1. Double-clic sur **`Démarrer.command`**
2. Attendre l’ouverture du navigateur → http://localhost:8080
3. Pour arrêter : double-clic sur **`Arrêter.command`**

Au premier lancement, macOS peut demander d’autoriser le script : clic droit → Ouvrir.

## Depuis le terminal

```bash
./start.sh          # démarre + ouvre le navigateur
./start.sh status   # déjà lancé ?
./stop.sh           # arrête
```

### Options

```bash
./start.sh --fg         # logs dans le terminal
./start.sh --restart    # force un redémarrage
./start.sh --tunnel     # + URL publique (Cloudflare)
./start.sh prod         # profil production
```

## Logs

```bash
tail -f app.log
```

## Prérequis

- **JDK 21** (détecté automatiquement via `/usr/libexec/java_home`)
- Le script utilise `./mvnw` (rien d’autre à installer pour Maven)
