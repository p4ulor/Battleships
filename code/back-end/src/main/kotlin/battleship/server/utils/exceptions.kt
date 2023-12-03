package battleship.server.utils

import battleship.server.services.Status
import org.springframework.beans.ConversionNotSupportedException
import org.springframework.beans.TypeMismatchException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindException
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingPathVariableException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.ServletRequestBindingException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.multipart.support.MissingServletRequestPartException
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import javax.servlet.http.HttpServletRequest


class StorageException(val apparentCause: Status, val exceptionMsg: String = "Storage Exception") : Exception(exceptionMsg)

// HTTP Exceptions
class NotFoundException(val msg: String, val path: String? = null) : ResponseStatusException(HttpStatus.NOT_FOUND, msg)
class BadRequestException(val msg: String?) : ResponseStatusException(HttpStatus.BAD_REQUEST, msg) //"the server cannot or will not process the request due to something that is perceived to be a client error"
class AuthorizationException(val msg: String?) : ResponseStatusException(HttpStatus.UNAUTHORIZED, msg) //"you are not authorized because you don't have the right authentication"
class ForbiddenException(val msg: String?) : ResponseStatusException(HttpStatus.FORBIDDEN, msg) //"you are not authorized regardless of authentication"
class ConflictException(val msg: String?) : ResponseStatusException(HttpStatus.CONFLICT, msg) //Hmmm... https://stackoverflow.com/a/9270432 e https://httpwg.org/specs/rfc9110.html#status.409
class InternalServerErrorException(val msg: String? = HttpStatus.INTERNAL_SERVER_ERROR.toString()) : ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, msg)

// just experimenting with it--------
/*@Configuration
class WebMvcConfiguration : WebMvcConfigurer { //just a test. See InfoController.redirect() too
    override fun addViewControllers(registry: ViewControllerRegistry) {
        registry.addViewController("/main").setViewName("redirect:/index.html")
    }
}*/

// just experimenting with it--------
//Works with application.properties -> server.error.path=/ups
//When active, it will overwrite spring's default handling of errors to error/404.html, error/505.html files, and will use the handling of this function instead
/*@Controller //Just a test. alternative to doing this is having folder named 'error' in the static folder and files named to each error status code https://stackoverflow.com/questions/37398385/spring-boot-and-custom-404-error-page
class MyErrorController : ErrorController { //https://www.baeldung.com/spring-boot-custom-error-page
    @RequestMapping("/ups")
    fun handleError(request: HttpServletRequest) : String {
        val status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)
        if (status != null) {
            val statusCode = Integer.valueOf(status.toString())
            if (statusCode == HttpStatus.NOT_FOUND.value()) return "/error/error-404.html"
        }
        return "/error/error-500.html"
    }
}*/

const val requestError = "#Request-Error"
const val bodyError = "#Body-Error"

@ControllerAdvice //Handles exceptions thrown in all Controllers
class ResponseExceptionHandler : ResponseEntityExceptionHandler() { //https://docs.spring.io/spring-framework/docs/3.2.x/spring-framework-reference/html/mvc.html#mvc-ann-rest-spring-mvc-exceptions

    //@ExceptionHandler(Exception::class)
    //fun handleAll(): ResponseEntity<Unit> = ResponseEntity.status(500).build() //This just makes it so when there's an InternalServerError it returns the status code, but the server doesn't crash. And crashing allows us to see the message

    @ExceptionHandler(NotFoundException::class)                                                //req.requestURL gets entire path. req.requestURI gets whatever is after the hostname. req.remoteAddr returns the IP, if you send request from postman you get "0:0:0:0:0:0:0:1"
    fun nf(nf: NotFoundException, req: HttpServletRequest) = respond(nf, ProblemJsonModel(requestError, nf.status.toString(), nf.msg, nf.path ?: req.requestURI))

    @ExceptionHandler(BadRequestException::class)
    fun br(br: BadRequestException, req: HttpServletRequest) = respond(br, ProblemJsonModel(requestError, br.status.toString(), br.msg, req.requestURI))

    @ExceptionHandler(AuthorizationException::class)
    fun ae(ae: AuthorizationException, req: HttpServletRequest) = respond(ae, ProblemJsonModel(requestError, ae.status.toString(), ae.msg, req.requestURI))

    @ExceptionHandler(value = [ForbiddenException::class] )
    fun f(f: ForbiddenException, req: HttpServletRequest) = respond(f, ProblemJsonModel(requestError, f.status.toString(), f.msg, req.requestURI))

