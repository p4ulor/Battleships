package battleship.server.services

import battleship.server.utils.pl
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import javax.annotation.PreDestroy
import kotlin.concurrent.withLock

@Service
class ReadinessService {
    private val listeners = mutableListOf<OnGoingGame>()
    private var currentId = 0
    private val lock = ReentrantLock()

    /*val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1).also {
        it.scheduleAtFixedRate({ keepAlive() }, 2, 15, TimeUnit.SECONDS)
    }*/

    @PreDestroy
    private fun preDestroy() {
        pl("shutting down ReadinessService")
        //scheduler.shutdown()
    }

    fun newListener(gameID: Int, userID: Int, opponentID: Int) : SseEmitter = lock.withLock {
        pl("newListener $userID")
        val listener = OnGoingGame(gameID, SseEmitter(TimeUnit.MINUTES.toMillis(5)), false)

        listener.sseEmitter.onCompletion {
            removeListener(listener)
        }
        listeners.add(listener)
        return listener.sseEmitter
    }

    fun saytoOpponentMyReadiness(userID: Int, gameID: Int, isReady: Boolean) = lock.withLock {
        pl("sendMessage")
        if(false) sendEventToOpponent(Event.IsOpponentReady(userID, isReady), gameID)
    }

    private fun removeListener(listener: OnGoingGame) = lock.withLock {
        pl("remove")
        listeners.remove(listener)
    }

    private fun keepAlive() = lock.withLock {
        pl("keepAlive, sending to listeners, ${listeners.count()}")
        listeners.forEach {
            val event = Event.KeepAlive(Instant.now().epochSecond)
            try { event.writeTo(it.sseEmitter) }
            catch (ex: Exception) { pl("Exception while sending event - {}, ${ex.message}") }
        }
    }

    private fun sendEventToOpponent(event: Event, gameID: Int) {
        val game = listeners.find { it.gameID==gameID } ?: throw Exception("Readiness Service exception, game not found")
        try { event.writeTo(game.sseEmitter) }
        catch (ex: Exception) { pl("Exception while sending event - {}, ${ex.message}") }
    }
}

data class OnGoingGame(
    val gameID: Int,
    val sseEmitter: SseEmitter,
    var opponentSaidHeIsReady: Boolean = false
)

sealed interface Event {
    fun writeTo(emitter: SseEmitter)

    class IsOpponentReady(val id: Int, private val isReady: Boolean) : Event {
        override fun writeTo(emitter: SseEmitter) {
            val eventOccurred = SseEmitter.event().id("User id=$id says he is ready").name("message").data(isReady)
            emitter.send(eventOccurred)
        }
    }

    class KeepAlive(private val timestamp: Long) : Event {
        override fun writeTo(emitter: SseEmitter) {
            val eventOcurred = SseEmitter.event().comment("Keeping alive at $timestamp")
            emitter.send(eventOcurred)
        }
    }
}
