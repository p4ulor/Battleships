package battleship.server.model

import battleship.server.utils.TimeInterval
import battleship.server.utils.doesSurpassStringBuilder
import battleship.server.utils.isDurationGood
import battleship.server.utils.pl
import java.lang.Math.abs
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import kotlin.math.absoluteValue

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

        if(gameStatus==GameStatus.HOST_TURN && isHost(userID)) {
            val res = makeShotAux(position, guestShips, hostShots)
            if(res==ShotResult.SUNK){ //todo, da para dar fix codigo repetido?
                if(guestShips.all { it.isDestroyed() }){
                    gameStatus=GameStatus.WINNER_IS_HOST
                    return ShotResult.WIN
                }
            }
            if(res!=ShotResult.INVALID_ALREADY_HIT) {
                if(abs(hostShots.size - guestShots.size)==rules.shotsPerRound) {
                    roundTime.delayDeadlineWithSameInterval() //restartRoundTimer
                    switchTurns()
                }
            }
            return res
        }
        else if(gameStatus==GameStatus.GUEST_TURN && !isHost(userID)){
            val res = makeShotAux(position, hostShips, guestShots)
            if(res==ShotResult.SUNK){ //todo, da para dar fix codigo repetido?
                if(hostShips.all { it.isDestroyed() }){
                    gameStatus=GameStatus.WINNER_IS_GUEST
                    return ShotResult.WIN
                }
            }
            if(res!=ShotResult.INVALID_ALREADY_HIT) {
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
        if(myShots.any { Position.arePositionsEqual(it.position, position) }) return ShotResult.INVALID_ALREADY_HIT //The user can't shoot to position he already shot before...
        myEnemyShips.forEach { enemyShip->
            enemyShip.positionsOccupying.forEach { enemyShipPosition ->
                if(Position.arePositionsEqual(enemyShipPosition, position)){
                    //if(enemyShipPosition.entity==Entity.DAMAGED) return ShotResult.INVALID_ALREADY_HIT
                    if(enemyShipPosition.entity==Entity.SHIP){ //I guess here there's no other possibility
                        position.entity = Entity.SHIP
                        myShots.add(Shot(position, ShotResult.HIT))
                        enemyShipPosition.entity=Entity.DAMAGED
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
            GameStatus.GUEST_TURN -> gameStatus=GameStatus.HOST_TURN
            GameStatus.HOST_TURN -> gameStatus=GameStatus.GUEST_TURN
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
        if(gameStatus==GameStatus.SHIPS_SETUP) return setupTime.timeRemainingSeconds()
        if(gameStatus.isGameOnGoing()) return roundTime.timeRemainingSeconds()
        else return 0
    }
}

//The ships allowed are all dependent on the dimension indicated
class Rules( //this data will not change after the game is created!
    val dimension: Dimension = Dimension(defaultBoardDimension, defaultBoardDimension),
    val shotsPerRound: Int = defaultShotsPerRound, //note that this is used to decide if it's time to switch turns or not
    val shipsAllowed: MutableList<ShipsTypesAndQuantity> = defaultShipTypes.toMutableList(), // .toMutableList() -> creates copy
    val setupTimeS: Int = defaultDurationS,
    val roundTimeS: Int = defaultDurationS,
    private val doAllShipTypesNeedToBeInserted: Boolean = defaultDoAllShipTypesNeedToBeInserted
) {
    constructor(dimension: Dimension, shotsPerRound: Int?, shipsAllowed: MutableList<ShipsTypesAndQuantity>?, setupTime: Int?, timeToMakeMove: Int?, doAllShipTypesNeedToBeInserted: Boolean?)
        : this(dimension, shotsPerRound ?: defaultShotsPerRound, shipsAllowed ?: defaultShipTypes, setupTime?: defaultDurationS, timeToMakeMove ?: defaultDurationS, doAllShipTypesNeedToBeInserted ?: defaultDoAllShipTypesNeedToBeInserted)

    init {
        if(shipsAllowed.isEmpty()) throw IllegalArgumentException("shipsAllowed can't be empty")
        if(shotsPerRound !in MIN_SHOTS_PER_ROUND..MAX_SHOTS_PER_ROUND) throw IllegalArgumentException("shotsPerRound must be in interval [$MIN_SHOTS_PER_ROUND, $MAX_SHOTS_PER_ROUND]")
        if(!isDurationGood(setupTimeS)) throw IllegalArgumentException("setupTime must be in interval [$MIN_DURATION_S, $MAX_DURATION_S] ")
        if(!isDurationGood(roundTimeS)) throw IllegalArgumentException("timeToMakeMove must be in interval [$MIN_DURATION_S, $MAX_DURATION_S] ")
        autoRemoveShipsTooBigForDimensions()
    }

    private fun autoRemoveShipsTooBigForDimensions(){
        shipsAllowed.removeIf {
            !isShipSizeAllowed(it.shipType)
        }
    }

    private fun isShipSizeAllowed(shipType: ShipType) =
        shipType.size<=dimension.columnDim && shipType.size<=dimension.rowDim

    private fun isShipTypeAndQuantityAllowed(shipType: ShipType, quantityOfTheShip: Int) : RejectedCause?{
        var rejectedCause: RejectedCause?
        val ship = shipsAllowed.find { it.shipType==shipType }
        rejectedCause = if(ship!=null) null else RejectedCause.SHIP_TYPE //if it was shiptype is included in the rules, dont reject
        if(rejectedCause==null && ship!=null) {
            if(doAllShipTypesNeedToBeInserted){
                rejectedCause = if(ship.quantityAllowed==quantityOfTheShip) null else RejectedCause.SHIP_TYPE_QUANTITY_UNFULFILLED
            } else {
                rejectedCause = if(ship.quantityAllowed >=quantityOfTheShip) null else RejectedCause.SHIP_TYPE_QUANTITY
            }
        }
        return rejectedCause
    }

    //todo check if max num of types for each type was reached
    fun areTheseShipsSizeAndTypeAllowed(ships: List<ShipsTypesAndQuantity>) : Pair<RejectedCause?, ShipType?> { //returns null if all ships are allowed
        ships.forEach {
            var shipTypeAndQuantityCheck = isShipTypeAndQuantityAllowed(it.shipType, it.quantityAllowed)
            if(shipTypeAndQuantityCheck!=null) return Pair(shipTypeAndQuantityCheck, it.shipType)

            shipTypeAndQuantityCheck = if(isShipSizeAllowed(it.shipType)) null else RejectedCause.SHIP_SIZE
            if(shipTypeAndQuantityCheck!=null) return Pair(shipTypeAndQuantityCheck, it.shipType)
        }
        return Pair(null, null)
    }

    enum class RejectedCause (val msg: String) {
        SHIP_SIZE("The ship is too big for the board"),
        SHIP_TYPE("The ship type is not allowed"),
        SHIP_TYPE_QUANTITY("The amount of ships of this type was exceeded"),
        SHIP_TYPE_QUANTITY_UNFULFILLED("The exact number of required ships was not met")
    }

    private fun buildShip(s: Ship) : Pair<String, MutableList<Position>>  { //it's assumed areTheseShipsSizeAndTypeAllowed() has already been called
        //todo improve this method
        if(s.head.doesExtendToANegativePos(s.shipType.size, s.direction)) return Pair("Ship extends to negative parts of the board", mutableListOf())
        var currentPos = s.head
        repeat(s.shipType.size){
            val doesSurpass = currentPos.doesSurpass(dimension) //a safe way to check if the position surpassed the board
            if(doesSurpass!=null){
                val sb = StringBuilder("The ship${if(currentPos==s.head) " head " else " "}surpassed the dimensions of the board by ")
                doesSurpassStringBuilder(sb, doesSurpass)
                return Pair(sb.toString(), mutableListOf())
            }
            s.positionsOccupying.add(currentPos)
            pl("Will move to direction=${s.direction} from pos=$currentPos")
            try { currentPos = currentPos.moveTowards(s.direction) } //a non safe operation to move to a position, the nature of this cycle makes us have to do it like this
            catch (e: Exception){}
        }
        val positionsOccupied = Position.getAllAround(s.head,s.direction, s.shipType.size)
        return Pair("", positionsOccupied)
    }

    //Will try to build the position slots of the ship according to the board's dimensions, the ships head position and direction and the length of the shiptype.
    fun buildShips(ships: MutableList<Ship>) : Triple<String, Boolean, MutableList<Ship>>{
        var error = "" //Explanation for the rejection
        val occupiedPositions: MutableList<Position> = mutableListOf()
        ships.forEach { ship ->
            val res = buildShip(ship)
            val isThereOverlap = occupiedPositions.any { occupiedPosition ->
                ship.positionsOccupying.any { builtShipPosition ->
                    if(occupiedPosition==builtShipPosition){
                        pl("Overlapped -> OccupiedPos: $occupiedPosition, BuiltShipPosition: $builtShipPosition")
                        true
                    } else false
                }
            }
            if(isThereOverlap) error = "The ship ${ship.shipType} overlaps with others"
            if(res.first.isNotEmpty()) error = res.first
            if(error.isNotEmpty()) return@forEach
            else {
                res.second.forEach {
                    occupiedPositions.add(it)
                }
            }
        }
        /* according to some of my calculations, when the ships are in corners (and their closest) the relation between the number of occupied slots and slots the ships occupy is at max 2.9 (rounded up)
        5x4 = 20positions occupied, ship slots 7 -> 20 / 7 = 2.857...
        4*7 = 28positions occupied, ship slots 11 -> 28 / 11 = 2.545...
        Board 5 by 6 = 30 positions, 2 ships of 1 by 4 are possible (1 space in all sides) = 8 ships slots, 30/8 = 3.75
        If the previous board would turn to 6 by 6 = 36, it would be possible to add another ship of 5 slots: 36/8 = 4.5, 36/13=2.76
        Board 3x6 = 18, ship of size 6 can fit. 18/6 = 2. If I turn to 4*6 = 24, 24/6 = 4. I can add another ship of 6, then it will turn to: 24/12 = 2
        Board 3 by 4 = 12 positions, 2 ships of 1 by 4 are possible (at the edges) = 8 ship slots, 12/8 = 1.5
         */
        val numOfShipSlots = ships.groupBy { it.positionsOccupying }.count()
        val canMoreShipsBeAdded = dimension.numOfPositions/numOfShipSlots >=4
        if(doAllShipTypesNeedToBeInserted && canMoreShipsBeAdded) if(!areAllShipTypesInList(ships)) return Triple("Not all ships were inserted", true, ships)
        return Triple(error, canMoreShipsBeAdded, ships)
    }

    private fun areAllShipTypesInList(ships: MutableList<Ship>) : Boolean {
        return shipsAllowed.all {typeAndQuantity ->
            ships.any { it.shipType == typeAndQuantity.shipType }
        }
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

    fun isGameOnGoing() = this==HOST_TURN || this==GUEST_TURN

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
) { init { require(result==ShotResult.MISS || result==ShotResult.HIT) }} //It's irrelevant to store other ShotResults IMO

enum class ShotResult {
    INVALID_ALREADY_HIT, MISS, HIT, SUNK, WIN,
    OFF_THE_BOARD
}

enum class Entity{
    WATER, SHIP, DAMAGED
}

data class Position(var column: Int, var row: Int, var entity: Entity = Entity.WATER) {
    //NOTE! BE WARRY AND NEVER SWITCH COLUMN WITH THE ROW ON INSTANTIATION
    init {
        require(column in 1..MAX_BOARD_SIZE)
        require(row in 1..MAX_BOARD_SIZE)
    }

    fun doesSurpass(dimension: Dimension) : Pair<Int?, Int?>? {
        val colDif = dimension.columnDim-column
        val rowDif = dimension.rowDim-row
        val didColFailed = colDif<0
        val didRowFailed = rowDif<0
        if(didColFailed || didRowFailed){
            return Pair(if(didColFailed) colDif.absoluteValue else null, if(didRowFailed) rowDif.absoluteValue else null)
        }
        return null
    }

    companion object {
        fun newPositionGivenLetterRow(column: Int, row: Char) : Position {
            var rowInInt = when (row) {
                in 'A'..'Z' -> { row - 'A'+1 }
                in 'a'..'z' -> { row - 'a'+1 }
                else -> throw IllegalArgumentException("Row char isn't A-Z or a-z")
            }
            return Position(column, rowInInt)
        }

        fun newPosition(column: Int?, row: Int?, entity: Entity = Entity.WATER) : Position? {
            return try { Position(column!!, row!!, entity) } catch (e: Exception ){ null }
        }

        //because using the default == compares Entity too
        fun arePositionsEqual(pos1: Position, pos2: Position) = pos1.column==pos2.column && pos1.row==pos2.row

        fun getAllAround(pos: Position, dir: Direction, range: Int) : MutableList<Position> {
            val occupyingPositions = mutableListOf<Position>()
            var initialPos = try { pos.moveTowards(dir.getOpposite()) } catch (e: Exception){ pos } //will go to 1 position towards the opposite direction given the current position if possible
            repeat(range+1){//+1 cuz it Will go towards the provided direction times range + 1 more position to cover all around
                try{
                    val sides = dir.getSides() //will extract the position it's at, the 2 positions on its sides
                    try {
                        val posSide1 = initialPos.moveTowards(sides.first)
                        occupyingPositions.add(posSide1)
                    } catch (e: Exception){}
                    try {
                        val posSide2 = initialPos.moveTowards(sides.second)
                        occupyingPositions.add(posSide2)
                    } catch (e: Exception){}
                    occupyingPositions.add(initialPos)
                    initialPos = initialPos.moveTowards(dir)
                } catch (e: Exception){}
            }
            pl("Positions occupied -> $occupyingPositions")
            return occupyingPositions
        }
    }
}

fun Position.doesExtendToANegativePos(steps: Int, dir: Direction) : Boolean {
    if(dir==Direction.LEFT || dir==Direction.UP) return false
    try{ Position(this.column + dir.x * (steps-1), this.row + dir.y * (steps-1), Entity.SHIP) }
    catch (e: Exception) { return true }
    return false
}
fun Position.moveTowards(dir: Direction) : Position {
    return Position(this.column + dir.x, this.row + dir.y, Entity.SHIP)
}
