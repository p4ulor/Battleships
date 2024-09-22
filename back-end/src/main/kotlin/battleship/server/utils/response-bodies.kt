package battleship.server.utils

import battleship.server.model.game.*

data class UserTokenAndIDResponse(
    val token: String,
    val userID: Int
)

data class GameCreatedResponse(
    val gameID: Int,
    val rules: Rules //since the game rules can return things by default the user might not know
)

data class GamesListResponse(
    val gameID: Int,
    val lobbyName: String,
    val hostID: Int,
    val hostName: String //tricky decisions here, all for the sake of the possibility of getting the name of the host, but it's preferable to send both id and name to avoid extra requests client side, which is less demanding compared to just DB accesses. The server at the level of services will get all the names from the hostID's obtained from data
)

data class BoardBuiltResponse( //User for board/setup
    val canAddMore: Boolean, //if the user said he was ready, this will be set as false and the shipTypesAndQuantityLeft will be empty
    val shipsBuilt: List<Ship>,
    val shipTypesAndQuantityLeft: List<ShipsTypesAndQuantity>
)

data class PlayersMatchedResponse( //the response a guest gets when joining a game
    val opponentName: String, //I have opponent name here because on the response of open games I didn't include the name, but now I do, but I'm just gonna keep it
    val rules: Rules //the user that joins a game, gets the rules
    //var gameStatus: GameStatus //when the players match (a guest joins a host) the gameStatus will be set to SHIP_SETUP afterwards, so it's not needed
)

data class ShotResultResponse(
    val position: PositionRequestSegment,
    val shotResult: ShotResult
)

data class GetGameInformation(
    val opponentName: String?,
    var gameStatus: GameStatus,
    val stateOfMyShips: List<Ship>,
    val latestShotOfMyOpponent: Shot? = null, //useful for updating front-end view. Yes, we could return all shots of the opponent, but shouldn't the client bare some responsibility to there store them too? Instead of the back-end to have to provide all the data all the time? And I guess its more important to make sure the client knows the state of his ships then the latest shot of the opponent that's why I return that list
    val currentTimer: Int = -1, //this can either be the setupShips timer or roundTimer, if it reaches 0 and the gameStatus is SHIPS_SETUP the game will be aborted, if it reaches 0 and gameStatus is host or guest turn, the opponent wins
    val myShots: List<Shot> //we could send the shots to the client, but supposedly he should know because he is the one that sends it... (in front end we store it in sessionStorage). UPDATE: A front-end application should work on 2 browsers, or even 2 computers at the same time (given the token), so this was added back
)

data class QuitMatchResponse(
    val wasQuitSuccessfull: Boolean
)
