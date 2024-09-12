# Report / Resume
- Back end docs -> [here](./back-end.md)
- Front end docs -> [here](./front-end.md)
- See [api-documentation](./api-documentation.md).
- See [problems](./problems.md), describes what my api returns when errors occur
- See [Postman tests collection](./DAW.postman_collection.json) a series of pre-made HTTP requests to test the API
- [OG game rules](https://www.hasbro.com/common/instruct/battleship.pdf), [game instructions video](https://youtu.be/RY4nAyRgkLo), [game strategy analysis video](https://youtu.be/LbALFZoRrw8)

## Deployment guides
- [Local deployment guide](../back-end/README.md)
- [Cloud deployment guide](./Cloud%20deployment.md)

# Tags info
- Tag 0.1.0 (Oct 31, 2022) contains everything working (except siren, setup timer and round timers) and using memory as data source.
- Tag 0.1.1 (Nov 4, 2022) contains improvements regarding the comunication of errors between modules and implements the flexibility of using data source as being a Postgreql DB.
- Tag 1.0.0 (Jan 2, 2023) contains the initial front-end delivery (...)
## Tag 1.0.1 (Jan 3, 2023) 
- changed the token so now it's stored in a cookie (non-Http for now). 
- The shots of the user are now also returned to the client in case of a refresh instead of being saved sessionStorage. 
- Front-end now displays the wins (or games played depending on route). Some improvements on the setup and round timers.
## Tag 1.0.2  (Jan 6, 2023)
- Improved get player controller (by dividing 1 controller into 2, between getting the profile of the user itself making the request and getting the profile of a user given the ID)
- Added lazy mp3 requests (only when a audio file is to be played, a server request is made to get it, after that, it's stored in the client in javascript as a variable, instead of requesting all audio files in the first request (or in a refresh))
- Now game settings configured in the ShipsSetup outlet is now applied, width and height can be changed, along with the number of ship types and shots per round.
- The miss shot of the opponent is now shown as a ligther blue square
- The rankings page now supports paging. 
- API documentation

## Things that weren't done / could be improved
- In-game, refreshing the page makes the user quit the game?
- No use of hypermedia (which is kinda shitty to do)
- React Router links used for navigations between pages and fetch URI's are hardcoded in some places
- I cant rotate a ship yet and can only have them facing right
- No integrated back-end tests
- Perform better input when a user registers to avoid needless API requests
- Make buttons that do fetches not clickable while theres no response
- Make fetches cancelable when exiting a page that is doing a request
- Configure more proper status codes and route names

## Extra things / future ideas
- Use server side events
- Search game by lobby name
- Insert ships in board randomizer
- Make it possible to disable the necessity of the ships being surrounded by 1 non-occupied square
- In the future change the error messages from browser window alerts to more fancy error messages (currently using window alerts)
- Do all shots need to be done, and do I instantly get the shot result of multiple shots? or all the results at the same time?
- Add customizable ship sizes on the go, and make it possible for a ShipType to have a width