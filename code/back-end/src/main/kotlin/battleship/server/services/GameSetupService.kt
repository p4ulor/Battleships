package battleship.server.services

import battleship.server.dataIsInMemory
import battleship.server.model.*
import battleship.server.storage.GameData
import battleship.server.storage.UserData
import battleship.server.storage.db.GamePostgres
import battleship.server.storage.db.UserPostgres
import battleship.server.storage.mem.GameMem
import battleship.server.storage.mem.UserMem
import battleship.server.utils.*
import org.jdbi.v3.core.Jdbi
import org.springframework.stereotype.Component
import java.util.concurrent.*
import javax.annotation.PreDestroy


/* The alternative (to my if else in the constructor) would be using spring injection would be to have something like:
GameSetupService(
gameMem: GameMem
userMem: UserMem
gamePostgres: GamePostgres
userPostgres: UserPostgres)

val gameData: GameData = if(dataIsInMemory) gameMem else gamePostgres,
val userData: UserData = if(dataIsInMemory) userMem else userPostgres
//And having the Mem and Postgres classes have @Component.
//But the thing here is that the data source can only be 1 or the other, so spring would always instantiate 2 classes that wouldn't be used... And its less 4 lines this way
 */

@Component
class GameSetupService (
    jdbi: Jdbi,
    val gameData: GameData = if(dataIsInMemory) GameMem() else GamePostgres(jdbi),
    val userData: UserData = if(dataIsInMemory) UserMem() else UserPostgres(jdbi),
    val readinessService: ReadinessService
) {
    private val setupTimeReferee: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor() //https://www.techiedelight.com/execute-a-function-after-initial-delay-or-periodically-in-kotlin/

    @PreDestroy
    private fun preDestroy() {
        pl("shutting down GameSetupService")
        setupTimeReferee.shutdown()
    }

    fun createGame(token: String, cgr: CreateGameRequest) : RequestResult {
        val hostID = userData.authenticateUser(token) ?: return RequestResult(error = Errors.InvalidTokenNotFound)
        if(getOnGoingGameOfUser(hostID, gameData)!=null){
            pl("User can't create game")
            return RequestResult(error = Errors.PlayerIsInAnotherGame)
        }
        pl("User can create game")
        var rules: Rules
        if(cgr.rules==null) rules = Rules() //default rules
        else { //todo review this...
            val dim = try {
                Dimension(cgr.rules.columnDim, cgr.rules.rowDim)
            } catch (e: IllegalArgumentException){ return RequestResult(error = Errors.BoardDimensionInvalid) }

            val shipTypesAllowed = cgr.rules?.shipsAllowed?.map{
                val type = ShipType.convertToShipType(it.shipType) ?: return RequestResult("Ship type '${it.shipType}' doesn't exist", Errors.ShipTypeDoesntExist)
                try {ShipsTypesAndQuantity(type, it.quantityAllowed) } catch(e: IllegalArgumentException){ return RequestResult(error = Errors.ShipTypeQuantityExceeded) }
            }
            pl("ships allowed obtained from body -> $shipTypesAllowed")
            try {
                rules = Rules(dim,
                              cgr.rules?.shotsPerRound,
                              shipTypesAllowed?.toMutableList(),
                              cgr.rules.setupTime, cgr.rules.timeToMakeMove, cgr.rules.doAllShipTypesNeedToBeInserted)
            } catch (e: Exception){ return RequestResult("${e.message}", Errors.InvalidRules) }
        }

        return acessStorage {
            val gameID = gameData.createGame(cgr.lobbyName, hostID, rules)
            RequestResult(GameCreatedResponse(gameID, rules))
        }
    }

    fun getOpenGames(paging: Paging) : RequestResult {
        return acessStorage {
            val openGames = gameData.getOpenGames(paging).map { GamesListResponse(it.id, it.lobbyName, it.hostID, userData.getPlayer(it.hostID)?.name ?: "null") }
            RequestResult(openGames)
        }
    }

    fun joinGame(token: String, jgr: JoinGameRequest) : RequestResult { //todo, ver se dá para simplificar e evitar consultas repetitivas
        val guestID = userData.authenticateUser(token) ?: return RequestResult(error = Errors.InvalidTokenNotFound)
        val game = gameData.getGame(jgr.gameID!!) ?: return RequestResult(error = Errors.GameDoesntExist)
        if(getOnGoingGameOfUser(guestID, gameData)!=null) return RequestResult(error = Errors.PlayerIsInAnotherGame)
        if(!game.enrollGuest(guestID)) return RequestResult(error = Errors.YouCantJoinThisGame)

        gameData.joinGame(guestID, jgr.gameID, game.setupTime)
        val name = userData.getPlayer(game.hostID)?.name

        setupTimeReferee.schedule({ //runs 1 time after setupTime has expired. Each joinGame() will be respective to 1 game, so it will work
            pl("Setup time referee started for gameID=${game.id}")
            val game = gameData.getGame(game.id)!!
            if(!game.gameStatus.isAFinaleState()){
                pl("Game status=${game.gameStatus}")
                if(!game.gameStatus.isGameOnGoing()) {
                    gameData.setGameStatus(game.id, GameStatus.ABORTED)
                    pl("Game aborted for one or both users taking too long to setup board")
                }
                else pl("Setup time referee of gameID=${game.id} has not aborted the game since both users submitted their boards in time")
            } else pl("Setup time referee no longer in effect, the game has been finished")
        }, game.rules.setupTimeS.toLong(), TimeUnit.SECONDS)

        return RequestResult(PlayersMatchedResponse(name.toString(), game.rules))
    }

    fun boardSetup(token: String, bsr: BoardSetupRequest) : RequestResult { //todo, ver se dá para simplificar e evitar consultas repetitivas
        val userID = userData.authenticateUser(token) ?: return RequestResult(error = Errors.InvalidTokenNotFound)
        val game = getOnGoingGameOfUser(userID, gameData) ?: return RequestResult(error = Errors.YouAreNotPartOfAnyOnGoingGame)
        if(game!=null){
            if(game.gameStatus==GameStatus.WAITING_FOR_GUEST) return RequestResult(error = Errors.YouCantSubmitYourBoardYet)
            if(game.gameStatus!=GameStatus.SHIPS_SETUP) //if true, then it may be: HOST_TURN or GUEST_TURN
                return RequestResult(error = Errors.YouCaNoLongerChangeYourBoard)
        }
        if(game.hasSetupTimeEnded()) return RequestResult(error = Errors.BoardSetupTimeHasEnded)
        val isThisUserHost = game.isHost(userID)
        if(isThisUserHost && game.isHostReady) return RequestResult("Host, you already said you were ready", error = Errors.YouCaNoLongerChangeYourBoard)
        else if(!isThisUserHost /*required!*/ && game.isGuestReady) return RequestResult("Guest, you already said you were ready", error = Errors.YouCaNoLongerChangeYourBoard)

        //convert the list of ShipRequestSegment to Ships
        val ships = mutableListOf<Ship>()
        bsr.ships.forEachIndexed { idx, it ->
            val type = ShipType.convertToShipType(it.shipType) ?:                        return RequestResult("Ship at index $idx has an non-existing shiptype -> ${it.shipType}", Errors.InvalidShip)
            val head = Position.newPosition(it.head.column, it.head.row, Entity.SHIP) ?: return RequestResult("Ship at index $idx has an invalid position -> [${it.head.column}|${it.head.row}]", Errors.InvalidShip)
            val dir = Direction.convertToDirection(it.direction) ?:                      return RequestResult("Ship at index $idx has an invalid direction -> ${it.direction}", Errors.InvalidShip)
            ships.add(Ship(type, head, dir))
        }

        val shipsTypesAndQuantity = ships.map { ship ->
            ShipsTypesAndQuantity(ship.shipType, ships.filter { it.shipType==ship.shipType }.size)
        }

        //check the types allowed according to the game-rules that were set and if the size of the type of that ship is greater than the board
        val rejectedShip = game.rules.areTheseShipsSizeAndTypeAllowed(shipsTypesAndQuantity)
        if(rejectedShip.first!=null) return RequestResult("ShipType '${rejectedShip.second?.name}' isn't allowed in this game. Reason -> ${rejectedShip.first?.msg}. ShipTypes allowed -> ${game.rules.shipsAllowed}", Errors.ShipTypeNotAllowed)
        /* Example when this upper RequestResult returns
            "ShipType not allowed. ShipType 'Destroyer' isn't allowed in this game. Reason ->
            The ship type is not allowedShipTypes allowed ->
            [ShipsTypesAndQuantity(shipType=Cruiser, quantityAllowed=1), ShipsTypesAndQuantity(shipType=Submarine, quantityAllowed=1)]"
         */

        /*build ships. The errors that can come from here are:
          the head of the ship, along with the direction and length it extends to, surpasses the board
          or the latest ship inserted is incompatible with the positions of the other ships
        */
        val setUserReady = bsr.setReady ?: true
        val isUserHost = game.isHost(userID)
        val shipsOfTheUser = if(isUserHost) game.hostShips else game.guestShips //in case he added other ships before and wanted to add more
        shipsOfTheUser.addAll(ships)
        val resultTriple = game.addShips(shipsOfTheUser, userID, setUserReady)
        if(resultTriple.first.isNotEmpty()) return RequestResult(resultTriple.first, Errors.ShipNotCompatible)

        if(game.gameStatus.isGameOnGoing()){
            val scheduler = Executors.newSingleThreadScheduledExecutor()
            val scheduledFuture = scheduler.scheduleAtFixedRate({ //runs in loop
                pl("Round Scheduler running for gameID=${game.id}")
                val game = gameData.getGame(game.id)!!
                pl("is game ongoing ->${game.gameStatus.isGameOnGoing()}")
                if(!game.gameStatus.isGameOnGoing()) {
                    scheduler.shutdown()
                    pl("Scheduler shutdown for gameID=${game.id}, since its finished with state ${game.gameStatus}")
                } else {
                    val ended = game.hasRoundTimeEnded()
                    pl("Scheduler asks: has round time ended? $ended")
                    if(ended) {
                        val winner = GameStatus.getWinForOpponent(game.gameStatus)
                        pl("Game id=${game.id} curr player has FAILED to complied by the round time rules, winner will be $winner")
                        gameData.setGameStatus(game.id!!, winner)
                        scheduler.shutdown()
                    } else pl("Game id=${game.id} curr player has complied by the round time rules")
                }
            }, game.rules.roundTimeS.toLong(), game.rules.roundTimeS.toLong(), TimeUnit.SECONDS)
        }

        val isUserReady = if(isUserHost) game.isHostReady else game.isGuestReady
        return acessStorage {
            gameData.setupBoard(game.id, isUserHost, shipsOfTheUser, isUserReady, game.gameStatus, game.setupTime, game.roundTime) //game status provided because if both users are ready it will be host's turn
            val shipsThatCanBeAdded = ShipsTypesAndQuantity.getAllMissingShipTypesAndQuantityInShipList(shipsOfTheUser, game.rules.shipsAllowed)
            val canAddMore = if(setUserReady) false else resultTriple.second
            readinessService.saytoOpponentMyReadiness(userID, game.id, true)
            RequestResult(BoardBuiltResponse(canAddMore, resultTriple.third, shipsThatCanBeAdded))
        }
    }

    fun deleteLobby(token: String) : RequestResult {
        val userID = userData.authenticateUser(token) ?: return RequestResult(error = Errors.InvalidTokenNotFound)
        val game = gameData.findGameWithUser(userID, listOf(GameStatus.WAITING_FOR_GUEST)) ?: return RequestResult(error = Errors.LobbyNotFound)
        if(game.hostID!=userID) return return RequestResult(error = Errors.YouCantDeleteThisLobby)
        return acessStorage {
            gameData.deleteLobby(game.id)
            RequestResult()
        }
    }

    fun newListener(token: String): RequestResult {
        val userID = userData.authenticateUser(token) ?: return RequestResult(error = Errors.InvalidTokenNotFound)
        val game = gameData.findGameWithUser(userID, mutableListOf(GameStatus.SHIPS_SETUP)) ?: return RequestResult(error = Errors.YouAreNotInTheShipsSetupPhase)
        val isThisUserTheHost = userID==game.hostID
        val opponentID = (if(isThisUserTheHost) game.hostID else game.guestID) ?: return RequestResult("opponentID is null",Errors.InvalidState)
        return RequestResult(readinessService.newListener(game.id, userID, opponentID))
    }
}
