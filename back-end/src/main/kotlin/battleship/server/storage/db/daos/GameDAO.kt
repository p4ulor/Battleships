package battleship.server.storage.db.daos

import battleship.server.model.game.Game
import battleship.server.model.game.GameStatus
import battleship.server.model.game.Rules
import battleship.server.utils.Paging
import battleship.server.utils.TimeInterval
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.introspect.VisibilityChecker
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.customizer.BindList
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

private const val table = "GAME"

@Repository
interface GameDAO {
    @GetGeneratedKeys("id")
    @SqlUpdate("INSERT INTO $table " +
            "(id, lobbyName, hostID, guestID, rules, gameCreationDateTime, isHostReady, isGuestReady, gameStatus, hostShips, hostShots, guestShips, guestShots, setupTime, roundTime) " +
            "VALUES(nextval('GAME_id_seq'), :lobbyName, :hostID, :guestID, :rules, :gameCreationDateTime, :ishostReady, :isguestReady, :gameStatus, :hostShips, :hostShots, :guestShips, :guestShots, :setupTime, :roundTime)")
    fun createGame(@BindBean game: DB_Game) : Int

    @SqlQuery("SELECT id, lobbyName, hostID FROM $table WHERE gameStatus = 'WAITING_FOR_GUEST' LIMIT :p.limit OFFSET :p.skip")
    fun getOpenGames(@BindBean ("p") paging: Paging) : List<GamesList>

    @SqlUpdate("UPDATE $table SET guestID = :guestID, gameStatus = 'SHIPS_SETUP' WHERE id = :gameID") //the disadvantage of making these calls with annotations is that they must be time constants, so we cant reference GameStatus.SHIPS_SETUP
    fun joinGame(guestID: Int, gameID: Int)

    @SqlQuery("SELECT * FROM $table WHERE id = :gameID")
    fun getGame(gameID: Int) : DB_Game?

    @SqlQuery("SELECT * FROM $table WHERE (hostID = :userID OR guestID = :userID) AND gameStatus IN (<states>)") //https://jdbi.org/#_binding_arguments_2:~:text=from%20users%20where%20id%20in%20(%3CuserIds%3E)
    fun findGameWithUser(userID: Int, @BindList("states") anyStatesToLookFor: List<String>): DB_Game?

    @SqlUpdate("UPDATE $table SET gameStatus = :gameStatus WHERE id = :gameID")
    fun setWinner(gameID: Int, gameStatus: String)

    @SqlUpdate("DELETE FROM $table WHERE id = :gameID")
    fun deleteGame(gameID: Int)

    /*@SqlUpdate("UPDATE $table SET " +
            "<if(:userishost)> hostShips = :ships, <endif> guestShips = :ships," +
            "<if(:userishost)>ishostready = :asready, <endif>isguestready = :asready  WHERE id = :gameID")
    fun setupBoard(gameID: Int, userishost: Boolean, ships: String, asready: Boolean)*/ //didn't work for me
}

val jacksonOjectMapper: ObjectMapper = ObjectMapper().registerKotlinModule()  //https://github.com/FasterXML/jackson-module-kotlin#usage   https://stackoverflow.com/a/53192674/9375488
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) //pq do Ship isDestroyed https://stackoverflow.com/a/26371693/9375488 senao dá com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException: Unrecognized field \"destroyed\" (class battleship.server.model.Ship), not marked as ignorable
    .setVisibility(VisibilityChecker.Std.defaultInstance().withFieldVisibility(JsonAutoDetect.Visibility.ANY))
    .findAndRegisterModules() // https://stackoverflow.com/a/37499348/9375488 if removed, causes -> Java 8 date/time type `java.time.Duration` not supported by default: add Module "com.fasterxml.jackson.datatype:jackson-datatype-jsr310" to enable handling (through reference chain: battleship.server.model.Rules

data class GamesList( //because the param names need to match with the names of the columns!
    val id: Int,
    val lobbyName: String,
    val hostID: Int
)

// https://jdbi.org/#jdbi3-json
// https://jdbi.org/#_jackson_json_processing

data class DB_Game(
    var id: Int,
    val lobbyName: String,
    val hostID: Int,
    var guestID: Int?,
    val rules: String, //Rules in json. Easier than having another table I guess
    val gameCreationDateTime: String = LocalDateTime.now().toString(), //https://jdbi.org/#_supported_argument_types

    var ishostReady: Boolean = false, //having the param like: isHostReady gave error saying it didnt find param to bind to... -> https://stackoverflow.com/a/66197706/9375488
    var isguestReady: Boolean = false,

    var gameStatus: String = "WAITING_FOR_GUEST", //enum from GameStatus
    var hostShips: String = "[]", //MutableList<Ship> in json
    var hostShots: String = "[]", //MutableList<Shot> in json

    var guestShips: String = "[]", //MutableList<Ship> in json
    var guestShots: String = "[]", //MutableList<Shot> in json

    var setupTime: String, //TimeInterval in json
    var roundTime: String //TimeInterval in json
) {
    constructor(lobbyName: String, hostID: Int, rules: Rules, setupTime: TimeInterval, roundTime: TimeInterval) :
            this(0, lobbyName, hostID, null,
                jacksonOjectMapper.writeValueAsString(rules),
                setupTime = jacksonOjectMapper.writeValueAsString(setupTime),
                roundTime = jacksonOjectMapper.writeValueAsString(roundTime)
            )
    companion object {
        fun GameToDB_Game(game: Game) : DB_Game{
            return DB_Game(
                game.id,
                game.lobbyName,
                game.hostID,
                game.guestID,
                jacksonOjectMapper.writeValueAsString(game.rules),
                game.gameCreationDateTime.toString(),
                game.isHostReady,
                game.isGuestReady,
                game.gameStatus.name, //todo check if its this
                jacksonOjectMapper.writeValueAsString(game.hostShips),
                jacksonOjectMapper.writeValueAsString(game.hostShots),
                jacksonOjectMapper.writeValueAsString(game.guestShips),
                jacksonOjectMapper.writeValueAsString(game.guestShots),
                jacksonOjectMapper.writeValueAsString(game.setupTime),
                jacksonOjectMapper.writeValueAsString(game.roundTime)
            )
        }

        fun DB_GameToGame(db_Game: DB_Game) : Game {
            return Game(
                db_Game.id,
                db_Game.lobbyName,
                db_Game.hostID,
                db_Game.guestID,
                jacksonOjectMapper.readValue(db_Game.rules),
                LocalDateTime.parse(db_Game.gameCreationDateTime),
                db_Game.ishostReady,
                db_Game.isguestReady,
                GameStatus.stringToGameStatus(db_Game.gameStatus),
                jacksonOjectMapper.readValue(db_Game.hostShips),
                jacksonOjectMapper.readValue(db_Game.hostShots),
                jacksonOjectMapper.readValue(db_Game.guestShips),
                jacksonOjectMapper.readValue(db_Game.guestShots),
                jacksonOjectMapper.readValue(db_Game.setupTime),
                jacksonOjectMapper.readValue(db_Game.roundTime)
            )
        }
    }
}