## Open this folder using IntelliJ
# Contents
- .run -> Some run configurations (for IntelliJ)
- gradle/wrapper -> Info about gradle version to be used during build
- src -> main/kotlin/ -> source code. main/resources/ -> static content (html and javascript sent to client's to have an interface to work with the server)
- staticdev -> A folder that is useful to use when testing solely the front-end build. Which will null out the necessity of restarting the server/re-building the server to see apply front-end code changes. In order for this to to work, make sure the file ``resources/application.properties` property called `spring.web.resources.static-locations` references this folder first.
- build.gradle.kts -> Plugins for the build system (gradle) and dependencies in use for the project
- [gradle.bat](https://stackoverflow.com/a/44860398/9375488)
## Docker files:
- docker-compose.yml -> Defines list of containers to run locally
- docker-compose-azure -> Specific docker-compose file to run containers in azure

The rest is kinda self-explanatory

## Also see [Cloud deployment guide](/docs/Cloud%20deployment.md) in docs!

# How to deploy
## 1 - In front-end folder, build the 1 .js and 1 document(.html) project
- npm run prod

Copy the resulting files in `dist/` to `src/main/resources/static` in the back-end. 
The name of this folder inside `resources/` will depend on the setting (or absence)
of this variable `spring.web.resources.static-locations=`
## 2 - Build jar (final artifact w/ dependencies)
- gradle bootJar

Make sure the front-end files are in the `back-end/build/libs/battleship-server-0.0.1-SNAPSHOT.jar`. 
Open the .jar with Winrar or 7-zip and check the files are in `/BOOT-INF/classes/static`.
Don't try to build the artifact using IntelliJ, the manifest and the .jar structure will not work out. Work with the framework (spring) [1](https://stackoverflow.com/questions/43520616/artifact-of-spring-boot-project-generated-by-intellij-idea-causes-errors#:~:text=You%20are%20using%20Spring%20Boot%20and%20as%20such%20should%20also%20use%20the%20Spring%20Boot%20Maven%20Plugin%20to%20create%20an%20executable%20artifact.%20You%20are%20working%20around%20the%20framework%20instead%20of%20with%20the%20framework)

## Run everything with [docker-compose](https://docs.docker.com/compose/)
Start docker desktop, and wait until the Docker Engine is running (green rectangle at the bottom left corner). And then execute command
in CMD:

- docker-compose -f docker-compose.yml -p "battleship-servers" up

The -f indicates that the container will run in the foreground (in docker compose),
meaning that if you close the command line you used to execute it, the container will shutdown. After you build,
the images list in docker won't update, you'll need to restart docker desktop.
The process of the container starts and attaches to the console to the process’s standard input, standard output, and standard error

-d would indcate to run in the background (detached mode)

-p indicates the name of the group of containers. By default it will be the name of the folder the docker-compose.yml file is in

Docker compose will run multiple containers in 1 containers named Battleship-servers

#### To only build the images and not start containers:
- docker-compose -f docker-compose.yml build

- "up" -> Builds, (re)creates, starts, and attaches to containers for a service
- "down -> Stops containers and removes containers, networks, volumes, and images created by up
- Note: docker-compose only allows putting the `-f` in the beggining and `build` at the end apparently
## Run 1 container at a time
Build image (see images page in docker desktop). The -f indicates filename, the -t indicates the name of the image
- docker build -f Dockerfile . -t battleship-image

Run container (see containers page in docker desktop, open (click) the container, and see the prints, and check the port)
- docker run -d -p 8080:8080 battleship-image

Now acess http://localhost:8080/ 

Note: Having `--server.port=8082` as part of the CMD args in the Dockerfile will make adding docker env variables like `--env SERVER_PORT=8083`
to not take effect, because it will not override (cmd args has more priority, and it will be set on the image)
To properly override the server.port variable, put `--env SERVER_PORT=8082` before `battleship-image`.
The 8080 is the port you use to access the server.

#### Meaning of the -p command (if I undertood it correcly):
Port that the host enviornment will use to connect to the container. 
- [External Port] **:** [Internal Container's port (which I think is shared among all containers of a container group)]

See [1](https://runnable.com/docker/binding-docker-ports), [2](8https://www.baeldung.com/linux/assign-port-docker-container#why-we-use-port-mapping), [3](https://stackoverflow.com/questions/25350496/running-docker-container-on-a-specific-port), [4](https://docs.docker.com/engine/reference/commandline/run/#publish-or-expose-port--p---expose)

## Run another container
- docker run -d -p 8081:8081 --env SERVER_PORT=8081 battleship-image

## Notes:
When some changes aren't making effect, gradle clean and delete the images and try again. When you run the docker compose it uses the latest
built images, thus you need to delete them so they are built again using the latest jar. Try to delete `docker_pg_database` docker volumes folder

The param `&auth-method=password` is in the JDBC_DATABASE_URL because if I didn't it would give the error: 
`org.postgresql.util.PSQLException: FATAL: no pg_hba.conf entry for host "172.21.0.3", user "postgres", database "postgres", no encryption` [stack overflow](https://stackoverflow.com/questions/25641047/org-postgresql-util-psqlexception-fatal-no-pg-hba-conf-entry-for-host).

If I did `&sslmode=require` it would give me `PSQLException: The server does not support SSL`. So I tried `&auth-method=password` and it worked. See [auth-method posgres docs](https://www.postgresql.org/docs/9.5/auth-pg-hba-conf.html#:~:text=and%20hostnossl%20records.-,auth%2Dmethod,-Specifies%20the%20authentication)

## Notes
- https://stackoverflow.com/questions/43951720/react-router-and-nginx
- https://gist.github.com/mattd/1006398/598df8f218a18bc1b0f3415550b4a369f37afb7c