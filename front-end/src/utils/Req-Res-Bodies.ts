import * as Game from '../Components/GameObjs';

// REQUESTS

export class LoginUserRequest {
    emailOrName: string
    password: string
    constructor(emailOrName: string, password: string) {
        this.emailOrName = emailOrName; this.password = password
    }
}

export class CreateUserRequest {
    name: string
    email: string
    password: string
    constructor(name: string, email: string, password: string) {
        this.name = name; this.email = email; this.password = password
    }
}

export class CreateGameRequest {
    lobbyName: string
    rules: RulesRequestSegment
    constructor(lobbyName: string, rules: RulesRequestSegment) {
        this.lobbyName = lobbyName; this.rules = rules
    }
}
export class RulesRequestSegment {
    columnDim: number; rowDim: number; shotsPerRound: number; shipsAllowed: Array<ShipsTypesAndQuantitySegment>
    setupTime: number; timeToMakeMove: number; doAllShipTypesNeedToBeInserted: boolean
    constructor(
        columnDim: number,
        rowDim: number,
        shotsPerRound: number,
        shipsAllowed: Array<ShipsTypesAndQuantitySegment>,
        setupTime: number,
        timeToMakeMove: number,
        doAllShipTypesNeedToBeInserted: boolean
    ) {
        this.columnDim = columnDim; this.rowDim = rowDim; this.shotsPerRound = shotsPerRound
        this.shipsAllowed = shipsAllowed; this.setupTime = setupTime; this.timeToMakeMove = timeToMakeMove
        this.doAllShipTypesNeedToBeInserted = doAllShipTypesNeedToBeInserted
    }
}
export class ShipsTypesAndQuantitySegment {
    shipType: string
    quantityAllowed: number
    constructor(shipType: string, quantityAllowed: number) {
        this.shipType = shipType; this.quantityAllowed = quantityAllowed
    }
}

export class JoinGameRequest {
    gameID: number
    constructor(gameID: number) {
        this.gameID = gameID
    }
}

export class BoardSetupRequest {
    ships: Array<ShipRequestSegment>
    constructor(ships: Array<ShipRequestSegment>) {
        this.ships = ships
    }
}

export class ShipRequestSegment { //Used for BoardSetupRequest
    shipType: String
    head: PositionRequestSegment
    direction: String
    constructor(shipType: String, head: PositionRequestSegment, direction: String) {
        this.shipType = shipType; this.head = head; this.direction = direction
    }
}

export class PositionRequestSegment {
    column: number
    row: number
    constructor(column: number, row: number) {
        this.column = column; this.row = row
    }
}

export class FireShotRequest { //must come with Authorization token (to identify the user)
    position: PositionRequestSegment | Game.Position
    constructor(position: PositionRequestSegment | Game.Position) {
        this.position = position
    }
}

// RESPONSES

//the 'token' is stored as HttpOnly cookie and 'userID' as cookie

export class GamesArrayResponse {
    gameID: number
    lobbyName: string
    hostID: number
    hostName: string
}

export class UserProfileResponse {
    id: number
    name: string
    playCount: number
    winCount: number
}

export class GameCreatedResponse {
    gameID: number
    rules: Game.Rules
    constructor(gameID: number = undefined, rules: Game.Rules = undefined) { //fields are undefined so that responseToObj() works when passing this class to it. Alternative is to not have a constructor at all
        this.gameID = gameID; this.rules = rules
    }
}

export type GameData = GameCreatedResponse //just for lexic purposes

export class PlayersMatchedResponse { //the response a guest gets when joining a game
    opponentName: String
    rules: Game.Rules
}

export class GetGameInformation {
    opponentName?: string
    gameStatus: Game.GameStatus
    stateOfMyShips: Array<Ship>
    latestShotOfMyOpponent?: Game.Shot
    currentTimer: number
    myShots: Array<Game.Shot>
}

export class Ship {
    shipType: Game.ShipTypes
    head: Position
    direction: Game.Direction
    positionsOccupying: Array<Position>
}

export class Position {
    column: number
    row: number
    entity: Game.EntityTypes
}

export class ShotResultResponse {
    position: PositionRequestSegment
    shotResult: Game.ShotResult
}

const responseToObjErrorMsg = "The response body doesn't have all of the expected fields. Fix either the front-end or back-end"

export function responseToObj<T>(responseClass: new () => T, jsonResponse: any): T /* | T[] */ { //https://www.typescriptlang.org/docs/handbook/2/generics.html#using-class-types-in-generics https://stackoverflow.com/a/17383858/9375488

    /*The following code would be in the case where we would like to return an array, but I IMO I rather leave the parsing to the caller, he'll know if he wants to parse it to an array or not
    And per example, by not doing it like this, a json array being passed to Object.assign() would produce something like -> {"0":{"gameID":2,"lobbyName":"paulo lobby"}, "1": {...}}
    And I would have to have the type T[] in the return type*/
    const responseObj = Object.assign(new responseClass(), jsonResponse) //https://www.geeksforgeeks.org/how-to-cast-a-json-object-inside-of-typescript-class/
    if (Array.isArray(jsonResponse)) {
        jsonResponse.forEach((item, index) => {
            if (!areFieldsFilled(item)) {
                throw new Error(`Array response at index=${index}, item=${item} is not a valid response body`)
            }
        })
    }
    else if (!areFieldsFilled(responseObj)) throw new Error(responseToObjErrorMsg)
    return responseObj
}

export function convertEnumerableObjToArrayOfBodies<T>(enumerableObj: any): Array<T> {
    const arrayOfTheObjs: Array<T> = []

    Object.keys(enumerableObj).map(key => { //why is Object.keys() returning nested keys?!
        //console.log(`key -> ${key}`)
        if (!Number.isNaN(Number(key))) arrayOfTheObjs.push(enumerableObj[key]) //https://stackoverflow.com/a/42121162/9375488
    })
    return arrayOfTheObjs
}

function areFieldsFilled(obj: any) {
    return Object.keys(obj).every(key => {
        if (obj[key] !== undefined) return true //!== will allow nullable properties in the object
        else {
            console.log(`The field ${key} is undefined!`)
            return false
        }
    })
}
