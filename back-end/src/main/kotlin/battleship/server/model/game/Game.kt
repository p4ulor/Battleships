package battleship.server.model.game

import battleship.server.utils.TimeInterval
import battleship.server.utils.doesSurpassStringBuilder
import battleship.server.utils.pl
import java.lang.Math.abs
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

const val MAX_BOARD_SIZE = 26
const val MIN_BOARD_SIZE = 2
const val MIN_COORDINATE = 1
const val MAX_NUM_OF_SHIPS_W_SAME_TYPE = 5
const val MIN_NUM_OF_SHIPS_W_SAME_TYPE = 1
const val MAX_SHOTS_PER_ROUND = 5
const val MIN_SHOTS_PER_ROUND = 1
const val MAX_DURATION_S = 5*60
const val MIN_DURATION_S = 5

const val defaultBoardDimension = 10
const val defaultQuantityOfEachShipType = MIN_NUM_OF_SHIPS_W_SAME_TYPE
const val defaultShotsPerRound = MIN_SHOTS_PER_ROUND
const val defaultDurationS = 3*60
const val defaultDoAllShipTypesNeedToBeInserted = false

val defaultShipTypes = mutableListOf(
    ShipsTypesAndQuantity(ShipType.Carrier, 1), //0
    ShipsTypesAndQuantity(ShipType.Battleship, 1),
    ShipsTypesAndQuantity(ShipType.Cruiser, 1), //2
    ShipsTypesAndQuantity(ShipType.Submarine, 1),
    ShipsTypesAndQuantity(ShipType.Destroyer, 1) //4
)

