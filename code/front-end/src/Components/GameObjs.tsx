import * as SetupShips from '../Pages/SetupShips'
import * as sfx from '../utils/SFXManager'

// Most of the logic in this file is inspired from the back-end model

export type EntityTypes = "WATER" | "SHIP" | "DAMAGED"

export class Entity { //https://stackoverflow.com/questions/41179474/use-object-literal-as-typescript-enum-values
    static readonly WATER = new Entity('WATER', sfx.localFire)
    static readonly SHIP = new Entity('SHIP')
    static readonly DAMAGED = new Entity('DAMAGED', sfx.distantMiss)

    // private to disallow creating other instances of this type
    private constructor(public readonly name: EntityTypes, public readonly sfx?: Function) { }

    static areEqual(entity1: Entity, entity2: Entity) {
        return entity1.name == entity2.name
    }
}

export class Position {
    column: number
    row: number
    entity: Entity
    constructor(column: number, row: number, entity: Entity) {
        if (column < 1 || row < 1) throw new RangeError("Column and row must be greater than 0")
        this.column = column; this.row = row; this.entity = entity
    }

    toString() {
        return `Column: ${this.column} | Row: ${this.row}`
    }

    static getAllPositionsAroundAPositionTowardsADirection(pos: Position, dir: Direction, range: number): Array<Position> { //the 'range' is the ships lenght
        const occupyingPositions = Array<Position>()
        var initialPos = pos //this position is the head of the ship
        try { initialPos = pos.moveTowards(Direction.getOpposite(dir)) } catch (e) { } //will go to 1 position towards the opposite direction given the current position if possible
        for (let i = 0; i < range + 2; i++) { //+2 cuz it Will go towards the provided direction extra 1, plus 1 position to the side of the head
            try {
                const sides = Direction.getSides(dir) //will extract the position it's at, the 2 positions on its sides
                try {
                    const posSide1 = initialPos.moveTowards(sides.first)
                    occupyingPositions.push(posSide1)
                } catch (e) { }
                try {
                    const posSide2 = initialPos.moveTowards(sides.second)
                    occupyingPositions.push(posSide2)
                } catch (e) { }
                occupyingPositions.push(initialPos)
                initialPos = initialPos.moveTowards(dir)
            } catch (e) { }
        }

        /* console.log("Positions occupied ->"+JSON.stringify(occupyingPositions)) */
        return occupyingPositions
    }

    moveTowards(dir: Direction) {
        return new Position(this.column + dir.x, this.row + dir.y, Entity.SHIP)
    }

    isEqual(position: Position) {
        return this.column == position.column && this.row == position.row
    }
}

export enum GameStatus {
    W4G = "WAITING_FOR_GUEST",
    SS = "SHIPS_SETUP",
    HT = "HOST_TURN",
    GT = "GUEST_TURN",
    WIH = "WINNER_IS_HOST",
    WIG = "WINNER_IS_GUEST",
    ABORTED = "ABORTED"
}

export function isGameOnGoing(gameStatus: GameStatus){ //this function could be in a typescript Enum-type-Class, todo later
    return (gameStatus == GameStatus.GT || gameStatus == GameStatus.HT)
}

export enum ShotResult { // I need to give provide string values or it sees each enum as a number (the index of the prop)...
    INVALID_ALREADY_HIT = "INVALID_ALREADY_HIT",
    MISS = "MISS",
    HIT = "HIT",
    SUNK = "SUNK", //not really in use
    WIN = "WIN",
    OFF_THE_BOARD = "OFF_THE_BOARD"
}

export class Shot {
    position: Position
    result: ShotResult
    getResult: Function
    constructor(position: Position, result: ShotResult) {
        this.position = position; this.result = result
    }

