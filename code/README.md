# Here are some useful commands to quickly run the server

## 1. Build the .jar (artifact)
```bash
cd back-end
gradle bootJar
```

## 2. Run in memory w/ some dummy data
```bash
java -jar back-end/build/libs/battleship-server-0.0.1-SNAPSHOT.jar --server-port=9000
```

## 2. Run with Posgresql DB
```bash
java -jar back-end/build/libs/battleship-server-0.0.1-SNAPSHOT.jar postgres
```

## 3. Run with Docker
```bash
cd back-end
docker-compose -f docker-compose.yml -p "battleship-servers" up
```