    @ExceptionHandler(value = [InternalServerErrorException::class])
    fun se(se: InternalServerErrorException, req: HttpServletRequest) = respond(se, ProblemJsonModel(requestError, se.status.toString(), se.msg, req.requestURI))

    @ExceptionHandler(value = [ConflictException::class])
    fun cf(cf: ConflictException, req: HttpServletRequest) = respond(cf, ProblemJsonModel(requestError, cf.status.toString(), cf.msg, req.requestURI))

    /*@ExceptionHandler(value = [InvalidFormatException::class]) // 'I have no memory of this place...'
    fun inf(inf: InvalidFormatException) = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(ProblemJsonModel.MEDIA_TYPE).body(inf.message)*/

    //The methods bellow handle badly formatted request bodies or URI's:

    override //When there are type mismatches in request bodies or invalid params or paths in URI's. Example: http://localhost:9000/setup/opengames?limit=a
    fun handleTypeMismatch(ex: TypeMismatchException, h: HttpHeaders, s: HttpStatus, r: WebRequest) : ResponseEntity<Any> {
        val br = BadRequestException("Required type=${ex.requiredType}. Error code=${ex.errorCode}")
        return respond(br, ProblemJsonModel(bodyError, title(ex), br.msg, (r as ServletWebRequest).request.requestURI)) //https://stackoverflow.com/a/51960592/9375488
    }

    override //metodo que é executado quando as anotações de validação e @Valid falham
    fun handleMethodArgumentNotValid(ex: MethodArgumentNotValidException, h: HttpHeaders, s: HttpStatus, r: WebRequest) : ResponseEntity<Any> {
        val sb = StringBuilder("Error in the field(s): ")
        ex.fieldErrors.forEach {
            sb.append("{${it.field}: '${it.rejectedValue}'}, Rejected because -> ${it.defaultMessage}; ")
        }
        sb.deleteCharAt(sb.length-1)
        val br = BadRequestException(sb.toString())
        return respond(br, ProblemJsonModel(bodyError, title(ex), br.msg, (r as ServletWebRequest).request.requestURI))
    }

    override //The one for the missing parameters
    fun handleHttpMessageNotReadable(ex: HttpMessageNotReadableException, h: HttpHeaders, s: HttpStatus, r: WebRequest): ResponseEntity<Any> {
        val br = BadRequestException("Missing or invalid param, Rejected because -> ${cutNotReadableMsg(ex.message.toString())}")
        return respond(br, ProblemJsonModel(bodyError, title(ex), br.msg, (r as ServletWebRequest).request.requestURI))
    }

    override //called when you make a request using POST to a path that should be called with a GET per example
    fun handleHttpRequestMethodNotSupported(ex: HttpRequestMethodNotSupportedException, h: HttpHeaders, s: HttpStatus, r: WebRequest) : ResponseEntity<Any> {
        val br = BadRequestException("Error=${ex}")
        return respond(br, ProblemJsonModel(bodyError, title(ex), br.msg, (r as ServletWebRequest).request.requestURI))
    }

    override
    fun handleNoHandlerFoundException(ex: NoHandlerFoundException, h: HttpHeaders, s: HttpStatus, r: WebRequest) : ResponseEntity<Any> {
        val br = BadRequestException("Error=${ex}")
        return respond(br, ProblemJsonModel(bodyError, title(ex), br.msg, (r as ServletWebRequest).request.requestURI))
    }

    override
    fun handleServletRequestBindingException(ex: ServletRequestBindingException, h: HttpHeaders, s: HttpStatus, r: WebRequest): ResponseEntity<Any> {
        val br = BadRequestException("Error=${ex}")
        return respond(br, ProblemJsonModel(bodyError, title(ex), br.msg, (r as ServletWebRequest).request.requestURI))
    }

    override
    fun handleBindException(ex: BindException, h: HttpHeaders, s: HttpStatus, r: WebRequest): ResponseEntity<Any> {
        val br = BadRequestException("Error=${ex}")
        return respond(br, ProblemJsonModel(bodyError, title(ex), br.msg, (r as ServletWebRequest).request.requestURI))
    }

    override
    fun handleMissingPathVariable(ex: MissingPathVariableException, h: HttpHeaders, s: HttpStatus, r: WebRequest) : ResponseEntity<Any> {
        val br = BadRequestException("Error=${ex}")
        return respond(br, ProblemJsonModel(bodyError, title(ex), br.msg, (r as ServletWebRequest).request.requestURI))
    }

    override
    fun handleMissingServletRequestPart(ex: MissingServletRequestPartException, h: HttpHeaders, s: HttpStatus, r: WebRequest): ResponseEntity<Any> {
        val br = BadRequestException("Missing part=${ex.requestPartName}. Message=${ex.message}")
        return respond(br, ProblemJsonModel(bodyError, title(ex), br.msg, (r as ServletWebRequest).request.requestURI))
    }

