package battleship.server.storage.mem;

import battleship.server.model.NewUser
import battleship.server.model.Player
import battleship.server.model.User
import battleship.server.services.Errors
import battleship.server.storage.UserData
import battleship.server.utils.Paging
import battleship.server.utils.StorageException

class UserMem : UserData {
    override fun createUser(u: NewUser) : Int {
        if(isEmailInUse(u.email!!)) throw StorageException(Errors.EmailInUse)
        if(isNameInUse(u.name!!)) throw StorageException(Errors.UserNameInUse)
        val nextUserID = nextUserID()
        users.add(User(nextUserID, u.name, u.email, u.hashedPassword, u.token))
        return nextUserID
    }

    override fun loginUser(emailOrName: String, withEmail: Boolean) : Triple<String, String, Int> {
        val user = if(withEmail) users.find { it.email == emailOrName } else users.find { it.name == emailOrName }
        if(user==null) throw StorageException(Errors.UserNotFound, "Failed finding user using $emailOrName")
        return Triple(user.token, user.hashedPassword, user.id)
    }

    override fun getPlayer(id: Int) : Player? = users.find { it.id == id }?.toPlayer()

    override fun getPlayerByToken(token: String) : Player? = users.find { it.token == token }?.toPlayer()

    override fun getPlayerRankings(orderByWinsOrPlays: String, paging: Paging) : List<Player> {
        require(orderByWinsOrPlays=="playCount" || orderByWinsOrPlays=="winCount")
        val isOrderByWins = orderByWinsOrPlays=="winCount"
        val range = paging.toIndexRange(users.size)
        return if(isOrderByWins) users.map { it.toPlayer() }.sortedByDescending { it.winCount }.subList(range.first, range.last)
               else users.map { it.toPlayer() }.sortedByDescending { it.playCount }.subList(range.first, range.last)
    }

    override fun updateStats(id: Int, playCount: Int, winCount: Int) {
        val user = users.find { it.id==id } ?: throw StorageException(Errors.UserNotFound)
        user.playCount += playCount
        user.winCount += winCount
    }

    override fun authenticateUser(token: String) : Int? {
        val user = users.find { it.token == token }
        return if(user!=null) user?.id else null
    }
}

private fun isEmailInUse(email: String) = users.find{it.email == email} != null
private fun isNameInUse(name: String) = users.find{it.name == name} != null
