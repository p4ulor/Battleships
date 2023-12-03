package battleship.server.storage.db

import battleship.server.model.NewUser
import battleship.server.model.Player
import battleship.server.model.User
import battleship.server.services.Status
import battleship.server.storage.UserData
import battleship.server.storage.db.daos.UserDAO
import battleship.server.utils.Paging
import battleship.server.utils.StorageException
import battleship.server.utils.pl
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.sqlobject.kotlin.onDemand

private const val table = "USERS"

class UserPostgres (private val jdbi: Jdbi) : UserData {

    override fun createUser(u: NewUser) : Int {
        try {
            val userID = jdbi.onDemand(UserDAO::class).createUser(u)
            pl("New user ID -> $userID")
            return userID
        }
        catch (e: Exception){
            if(e.toString().contains("name")) throw StorageException(Status.UserNameInUse, e.toString())
            else throw StorageException(Status.EmailInUse, e.toString())
        }
    }

    override fun loginUser(emailOrName: String, withEmail: Boolean) : Triple<String, String, Int> {
        val fieldEmailOrName = if(withEmail) "email" else "name"
        try {
           jdbi.withHandle<User, Exception> { handle ->
                val query = handle.createQuery("SELECT * FROM $table WHERE $fieldEmailOrName = :emailOrname")
                .bind("emailOrname", emailOrName)
                query.mapTo(User::class.java).single()
                //todo try to use this one day https://jdbi.org/#_rowmapperfactory or like this https://jdbi.org/#_binding_arguments_2:~:text=Using%20the%20prefix%20attribute%20causes%20the%20bean%20mapper%20to%20map%20only%20those%20columns%20that%20begin%20with%20the%20prefix
           }.apply {
               return Triple(this.token, this.hashedPassword, this.id)
           }
        } catch (e: Exception){ throw StorageException(Status.UserNotFound, "Failed finding user using $fieldEmailOrName. SQL says -> ${e.message}") }
    }

    override fun getPlayer(id: Int): Player? {
        try { return jdbi.onDemand(UserDAO::class).getPlayer(id) }
        catch (e: Exception){ throw StorageException(Status.UserNotFound, "SQL says -> ${e.message}") }
    }

    override fun getPlayerByToken(token: String): Player? {
        try { return jdbi.onDemand(UserDAO::class).getPlayerByToken(token) }
        catch (e: Exception){ throw StorageException(Status.UserNotFound, "SQL says -> ${e.message}") }
    }

    override fun getPlayerRankings(orderByWinsOrPlays: String, paging: Paging) : List<Player> {
        require(orderByWinsOrPlays=="playCount" || orderByWinsOrPlays=="winCount")
        return try { jdbi.onDemand(UserDAO::class).getPlayerRankings(orderByWinsOrPlays, paging) }
        catch (e: Exception){ throw StorageException(Status.StorageError, e.toString()) }
    }

    override fun updateStats(id: Int, playCount: Int, winCount: Int) {
        try { jdbi.onDemand(UserDAO::class).updateStats(id, playCount, winCount) }
        catch (e: Exception){ throw StorageException(Status.UserNotFound, e.toString()) }
    }

    override fun authenticateUser(token: String): Int? {
        return try { jdbi.onDemand(UserDAO::class).authenticateUser(token)}
        catch (e: Exception){ throw StorageException(Status.InvalidTokenNotFound, e.toString()) }
    }
}


private fun processErrorMessage(e: Exception) : String { //todo
    val s = e.toString()
    if(s.contains("token")) return "Token not found"
    if(s.contains("email")) {
        if(s.contains("found")) return "Email not found"
        return "Name already exists"
    }
    if(s.contains("name"))  {
        if(s.contains("found")) return "Name not found"
        return "Name already exists"
    }
    return "SQL Error -> $s"
}
