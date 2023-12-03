import * as React from 'react'
import * as ReactRouter from "react-router-dom"

import * as UtilsFetch from '../utils/Utils-Fetch'
import * as Utils from '../utils/Utils'
import * as Body from '../utils/Req-Res-Bodies'
import * as Game from '../Components/GameObjs'
import * as sfx from '../utils/SFXManager'
import Timer from '../Components/Timer'

const getUpdateGameStatus_URI = (gameID: number) => { return `/play/update/${gameID}` }
const postSetupBoard_URI = "/setup/board"
const putQuitGame_URI = "/play/quit" //given that the game is in ships setup the game will be marked as aborted

//const shipTypes = [Game.ShipType.Carrier, Game.ShipType.Battleship, Game.ShipType.Cruiser, Game.ShipType.Submarine, Game.ShipType.Destroyer]
const updateIntervalSeconds = 3

let shipsInGameFormat = new Array<Game.Ship>() //not required to be an useState given the way I use it, and it's not something that requires rendering to the screen
let didOpponentLeave: boolean = undefined
let shipsIDs: number = 0
export function SetupShips() {
    const navigate = ReactRouter.useNavigate()
    const location = ReactRouter.useLocation()
    const game = location.state as Utils.LocStateSetup //doing this way for organisation purposes

    const [shipsInRules] = React.useState(Game.ShipType.getShipTypesAndQuantityInArray(game.gameData.rules.shipsAllowed))
    const [columnDim] = React.useState(game.gameData.rules.dimension.columnDim)
    const [rowDim] = React.useState(game.gameData.rules.dimension.rowDim)
    const [startingTimeTimer, setStartingTimeTimer] = React.useState(game.gameData.rules.setupTimeS)

    const [shipsList, setShipsList] = React.useState(shipsInRules) //ships in the list of ships that can be added to the board
    const [shipsSet, setShipsSet] = React.useState(Array<ShipSelection>()) //ships placed in the board

    const [isThisUserReady, setIsThisUserReady] = React.useState(false) //used for controlling the fetching == "isFetching"
    const [isFetching, setIsFetching] = React.useState(false) //for user feedback
    const [countDown, setCountdown] = React.useState(updateIntervalSeconds) //for user feedback
    const [isOpponentReady, setIsOpponentReady] = React.useState(false)
    const [wasBoardSentValid, setWasBoardSentValid] = React.useState(false)

    let currentlySelectedShip: ShipDropping = { shipType: null, indexTail: null }
    let latestSquaredThatWasDroppedTo: number = null //can be set null by onDragLeaveSquare!

    React.useEffect(() => { //console.log("useEffect (SetupShips)")
        if (game == null || !Utils.isLoggedIn()) { //acceptable way to prohibit a user from being here if he didn't came from setting up a game or joining one, the alternative would be to do a get request
            alert("You can't be on this page")
            navigate("/")
        }

        sfx.guestJoined()
        //because of refreshing the page...
        async function checkIsMyTurn() {
            const jsonRsp = await UtilsFetch.doFetch(getUpdateGameStatus_URI(game.gameData.gameID), "GET", null)
            if (jsonRsp != null) {
                const gameInfo = Body.responseToObj(Body.GetGameInformation, jsonRsp)
                setStartingTimeTimer(gameInfo.currentTimer)
                //aparently I dont need to set the ships, even on "clear cache and hard reset", its all kept stored in location.state
            }
        }
        checkIsMyTurn()

        return () => {
            shipsInGameFormat = new Array<Game.Ship>()
            console.log(`didOpponentLeave=${didOpponentLeave}`)
            if (didOpponentLeave === undefined) { //=== because undefined==null... We can can know if this user should or shoulnd't make this request up to a certain point, when isGameOnGoing is true, we set didOpponentLeave=undefined. But none of the users are fetching or listening to the server, there's no way to know if the other user aborted the game.
                UtilsFetch.doFetch(putQuitGame_URI, "PUT", null, false) //if this user has setup the board, he will be fetching and might know if the other user aborted the game, thus he wont try to execute this. Other wise, if we dont have the info to decide if we're going to quit the game or not it's futile to do an extra request to know if we should or not, we just do the quit request. Since its PUT request which is expected to be (and is, in my server) an indepotenet
            }
            didOpponentLeave = undefined
            shipsIDs = 0
        }
    }, [])

    /* React.useEffect(() => { //even source not working yet
        let eventSource: EventSource //https://developer.mozilla.org/en-US/docs/Web/API/EventSource
        if(locState!=null || true) {
            eventSource = new EventSource(`${UtilsFetch.hostname}/setup/listenready`);
            eventSource.onopen = (ev) => {
                setStatus(UtilsFetch.getEventSourceStatus(eventSource))
            }
            eventSource.onmessage = (ev) => {
                setStatus(UtilsFetch.getEventSourceStatus(eventSource))
                console.log(`data obtained ${ev.data}`)
            }
            eventSource.onerror = (ev: ErrorEvent) => {
                setStatus(UtilsFetch.getEventSourceStatus(eventSource)+ev.message)
            }
        }

        return () => {
            eventSource.close()
        }
    }, []) */

    React.useEffect(() => {
        console.log("isThisUserReady->", isThisUserReady)

        function decrementCounter() {
            if (didOpponentLeave) return
            if (!isFetching) { //while fetching, the counter stays at 0
                if (countDown == 0) fetchIsOpponentReady()
                else setCountdown(countDown - 1)
            }
        }

        const timeout = isThisUserReady && !didOpponentLeave ? setTimeout(decrementCounter, 1000) : null

        return () => {
            if (timeout) clearTimeout(timeout)
        }
    }, [isThisUserReady, countDown])

    const fetchIsOpponentReady = React.useCallback(() => { //caching just to try it out
        async function fetchIsOpponentReadyCB() {
            setIsFetching(true)
            const jsonRsp = await UtilsFetch.doFetch(getUpdateGameStatus_URI(game.gameData.gameID), "GET", null/* , false */)
            if (jsonRsp != null) {
                const gameInfo = Body.responseToObj(Body.GetGameInformation, jsonRsp)
                if (Game.isGameOnGoing(gameInfo.gameStatus)) {
                    setIsThisUserReady(false)
                    didOpponentLeave = null
                    navigate("/ingame", { state: new Utils.LocStateInGame(gameInfo.opponentName, shipsSet, game.amIHost, game.gameData) }) //TODO:, send data to /ingame
                }
                setIsFetching(false)
                setCountdown(updateIntervalSeconds)
            } else {
                didOpponentLeave = true
                setIsFetching(false)
                navigate("/opengames")
            }
        }
        fetchIsOpponentReadyCB()
    }, [shipsSet])

    //FUNCTIONS TO CONVERT SHIP SELECTION OBJS AND SQUARE INDEXES TO GAME OBJS

    function convertShipSet_SelectionToShip(shipSelection: ShipSelection) {
        const head = squareIndexToHeadPosition(shipSelection)
        return new Game.Ship(shipSelection.shipType, head, Game.Direction.RIGHT, shipSelection.id)
    }

    function squareIndexToTailPosition(shipSelection: ShipSelection) { return squareIndexToPosition(shipSelection.indexTail, columnDim) }

    function squareIndexToHeadPosition(shipSelection: ShipSelection) {
        const position = squareIndexToPosition(shipSelection.indexTail + shipSelection.shipType.size - 1, columnDim)
        return new Game.Position(position.column, position.row, position.entity)
    }

    function shipDroppingToShipSelection(shipDropped: ShipListing): ShipSelection {
        return new ShipSelection(shipDropped.shipType, latestSquaredThatWasDroppedTo, Game.Direction.RIGHT, shipDropped.id) //TODO: direction
    }

    //ON DRAGS:

    function onDragStartedShip(shipBeingDragged: ShipListing) {
        console.log("Started dragging->" + JSON.stringify(shipBeingDragged))
        if (isThisUserReady) currentlySelectedShip = { shipType: null, indexTail: null }
        else currentlySelectedShip = { shipType: shipBeingDragged.shipType, indexTail: null }
    }

    function onDragEndedShip(shipDropped: ShipListing) {
        console.log("Ended dragging ship->" + JSON.stringify(shipDropped) + " on square->" + latestSquaredThatWasDroppedTo)
        if (latestSquaredThatWasDroppedTo != null && currentlySelectedShip.shipType != null) {

            if (!(columnDim < currentlySelectedShip.shipType.size + (latestSquaredThatWasDroppedTo - 1) % columnDim)) { //only move if the position of the square that this ship was dropped of too, is compatible with it's size (and direction)
                const shipToAdd: ShipSelection = shipDroppingToShipSelection(shipDropped)

                const shipGenerated = convertShipSet_SelectionToShip(shipToAdd)
                if (!Game.Ship.doesShipOverlapWithOthers(shipGenerated, shipsInGameFormat)) {
                    if (shipGenerated.isThisShipPositionIncompatibleWithOthers(shipsInGameFormat)) {
                        console.log("This ship is incompatible w/ others, leave 1 square around a ship")
                        return
                    }

                    updateShipsInGameFormat(shipGenerated)

                    //Config shipsList (needs to be like this using an ID because on a game that allows multiple types of ships, if I place a type of a ship, all others will be removed from the list...)
                    const updatedShipList = shipsList.filter(shipListing => {
                        return shipListing.id != shipDropped.id
                    })
                    setShipsList(updatedShipList)

                    //Config shipsSet
                    const possibleClone = shipsSet.find(shipSelection => { //preventing a cloning
                        return shipSelection.id == shipDropped.id && shipSelection.shipType.name == shipDropped.shipType.name
                    })
                    let ships = shipsSet
                    if (possibleClone) { //if there's a clone (which is most likely the ship being dragged), remove it from the board (it's old position)
                        ships = ships.filter(shipSelection => {
                            return shipSelection.id != shipDropped.id
                        })
                    }
                    setShipsSet([...ships, shipToAdd])
                } else { console.log("Ship overlaps with others") }
            } else console.log("An attempt to put the ship outside the boader was made")
        }
    }

    function onDragOverSquare(squareID: number, e: React.DragEvent<HTMLDivElement>) {
        e.preventDefault() //just changes the cursor from "forbidden" icon to "allowable drag" icon
        console.log("Started dragging over square->" + squareID)
        latestSquaredThatWasDroppedTo = squareID
    }

    function onDragEndedSquare(squareID: number) {
        console.log("Ended dragging on square->" + squareID)
        if (latestSquaredThatWasDroppedTo != null) currentlySelectedShip.indexTail = squareID
    }

    function onDragLeaveSquare() {
        latestSquaredThatWasDroppedTo = null
        console.log("Cursor left square, canceled dragging")
    }

    //RENDERS

    const renderShipList = () => {
        let shipsToBeRendered = Array<JSX.Element>()
        shipsList.forEach(ship => {
            shipsToBeRendered.push(<div key={`${ship.shipType.name}-${ship.id}`}>
                <p>{ship.shipType.name} (lenght={ship.shipType.size})</p>
                <img src={ship.shipType.file} id={`${ship.shipType.name}-${ship.shipType.size}-${ship.id}`} className='ship draggable' style={{
                    width: (50 * ship.shipType.size + 2 * (ship.shipType.size - 1)).toString() + "px",
                }}
                    draggable="true" onDragStart={() => onDragStartedShip(ship)} onDragEnd={() => onDragEndedShip(ship)} />
            </div>)
        })
        return shipsToBeRendered
    }

    const renderBoard = () => {
        let squareID = 1
        let rows = Array<JSX.Element>()
        for (let r = 0; r < rowDim; r++) {
            let columns = Array<JSX.Element>()
            var noBoardersNTimes = 0
            for (let c = 0; c < columnDim; c++) {
                const id = squareID //gotta use this or it gets the last incremented squareID which is off the board...
                let s = shipsSet.find(value => { //find if there's a ship in this square
                    return value.indexTail == id
                })
                let shipInThisSquareJSX = <></>
                if (s != undefined) {
                    shipInThisSquareJSX =
                        <img src={s.shipType.file} id={`${s.shipType.name}-${s.shipType.size}`} className='ship draggable' style={{
                            width: (50 * s.shipType.size + 2 * (s.shipType.size - 1)).toString() + "px", /* the width is adapter given the ships size */
                        }}
                            draggable="true" onDragStart={() => onDragStartedShip(new ShipListing(s.id, s.shipType))} onDragEnd={() => onDragEndedShip(new ShipListing(s.id, s.shipType))} />

                    noBoardersNTimes = s.shipType.size
                }

                columns.push(
                    <div className={noBoardersNTimes < 1 ? "square" : "noBoarder"} id={squareID.toString()} key={squareID.toString()}
                        onDragOver={(e) => onDragOverSquare(id, e)} onDragEnd={() => onDragEndedSquare(id)} onDragLeave={() => onDragLeaveSquare()}>
                        {shipInThisSquareJSX}
                    </div>
                )
                squareID++
                noBoardersNTimes--
            }
            const row = <div className='row' id={`row${r + 1}`} key={`row${r + 1}`}>{columns}</div>
            rows.push(row)
        }
        return rows
    }

    const generateRepresentativeTable = () => {
        return ( //tbody? -> https://stackoverflow.com/a/44217800/9375488
            <table><tbody>
                <tr>
                    <th>Ship Type</th>
                    <th>Head Position</th>
                    <th>Direction</th>
                </tr>
                {
                    shipsInGameFormat.map(ship => {
                        return (
                            <tr key={ship.id}>
                                <td>{ship.shipType.name}</td>
                                <td>{ship.head.toString()}</td>
                                <td>{ship.direction.inString}</td>
                            </tr>
                        )
                    })
                }
            </tbody></table>)
    }

    function onReset() {
        if (isThisUserReady) return
        setShipsList(shipsInRules)
        setShipsSet(Array<ShipSelection>())
        shipsInGameFormat = new Array<Game.Ship>()
    }

    function setReady() {
        if (shipsInGameFormat.length == 0) return
        if (isThisUserReady) return
        setIsThisUserReady(true)
        const body = new Body.BoardSetupRequest(shipsInGameFormat.map(s => {
            return new Body.ShipRequestSegment(
                s.shipType.name,
                new Body.PositionRequestSegment(s.head.column, s.head.row),
                s.direction.inString
            )
        }))

        async function doFetch() { //TODO: is the responde body of this useful for the front end?...
            const response = await UtilsFetch.doFetch(postSetupBoard_URI, "POST", body)
            if (response != null) {
                setIsThisUserReady(true)
                setWasBoardSentValid(true)
            }
            //else opponentLeft() //fix this... on any error it says that the opponent left... the doFetch func must have richer functinalities to handle errors
        }
        doFetch()
    }

    function opponentLeft() {
        didOpponentLeave = true
        setIsFetching(false)
        alert("The opponent has left, apparently, or took too long to setup board")
        navigate("/play")
    }

    return (
        <div className="basicContainer">
            <h2>Setup your ships against {(game != null) ? game.opponentName : "?"}</h2>
            <button onClick={onReset}>Reset</button>
            {/* <button>Randomize</button> */}

            <Timer initTimeSeconds={startingTimeTimer} onTimerReachesZero={() => {
                alert("You took too long to setup board")
                navigate("/play")
            }} />

            <div className='simplerBasicContainer2' style={{ height: "200px" }} >
                {renderShipList()}
            </div>
            <div className='simplerBasicContainer2' id="board">
                {renderBoard()}
            </div>

            {
                shipsInGameFormat.length != 0 ?
                    <>
                        <h2>Please confirm</h2>
                        {generateRepresentativeTable()}
                        <label className="switch"> {/* I need to have the onChange when checked= is used, says React*/}
                            <input type="checkbox" onClick={setReady} checked={isThisUserReady} onChange={() => { }} />
                            <span className="slider"></span>
                        </label>
                    </>
                    :
                    <h2>
                        No Ships set yet
                    </h2>
            }

            {
                wasBoardSentValid && isThisUserReady ?
                    <div className='simplerBasicContainer2' id="board">
                        {
                            isOpponentReady ?
                                <h2>Opponent is ready</h2>
                                :
                                <>
                                    <h2>Waiting for the opponent to be ready</h2>
                                    <p>Updating in {countDown}s</p>
                                    {isFetching ? <p>Fetching...</p> : <p>Opponent is not ready yet</p>}
                                </>
                        }
                    </div>
                    :
                    <></>
            }

        </div>)
}

