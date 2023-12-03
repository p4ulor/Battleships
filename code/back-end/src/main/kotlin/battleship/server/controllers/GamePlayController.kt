package battleship.server.controllers

import battleship.server.services.GamePlayService
import battleship.server.services.processResult
import battleship.server.utils.*
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid

@RestController
@RequestMapping("play")
class GamePlayController(private val gamePlayService: GamePlayService) {
    @PostMapping("shoot") //Will inform the result of the shot
    fun shoot(@Valid @RequestBody shot: FireShotRequest, req: HttpServletRequest) : ShotResultResponse {
        val token = getAuthorizationToken(req) ?: throw ForbiddenException("Authorization header missing")
        val res = gamePlayService.shoot(token, shot)
        return processResult<ShotResultResponse>(res)
    }

    //Used for polling (loop fetching) while we dont implement server side events
    @GetMapping("update/{gameID}")
    fun getKnownInformation(@PathVariable gameID: Int, @RequestParam consult: Boolean?, req: HttpServletRequest) : Any { //It can either return the entire Game data (if game is finished) or the game data respective to a user
        val token = getAuthorizationToken(req) ?: throw ForbiddenException("Authorization header missing")
        val res = gamePlayService.getKnownInformation(token, gameID, consult)
        return processResult<Any>(res)
    }

    /** Should the client be required to send the gameID? Not really, cuz if I require for the client to
     * send the gameID? Not really, because:
     * - when I send the ID, the server will have to look for the game AND check if the user associated with the Auth token provided is in that game AND if the game status is valid (an going name which will necessarily be 1)
     * - And if I don't send the ID, the server will still have to look for the game, and it will look for game where the user is present AND check if the game status is valid (an going name which will necessarily be 1)
     * So it will end up leading to the same thing. The difference is that in one I need a request body with the ID and the query will use a primary key. But the constraints of our DB and rules, will be used to uniquely identify the game anyway
     */
    @PutMapping("quit")
    fun quitGame(req: HttpServletRequest) {
        val token = getAuthorizationToken(req) ?: throw ForbiddenException("Authorization header missing")
        val res = gamePlayService.quitGame(token)
        processResult<String>(res)
    }
}
