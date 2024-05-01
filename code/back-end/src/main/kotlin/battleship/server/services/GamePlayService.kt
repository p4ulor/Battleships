package battleship.server.services

import battleship.server.dataIsInMemory
import battleship.server.model.GameStatus
import battleship.server.model.Position
import battleship.server.model.ShotResult
import battleship.server.storage.GameData
import battleship.server.storage.UserData
import battleship.server.storage.db.GamePostgres
import battleship.server.storage.db.UserPostgres
import battleship.server.storage.mem.GameMem
import battleship.server.storage.mem.UserMem
import battleship.server.utils.*
import org.jdbi.v3.core.Jdbi
import org.springframework.stereotype.Component

@Component
class GamePlayService ( //GameSetupService comment at the top to see why I did it like this
    jdbi: Jdbi,
    val gameData: GameData = if(dataIsInMemory) GameMem() else GamePostgres(jdbi),
    val userData: UserData = if(dataIsInMemory) UserMem() else UserPostgres(jdbi)
) {

    fun shoot(token: String, shot: FireShotRequest) : RequestResult {
        val userID = userData.authenticateUser(token) ?: return RequestResult(error = Errors.InvalidTokenNotFound)
        val game = getOnGoingGameOfUser(userID, gameData) ?: return RequestResult(error = Errors.YouAreNotPartOfAnyOnGoingGame)
        //if(game.rules.hasRoundTimeEnded()) return RequestResult(errorStatus = Status.RoundTimeAsEnded) //this line if futile, because if the round time has ended, then the game would be over
        val positionOfTheShot = Position.newPosition(shot.position.column, shot.position.row) ?: return RequestResult("The shot has an invalid position -> [${shot.position.column}|${shot.position.row}]", Errors.InvalidShot)
        val shotResult = game.makeShot(userID, positionOfTheShot) ?: return RequestResult(error = Errors.ItsNotUsersTurnToShoot) //can change data (on success) -> gameStatus (switches turn or sets winner), hostShips, hostShots, guestShips, guestShots
        val isHost = game.isHost(userID)
        when(shotResult){
            ShotResult.INVALID_ALREADY_HIT -> return RequestResult(shotResult.toString(), Errors.InvalidShot) //no game data change
            ShotResult.OFF_THE_BOARD -> return RequestResult(shotResult.toString(), Errors.InvalidShot) //no game data change
            //It's MISS, HIT, SUNK or WIN. store the changed object in DB:
            else -> { //I made this else to avoid repetitive lines of calling 'gameData.storeUserRound', both in non-win and win case
                gameData.storeUserRound(game.id, game.gameStatus, isHost, if(isHost) game.hostShots else game.guestShots, if(isHost) game.guestShips else game.hostShips, game.roundTime)
                if(game.gameStatus==GameStatus.WINNER_IS_HOST){
                    userData.updateStats(game.hostID, 1, 1)
                    userData.updateStats(game.guestID!!, 1, 0)
                } else if (game.gameStatus==GameStatus.WINNER_IS_GUEST){
                    userData.updateStats(game.hostID, 1, 0)
                    userData.updateStats(game.guestID!!, 1, 1)
                }
            }
        }
        return RequestResult(ShotResultResponse(PositionRequestSegment(positionOfTheShot.column, positionOfTheShot.row), shotResult))
    }

    /** Previously this function automatically checked if a user was in an ongoing game. But the problem is that when the
     *  game ends, it throws a forbidden exception because the user is no longer in an "on going game".
     *  So now this function requires the gameID.
     *  This function return the appropriate info respective to each player, if the query param consult is true and the
     *  game is finished, all info of the game will be returned
     */
    fun getKnownInformation(token: String, gameID: Int, consult: Boolean?) : RequestResult {
        val userID = userData.authenticateUser(token) ?: return RequestResult(error = Errors.InvalidTokenNotFound)
        var game = gameData.getGame(gameID) ?: return RequestResult("gameID provided=$gameID", Errors.GameDoesntExist)
        val isUserHost = game.hostID==userID
        if(consult!=null){
            if(game.gameStatus.isAFinaleState()) return RequestResult(game)
            return RequestResult(error = Errors.YouCantConsultThisGameYet)
        } else {
            if(!isUserHost){
                if (game.guestID != userID) return RequestResult("gameID provided=$gameID", Errors.YouAreNotPartOfThisGame)
            }
        }
        if(game.guestID==null) return RequestResult(GetGameInformation(null, GameStatus.WAITING_FOR_GUEST, mutableListOf(), myShots = mutableListOf()))

        return if(isUserHost){
            val opponentName = userData.getPlayer(game.guestID!!)?.name
            val latestShotOfMyOpponent = if(game.guestShots.isEmpty()) null else game.guestShots.last()
            RequestResult(GetGameInformation(opponentName.toString(), game.gameStatus, game.hostShips, latestShotOfMyOpponent, game.getCurrentTimerInEffect(), game.hostShots))
        } else {
            val opponentName = userData.getPlayer(game.hostID)?.name
            val latestShotOfMyOpponent = if(game.hostShots.isEmpty()) null else game.hostShots.last()
            RequestResult(GetGameInformation(opponentName.toString(), game.gameStatus, game.guestShips, latestShotOfMyOpponent, game.getCurrentTimerInEffect(), game.guestShots))
        }
    }

    fun quitGame(token: String) : RequestResult { //Note: the user can quit in both host and guest turns
        val userID = userData.authenticateUser(token) ?: return RequestResult(error = Errors.InvalidTokenNotFound)
        val game = getOnGoingGameOfUser(userID, gameData) ?: return RequestResult(error = Errors.YouAreNotPartOfAnyOnGoingGame)
        val isValidTimeToQuit = game.isGameInTheFollowingStates((mutableListOf(GameStatus.HOST_TURN, GameStatus.GUEST_TURN, GameStatus.SHIPS_SETUP)))
        if(!isValidTimeToQuit) return RequestResult(error = Errors.NoValidStateToQuit)
        var newGameStatus: GameStatus
        if(game.gameStatus==GameStatus.SHIPS_SETUP){
            gameData.setGameStatus(game.id, GameStatus.ABORTED)
            pl("set game as aborted")
        } else {
            val isQuittingUserHost = game.hostID==userID
            newGameStatus = if(isQuittingUserHost) GameStatus.WINNER_IS_GUEST else GameStatus.WINNER_IS_HOST
            pl("set game as $newGameStatus")
            gameData.setGameStatus(game.id, newGameStatus)
            userData.updateStats(game.hostID, 1, if(isQuittingUserHost) 0 else 1)
            userData.updateStats(game.guestID!!, 1, if(isQuittingUserHost) 1 else 0 )
        }
        return RequestResult()
    }
}

//returns first game found in the following states -> WAITING_FOR_GUEST, HOST_TURN, GUEST_TURN, SHIPS_SETUP
internal fun getOnGoingGameOfUser(userID: Int, gameData: GameData) =
    gameData.findGameWithUser(userID, listOf(GameStatus.WAITING_FOR_GUEST, GameStatus.HOST_TURN, GameStatus.GUEST_TURN, GameStatus.SHIPS_SETUP))
