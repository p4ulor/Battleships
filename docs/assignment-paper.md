# Phase 1 (back-end)
## Goal
The goal of this project is the development of a Web-based system allowing multiple players to play the [Battleship game](https://en.wikipedia.org/wiki/Battleship_(game)).

## Architecture
This system will be composed of a centralized backend service and one or more frontend applications. The frontend applications will run on the user's devices, providing the interface between those users and the system. The backend service will manage all the game related data and enforce the game rules.

Frontend applications will communicate with the backend service using an HTTP API. These applications will not communicate directly between themselves. All communication should be done via the backend service, which has the responsibility of ensuring all the game rules are followed, as well as storing the game states and final outcomes.

## Project phases
This project is divided into two phases:

- The first phase consists of the development of the backend service.
- The second phase consists of the development of a browser-based frontend application, which will use the backend service developed in the first phase.
The HTTP API exposed by the backend service, developed in the first phase, can also be usable by an Android-based frontend application, developed in the context of the Programação de Dispositivos Móvies course.

## Functionality
The HTTP API should provide the functionality required for a front-end application to:

- Obtain information about the system, such as the system authors and the system version, by an unauthenticated user.
- Obtain statistical and ranking information, such as number of played games and users ranking, by an unauthenticated user.
- Register a new user.
- Allow an user to express their desire to start a new game - users will enter a waiting lobby, where a matchmaking algorithm - will select pairs of users and start games with them.
- Allow an user to define the layout of their fleet in the grid.
- Allow an user to define a set of shots on each round.
- Inform the user about the state of its fleet.
- Inform the user about the state of the opponent's fleet.
- Inform the user about the overall state of a game, namely: game phase (layout definition phase, shooting phase, completed phase).

## Game rules and evolution
The Battleship game does not have a single set or rules. Instead, it allows for multiple variations on the game rules, such as:

- Grid size (e.g. a 10 by 10 grid).
- Fleet composition (e.g. a "carrier, a "battleship", a "cruiser", a "submarine", and a "destroyer".
- Ship size and layout (e.g. the carrier taking five grid slots).
- Number of shots per round (e.g. one shot per round).
- Maximum time for a player to define the grid layout or define a round's shot.

The backend service and exposed HTTP API should be designed in a way to allow the evolution of these gaming rules, including allowing different games to use different rules. The frontend applications should adapt automatically to these evolving and dynamic game rules.

## Delivery
The first phase should be delivered until October 31 2022, via the creation of the 0.1.0 tag on the group's repository. Any change after that data should result in the creation of a patch tag (e.g. 0.1.1).

The backend service should be executable via the docker-compose system without any other dependencies. I.e. the backend service should be executable on any machine with docker desktop, via a docker compose command, without requiring any additional software installation or configuration.

The delivery should also contain:

The HTTP API documentation required for a frontend client application to use this API. This documentation should not include any information about the internal backend implementation.
A single technical document, with the backend internal software organization, the data model, and the main implementation challenges.
Both these two documents should be linked from the README.md file, located in the repositories root directory


# Phase 2 (front-end)
## Goals for the second phase
The goal of the project's second phase is the creation of a browser-based application front-end application, exposing a user-interface for all the Battleship game functionalities.
This application should use the HTTP API developed on the first phase, and be based on the single page application (SPA) architecture:

- The application is composed by a static HTML file, a set of JavaScript files, and other resources such as images and stylesheets.
- The non-static UI interface is generated on the browser-side, based on interactions with the HTTP API developed on the first phase.

If required, this second phase can include changes to the Backend Service and its HTTP API.

Delivery
The complete project should be delivered until January 3 2023, via the creation of the 1.0.0 tag on the group's repository. Any change after that data should result in the creation of a patch tag (e.g. 1.0.1).

The complete system, backend service and front-end static resource server, should be executable via the docker-compose system without any other dependencies.

In addition to the first phase documentation requirements, the final delivery should also contain:

- A single technical document, with the frontend application internal software organization, and the main implementation challenges.