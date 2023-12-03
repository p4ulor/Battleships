package battleship.server.utils

import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder

/**
 * https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html#authentication-password-storage-bcrypt
 * https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/crypto/password/Pbkdf2PasswordEncoder.html
 */
var pbkdf2PasswordEncoder = Pbkdf2PasswordEncoder("salt", 1000, 128) //aumentar estes valores vai metendo mais lento(mas mais "seguro" e hash maior). Se o salt for alterado as PW passadas ja nao sao válidas

fun hashPassword(password: String) : String {
    val ret = pbkdf2PasswordEncoder.encode(password)
    pl("PW hashed->$ret")
    return ret
}

fun isPasswordCorrect(password: String, hashedPassword: String) : Boolean {
    var res = false
    //I putted this in a try catch cuz there was a time when there was a strange internal server error, and I presume the exception came from here
    try { res = pbkdf2PasswordEncoder.matches(password, hashedPassword) }
    catch (e: Exception){ pl("isPasswordCorrect exception ->$e") }
    if(res) pl("Correct PW") else pl("Wrong PW")
    return res
}