    override
    fun handleMissingServletRequestParameter(ex: MissingServletRequestParameterException, h: HttpHeaders, s: HttpStatus, r: WebRequest): ResponseEntity<Any> {
        val br = BadRequestException("Missing param=${ex.parameterName} of type ${ex.parameterType}")
        return respond(br, ProblemJsonModel(bodyError, title(ex), br.msg, (r as ServletWebRequest).request.requestURI))
    }

    override
    fun handleConversionNotSupported(ex: ConversionNotSupportedException, h: HttpHeaders, s: HttpStatus, r: WebRequest): ResponseEntity<Any> {
        val br = BadRequestException("Message=${ex.value} is not of type ${ex.requiredType}")
        return respond(br, ProblemJsonModel(bodyError, title(ex), br.msg, (r as ServletWebRequest).request.requestURI))
    }

    override
    fun handleHttpMediaTypeNotSupported(ex: HttpMediaTypeNotSupportedException, h: HttpHeaders, s: HttpStatus, r: WebRequest): ResponseEntity<Any> {
        val br = BadRequestException("Message=${ex.message}")
        return respond(br, ProblemJsonModel(bodyError, title(ex), br.msg, (r as ServletWebRequest).request.requestURI))
    }

    override fun handleHttpMediaTypeNotAcceptable(ex: HttpMediaTypeNotAcceptableException, h: HttpHeaders, s: HttpStatus, r: WebRequest): ResponseEntity<Any> {
        val br = BadRequestException("Message=${ex.message}")
        return respond(br, ProblemJsonModel(bodyError, title(ex), br.msg, (r as ServletWebRequest).request.requestURI))
    }

    override
    fun handleExceptionInternal(ex: java.lang.Exception, body: Any?, h: HttpHeaders, s: HttpStatus, r: WebRequest): ResponseEntity<Any> {
        val br = BadRequestException("Message=${ex.message}")
        return respond(br, ProblemJsonModel(bodyError, title(ex), br.msg, (r as ServletWebRequest).request.requestURI))
    }
}

private fun title(ex: Any) = "Bad Request: ${ex.javaClass.simpleName}"

/* Aux method to reduce repetitive code like:
    @ExceptionHandler(NotFoundException::class)
    fun nf(nf: NotFoundException) = ResponseEntity.status(nf.status).contentType(ProblemJsonModel.MEDIA_TYPE)
        .body(ProblemJsonModel("type", nf.status.name, nf.msg)) //nf.message e nf.status.localizedMessage retorna = "detail": "404 NOT_FOUND \"User not found\"". Ou seja o q tá em \"   \" é nf.msg
*/ //👇
private fun respond(e: ResponseStatusException, body: ProblemJsonModel) : ResponseEntity<Any> =
    ResponseEntity.status(e.status).contentType(ProblemJsonModel.MEDIA_TYPE).body(body)

private fun cutNotReadableMsg(m: String) : String {
    val s = StringBuilder(m.dropWhile { it!=']' })
    val finalCut = s.indexOfFirst { it == ';'}
    s.replace(0, s.length, s.substring(2, finalCut)) //2 -> to skip "] "

    //adds '   ' around the name of the field
    val paramIndex = s.indexOf("parameter")
    s.replace(paramIndex, paramIndex+10, "parameter '")
    val whichIndex = s.indexOf("which")
    s.setCharAt(whichIndex-1, '\'')
    val final = StringBuilder(s.substring(0, whichIndex)).append(" ").append(s.substring(whichIndex, s.length))

    return final.toString()
}

class ProblemJsonModel( //https://labs.pedrofelix.org/notes/http/how-to-fail
    linkAnchor : String, //should be instatialed like: #NotFound
    val title : String? = null,
    val detail: String? = null,
    val API_path: String? = null, //AKA instance AKA URI, like /users/login
    val objectsInvolved: List<Any>? = null
){  val type = StringBuilder("https://github.com/isel-leic-daw/2022-daw-leic52d-g05-1/tree/main/docs/problems.md") //AKA URL. It's mostly for the programmer that is using the API to consult. Not for processing the contents of the page... https://www.mnot.net/blog/2013/05/15/http_problem
    init {
        require(linkAnchor[0]=='#') { "An link anchor should start with '#'"}
        type.append(linkAnchor)
    }
    companion object { val MEDIA_TYPE = MediaType.parseMediaType("application/problem+json") }
}
