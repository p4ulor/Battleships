# Battleships 🚢💥
- A medium-large sized web-application done as the assignment for the college subject *Web Applications Development* ([DAW](https://github.com/isel-leic-daw/), [subject curricular unit](https://www.isel.pt/en/leic/web-application-development)) in the semester 22/23-winter. The assignment paper can be found [here p1](https://github.com/isel-leic-daw/s2223i-51d-51n-public/issues/1) & [here p2](https://github.com/isel-leic-daw/s2223i-51d-51n-public/issues/13) or in [docs](./docs/assignment-paper.md) in case it's deleted.
- Allows playing the Battleships game PvP with an account
- When creating a game, you can configure the types and number of each type of ship that's allowed, along with the time limits for configuring and the round of players. Rotating the ship is not supported, I didn't have time and it would be kinda hard for the way I made things work at the time

<p align="center">
    <img style="width: 350px; margin: auto auto;" src="./docs/imgs/cover_main.jpeg" />
</p>

# Quick demo 🎥
TODO

# Run guide 🛠️
Since I made a bash script and I don't want to repeat what's there, see
- [build everything & run.sh](build%20everything%20&%20run.sh)
- $ `bash build\ everything\ \&\ run.sh`
- If you intended to run with a PostgreSQL database, make sure it's running and you added the tables
```c
$ systemctl start postgresql // start service
$ systemctl status postgresql // check if service started
// To add the tables you can use pgAdmin or the terminal
$ sudo -u postgres psql // enter postgresql terminal for user postgres (easy way)
postgres=# \i ./back-end/src/main/sql-scripts/everythingScript.sql
// if you're in pgAdmin, refresh the schema, the tables should be there and with some data
```
- If you're on Windows you can use WSL to run script. Or convert it to a batch script with ChatGPT 🤷. And use the gradlew.bat

# Quick technical overview 📋
For the full details see [docs/README.md](./docs/README.md). Main docs:
- [back-end docs](./docs/back-end.md)
- [front-end docs](./docs/front-end.md)
## Programming Languages 🖥️
### Used in [back-end](./back-end/)
| Kotlin | PostgreSQL |
|:-:|:-:|
| <img width="60" src='https://upload.wikimedia.org/wikipedia/commons/thumb/7/74/Kotlin_Icon.png/480px-Kotlin_Icon.png'> | <img width="60" src='https://raw.githubusercontent.com/devicons/devicon/refs/heads/master/icons/postgresql/postgresql-plain-wordmark.svg'> |

As the build tool, I used Gradle with Kotlin DSL
### Used in [front-end](./front-end/)
| Typescript | JSX | CSS | HTML | 
|:-:|:-:|:-:|:-:|
| <img width="60" src='https://raw.githubusercontent.com/devicons/devicon/refs/heads/master/icons/typescript/typescript-original.svg'> | <img width="60" src='./docs/imgs/jsx.png'> | <img width="60" src='https://raw.githubusercontent.com/devicons/devicon/refs/heads/master/icons/css3/css3-plain-wordmark.svg'> | <img width="60" src='https://raw.githubusercontent.com/devicons/devicon/refs/heads/master/icons/html5/html5-plain-wordmark.svg'> |

## Frameworks 🧩
| Spring (for back-end) | React (for front-end) |
|:-:|:-:|
| <img width="60" src='https://raw.githubusercontent.com/devicons/devicon/refs/heads/master/icons/spring/spring-original-wordmark.svg'> | <img width="60" src='https://raw.githubusercontent.com/devicons/devicon/refs/heads/master/icons/react/react-original-wordmark.svg'> |

### Relevant libraries or tools used in back-end
- [JDBI](https://jdbi.org/) (built on top of JDBC), used to interact with the PostgreSQL database
- [Jackson JSON parsing](https://github.com/FasterXML/jackson), a JSON parsing library (similar to Google's `gson`), used to store some lists of complex objects into a JSON string in the database
## Recommended editors/programs to use 🔌
- **IntelliJ** (for back-end)
- **Visual Studio Code** (for front-end)
- [vscode-icons](https://marketplace.visualstudio.com/items?itemName=vscode-icons-team.vscode-icons) extension, for much better work-directory visuals in front-end
- **pgAdmin 4** (for PostgreSQL database operations, testing and consultation)
- **Docker desktop**  to manage the deployment of the application into containers

## Deployment methods experimented with 🐋
- Docker (and docker compose)
- Microsoft Azure

## Project resume ✍️
```
It consists of a full-stack web-application that allows users to play the Battleship game against other players. 
	The back-end uses the Spring framework with Kotlin and uses PostgreSQL server as the database (and the option to use plain memory for ease of use). The front-end uses the React framework with Typescript in the form of JSX (or TSX) to form the SPA (Single Page Application). I used pure CSS and Webpack as the bundler of the code and the static content (i.e. the single HTML page, the bundle.js file and all the fonts, images and audio files).
	After all was set and done, I wrote some Docker and Docker Compose files/scripts. Some to deploy the project locally, others to deploy in Microsoft Azure Cloud. The environment included an nginx service to perform the load balancing between the 2 instances of my application. In the case of deployment in Azure, I chose to setup separate PostgreSQL server (in Azure) external to the container so I could access and monitor it.
```

## Trivia 🎓
### 1 - Evaluation / rating
- 1st season exam: 9.6/20
- 2nd season exam: 14.5/20
- Pratical assignment: 16
- Final note: 15
### 2 - The grind
- Done alone, due to some group complications, but gave me a lot of motivation and I learned a lot. So it was actually a very good thing for me
- Thank you professors Pedro Felix and Filipe Freitas for the help
- Done in the winter, which is a season I don't like very much for the lack of sun and the cold, but in a sense it helps me focus and calms my mind. At the same time, I did 2 other hard subjects: Introduction to Programming in the Web ([IPW](https://github.com/isel-leic-ipw/). 90% alone) and C language and Assembly (PSC, alone). **I grew a lot as a programmer in the winter semester of 2022.** I wished developing web-apps wasn't so pushed towards the end of the course...
- Thankfully, I had Three 6 Mafia and Svicideboys to dwell into this challenging semester. Music was very helpful for me in order to have the right mindset and energy. In some instances, the loneliness of this path and the expectation of great challenges to come while in the cold of these nights reminded that it all depended on me. To go long with this, the gangsta mindset coming from my favorite artists made me man up and put in work. When you see the code and this work, just know that I was bangin' memphis rap while doing it. Putting in work while in the cold study-rooms and in the darkness of winter. This is T6M, and specially Mystic Stylez, fueled & certified code 😤😈. Now, I'll continue this journey while Ridin' N' Da Chevy...

<p align="center">
    <img style="width: 350px; margin: auto auto;" src="./docs/imgs/vibe-albums.jpg" />
</p>