export class ShipSelection { //then use this to convert to Game.Ship
    shipType: Game.ShipType
    indexTail: number
    direction: Game.Direction
    id: number //to uniquely identify ships (in case of a game that allows multiple ship types)
    constructor(shipType: Game.ShipType, indexTail: number, direction: Game.Direction, id: number) {
        this.shipType = shipType; this.indexTail = indexTail; this.direction = direction; this.id = id
    }

    //I made static cuz it was giving me 'getHead' is not a function...
    static getHead(ship: ShipSelection, columnDim: number): Game.Position { //TODO: depends on direction
        const indexHead = ship.indexTail + ship.shipType.size - 1
        return squareIndexToPosition(indexHead, columnDim)
    }
}

export class ShipListing {
    id: number //to uniquely identify ships (in case of a game that allows multiple ship types)
    shipType: Game.ShipType
    constructor(id: number, shipType: Game.ShipType) {
        this.id = id; this.shipType = shipType
    }
}

interface ShipDropping {
    shipType?: Game.ShipType,
    indexTail?: number
}

export function squareIndexToPosition(index: number, columnDim: number) { //note, the index of squares starts at 1
    if (index < 1) throw new RangeError("Index must me greater than 0")
    return new Game.Position(Math.floor((index - 1) % columnDim) + 1, Math.floor((index - 1) / columnDim) + 1, Game.Entity.SHIP)
}

function updateShipsInGameFormat(shipAdded: Game.Ship) {
    shipsInGameFormat = shipsInGameFormat.filter(ship => {
        return ship.id != shipAdded.id
    })
    shipsInGameFormat.push(shipAdded)
}