    static getResult(shot: Shot){ //Im defining this here because in the responde obj of the server we cant return a function (or can we idk?, anyways it's not supposed to), And when the response is converted to our class (using Object.assign()) the function will not be included in that obj!
        if(shot.result==ShotResult.HIT || shot.result==ShotResult.SUNK || shot.result==ShotResult.WIN) return Entity.DAMAGED
        if(shot.result==ShotResult.MISS) return Entity.WATER
        else throw new Error("Invalid state in shot")
    }
}

export let shipsTypes: string[]

export type ShipTypes = 'Carrier' | 'Battleship' | 'Cruiser' | 'Submarine' | 'Destroyer'

export class ShipType {
    static readonly Carrier = new ShipType('Carrier', 5, require('../../public/imgs/_carrier.png'))
    static readonly Battleship = new ShipType('Battleship', 4, require('../../public/imgs/_battleship.png'))
    static readonly Cruiser = new ShipType('Cruiser', 3, require('../../public/imgs/_cruiser.png'))
    static readonly Submarine = new ShipType('Submarine', 3, require('../../public/imgs/_submarine.png'))
    static readonly Destroyer = new ShipType('Destroyer', 2, require('../../public/imgs/_destroyer.png'))
    static readonly _ = new ShipType('Destroyer', null, null) //This class requires this filler or it will never reach the Destroyer... because Object.values() doesn't work really well https://stackoverflow.com/q/42966362/9375488, aparently pre es7 (and we are using es6 because versions above aren't compatible with the browsers) https://caniuse.com/?search=es6

    private constructor(public readonly name: ShipTypes | "", public readonly size: number, public readonly file: any) {
        if (name.length == 0) throw Error("ShipType name cant be empty")
        const names: string[] = []
        Object.values(ShipType).forEach(value => {
            const shipType = value as ShipType
            if (shipType.name != "") { //because during the inicialization it always starts with "" ...
                if (names.includes(shipType.name)) throw Error("There's already a ship w/ that name")
                names.push(shipType.name)
            }
        })
        shipsTypes = names
        /* console.log(names) */
    }

    static getShipTypeByName(shipName: any) {
        if (shipName == ShipType.Carrier.name) return ShipType.Carrier
        if (shipName == ShipType.Battleship.name) return ShipType.Battleship
        if (shipName == ShipType.Cruiser.name) return ShipType.Cruiser
        if (shipName == ShipType.Submarine.name) return ShipType.Submarine
        if (shipName == ShipType.Destroyer.name) return ShipType.Destroyer
        throw new Error(`Ship type=${shipName} doesn't exist `)
    }

    static getShipTypesAndQuantityInArray(shipTypesAndQuantity: Array<ShipsTypesAndQuantity>){
        console.log("getShipTypesAndQuantityInArray called")
        const res = new Array<SetupShips.ShipListing>()
        let id = 0 //unique id for all ships
        shipTypesAndQuantity.forEach((ship, index) => {
            for(let i = 0; i<ship.quantityAllowed; i++)
                res.push(new SetupShips.ShipListing(id++, this.getShipTypeByName(ship.shipType)))
        })
        return res
    }

    static areEqual(st1: ShipType, st2: ShipType) {
        return st1.name == st2.name
    }
}

export class Direction {
    static readonly UP = new Direction("UP", 0, 1)
    static readonly RIGHT = new Direction("RIGHT", -1, 0)
    static readonly DOWN = new Direction("DOWN", 0, -1)
    static readonly LEFT = new Direction("LEFT", 1, 0)

    private constructor(public readonly inString: string, public readonly x: number, public readonly y: number) { }

    static stringToDirection(direc: typeof Direction) {

    }

    static getOpposite(dir: Direction): Direction {
        if (dir == Direction.UP) return Direction.DOWN
        if (dir == Direction.DOWN) return Direction.UP
        if (dir == Direction.LEFT) return Direction.RIGHT
        if (dir == Direction.RIGHT) return Direction.LEFT
    }

