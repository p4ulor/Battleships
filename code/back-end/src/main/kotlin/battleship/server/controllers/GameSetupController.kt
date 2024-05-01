package battleship.server.controllers

import battleship.server.model.Game
import battleship.server.services.GameSetupService
import battleship.server.services.processResult
import battleship.server.storage.mem.games
import battleship.server.utils.*
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid

@RestController
@RequestMapping("setup")
class GameSetupController(private val gameSetupService: GameSetupService) {

    @GetMapping("") //just for quick and easier debugging (when running in memory)
    fun ay(req: HttpServletRequest) : List<Game>? {
        val token = getAuthorizationToken(req) ?: throw ForbiddenException("Authorization header missing")
        return if(token=="d83a659c-b452-11ec-b909-0242ac120002") games else null
    }

    @PostMapping("newgame")
    fun newGame(@Valid @RequestBody createGameRequest: CreateGameRequest, req: HttpServletRequest) : GameCreatedResponse {
        val token = getAuthorizationToken(req) ?: throw ForbiddenException("Authorization header missing")
        val res = gameSetupService.createGame(token, createGameRequest)
        return processResult<GameCreatedResponse>(res)
    }

    @GetMapping("opengames") //todo should a user be logged in to get the games?
    fun getOpenGames(@RequestParam limit: Int?, @RequestParam skip: Int?) : List<GamesListResponse> {
        val paging = Paging.processNullablePaging(limit, skip)
        val res = gameSetupService.getOpenGames(paging)
        return processResult<List<GamesListResponse>>(res)
    }

    @PutMapping("joingame")
    fun joinGame(@Valid @RequestBody jgr: JoinGameRequest, req: HttpServletRequest) : PlayersMatchedResponse {
        val token = getAuthorizationToken(req) ?: throw ForbiddenException("Authorization header missing")
        val res = gameSetupService.joinGame(token, jgr)
        return processResult<PlayersMatchedResponse>(res)
    }

    @PostMapping("board") //Can be called after both users join same lobby
    fun boardSetup(@Valid @RequestBody bsr: BoardSetupRequest, req: HttpServletRequest) : BoardBuiltResponse {
        val token = getAuthorizationToken(req) ?: throw ForbiddenException("Authorization header missing")
        val res = gameSetupService.boardSetup(token, bsr)
        return processResult<BoardBuiltResponse>(res)
    }

    @DeleteMapping("lobby") //Added in phase 2
    fun deleteLobby(req: HttpServletRequest) : String {
        val token = getAuthorizationToken(req) ?: throw ForbiddenException("Authorization header missing")
        val res = gameSetupService.deleteLobby(token)
        return processResult(res)
    }

    //Inspired by https://github.com/isel-lReic-daw/s2223i-51d-51n-public/blob/main/code/example-spring-boot-demo/src/main/kotlin/com/example/demo/controllers/ChatController.kt
    @GetMapping("listenready") //Not in use
    fun listen(req: HttpServletRequest) : SseEmitter {
        //val token = getAuthorizationToken(req) ?: throw ForbiddenException("Authorization header missing")
        val res = gameSetupService.newListener("4529bd41-9b91-4a53-b191-02750487af86")
        return processResult(res)
    }

    //the server will send the server side event when /setup/board (boardSetup()) is called
}
