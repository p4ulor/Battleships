package battleship.server.utils

import battleship.server.model.game.*
import java.lang.reflect.Field
import javax.validation.Valid
import javax.validation.constraints.*

/** The request bodies are ordered by CRUD - Create, Read, Update, Delete, ...
 * Most fields that are null or not explicitly indicated in the request bodies and that can be set to an acceptable default, will be auto-default-filled
 * When non-null elements that contain bad values, services/ControllerAdvice will return the proper responses
 *
 *
 * About field Validation / references
 * According to my tests and research, '@field:' in @field:ANNONATION_NAME is only mandatory on data classes
 * https://stackoverflow.com/questions/61992596/spring-boot-valid-on-requestbody-in-controller-method-not-working mas isto nao funcionou segundo uns testes q fiz
 * Alternativa à soluçao de cima mas é preciso empacotar o request body dentro de um campo com o nome desse requestBody https://stackoverflow.com/questions/57325466/not-able-to-validate-request-body-in-spring-boot-with-valid
 * O kotlin não faz inherit de anotações... https://discuss.kotlinlang.org/t/inherited-annotations-and-other-reflections-enchancements/6209             https://discuss.kotlinlang.org/t/implement-inherit-extend-annotation-in-kotlin/8916
 * Creating custom annotations to validate fields:
 * https://www.digitalocean.com/community/tutorials/spring-validation-example-mvc-validator
 * * Other links
 * https://docs.spring.io/spring-framework/docs/4.1.x/spring-framework-reference/html/validation.html
 * https://stackoverflow.com/questions/5142065/valid-annotation-is-not-validating-the-list-of-child-objects
 */

const val nam = "^\\w+(\\s\\w+)*\$"  //Only letters, numbers, only 1 space between words. [a-zA-Z0-9_]
const val name_msg = "A name can only contain alphabet letters, numbers, underscore and not trailing or leading spaces"

const val notEmpty = "^\\s*\\S.*$" //cant be empty or blank
const val msg = "Fields can't be empty or blank"

const val id = "Id's cant be inferior to 0"
const val dir = "Valid values for field 'direction' (not case sensitive) -> UP, LEFT, RIGHT, DOWN" //${Direction.values()} -> doesn't work, must be compile time constant
const val nullObjsInList = "There are null objects in the list/array"
//CREATE (POST's)

class CreateUserRequest( //example using a different way of validating using Spring. Doing it this way, forced us to have to override methods from ResponseEntityExceptionHandler. Believe it or not, it only works by putting 'field:' after the @. I think it's because of kotlin, or when its a data class
    @field:Pattern(regexp = nam, message = name_msg)
    val name: String,
    @field:Pattern(regexp = notEmpty, message = msg)
    val email: String,
    @field:Pattern(regexp = notEmpty, message = msg)
    var password: String //var because this field will be replaced by the encrypted password
) { //THE FOLLOWING ARE NOTES AND ALTERNATIVES TO THE ANNOTATIONS ABOVE
    /*init {
        //Note: throwing an exception here, it will not be redirected to controller advice:
        if(name.isEmpty()) throw BadRequestException("name is empty") //so, it doesn't work!
    }

    //How to compress several checks into 1 field, by calling a method:
    @AssertTrue
    val areFieldsFilled = checkIsFilled(arrayOf("name", "email", "password"), this)==null
    *//* In case it's false, it will return like:
    "title": "MethodArgumentNotValidException",
    "detail": "Error in the field(s): {areFieldsFilled: 'false'}, Rejected because -> must be true;",
     *//*
    */
}

class CreateGameRequest( //must come with Authorization token (to identify the user)
    @field:Pattern(regexp = notEmpty, message = msg)
    val lobbyName: String,
    @field:Valid
    val rules: RulesRequestSegment?
)

class RulesRequestSegment( //Part of CreateGameRequest
    @field:Max(value = MAX_BOARD_SIZE.toLong(), message = "Max board size is $MAX_BOARD_SIZE")
    @field:Min(value = MIN_BOARD_SIZE.toLong(), message = "Min board size is $MIN_BOARD_SIZE")
    @field:NotNull //this and '?' because it seems that missing params that are Ints are automatically set to zero
    val columnDim: Int? = defaultBoardDimension,
    @field:Max(value = MAX_BOARD_SIZE.toLong(), message = "Max board size is $MAX_BOARD_SIZE")
    @field:Min(value = MIN_BOARD_SIZE.toLong(), message = "Min board size is $MIN_BOARD_SIZE")
    @field:NotNull
    val rowDim: Int? = defaultBoardDimension,
    @field:Max(value = MAX_SHOTS_PER_ROUND.toLong(), message = "Max shotsPerRound is $MAX_SHOTS_PER_ROUND")
    @field:Min(value = MIN_SHOTS_PER_ROUND.toLong(), message = "Min shotsPerRound is $MIN_SHOTS_PER_ROUND")
    @field:NotNull
    val shotsPerRound: Int? = defaultShotsPerRound,

    @field:Valid
    //Interestingly, not putting nullable in ShipsTypesAndQuantitySegment will still NOT reject the request body as invalidated when an item is null. This list CAN STILL contain null types! O.O Will use validation function instead (since I found no alternative to checking that it's null (<@NotNull ShipsTypesAndQuantitySegment>) will not work
    val shipsAllowed: List<ShipsTypesAndQuantitySegment>?,

    @field:Max(value = MAX_DURATION_S.toLong(), message = "Max setupTime is $MAX_DURATION_S")
    @field:Min(value = MIN_DURATION_S.toLong(), message = "Min setupTime is $MIN_DURATION_S")
    @field:NotNull
    val setupTime: Int? = defaultDurationS,
    @field:Max(value = MAX_DURATION_S.toLong(), message = "Max setupTime is $MAX_DURATION_S")
    @field:Min(value = MIN_DURATION_S.toLong(), message = "Min setupTime is $MIN_DURATION_S")
    @field:NotNull
    val timeToMakeMove: Int? = defaultDurationS,
    @field:NotNull //cuz spring by defaults puts false...
    val doAllShipTypesNeedToBeInserted: Boolean? = defaultDoAllShipTypesNeedToBeInserted
){
    @AssertTrue(message = nullObjsInList)
    val _areAllShipsTypesAndQuantityNotNull = shipsAllowed?.all {it!=null } ?: true
}

