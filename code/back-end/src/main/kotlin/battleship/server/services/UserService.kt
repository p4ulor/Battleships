package battleship.server.services

import battleship.server.dataIsInMemory
import battleship.server.model.NewUser
import battleship.server.model.Player
import battleship.server.storage.UserData
import battleship.server.storage.db.UserPostgres
import battleship.server.storage.mem.UserMem
import battleship.server.utils.*
import org.jdbi.v3.core.Jdbi
import org.springframework.stereotype.Component
import java.util.regex.Pattern

@Component
class UserService(
    jdbi: Jdbi,
    val userData: UserData = if(dataIsInMemory) UserMem() else UserPostgres(jdbi)
){

    fun createUser(u: CreateUserRequest) : RequestResult {
        if(!isEmailGood(u.email)) return RequestResult(error = Errors.EmailBadFormat)
        val user = NewUser(u.name, u.email, u.password) //the class will already hash and generate token!
        return acessStorage {
            val id = userData.createUser(user)
            RequestResult(UserTokenAndIDResponse(user.token, id))
        }
    }

    fun loginUser(u: LoginUserRequest) : RequestResult {
        return acessStorage {
            val tokenAndPwAndID = userData.loginUser(u.emailOrName, isEmailGood(u.emailOrName)) //2nd param will tell if the method of user identification will be through email or name of the user
            if(!isPasswordCorrect(u.password, tokenAndPwAndID.second)) RequestResult(error = Errors.WrongPassword)
            else RequestResult(UserTokenAndIDResponse(tokenAndPwAndID.first, tokenAndPwAndID.third))
        }
    }

    fun getPlayer(userID: Int?, token: String?) : Player? {
        if(userID!=null) return userData.getPlayer(userID)
        if(token!=null) return userData.getPlayerByToken(token)
        else throw BadRequestException("To get a player profile, either provide a player ID or send your token to get your profile")
    }

    fun getPlayerRankings(orderByWins: Boolean, paging: Paging) : List<Player> {
        val isOrderByWinsOrPlays = if(orderByWins) "winCount" else "playCount"
        return userData.getPlayerRankings(isOrderByWinsOrPlays, paging)
    }
}

private fun isEmailGood(emailAddress: String) : Boolean { //NAO TOLERA O 'Ç' !!!
    val regexPattern = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@" + "[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$"
    return Pattern.compile(regexPattern).matcher(emailAddress).matches()
}