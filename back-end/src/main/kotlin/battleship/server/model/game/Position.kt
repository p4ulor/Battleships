package battleship.server.model.game

import battleship.server.utils.pl
import kotlin.math.absoluteValue

/**
 * NOTE! BE WARRY AND NEVER SWITCH COLUMN WITH THE ROW ON INSTANTIATION
 */
data class Position(var column: Int, var row: Int, var entity: Entity = Entity.WATER) {

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
            return Pair(
                if(didColFailed) colDif.absoluteValue else null,
                if(didRowFailed) rowDif.absoluteValue else null
            )
        }
        return null
    }

    fun doesExtendToANegativePos(steps: Int, dir: Direction) : Boolean {
        if(dir== Direction.LEFT || dir== Direction.UP) return false
        try{
            Position(
                this.column + dir.x * (steps - 1),
                this.row + dir.y * (steps - 1), Entity.SHIP
            )
        }
        catch (e: Exception) {
            return true
        }
        return false
    }
    fun moveTowards(dir: Direction) = Position(this.column + dir.x, this.row + dir.y, Entity.SHIP)

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
            return try {
                Position(column!!, row!!, entity)
            } catch (e: Exception ){ null }
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
