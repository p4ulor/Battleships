package battleship.server.utils

//to be used on GET requests that possibly return large lists of objects
const val LIMIT = 20
const val UPPERBOUND_LIMIT = 200
data class Paging(var limit: Int = LIMIT, var skip: Int = 0) {
    init {
        if(doesLimitExceedMax(limit)) limit = UPPERBOUND_LIMIT
        else if(!isValidLimit(limit)) limit = LIMIT
        if(!isValidSkip(skip)) skip = 0
    }

    fun toIndexRange(listSize: Int) : IntRange { //turns a Paging into a valid range according to the length of a list
        var startIndex = this.skip
        if(this.limit==0 || listSize==0 || startIndex >= listSize) return IntRange(0, 0)
        var limitIndex = startIndex + this.limit
        if(limitIndex >= listSize) limitIndex = listSize ////no -1 because .subList is exclusive
        pl("Range: [$startIndex, $limitIndex[. Listsize given: $listSize.")
        return IntRange(startIndex, limitIndex)
    }

    companion object {
        fun processNullablePaging(limit: Int? , skip: Int?) = Paging(limit ?: LIMIT, skip ?: 0)
        fun isValid(limit: Int, skip: Int) = !doesLimitExceedMax(limit) && isValidLimit(limit) && isValidSkip(skip)
        private fun doesLimitExceedMax(limit: Int) = limit > UPPERBOUND_LIMIT
        private fun isValidLimit(limit: Int) = limit >=0
        private fun isValidSkip(skip: Int) = skip >= 0
    }
}