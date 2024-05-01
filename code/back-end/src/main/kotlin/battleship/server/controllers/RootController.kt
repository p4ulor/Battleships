package battleship.server.controllers

import battleship.server.serverInfo
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody


/**
 * Defines the handler for the root path (home page) and a handler to obtain server info
 * https://www.baeldung.com/spring-requestparam-vs-pathvariable
 * https://www.baeldung.com/spring-optional-path-variables
 */

@Controller
class RootController {

    @GetMapping("system-info")
    @ResponseBody //this is not needed when using @RestController, but it's needed when using @Controller
    fun getSystemInfo() = serverInfo //cant have .toString() or the attempt of turning the object into JSON via the @ResponseBody is useless -_-

    @GetMapping(value = ["", "/", "/root"]) //https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#web.servlet.spring-mvc.welcome-page
    fun redirect() : String { //having @Controller instead of @RestController allows us to serve files https://stackoverflow.com/q/27381781/9375488
        return "forward:/index.html" //status code = 200
        //return "redirect:/index.html" //status code = 302
    }
}

/*
@Controller
class CustomErrorController : ErrorController {

    @RequestMapping("*")
    fun error(request: HttpServletRequest) {
        throw NotFoundException("The /api doesn't contain this route")
    }
}
*/
