package battleship.server.model.game

import battleship.server.utils.doesSurpassStringBuilder
import battleship.server.utils.isDurationGood
import battleship.server.utils.pl

/**
 * The ships allowed are all dependent on the dimension indicated
 */
class Rules( //this data will not change after the game is created!
    val dimension: Dimension = Dimension(defaultBoardDimension, defaultBoardDimension),
    val shotsPerRound: Int = defaultShotsPerRound, //note that this is used to decide if it's time to switch turns or not
    val shipsAllowed: MutableList<ShipsTypesAndQuantity> = defaultShipTypes.toMutableList(), // .toMutableList() -> creates a copy
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
        val positionsOccupied = Position.getAllAround(s.head, s.direction, s.shipType.size)
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