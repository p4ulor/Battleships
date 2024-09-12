package battleship.server.utils

import battleship.server.enableDebugPrints
import javax.servlet.http.HttpServletRequest

fun pl(s: String) = if(enableDebugPrints) println(s) else Unit

fun isNotBlank_NotEmptyAndOnlyA_Z_0_9(s: String) : Boolean {
    val pat = "^\\w+(\\s\\w+)*\$" //Only letters, numbers, only 1 space between words. [a-zA-Z0-9_]. https://stackoverflow.com/a/37402198/9375488
    return s.matches(pat.toRegex()) //or Pattern.compile(pat).matcher(s).matches()
}

fun isNotBlank_NotEmpty_NoExtraSpaces(s: String?, checkForSpaces: Boolean = false) : Boolean { //checks is is null or empty, or blank, or extra worthless spaces
    if (s.isNullOrEmpty() || s.isBlank()) return false
    if (checkForSpaces && s.matches(".*\\s.*".toRegex())) return false
    return true
}

fun getAuthorizationToken(request: HttpServletRequest) : String? {// Bearer token
    //split " " and get(1) because it returns-> "Bearer 359fcc7e-3c63-4258-8840-30e408494ba0"
    val bearerToken = request.getHeader("Authorization")?.split(" ")?.get(1) ?: null
    if(bearerToken==null) { //this processing depends (hard-coded-type-solution) on the cookies that we send to the client...
        pl("Cookie obtained: ${request.getHeader("Cookie")}")
        val token = request.getHeader("Cookie")?.split("=")?.get(1) ?: null
        return token
    } else return bearerToken
}

fun doesSurpassStringBuilder(sb: StringBuilder, dim: Pair<Int?, Int?>) {
    if(dim.first!=null){ sb.append("${dim.first} in the columns") }
    if(dim.second!=null){
        if(dim.first!=null) sb.append("and by ")
        sb.append("${dim.second} in the rows")
    }
}

fun printRequestInfo(request: HttpServletRequest) { //for debugging / explorational / educational purposes
    val sb = StringBuilder("Remote address -> ${request.remoteAddr}\n")
    sb.append("Local address -> ${request.localAddr}\n")
    sb.append("Remote user -> ${request.remoteUser}\n")
    sb.append("Locale user -> ${request.locale}\n")
    sb.append("Device memory -> ${request.getHeader("Device-Memory")}\n")

    request.headerNames.toList().forEach {
        sb.append("$it -> ${request.getHeader(it)}\n")
    }
    sb.deleteCharAt(sb.length - 1)
    println(sb)
}
