package battleship.server.model

import battleship.server.utils.hashPassword
import java.util.*

//not data class, so I'm not forced to use 'val' on password
class NewUser( //Used in creation
    val name: String,
    val email: String? = null, //must be valid on creation
    password: String, //for obtaining the token (logging in)
    val token: String = UUID.randomUUID().toString(), //for front-end authentication
) { var hashedPassword: String
    init { hashedPassword = hashPassword(password) }
}

data class User ( //AKA DB_User
    val id: Int, // to be incremented automatically
    val name: String, //must be unique on creation
    val email: String? = null, //must be unique on creation
    val hashedPassword: String,
    val token: String = UUID.randomUUID().toString(),
    var playCount: Int = 0,
    var winCount: Int = 0
){
    fun toPlayer() = Player(this)
    fun toInGamePlayer() = InGamePlayer(this)
}

data class Player( //only used for representing a user in a leader-board context, not stored
    val id: Int,
    val name: String,
    val playCount: Int,
    val winCount: Int
) {
    constructor(u: User): this(u.id, u.name, u.playCount, u.winCount)
}

data class InGamePlayer( //only used for representing a user in a gaming context, not stored
    val id: Int,
    val name: String,
) {
    constructor(u: User): this(u.id, u.name)
}

