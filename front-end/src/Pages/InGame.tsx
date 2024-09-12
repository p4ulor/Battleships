import * as React from 'react'
import * as ReactRouter from "react-router-dom"

import * as UtilsFetch from '../utils/Utils-Fetch'
import * as Utils from '../utils/Utils'
import * as Body from '../utils/Req-Res-Bodies'
import * as Game from '../Components/GameObjs'
import * as sfx from '../utils/SFXManager'

import Timer from '../Components/Timer'

import { ShipSelection, squareIndexToPosition } from '../Pages/SetupShips'

const getUpdateGameStatus_URI = (gameID: number) => { return `/play/update/${gameID}` }
const postShoot_URI = "/play/shoot"
const putQuitGame_URI = "/play/quit"
const updateIntervalSeconds = 3

export function InGame() {
    const location = ReactRouter.useLocation()
    if(location.state==null) return <ReactRouter.Navigate to="/play" replace /> /*Dont use navigate("/login") or it will continue to the rest of the code... */
    const game = location.state as Utils.LocStateInGame //doing this way for organisation purposes

    const [myShips] = React.useState(game.myShips)
    const [myDamagedShips, setMyDamagedShips] = React.useState(Array<number>())
    const [missesOfMyOpponent, setMissesOfMyOpponent] = React.useState(Array<number>())
    const [myShots, setMyShots] = React.useState(Array<Shot>()) //it could be Game.Position but its a bit simpler like this
    const [squareSelected, setSquareSelected] = React.useState<number>(undefined)
    const [isMyTurn, setIsMyTurn] = React.useState(game.amIHost)
    const [isGameFinished, setIsGameFinished] = React.useState(false) // I need to use useRef because I need the current value immediately at the destructor of the useEffect w/ []. This is the proper alternative to having a variable outside of the scope of the component, similar to what I have done with didOpponentLeave in SetupShips 

    const [isFetching, setIsFetching] = React.useState(false) //for user feedback
    const [countDown, setCountdown] = React.useState(updateIntervalSeconds)

    const [columnDim] = React.useState(game.gameData.rules.dimension.columnDim)
    const [rowDim] = React.useState(game.gameData.rules.dimension.rowDim)
    const [startingTimeTimer, setStartingTimeTimer] = React.useState(game.gameData.rules.roundTimeS)

    React.useEffect(() => {
        function decrementCounter(){
            if(!isFetching){ //while fetching, the counter stays at 0
                if(countDown==0) fetchIsMyTurn()
                else setCountdown(countDown-1)
            }
        }

        const timeout = !isMyTurn ? setTimeout(decrementCounter, 1000) : null
        
        return () => { 
            if(timeout) clearTimeout(timeout)
        }
    }, [isMyTurn, countDown])

    React.useEffect(() => {

        //because of refreshing the page...
        async function checkIsMyTurn(){
            const jsonRsp = await UtilsFetch.doFetch(getUpdateGameStatus_URI(game.gameData.gameID), "GET", null)
            if(jsonRsp!=null) {
                const gameInfo = Body.responseToObj(Body.GetGameInformation, jsonRsp)
                const _isItMyTurn = isItMyTurn(gameInfo.gameStatus)
                console.log("Is it my turn?", _isItMyTurn)
                setIsMyTurn(_isItMyTurn)
                checkIfMyShipsAreDamaged(gameInfo.stateOfMyShips, gameInfo.latestShotOfMyOpponent, false) //false to avoid many sounds at the same time upon refresh
                setStartingTimeTimer(gameInfo.currentTimer)
                defineMyShots(gameInfo.myShots)
            }
        }
        checkIsMyTurn()
        
        return () => { //NOTE, when the page is refreshed (even on "empty cache and hard reload") this destructor will not run, but the variables in useState will be reset. Its kinda futile to store some of them in sessionStorage per example because we do fetches to get the state of the game anyway. This will only run when the location.href is changed
            console.log("Destructor called")
            quitGame()
        }
    }, [])

    const quitGame = React.useCallback(() => { //since this function doesnt use any useState vars it can easily get set to a cached function right away
        async function quit(){
            if(!isGameFinished) {//only the player that just performed a shot and is then waiting for opponent might have have this value to true or if a player that just performed a shot knows that the shot result is a win, since he is constantly fetching the gameState and not require to perform a request
                console.log("Gonna quit game")
                UtilsFetch.doFetch(putQuitGame_URI, "PUT", null, false) //since this is an idempotent action it's just better off doing it, rather than doing an extra request to check if we should or not I guess
            }
        }
        quit()
    }, [])

    function defineMyShots(shots: Array<Game.Shot>){
        const newShots = shots.map(shot => {
            return new Shot(positionToIndex(shot.position), Game.Shot.getResult(shot))
        })
        console.log("Shots obtained", JSON.stringify(newShots))
        setMyShots([...newShots])
    }
    
    async function fetchIsMyTurn(){
        console.log("my damaged squares->", JSON.stringify(myDamagedShips))
        console.log("myshots", JSON.stringify(myShots))
        setIsFetching(true)
        const jsonRsp = await UtilsFetch.doFetch(getUpdateGameStatus_URI(game.gameData.gameID), "GET", null)
        if(jsonRsp!=null) {
            const gameInfo = Body.responseToObj(Body.GetGameInformation, jsonRsp)
            console.log("Game info="+JSON.stringify(gameInfo))
            const res = amIWinner(gameInfo.gameStatus)
            if(res==true) { alert("You won!"); sfx.win();  }
            else if(res==false) { checkIfMyShipsAreDamaged(gameInfo.stateOfMyShips, gameInfo.latestShotOfMyOpponent); alert("You lost"); sfx.loss(); }
            else if(isItMyTurn(gameInfo.gameStatus)) {
                const wereDamaged = checkIfMyShipsAreDamaged(gameInfo.stateOfMyShips, gameInfo.latestShotOfMyOpponent) //only update board when it's users turn
                setIsMyTurn(true)
                setStartingTimeTimer(gameInfo.currentTimer) //will sync round timer with server, since we get the info I guess we'll use it (we could just reset locally though)
            }
            else setCountdown(updateIntervalSeconds)
        }
        setIsFetching(false)
    }

    function amIWinner(gameStatus: Game.GameStatus){
        if(gameStatus==Game.GameStatus.WIG) {
            setIsGameFinished(true)
            if(game.amIHost) return false
            else return true 
        }
        if(gameStatus==Game.GameStatus.WIH) {
            setIsGameFinished(true)
            if(game.amIHost) return true
            else return false 
        }
        else return undefined
    }

    function isItMyTurn(gameStatus: Game.GameStatus){
        if(game.amIHost) return gameStatus==Game.GameStatus.HT
        return gameStatus==Game.GameStatus.GT
    }

    function checkIfMyShipsAreDamaged(ships: Array<Body.Ship>, latestShotOfMyOpponent?: Game.Shot, playSounds: boolean = true){
        if(latestShotOfMyOpponent==null) return false //this is a small otimization which will only take effect when the guest is waiting for the first shot of the host
        
        let positionsOfSubmarines = Array<Game.Position>()
        myShips.forEach(ship => {
            if(Game.ShipType.areEqual(ship.shipType, Game.ShipType.Submarine)) {
                //TODO: direction
                new Game.Ship(ship.shipType, ShipSelection.getHead(ship, columnDim), Game.Direction.RIGHT).positionsOccupying.forEach(pos => { //TODO: simplify this mess
                    positionsOfSubmarines.push(new Game.Position(pos.column, pos.row, Game.Entity.SHIP)) 
                })
            }
        })

        const wasASubmarineHit = positionsOfSubmarines.some(pos =>{
            return pos.isEqual(latestShotOfMyOpponent.position)
        })

        const updatedDamagedIndexesList = Array<number>() 
        const updatedIndexesOfDamagedSubmarines = Array<number>()
        ships.forEach(ship => {
            ship.positionsOccupying.forEach(pos => {
                if(pos.entity==Game.Entity.DAMAGED.name) {
                    updatedDamagedIndexesList.push(positionToIndex(pos))
                    if(ship.shipType==Game.ShipType.Submarine.name) updatedIndexesOfDamagedSubmarines.push(positionToIndex(pos))
                }
            })
        })
        let wereDamaged
        if(updatedDamagedIndexesList.length==myDamagedShips.length) {
            wereDamaged = false 
        }
        else if(wasASubmarineHit) {
            if(playSounds) sfx.localDamagedSubmarine()
            wereDamaged = true
        } 
        else {
            if(playSounds) sfx.localDamaged()
            wereDamaged = true
        }
        if(wereDamaged) setMyDamagedShips([...updatedDamagedIndexesList]) // previous version for the record: setMyDamagedShips([...myDamagedShips, indexOfLastShot])
        else {
            setMissesOfMyOpponent([...missesOfMyOpponent, positionToIndex(latestShotOfMyOpponent.position)])
            if(playSounds) sfx.localMiss()
        }
        return wereDamaged
    }

    function positionToIndex(position: Body.Position | Game.Position){
        console.log("position ->"+JSON.stringify(position))
        const index = (position.row * columnDim) - columnDim + position.column
        console.log("index->"+index)
        return index
    }

    const renderMyBoard = () => {
        let squareID = 1
        let rows = Array<JSX.Element>()
        for (let r = 0; r < rowDim; r++) {
            let columns = Array<JSX.Element>()
            var noBoardersNTimes = 0
            for (let c = 0; c < columnDim; c++) {
                const id = squareID //index of a square. gotta use this or it gets the last incremented squareID lol
                let s = myShips.find(value => { //find if there's a ship in this square
                    return value.indexTail == id
                })

                let isThisAPositionOfADamagedShip = myDamagedShips.findIndex(index => { return index==id })

                let shipInThisSquareJSX = <></>
                if (s != undefined) {
                    shipInThisSquareJSX =
                    <>
                        <img src={s.shipType.file} id={`${s.shipType.name}-${s.shipType.size}`} className={"ship"} style={{
                            border: "1px solid rgb(0, 30, 129)",
                            width: (50 * s.shipType.size + 2 * (s.shipType.size - 1)).toString() + "px", /* the width is adapter given the ships size */
                        }} />
                        { isThisAPositionOfADamagedShip!=-1 ? <div className='damagedTail' ></div> : <></>} {/* This places the red color at the tail of the ship. I didnt find easier alternatives */}
                    </>
                    noBoardersNTimes = s.shipType.size
                }
                
                let clas = noBoardersNTimes < 1 ? "blueSquare" : "noBoarder"

                let isThisPositionOfAMiss = missesOfMyOpponent.findIndex(index => {return index==id})
                if(isThisPositionOfAMiss!=-1) clas = "miss"
                
                if(isThisAPositionOfADamagedShip!=-1) clas = "damaged"
                columns.push(
                    <div className={clas} id={squareID.toString()} key={squareID.toString()}>
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

    const renderOppponentBoard = () => {
        let squareID = 1
        let rows = Array<JSX.Element>()
        for (let r = 0; r < rowDim; r++) {
            let columns = Array<JSX.Element>()
            for (let c = 0; c < columnDim; c++) {
                const id = squareID //gotta use this or it gets the last incremented squareID which is off the board...
                let clas = "square"
                if(squareSelected==id) clas = "square selected"
                let shottedSquare = myShots.find(shot => { return shot.squareIndex==id })
                if(shottedSquare){
                    if(Game.Entity.areEqual(shottedSquare.entity, Game.Entity.DAMAGED)) clas = "hit"
                    if(Game.Entity.areEqual(shottedSquare.entity, Game.Entity.WATER)) clas = "miss"
                }
                
                columns.push(
                    <div className={clas} id={squareID.toString()} key={squareID.toString()}
                        onClick={(e) => setSquareSelected(id)} />
                )
                squareID++
            }
            const row = <div className='row' id={`row${r + 1}`} key={`row${r + 1}`}>{columns}</div>
            rows.push(row)
        }
        return rows
    }

    function onShoot() {
        sfx.localFire()
        async function shoot() {
            const jsonRsp = await UtilsFetch.doFetch(postShoot_URI, "POST", new Body.FireShotRequest(squareIndexToPosition(squareSelected, columnDim)))
            if (jsonRsp) { //if it's not null or undefined
                const resp = Body.responseToObj(Body.ShotResultResponse, jsonRsp)
                if (resp.shotResult == Game.ShotResult.HIT || resp.shotResult == Game.ShotResult.SUNK || resp.shotResult == Game.ShotResult.WIN) {
                    sfx.distantHit()
                    addShot(squareSelected, false)
                    if (resp.shotResult == Game.ShotResult.WIN) {
                        setIsGameFinished(true)
                        sfx.win()
                        alert("You won")
                    } 
                    else setIsMyTurn(false)
                } else if (resp.shotResult == Game.ShotResult.MISS) {
                    sfx.distantMiss()
                    addShot(squareSelected, true)
                    setIsMyTurn(false)
                } else { //the other options is off the board or already hit, which will be avoided by the UI, but I put this else because the user may change the DOM manually
                    alert(resp.shotResult)
                }
            }
        }
        shoot()
    }

    function addShot(squareSelected: number, isMiss: boolean) {
        const shot = new Shot(squareSelected, isMiss ? Game.Entity.WATER : Game.Entity.DAMAGED)
        const shots = myShots.map(v => {return v})
        shots.push(shot)
        setMyShots([...myShots, shot])
        console.log("Gonna store", JSON.stringify(myShots))
    }

    return (
        <div className="basicContainer" style={{ margin: "15px 15px 15px 15px", padding: "15px 15px 15px 15px" }}>
            <h2>Playing against {(game != null) ? game.opponentName : "?"}</h2>

            <div style={{
                display: "flex",
                /* display: 'inline-flex' */
            }}>
                <div className='simplerBasicContainer2' id="board" style={{ margin: "15px 10px auto auto", padding: "15px 15px 15px 15px" }}> {/* The 15px at the top maintains the boards at the same horizontal level when the position selected and button is shown */}
                    {renderMyBoard()}
                    { isGameFinished ? <></> :
                        isMyTurn ?  <>
                                        <p>It's your turn</p>
                                        <Timer initTimeSeconds={startingTimeTimer} onTimerReachesZero={() => {
                                            setIsGameFinished(true)
                                            console.log("Gonna quit game")
                                            UtilsFetch.doFetch(putQuitGame_URI, "PUT", null, false) 
                                            alert("You lost"); sfx.loss(); 
                                        }}/>
                                    </> : 
                            isFetching ? <p>Fetching...</p> : 
                                <p>It's {game.opponentName}'s turn. Fetching in {countDown}</p>
                    }
                </div>

                <div className='simplerBasicContainer2' id="Opponentboard" style={{ margin: "15px auto 10px auto", padding: "15px 15px 15px 15px" }}> {/* The 10px right of this user and the 10px left on opponents board is to make the boards closer together */}
                    {renderOppponentBoard()}
                    {isGameFinished ? <></> :
                        !isMyTurn ? <></> :
                            <>
                                <p>Position selected: {squareSelected ? squareIndexToPosition(squareSelected, columnDim).toString() : "none"}</p>
                                <button onClick={onShoot}>Shoot</button>
                            </>
                    }
                </div>
            </div>
        </div>
    )
}

class Shot {
    squareIndex: number
    entity: Game.Entity
    constructor(squareIndex: number, entity: Game.Entity) {
        this.squareIndex = squareIndex; this.entity = entity
    }
}
