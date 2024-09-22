package battleship.server.model.game

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
        else isdestroyed = positionsOccupying.all { it.entity== Entity.DAMAGED }
        return isdestroyed
    }
}