/**
 * Note: choosing to use UserID's here forces us to do an extra DB request to know each user's name when joining a game
 * the cost of other options would make us have duplicate data (the name) on this table
 *
 * Note2: we should not store the 2 boards (host and guest) simply as an 2D array or a List of the
 * current Entities (which includes the ships) because we can't extract the information of the ships to validate the
 * rules and state of the game so easily...
 * it would require an exhausting decode of the board. In the current format, we can recreate
 * the game from the start
*/
data class Game(
    //data for creation of game
    val id: Int, //todo one day: change to UUID?
    val lobbyName: String,
    val hostID: Int,
    var guestID: Int? = null, //MUTABLE
    val rules: Rules = Rules(),
    val gameCreationDateTime: LocalDateTime = LocalDateTime.now(), //only the server knows

    //data that varies throught GameSetup
    var isHostReady: Boolean = false, //MUTABLE
    var isGuestReady: Boolean = false, //MUTABLE

    //data that varies throught GamePlay
    var gameStatus: GameStatus = GameStatus.WAITING_FOR_GUEST, //MUTABLE
    var hostShips: MutableList<Ship> = mutableListOf(), //it's a string json in DB //MUTABLE
    var hostShots: MutableList<Shot> = mutableListOf(), //it's a string json in DB //MUTABLE

    var guestShips: MutableList<Ship> = mutableListOf(), //it's a string json in DB //MUTABLE
    var guestShots: MutableList<Shot> = mutableListOf(),  //it's a string json in DB //MUTABLE

    //values will be common/shared by both players:
    var setupTime: TimeInterval = TimeInterval(Duration.ofSeconds(rules.setupTimeS.toLong())), //MUTABLE
    var roundTime: TimeInterval = TimeInterval(Duration.ofSeconds(rules.roundTimeS.toLong())) //MUTABLE
){
    fun hasSetupTimeEnded() = setupTime.hasBeenSurpassed()
    fun hasRoundTimeEnded() = roundTime.hasBeenSurpassed()

    private fun setShips(isHost: Boolean, ships: MutableList<Ship>, setAsReady: Boolean) { //checks basic dimension and type checks. Then tries to build the ship
        if(isHost) {
            hostShips = ships
            isHostReady = setAsReady
        }
        else {
            guestShips = ships
            isGuestReady = setAsReady
        }
        if(isHostReady && isGuestReady) {
            roundTime.timeStampNow()
            gameStatus = GameStatus.HOST_TURN
        }
    }

    fun addShips(shipsOfTheUser: MutableList<Ship>, userID: Int, setAsReady: Boolean) : Triple<String, Boolean, MutableList<Ship>> {
        val isUserHost = isHost(userID)
        val resultTriple = rules.buildShips(shipsOfTheUser)
        if(resultTriple.first.isEmpty()) setShips(isUserHost, shipsOfTheUser, setAsReady)
        return resultTriple
    }

    /** This function may change the list of shots of a user, and the Entities in the positions where there are ships
        in the user's opponent list of ships, in case there's a HIT, SUNK or MISS (in the case of MSIS, it only changes user's list of shots)
        If a SUNK was detected, all the ships of that list will be cycled to check if they are all destroyed,
        in that case the user with the equal userID in the param will be the winner
        Resume: can change data -> gameStatus, hostShips, hostShots, guestShips, guestShots, unless it's an INVALID_SELF_SHOT OR INVALID_ALREADY_HIT
     */
    fun makeShot(userID: Int, position: Position) : ShotResult? {
        val isOutSide = position.doesSurpass(rules.dimension) //check if the position is inside the board's dimensions
        if(isOutSide!=null){
            val sb = StringBuilder("The shot position surpassed the dimensions of the board by ")
            doesSurpassStringBuilder(sb, isOutSide)
            pl(sb.toString())
            return ShotResult.OFF_THE_BOARD
        }

        if(gameStatus== GameStatus.HOST_TURN && isHost(userID)) {
            val res = makeShotAux(position, guestShips, hostShots)
            if(res== ShotResult.SUNK){ //todo, da para dar fix codigo repetido?
                if(guestShips.all { it.isDestroyed() }){
                    gameStatus= GameStatus.WINNER_IS_HOST
                    return ShotResult.WIN
                }
            }
            if(res!= ShotResult.INVALID_ALREADY_HIT) {
                if(abs(hostShots.size - guestShots.size)==rules.shotsPerRound) {
                    roundTime.delayDeadlineWithSameInterval() //restartRoundTimer
                    switchTurns()
                }
            }
            return res
        }
        else if(gameStatus== GameStatus.GUEST_TURN && !isHost(userID)){
            val res = makeShotAux(position, hostShips, guestShots)
            if(res== ShotResult.SUNK){ //todo, da para dar fix codigo repetido?
                if(hostShips.all { it.isDestroyed() }){
                    gameStatus= GameStatus.WINNER_IS_GUEST
                    return ShotResult.WIN
                }
            }
            if(res!= ShotResult.INVALID_ALREADY_HIT) {
                if(abs(guestShots.size-hostShots.size)+1==rules.shotsPerRound){ //+1 cuz of the difference of 1 player always starting first
                    roundTime.delayDeadlineWithSameInterval() //restartRoundTimer
                    switchTurns()
                }
            }
            return res
        }
        else return null //it's not the indicatedUser's turn! or the game is in another state which makes it impossible to start shots
    }

    private fun makeShotAux(position: Position, myEnemyShips: List<Ship>, myShots: MutableList<Shot>) : ShotResult {
        if(myShots.any {
                Position.arePositionsEqual(
                    it.position,
                    position
                )
            }) return ShotResult.INVALID_ALREADY_HIT //The user can't shoot to position he already shot before...
        myEnemyShips.forEach { enemyShip->
            enemyShip.positionsOccupying.forEach { enemyShipPosition ->
                if(Position.arePositionsEqual(enemyShipPosition, position)){
                    //if(enemyShipPosition.entity==Entity.DAMAGED) return ShotResult.INVALID_ALREADY_HIT
                    if(enemyShipPosition.entity== Entity.SHIP){ //I guess here there's no other possibility
                        position.entity = Entity.SHIP
                        myShots.add(Shot(position, ShotResult.HIT))
                        enemyShipPosition.entity= Entity.DAMAGED
                        if(enemyShip.isDestroyed()) return ShotResult.SUNK
                        return ShotResult.HIT
                    }
                }
            }
        }

        myShots.add(Shot(position, ShotResult.MISS))
        return ShotResult.MISS
    }

    fun isGameInTheFollowingStates(states: MutableList<GameStatus>) : Boolean {
        if(states.isEmpty()) throw IllegalArgumentException("statesToLookFor can't be empty")
        return states.any {
            this.gameStatus==it
        }
    }

    private fun switchTurns(){
        when (gameStatus) {
            GameStatus.GUEST_TURN -> gameStatus= GameStatus.HOST_TURN
            GameStatus.HOST_TURN -> gameStatus= GameStatus.GUEST_TURN
            else -> IllegalArgumentException("Can only call this function when gameStatus is ${GameStatus.GUEST_TURN} or ${GameStatus.HOST_TURN}")
        }
        pl("The turns have switched, gameStatus = $gameStatus")
    }

    fun isHost(userID: Int) = hostID==userID

    fun enrollGuest(guestID: Int) : Boolean {
        if(this.guestID==null) {
            this.guestID = guestID
            setupTime.timeStampNow()
            return true
        }
        return false
    }

    fun getCurrentTimerInEffect() : Int {
        if(gameStatus== GameStatus.SHIPS_SETUP) return setupTime.timeRemainingSeconds()
        if(gameStatus.isGameOnGoing()) return roundTime.timeRemainingSeconds()
        else return 0
    }
}

