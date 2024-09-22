package battleship.server.model.game

import battleship.server.utils.pl
import java.util.*

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