    static getSides(dir: Direction): { first: Direction, second: Direction } {
        if (dir == Direction.UP || dir == Direction.DOWN) return { first: Direction.LEFT, second: Direction.RIGHT }
        if (dir == Direction.LEFT || dir == Direction.RIGHT) return { first: Direction.UP, second: Direction.DOWN }
    }
}

export class Ship {
    id?: number = 0
    shipType: ShipType
    head: Position
    direction: Direction
    positionsOccupying: Array<Position>
    constructor(shipType: ShipType, head: Position, direction: Direction, id?: number) {
        this.shipType = shipType; this.head = head; this.direction = direction; this.id = id

        let currentPos = head
        this.positionsOccupying = Array<Position>()
        for (let i = 0; i < this.shipType.size - 1; i++) {
            this.positionsOccupying.push(currentPos)
            currentPos = currentPos.moveTowards(this.direction)
        }
        this.positionsOccupying.push(currentPos)
    }

    isThisShipPositionIncompatibleWithOthers(otherShips: Array<Ship>) {
        const positionsAroundThisShip = Array<Position>()
        Position.getAllPositionsAroundAPositionTowardsADirection(this.head, this.direction, this.shipType.size).forEach(p => {
            positionsAroundThisShip.push(p)
        })
        return otherShips.some(otherShip => {
            if (otherShip.id == this.id && otherShip.shipType.name == this.shipType.name) return false //we say it's acceptable, cuz it might be the ship itself
            return otherShip.positionsOccupying.some(otherPos => {
                return positionsAroundThisShip.some(thisShipPos => {
                    return thisShipPos.isEqual(otherPos)
                })
            })
        })
    }

    static doesShipOverlapWithOthers(ship: Ship, otherShips: Array<Ship>) {
        const result = otherShips.some(gameShip => {
            if (gameShip.id == ship.id && gameShip.shipType.name == gameShip.shipType.name) return false //we say it's acceptable, cuz it might be the ship itself
            return gameShip.positionsOccupying.some(position => {
                return ship.positionsOccupying.some(shipGeneratedPos => {
                    return shipGeneratedPos.isEqual(position)
                })
            })
        })
        return result
    }
}

class Dimension {
    columnDim: number
    rowDim: number
    numOfPositions: number
    constructor(columnDim: number, rowDim: number) {
        this.columnDim = columnDim; this.rowDim = rowDim; this.numOfPositions = columnDim * rowDim
    }
}

class ShipsTypesAndQuantity {
    shipType: string
    quantityAllowed: number
    constructor(shipType: string, quantityAllowed: number) {
        this.shipType = shipType; this.quantityAllowed = quantityAllowed
    }
}

export class Rules {
    dimension: Dimension
    shipsAllowed: Array<ShipsTypesAndQuantity>
    shotsPerRound: number
    setupTimeS: number
    roundTimeS: number
    constructor(dimension: Dimension, shipsAllowed: Array<ShipsTypesAndQuantity>, shotsPerRound: number, setupTimeS: number, roundTimeS: number) {
        this.dimension = dimension; this.shipsAllowed = shipsAllowed; this.shotsPerRound = shotsPerRound; this.setupTimeS = setupTimeS; this.roundTimeS = roundTimeS
    }
}

export const vals = {
    MAX_BOARD_SIZE: 26,
    MIN_BOARD_SIZE: 2,
    MIN_COORDINATE: 1,
    MAX_NUM_OF_SHIPS_W_SAME_TYPE: 5,
    MIN_NUM_OF_SHIPS_W_SAME_TYPE: 1,
    MAX_SHOTS_PER_ROUND: 5,
    MIN_SHOTS_PER_ROUND: 1,
    MAX_DURATION_S: 5 * 60,
    MIN_DURATION_S: 5,

    defaultBoardDimension: 10,
    defaultQuantityOfEachShipType: 1,
    defaultShotsPerRound: 1,
    defaultDurationS: 3 * 60,
    defaultDoAllShipTypesNeedToBeInserted: false
}
