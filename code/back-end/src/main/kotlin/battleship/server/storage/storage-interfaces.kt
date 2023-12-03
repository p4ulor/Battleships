package battleship.server.storage

import battleship.server.model.*
import battleship.server.storage.db.daos.GamesList
import battleship.server.utils.Paging
import battleship.server.utils.TimeInterval

// This is the Storage AKA Repository interfaces
// Ordered preferably by CRUD - Create, Read, Update, Delete, ...
// Parameter orders: request (for PUT's and POST's), path-param, paging (for GETS), ...

interface InfoData { //Hmmmm...
   fun getSystemInfo() : ServerInfo
}
/*
There will be nullable returns for cases when something is not found
Note, since DB already checks for constraints like email and user already in use.
We aren't going to have methods that consult the DB to check that. That would cause a double check...
But these checks need to be implemented when the datasource is from memory!
 */
interface UserData {
    fun createUser(u: NewUser) : Int //the services returns the token! This returns the ID
    fun loginUser(emailOrName: String, withEmail: Boolean) : Triple<String, String, Int> //returns token, hashedPassword and userID to check with the provided password (in services)
    fun authenticateUser(token: String) : Int? //Used after a user has supposedly logged in, I expect him to send the token in the Authorization header on operations that require authentication/logging in. Returns his ID, for internal server operations
    fun getPlayer(id: Int) : Player?
    fun getPlayerByToken(token: String) : Player?
    fun getPlayerRankings(orderByWinsOrPlays: String, paging: Paging) : List<Player>
    fun updateStats(id: Int, playCount: Int, winCount: Int) //receives what increments are to be done to the playCount and winCount
}

interface GameData { //TODO CHECK IM NOT UPDATING THE RULES?
    //GameSetup methods:
    fun createGame(lobbyName: String, hostID: Int, rules: Rules) : Int //returns gameID
    fun joinGame(guestID: Int, gameID: Int, currentSetupTime: TimeInterval)
    fun setupBoard(gameID: Int, isUserHost: Boolean, ships: MutableList<Ship>, setAsReady: Boolean, gameStatus: GameStatus, currentSetupTime: TimeInterval, currentRoundTime: TimeInterval)
    fun getOpenGames(paging: Paging) : List<GamesList>

    //GamePlay methods:
    fun storeUserRound(gameID: Int, nextRoundStatus: GameStatus, isUserHost: Boolean, userShots: MutableList<Shot>, enemyShips: MutableList<Ship>, currentRoundTime: TimeInterval)
    fun setGameStatus(gameID: Int, gameStatus: GameStatus)

    //Aux/General methods
    fun getGame(gameID: Int) : Game?
    fun findGameWithUser(userID: Int, anyStatesToLookFor: List<GameStatus>) : Game?
    fun deleteLobby(gameID: Int)
}
