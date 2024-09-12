package battleship.server.storage.db.daos

import battleship.server.model.NewUser
import battleship.server.model.Player
import battleship.server.utils.Paging
import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.customizer.Define
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.springframework.stereotype.Repository

private const val table = "USERS"

@Repository
interface UserDAO { //https://jdbi.org/#_declarative_api
    //todo, research @Transaction

    @GetGeneratedKeys("id") //just for debugging https://jdbi.org/#_getgeneratedkeys     https://jdbi.org/#_getgeneratedkeys_4
    @SqlUpdate("INSERT INTO $table " +
             "(id, name, email, hashedPassword, token, playCount, winCount) " +
             "VALUES(nextval('USERS_id_seq'), :name, :email, :hashedPassword, :token, 0, 0)")
    fun createUser(@BindBean newUserDB: NewUser) : Int //if you remove @BindBean no error will occur, but the insertion will not work!!! The annotation will extract all the fields in the object and bind with the places where u put ':'

    /*@SqlQuery("SELECT hashedPassword FROM $table WHERE name= :u.emailOrname") //I decided to use the Spring function to check for the password instead of checking it in the db
    fun loginUserEmail(u: LoginUserRequest, withEmail: Boolean) : String? //returns hashedPassword
    In comment cuz idk how to return Pair<String, String> with this, i think we cant
    @SqlQuery("SELECT hashedPassword FROM $table WHERE name = :u.emailOrname") //I decided to use the Spring function to check for the password instead of checking it in the db
    fun loginUserName(u: LoginUserRequest, withEmail: Boolean) : String? //returns hashedPassword*/

    @SqlQuery("SELECT id FROM $table WHERE token = :token")
    fun authenticateUser(token: String) : Int?

    @SqlQuery("SELECT * FROM $table WHERE id=:id")
    fun getPlayer(id: Int) : Player? //to return the entire object just put *

    @SqlQuery("SELECT * FROM $table WHERE token=:token")
    fun getPlayerByToken(token: String): Player?

    @SqlQuery("SELECT * FROM $table ORDER BY <orderByWinsOrPlays> DESC LIMIT :paging.limit OFFSET :paging.skip") //:paging.limit is an alternative way of addressing the fields since I named the Bean
    fun getPlayerRankings(@Define orderByWinsOrPlays: String, @BindBean("paging") paging: Paging) : List<Player> //@Define is mostly used for conditions. Apparently the ':' only applies between certain SQL operators, like WHERE and VALUES. @BindBean for more complex objects w/ properties that need to be referenced, I think. https://stackoverflow.com/a/48757814/9375488

    @SqlUpdate("UPDATE $table SET playCount = :playCount, winCount = :winCount WHERE id = :id")
    fun updateStats(id: Int, playCount: Int, winCount: Int)
}
