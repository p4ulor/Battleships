package battleship.server.storage.mem

import battleship.server.model.*
import battleship.server.services.Status
import battleship.server.storage.GameData
import battleship.server.storage.db.daos.GamesList
import battleship.server.utils.*

class GameMem : GameData {
    //GameSetup methods:
    override fun createGame(lobbyName: String, hostID: Int, rules: Rules) : Int {
        val id = nextGameID()
        games.add(Game(id, lobbyName, hostID, rules = rules))
        return id
    }

    override fun joinGame(guestID: Int, gameID: Int, currentSetupTime: TimeInterval) {
        val idx = games.indexOfFirst { it.id==gameID }
        val g = games[idx].copy() //preciso de rever isto nao sei se é preciso fazer assim
        g.guestID = guestID
        g.gameStatus = GameStatus.SHIPS_SETUP
        g.setupTime = currentSetupTime
        games[idx] = g
    }

    override fun setupBoard(gameID: Int, isUserHost: Boolean, ships: MutableList<Ship>, setAsReady: Boolean, gameStatus: GameStatus, currentSetupTime: TimeInterval, currentRoundTime: TimeInterval) {
        val game = games.find { it.id==gameID} ?: throw StorageException(Status.GameDoesntExist)
        if(isUserHost){
            game.hostShips = ships
            game.isHostReady = setAsReady
        }
        else {
            game.guestShips = ships
            game.isGuestReady = setAsReady
        }
        game.gameStatus = gameStatus
        game.setupTime = currentSetupTime
        game.roundTime = currentRoundTime
    }

    override fun getOpenGames(paging: Paging): List<GamesList> {
        val g = games.filter { it.gameStatus==GameStatus.WAITING_FOR_GUEST }.map { GamesList(it.id, it.lobbyName, it.hostID) }
        val range = paging.toIndexRange(g.size)
        return g.subList(range.first, range.last)
    }

    override fun storeUserRound(gameID: Int, nextRoundStatus: GameStatus, isHost: Boolean, userShots: MutableList<Shot>, enemyShips: MutableList<Ship>, currentRoundTime: TimeInterval) {
        val game = games.find { it.id==gameID } ?: throw StorageException(Status.GameDoesntExist)
        game.gameStatus = nextRoundStatus
        if(isHost){
            game.hostShots = userShots //or use replaceAll?
            game.guestShips = enemyShips
        } else {
            game.guestShots = userShots
            game.hostShips = enemyShips
        }
    }

    //GamePlay methods:
    override fun setGameStatus(gameID: Int, status: GameStatus) {
        require(status.isAFinaleState()) { "Setting a game state through these means requires a finalizing game state" }
        val game = games.find { it.id==gameID }
        if(game==null) throw Exception("Game not found")
        else game.gameStatus = status
    }

    //Common/General methods
    override fun getGame(gameID: Int) : Game? = games.find { it.id==gameID }

    override fun findGameWithUser(userID: Int, anyStatesToLookFor: List<GameStatus>) : Game? { //3.1
        if(anyStatesToLookFor.isEmpty()) throw IllegalArgumentException("statesToLookFor can't be empty")
        var state: GameStatus? = null
        val game = games.find {game ->
            if(game.hostID==userID || game.guestID==userID){
                //previously I had .all which didnt make any sense cuz a game can only have 1 state
                anyStatesToLookFor.any {
                    if(game.gameStatus==it){
                        state = it
                        true
                    } else false
                }
            } //dont forget the 'else'!
            else false
        }
        pl("Found game w/ state ${state?.name}-> "+game?.id)
        return game
    }

    override fun deleteLobby(gameID: Int) {
        val wasRemoved = games.removeIf { it.id == gameID }
        if(!wasRemoved) throw NotFoundException("Game with id=$gameID not found")
    }
}
