package battleship.server.utils

import battleship.server.model.game.MAX_DURATION_S
import battleship.server.model.game.MIN_DURATION_S
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

class TimeInterval(private val interval: Duration) { //todo review
    private lateinit var deadLine: LocalDateTime

    fun timeStampNow(){ deadLine = LocalDateTime.now().plus(interval) }

    fun hasBeenSurpassed(): Boolean {
        val now = LocalDateTime.now()
        pl("Had deadline been surpased? Deadline->$deadLine. Now -> $now")
        if (deadLine.isBefore(now)) {
            pl("The time interval has been surpassed")
            return true
        } else {
            pl("The time interval has NOT been surpassed")
            return false
        }
    }

    fun delayDeadlineWithSameInterval() { //meant only to be used to reset round times
        if(!this::deadLine.isInitialized) return //https://stackoverflow.com/a/46584412/9375488
        val sb = StringBuilder("Deadline delayed from $deadLine ")
        deadLine = LocalDateTime.now().plus(interval)
        pl(sb.append("to -> $deadLine").toString())
        timeRemainingSeconds()
    }

    fun timeRemainingSeconds() : Int {
        if(!this::deadLine.isInitialized) return -1
        val ret = deadLine.toLocalTime().toSecondOfDay() - LocalTime.now().toSecondOfDay()
        pl("Time remaining: $ret")
        return ret
    }
}

fun isDurationGood(seconds: Int) : Boolean = seconds in MIN_DURATION_S..MAX_DURATION_S