data class ShipsTypesAndQuantitySegment( //Part of CreateGameRequest
    @field:Pattern(regexp = notEmpty, message = msg)
    val shipType: String,
    @field:Max(value = MAX_NUM_OF_SHIPS_W_SAME_TYPE.toLong(), message = "Max quantity allowed for a ShipType is $MAX_NUM_OF_SHIPS_W_SAME_TYPE")
    @field:Min(value = MIN_NUM_OF_SHIPS_W_SAME_TYPE.toLong(), message = "Min quantity allowed for a ShipType is $MIN_NUM_OF_SHIPS_W_SAME_TYPE")
    @field:NotNull
    val quantityAllowed: Int? = defaultQuantityOfEachShipType
)

//READ
data class LoginUserRequest(
    @field:Pattern(regexp = notEmpty, message = msg)
    val emailOrName: String,
    @field:Pattern(regexp = notEmpty, message = msg)
    val password: String
)

//UPDATE (Post Requests)

data class JoinGameRequest( //must come with Authorization token (to identify the user)
    @field:PositiveOrZero(message = id)
    @field:NotNull
    val gameID: Int?,
)

// Should the server auto find the game associated with the user to setup the board?
data class BoardSetupRequest( //must come with Authorization token (to identify the user)
    @field:Valid //this valid is probably recommended to be here. Removing it will transfer the errors from 'MethodArgumentNotValidException' and 'HttpMessageNotReadableException' to the controllers (and services) which will reply with BadRequests and similar error messages (and these can be more detailed)
    val ships: List<ShipRequestSegment>, //pustting @Valid here -> List<@Valid ShipRequestSegment> doesnt work
    @field:NotNull
    val setReady: Boolean? = true //I thought about the client having to do a seperate request to set as ready, but that would require a lot of repetitive code
)

/* Example when BoardSetupRequest is invalid:
  "title": "MethodArgumentNotValidException",
    "detail": "Error in the field(s):
    {ships[0].head.column: 'null'}, Rejected because -> must not be null;
    {ships[0]._isDirectionValid: 'false'}, Rejected because -> Valid values for field 'direction' (not case sensitive) -> UP, LEFT, RIGHT, DOWN;"
 */

data class ShipRequestSegment( //Used for BoardSetupRequest
    @field:Pattern(regexp = notEmpty, message = msg)
    val shipType: String,
    @field:Valid
    val head: PositionRequestSegment,
    @field:Pattern(regexp = notEmpty, message = msg)
    val direction: String
) {
    @AssertTrue(message = dir)
    val _isDirectionValid = Direction.convertToDirection(direction) != null
}

data class PositionRequestSegment( //Used for ShipRequestSegment and FireShotRequest
    @field:Max(value = MAX_BOARD_SIZE.toLong(), message = "Max column is $MAX_BOARD_SIZE")
    @field:Min(value = MIN_COORDINATE.toLong(), message = "Min column is $MIN_COORDINATE")
    @field:NotNull
    var column: Int?,
    @field:Max(value = MAX_BOARD_SIZE.toLong(), message = "Max row is $MAX_BOARD_SIZE")
    @field:Min(value = MIN_COORDINATE.toLong(), message = "Min row is $MIN_COORDINATE")
    @field:NotNull
    var row: Int?
)

data class FireShotRequest( //must come with Authorization token (to identify the user)
    @field:Valid
    val position: PositionRequestSegment
)

data class QuitGameRequest( //must come with Authorization token (to identify the user)
    @field:PositiveOrZero(message = id)
    @field:NotNull
    val gameID: Int?,
)

//DELETE

//////////////// Other things

// Instead of checking all over the place if Strings are null or empty, I made this function
// More compact method to validate request methods. Only applies to Strings to check if they are blank or empty
private fun checkIsFilled(namesOfTheFields: Array<String>, obj: Any) : String? {
    val clss = obj::class.java //igual a: obj::class.java
    var sb = StringBuilder("")
    namesOfTheFields.forEach { name ->
        try {
            val field: Field = clss.getDeclaredField(name) //ele nao retorna null se nao encontra, dá exceçao
            field.isAccessible = true //goes over kotlin.reflect.full.IllegalCallableAccessException . field.trySetAccessible() -> only exists from java 9 forward
            val value = field.get(obj)
            if(value is String){
                if(value.isEmpty() || value.isBlank()){
                    sb.append("The field '${field.name}' is empty or blank in the request body; ")
                }
            }
        } catch(e: NoSuchFieldException){ println(e) }
    }
    println("checkIsFilled returns -> $sb")
    if(sb.isBlank()) return null
    return sb.deleteCharAt(sb.length - 1).toString()
}
