package battleship.server.storage

import battleship.server.model.NewUser
import battleship.server.storage.mem.UserMem
import battleship.server.storage.mem.users
import battleship.server.utils.Paging
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import java.util.*

class MemTests {
    @Test
    fun `Creating a new user`() {
        val userData = UserMem()
        val id = userData.createUser(NewUser("newuser", "newuser@gmail.com", "newuser"))
        assertTrue { userData.getPlayer(id)?.id==id }
        users.removeLastOrNull()
    }

    @Test
    fun `get user by name and authenticate`() {
        val userData = UserMem()
        userData.createUser(NewUser("newuser", "newuser@gmail.com", "newuser"))
        val token_hashedpw_id = userData.loginUser("newuser", false)
        assertTrue { userData.authenticateUser(token_hashedpw_id.first)!=null }
        users.removeLastOrNull()
    }

    @Test
    fun `update stats and get rankings wins and playCount`(){
        val userData = UserMem()
        val id = userData.createUser(NewUser("newuser", "newuser@gmail.com", "newuser"))

        userData.updateStats(id, 0, Integer.MAX_VALUE)
        val players = userData.getPlayerRankings("winCount", Paging.processNullablePaging(1, 0))
        assertTrue { players.first().id==id }

        userData.updateStats(id, Integer.MAX_VALUE, 0)
        val players2 = userData.getPlayerRankings("playCount", Paging.processNullablePaging(1, 0))
        assertTrue { players.first().id==id }
        users.removeLastOrNull()
    }
}