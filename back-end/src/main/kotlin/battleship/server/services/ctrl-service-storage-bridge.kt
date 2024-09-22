package battleship.server.services

import battleship.server.model.game.MAX_BOARD_SIZE
import battleship.server.model.game.MAX_DURATION_S
import battleship.server.model.game.MIN_BOARD_SIZE
import battleship.server.model.game.MIN_DURATION_S
import battleship.server.utils.*
import org.springframework.web.server.ResponseStatusException
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

/* Utility method for easier communication with controllers
 Used when the call to services has chances to returns various types of objects from enum Status
 Doing it like this will avoid writing a bunch of manual if's in the controllers to check what Status was returned from services!!!!!!
 services returned. Since an enum Status has a HTTP exception associated to it, we just access it and call it
 */
internal inline fun <reified T> processResult(res: RequestResult, onSuccess: () -> Any = { -> true }) : T { //https://stackoverflow.com/a/72118752/9375488
    if(res.error==null){
        onSuccess()
        if(res.data!=null) return res.data as T
        else return "" as T
    }
    else {
        val errorMessage = StringBuilder(res.error.msg)
        if(res.data!=null) errorMessage.append(". "+res.data.toString()) //DO NOT REMOVE .toString() bellow this line!
        throw res.error.exception.primaryConstructor?.call(errorMessage.toString())!! //calls my HTTP Exceptions
    }
}

//Utility method for services which automatically handles storage exceptions into proper responses
fun acessStorage (action: () -> RequestResult) : RequestResult {
    return try { action() }
    catch (se: StorageException){ RequestResult(se.exceptionMsg , se.apparentCause) }
}

//Controller - Services - Data Bridge
data class RequestResult(
    val data: Any? = null, //valid data. If errorStatus is not null, this field can have extra info about the error which must be a string
    val error: Errors? = null //being null implies there's no error
) {
    init {
        if(error!=null && data!=null){
            if(data !is String) throw IllegalStateException("A RequestResult must have data of type string when there's an errorStatus")
        }
    }
}

enum class Errors(val msg: String, val exception: KClass<out ResponseStatusException>){
    StorageError("Error in storage", NotFoundException::class), //General storage error, it's more likely a not found than a bad request

    //Users
    UserNotFound("User Not Found", NotFoundException::class),
    EmailInUse("Email is already in use", ConflictException::class),
    EmailBadFormat("Email is badly formatted", BadRequestException::class),
    UserNameInUse("User name already in use", ConflictException::class),
    WrongPassword("Wrong Password", ForbiddenException::class),
    InvalidTokenNotFound("Invalid authorization token. It wasn't found in the DB", NotFoundException::class),

    //Games
    PlayerIsInAnotherGame("Player Is In Another Ongoing Game", ForbiddenException::class),
    YouCantJoinThisGame("You Cant Join This Game, there's already a guest", ForbiddenException::class), //because it already has 2 players
    YouAreNotPartOfAnyOnGoingGame("You Are Not Part Of Any On Going Game", ForbiddenException::class),
    YouAreNotPartOfThisGame("You Are Not Part Of This Game", ForbiddenException::class),
    LobbyNotFound("Lobby not found", NotFoundException::class),
    YouCantDeleteThisLobby("Only the host can delete this lobby", ForbiddenException::class),
    YouAreNotInTheShipsSetupPhase("You are not in a ships setup phase to consult if the apponent is ready from setting up his ships", ForbiddenException::class),
    GameDoesntExist("Game doesn't exist", NotFoundException::class),
    NoValidStateToQuit("No Valid State To Quit", ForbiddenException::class),
    YouCaNoLongerChangeYourBoard("You Can No Longer Change Your Board.", ForbiddenException::class),
    BoardDimensionInvalid("The min and max of duration are [$MIN_BOARD_SIZE, $MAX_BOARD_SIZE]", BadRequestException::class),
    BadDuration("The min and max of duration are [$MIN_DURATION_S, $MAX_DURATION_S]", BadRequestException::class),
    ThisGameAlreadyEnded("This game has been finished. No further action is allowable", ForbiddenException::class),
    YouCantSubmitYourBoardYet("You Cant Submit Your Board Yet. An opponent is required to start the phase of board setup", ForbiddenException::class),
    BoardSetupTimeHasEnded("The board setup time has ended, game aborted", ForbiddenException::class),
    YouCantConsultThisGameYet("You Cant Consult This Game Yet", AuthorizationException::class),

    //Ships
    InvalidShip("One or more ships have invalid properties", BadRequestException::class),
    ShipTypeNotAllowed("ShipType not compatible with the rules", ForbiddenException::class),
    ShipTypeDoesntExist("ShipType doesn't exist", BadRequestException::class),
    ShipTypeQuantityExceeded("The amount of ships associated to a type was exceeded", ForbiddenException::class),
    ShipNotCompatible("The properties of the ship makes it incompatible with the rules", BadRequestException::class),

    //Shots
    InvalidShot("Invalid shot position", BadRequestException::class),
    ItsNotUsersTurnToShoot("Its Not Users Turn To Shoot. Or you can't shoot yet", ForbiddenException::class),
    RoundTimeAsEnded("You surpassed the time to shoot your shot, you have lost", ForbiddenException::class),

    InvalidState("Something went wrong regarding the logic of storing state", InternalServerErrorException::class),
    InvalidRules("Invalid rules", BadRequestException::class),
}