data class ShipsTypesAndQuantity (val shipType: ShipType, var quantityAllowed: Int){
    constructor(shipType: ShipType, quantityAllowed: Int?) : this(shipType, quantityAllowed?: defaultQuantityOfEachShipType)
    init { require(quantityAllowed in 1..MAX_NUM_OF_SHIPS_W_SAME_TYPE) }
    companion object {
        fun getAllMissingShipTypesAndQuantityInShipList(ships: MutableList<Ship>, typeAndQuantity: MutableList<ShipsTypesAndQuantity>) : MutableList<ShipsTypesAndQuantity> {
            val canBeAdded: MutableList<ShipsTypesAndQuantity> = mutableListOf()
            typeAndQuantity.forEach { typeAndQuantity ->
                val found = ships.filter { it.shipType==typeAndQuantity.shipType }
                if(found.isEmpty()) canBeAdded.add(typeAndQuantity)
                else {
                    val quantityLeft = typeAndQuantity.quantityAllowed-found.size
                    if(quantityLeft<0) throw IllegalStateException("Something went wrong in getAllMissingShipTypesAndQuantityInShipList")
                    else if(quantityLeft>0) canBeAdded.add(ShipsTypesAndQuantity(typeAndQuantity.shipType, quantityLeft))
                }
            }
            return canBeAdded
        }
    }
}

data class Dimension(val columnDim: Int, val rowDim: Int/*, val mustBeSquare: Boolean = true*/) {

    constructor(column: Int?, rowDim: Int?)
            : this(column ?: defaultBoardDimension, rowDim ?: defaultBoardDimension)
    init {
        //if(mustBeSquare) require(columnDim - rowDim == 0)
        require(columnDim in MIN_BOARD_SIZE..MAX_BOARD_SIZE)
        require(rowDim in MIN_BOARD_SIZE..MAX_BOARD_SIZE)
    }

    val numOfPositions = columnDim * rowDim
}

enum class GameStatus{
    WAITING_FOR_GUEST,
    SHIPS_SETUP,
    HOST_TURN, GUEST_TURN,
    WINNER_IS_HOST, WINNER_IS_GUEST,
    ABORTED; //No winner -> if 1 of the players leave
    //todo one day: create a command/operation to delete all games that are with ABORTED status since they are worthless
    fun isAFinaleState() : Boolean {
        return when(this){
            WINNER_IS_HOST -> true
            WINNER_IS_GUEST -> true
            ABORTED -> true
            else -> false
        }
    }

    fun isGameOnGoing() = this== HOST_TURN || this== GUEST_TURN

    companion object {
        fun stringToGameStatus(s: String) : GameStatus {
            return when(s.uppercase(Locale.getDefault())){
                "WAITING_FOR_GUEST" -> WAITING_FOR_GUEST
                "SHIPS_SETUP" -> SHIPS_SETUP
                "HOST_TURN" -> HOST_TURN
                "GUEST_TURN" -> GUEST_TURN
                "WINNER_IS_HOST" -> WINNER_IS_HOST
                "WINNER_IS_GUEST" -> WINNER_IS_GUEST
                "ABORTED" -> ABORTED
                else -> throw IllegalArgumentException("$s isn't a GameStatus")
            }
        }

        fun getWinForOpponent(s: GameStatus) : GameStatus {
            return when(s){
                GUEST_TURN -> WINNER_IS_HOST
                HOST_TURN -> WINNER_IS_GUEST
                else -> throw IllegalArgumentException("Bad use of getWinForOpponent method")
            }
        }
    }
}

data class Shot(
    val position: Position,
    var result: ShotResult
) { init { require(result== ShotResult.MISS || result== ShotResult.HIT) }} //It's irrelevant to store other ShotResults IMO

enum class ShotResult {
    INVALID_ALREADY_HIT, MISS, HIT, SUNK, WIN,
    OFF_THE_BOARD
}

enum class Entity{
    WATER, SHIP, DAMAGED
}
