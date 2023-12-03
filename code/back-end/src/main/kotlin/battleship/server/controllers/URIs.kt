package battleship.server.controllers

object URIs {
    const val api = "/api"

    object General {
        val systemInfo = "$api/system-info"
        val home = arrayOf("", "/", "/root")
    }

    object Users {
        private val root = "$api/users"
        val newUser = "$root/newuser"
        val getMyUser = "$root/me"
        val getUser = "$root/{id}"
        val loginUser = "$root/login"
        val getPlayerRankings = arrayOf("$api/ranking", "$api/ranking/{scheme}")
    }

    object GameSetup {
        private val root = "$api/setup"
        val adminDebug = root
        val newGame = "$root/newgame"
        val openGames = "$root/opengames"
        val joinGame = "$root/joingame"
        val boardSetup = "$root/board"
        val deleteLobby = "$root/lobby"
    }

    object GamePlay {
        private val root = "$api/play"
        val shoot = "$root/shoot"
        val getKnownInformation = "$root/update/{gameID}"
        val quit = "$root/quit"
    }
}