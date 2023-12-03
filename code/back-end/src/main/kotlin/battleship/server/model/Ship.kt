package battleship.server.model

import battleship.server.utils.pl
import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.*

//todo, one day make so the host can create new ShipTypes when creating a game.a
// And add way for ships to have width instead of lenght
enum class ShipType (val size: Int) { //Enum to avoid repeated ship names / types
    Carrier(5), Battleship(4), Cruiser(3), Submarine(3), Destroyer(2);
    init {
        require(size in 1..MAX_BOARD_SIZE)
    }
    companion object {
        fun convertToShipType(s: String) : ShipType? {
            return when(s.lowercase(Locale.getDefault())){ //toLowerCase() was deprecated
                "carrier" -> Carrier
                "battleship" -> Battleship
                "cruiser" -> Cruiser
                "submarine" -> Submarine
                "destroyer" -> Destroyer
                else -> null
            }
        }
    }
}

data class Ship(
    val shipType: ShipType,
    val head: Position,
    val direction: Direction,
    @JsonIgnore private var isdestroyed: Boolean = false, //not in use because it was causing problems? or cuz its really not in use anymore?
) {
    val positionsOccupying: MutableList<Position> = mutableListOf() //filled and computed by the rules

    fun isDestroyed() : Boolean {
        if(isdestroyed) return true
        else isdestroyed = positionsOccupying.all { it.entity==Entity.DAMAGED }
        return isdestroyed
    }
}

enum class Direction(val x: Int, val y: Int) {
    UP(0, 1), RIGHT(-1, 0), DOWN(0, -1), LEFT(1, 0); //already provides the inverse direction when Position.extend() or Position.moveTowards() is called

    fun getOpposite() : Direction {
        return when(this){
            UP -> DOWN
            DOWN -> UP
            LEFT -> RIGHT
            RIGHT -> LEFT
        }
    }

    fun getSides() : Pair<Direction, Direction> {
        return when(this){
            UP, DOWN -> Pair(LEFT, RIGHT)
            LEFT, RIGHT -> Pair(UP, DOWN)
        }
    }

    companion object{
        fun convertToDirection(s: String) : Direction? {
            val ret = when(s.lowercase(Locale.getDefault())){ //toLowerCase() was deprecated
                "up" -> UP
                "down" -> DOWN
                "left" -> LEFT
                "right" -> RIGHT
                else -> null
            }
            pl("Direction $s converted to -> $ret")
            return ret
        }
    }

}
