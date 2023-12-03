package battleship.server.storage.db

import battleship.server.model.*
import battleship.server.services.Status
import battleship.server.storage.GameData
import battleship.server.storage.db.daos.DB_Game
import battleship.server.storage.db.daos.GameDAO
import battleship.server.storage.db.daos.GamesList
import battleship.server.storage.db.daos.jacksonOjectMapper
import battleship.server.utils.*
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.sqlobject.kotlin.onDemand
import java.time.Duration

class GamePostgres(private val jdbi: Jdbi) : GameData {

    override fun createGame(lobbyName: String, hostID: Int, rules: Rules): Int {
        val gameToAdd = DB_Game(lobbyName, hostID, rules = rules, TimeInterval(Duration.ofSeconds(rules.setupTimeS.toLong())), TimeInterval(Duration.ofSeconds(rules.roundTimeS.toLong())))
        var newGameID: Int = -1
        try{ newGameID = jdbi.onDemand(GameDAO::class).createGame(gameToAdd) } //When using jdbi.onDemand(___:.class) -> In cases when there's an error, neither at run time the error will be thrown, and the debugger will say this method wasn't executed. You'll have to remove the call from try catch to see whats happening
        catch (e: Exception){ throw StorageException(Status.StorageError, e.toString()) }
        pl("New game ID -> $newGameID")
        return newGameID
    }

    override fun joinGame(guestID: Int, gameID: Int, currentSetupTime: TimeInterval) {
        try{
            jdbi.onDemand(GameDAO::class).joinGame(guestID, gameID)

            val currentSetupTimeString = jacksonOjectMapper.writeValueAsString(currentSetupTime)!!
            val sb = StringBuilder("UPDATE GAME SET ")
            sb.append("setupTime = :currentSetupTimeString ")
            sb.append("WHERE id = :gameID")
            jdbi.useHandle<Exception> { handle ->
                val query = handle.createUpdate(sb.toString())
                    .bind("currentSetupTimeString", currentSetupTimeString)
                    .bind("gameID", gameID)
                query.execute()
            }
        }
        catch (e: Exception){ throw StorageException(Status.StorageError, e.toString()) }
    }

    override fun setupBoard(gameID: Int, isUserHost: Boolean, ships: MutableList<Ship>, setAsReady: Boolean, gameStatus: GameStatus, currentSetupTime: TimeInterval, currentRoundTime: TimeInterval) {
        try{
            val shipsString = jacksonOjectMapper.writeValueAsString(ships)!!
            val currentSetupTimeString = jacksonOjectMapper.writeValueAsString(currentSetupTime)!!
            val currentRoundTimeString = jacksonOjectMapper.writeValueAsString(currentRoundTime)!!
            val sb = StringBuilder("UPDATE GAME SET ")
            if(isUserHost) sb.append("hostShips = :shipsString, isHostReady = :setAsReady, ")
            else sb.append("guestShips = :shipsString, isGuestReady = :setAsReady, ")
            sb.append("gameStatus = :gameStatus, ")
            sb.append("setupTime = :currentSetupTimeString, ")
            sb.append("roundTime = :currentRoundTimeString ")

            sb.append("WHERE id = :gameID")
            jdbi.useHandle<Exception> { handle ->
                val query = handle.createUpdate(sb.toString())
                    .bind("shipsString", shipsString)
                    .bind("setAsReady", setAsReady)
                    .bind("gameStatus", gameStatus.name)
                    .bind("currentSetupTimeString", currentSetupTimeString)
                    .bind("currentRoundTimeString", currentRoundTimeString)
                    .bind("gameID", gameID)
                query.execute()
            }
        } catch (e: Exception){ throw StorageException(Status.StorageError, e.toString()) }
    }

    override fun getOpenGames(paging: Paging): List<GamesList> {
        val list = try{ jdbi.onDemand(GameDAO::class).getOpenGames(paging) }
        catch (e: Exception){ throw StorageException(Status.StorageError, e.toString()) }
        return list
    }

    override fun storeUserRound(gameID: Int, nextRoundStatus: GameStatus, isHost: Boolean, userShots: MutableList<Shot>, enemyShips: MutableList<Ship>, currentRoundTime: TimeInterval) {
        try{
            val userShotsString = jacksonOjectMapper.writeValueAsString(userShots)!!
            val enemyShipsString = jacksonOjectMapper.writeValueAsString(enemyShips)!!
            val currentRoundTimeString = jacksonOjectMapper.writeValueAsString(currentRoundTime)!!
            val sb = StringBuilder("UPDATE GAME SET ")
            if(isHost) sb.append("hostShots = :userShotsString, guestShips = :enemyShipsString, ")
            else sb.append("guestShots = :userShotsString, hostShips = :enemyShipsString, ")
            sb.append("roundTime = :currentRoundTimeString, ")
            sb.append("gameStatus = :nextRoundStatus ")
            sb.append("WHERE id = :gameID")
            jdbi.useHandle<Exception> { handle ->
                val query = handle.createUpdate(sb.toString())
                    .bind("userShotsString", userShotsString)
                    .bind("enemyShipsString", enemyShipsString)
                    .bind("currentRoundTimeString", currentRoundTimeString)
                    .bind("nextRoundStatus", nextRoundStatus.name)
                    .bind("gameID", gameID)
                query.execute()
            }
        } catch (e: Exception){ throw StorageException(Status.StorageError, e.toString()) }
    }

    override fun setGameStatus(gameID: Int, gameStatus: GameStatus) {
        require(gameStatus.isAFinaleState()) { "Setting a game state through these means requires a finalizing game state" }
        try{ jdbi.onDemand(GameDAO::class).setWinner(gameID, gameStatus.name) }
        catch (e: Exception){ throw StorageException(Status.StorageError, e.toString()) }
    }

    override fun getGame(gameID: Int): Game? {
        return try{
            val db_Game = jdbi.onDemand(GameDAO::class).getGame(gameID) ?: return null
            DB_Game.DB_GameToGame(db_Game)
        }
        catch (e: Exception){ throw StorageException(Status.StorageError, e.toString()) }
    }

    override fun findGameWithUser(userID: Int, anyStatesToLookFor: List<GameStatus>): Game? {
        return try{
            val db_Game = jdbi.onDemand(GameDAO::class).findGameWithUser(userID, anyStatesToLookFor.map { it.name }) ?: return null
            DB_Game.DB_GameToGame(db_Game)
        }
        catch (e: Exception){ throw StorageException(Status.StorageError, e.toString()) }
    }

    override fun deleteLobby(gameID: Int) {
        if(getGame(gameID)==null) throw NotFoundException("Game with id=$gameID not found")
        return try{ jdbi.onDemand(GameDAO::class).deleteGame(gameID) }
        catch (e: Exception){ throw StorageException(Status.StorageError, e.toString()) }
    }
}