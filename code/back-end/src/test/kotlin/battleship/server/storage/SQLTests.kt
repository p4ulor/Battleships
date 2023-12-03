package battleship.server.storage


import battleship.server.model.NewUser
import battleship.server.storage.db.UserPostgres
import battleship.server.utils.Paging
import battleship.server.utils.pl
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.postgres.PostgresPlugin
import org.jdbi.v3.sqlobject.SqlObjectPlugin
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import java.sql.SQLException
import kotlin.system.exitProcess
import kotlin.test.assertTrue

class SQLTests {

    private val postgresDataSource = PGSimpleDataSource()

    init {
        postgresDataSource.setUrl("jdbc:postgresql://localhost/postgres?user=postgres&password=MYDB")
        try{
            pl("Will wait 30s for the database connection validation...")
            if(postgresDataSource.connection.isValid(30)) pl("Connection successful")
        } catch (sqle: SQLException){
            pl("Connection failed because -> $sqle.\n\n stackTrace -> ${sqle.stackTrace.toList()}\n\n")
            exitProcess(-1)
        }
    }

    @Test
    fun `Creating a new user`() {
        runAndRollback { jdbi ->
            val userData = UserPostgres(jdbi)
            val id = userData.createUser(NewUser("newuser", "newuser@gmail.com", "newuser"))
            assertTrue { userData.getPlayer(id)?.id==id }
        }
    }

    @Test
    fun `get user by name and authenticate`() {
        runAndRollback { jdbi ->
            val userData = UserPostgres(jdbi)
            userData.createUser(NewUser("newuser", "newuser@gmail.com", "newuser"))
            val token_hashedpw_id = userData.loginUser("newuser", false)
            assertTrue { userData.authenticateUser(token_hashedpw_id.first)!=null }
        }
    }

    @Test
    fun `update stats and get rankings wins and playCount`(){
        runAndRollback { jdbi ->
            val userData = UserPostgres(jdbi)
            val id = userData.createUser(NewUser("newuser", "newuser@gmail.com", "newuser"))

            userData.updateStats(id, 0, Integer.MAX_VALUE)
            val players = userData.getPlayerRankings("winCount", Paging.processNullablePaging(1, 0))
            assertTrue { players.first().id==id }

            userData.updateStats(id, Integer.MAX_VALUE, 0)
            val players2 = userData.getPlayerRankings("playCount", Paging.processNullablePaging(1, 0))
            assertTrue { players.first().id==id }
        }
    }

    private fun runAndRollback(runOperations: (jdbi: Jdbi) -> Unit){
        val jdbi = Jdbi.create(postgresDataSource).apply {
            installPlugin(KotlinPlugin()) //https://jdbi.org/#_resultset_mapping
            installPlugin(PostgresPlugin()) //https://jdbi.org/#_postgresql
            installPlugin(SqlObjectPlugin()) //https://jdbi.org/#_declarative_api
        }

        jdbi.useTransaction<Exception> { handle -> //Auto commits and closes the handle at the end
            runOperations(jdbi)
            handle.rollback()
        }
    }

}
