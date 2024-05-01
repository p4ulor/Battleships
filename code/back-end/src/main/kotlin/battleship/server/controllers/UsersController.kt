package battleship.server.controllers

import battleship.server.model.Player
import battleship.server.services.UserService
import battleship.server.services.processResult
import battleship.server.utils.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.validation.Valid

/**
A controller is a container of handlers.
A handler is a function that is responsible for processing HTTP requests. Handlers contain @___Mapping annotation
The annotations defines for spring a handler, which is associated with the function. This will make a bridge/channel
 that will receive the HTTP requests and forward them to the function
*/

@RestController
@RequestMapping("users")
class UsersController(private val userService: UserService) {

    //Example of PostMapping method using Map as the type of the function parameter for educational purposes and to demonstrate alternatives
    /*@PostMapping("newuser2")
    fun createUser2(@RequestBody body: Map<String, String>): ResponseEntity<String>{ //trying to use MultiValueMap results in the error: "Content type 'application/json;charset=UTF-8' not supported" https://stackoverflow.com/a/69199760/9375488 on browser and postman (atleast for me)
        val name = body.get("name") //getting the values like this, makes us have to write a bunch of code to validate here the values obtained. It's much easier and cleaner using data-classes and annotations that help us validate the request
        if (!isNotBlank_NotEmpty_NoExtraSpaces(name, true))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Name can't be null, empty or have white spaces")
        return ResponseEntity.ok(name)
    }*/

    @PostMapping("newuser") //note, this way this works is that, for this path to be accessed, the URI must include the string in @RequestMapping of this class, like so: users/newusers
    fun createUser(@Valid @RequestBody u: CreateUserRequest, response: HttpServletResponse) : UserTokenAndIDResponse {
        val res = userService.createUser(u)
        return processResult<UserTokenAndIDResponse>(res) { //the process result will see if there was an error, like password being wrong
            setCookies(response, res.data as UserTokenAndIDResponse)
        }
    }

    @GetMapping("me")
    fun getMyUser(req: HttpServletRequest) : Player {
        val token = getAuthorizationToken(req) ?: throw ForbiddenException("You are not logged in, so you can't get your profile")
        return userService.getPlayer(null, token) ?: throw NotFoundException("User not found")
    }

    @GetMapping("{id}") //use example: users/1 this controller is kinda scuffed... a separate controller should be done
    fun getUser(@PathVariable id: String, req: HttpServletRequest) : Player {
        pl("obtained id=$id")
        val userID: Int = try { Integer.valueOf(id) } catch (e: NumberFormatException){ throw BadRequestException("Path variable :id not a number") }
        if(userID!!<0) throw BadRequestException("Path variable {id} must be a non negative number")
        return userService.getPlayer(userID, null) ?: throw NotFoundException("User not found")
    }

    @PostMapping("login")
    fun loginUser(@Valid @RequestBody u: LoginUserRequest, response: HttpServletResponse) : UserTokenAndIDResponse {
        val res = userService.loginUser(u)
        return processResult<UserTokenAndIDResponse>(res) {
            setCookies(response, res.data as UserTokenAndIDResponse)
        }
    }

    //By default gives wins ranking
    @GetMapping("ranking", "/ranking/{scheme}") //Here this scheme could be a RequestParam, but I put it like this just to experiment with it
    fun getPlayerRankings(@PathVariable scheme: String?, @RequestParam limit: Int?, @RequestParam skip: Int?) : List<Player> { //alternative to putting nullable -> <RequestParam(required=false) skip: Int>, mas dps no codigo nao processa como sendo nullable...
        val paging = Paging.processNullablePaging(limit, skip)
        val orderByWins = scheme != "played"
        return userService.getPlayerRankings(orderByWins, paging)
    }

    fun setCookies(response: HttpServletResponse, data: UserTokenAndIDResponse) {
        //option 1
        val tokenCookie = Cookie("token", data.token)
        tokenCookie.path = "/"
        //tokenCookie.maxAge = 0
        //tokenCookie.isHttpOnly = true
        //response.addCookie(tokenCookie)

        //option 2
        response.setHeader("Set-Cookie", "token=${data.token};Path=/") //todo: Add SameSite adding ;HttpOnly makes so the cookie can only be used in the context of a Http request. To delete this type of cookie, a request must be done to the server to expire/delete the cookie https://developer.mozilla.org/en-US/docs/Web/HTTP/Cookies#restrict_access_to_cookies
        //response.setHeader("Set-Cookie", "userID=${data.userID};Path=/") //doing set cookie again overrides with the latest
    }

    //@PutMapping("logout")
    fun deleteCookie(){
        //todo in case of using HttpOnly
    }
}
