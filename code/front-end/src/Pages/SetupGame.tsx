import * as React from 'react'
import * as ReactRouter from "react-router-dom"

import * as UtilsFetch from '../utils/Utils-Fetch'
import * as Utils from '../utils/Utils'
import * as Body from '../utils/Req-Res-Bodies'
import * as Game from '../Components/GameObjs'

import { WaitingForGuest } from '../Components/WaitingForGuest'

const postCreateGame_URI = "/setup/newgame"

const minDuration = Utils.secondsToTime(Game.vals.MIN_DURATION_S).toString()
const maxDuration = Utils.secondsToTime(Game.vals.MAX_DURATION_S).toString()

export function SetupGame() {

    const [lobbyName, setLobbyName] = React.useState("")
    const [columnDim, setColumnDim] = React.useState(Game.vals.defaultBoardDimension)
    const [rowDim, setRowDim] = React.useState(Game.vals.defaultBoardDimension)
    const [shotsPerRound, setShotsPerRound] = React.useState(Game.vals.defaultShotsPerRound)
    const [shipsAllowed, setShipsAllowed] = React.useState(
        Object.values(Game.shipsTypes).map(value => {
            return { shipType: value, isChecked: true, quantity: Game.vals.defaultQuantityOfEachShipType }
        })
    )
    const [doAllShipTypesNeedToBeInserted, setdoAllShipTypesNeedToBeInserted] = React.useState(Game.vals.defaultDoAllShipTypesNeedToBeInserted)

    const [setupTime, setSetupTime] = React.useState<Utils.Time>(Utils.secondsToTime(Game.vals.defaultDurationS))
    const [roundTime, setRoundTime] = React.useState<Utils.Time>(Utils.secondsToTime(Game.vals.defaultDurationS))

    const [isWaitingForGuest, setIsWaitingForGuest] = React.useState(false)

    const [gameCreated, setGameCreated] = React.useState<Body.GameCreatedResponse>(undefined)

    React.useEffect(() => {
        const thisElement = document.getElementById('setupGame')
        if(thisElement) thisElement.scrollIntoView()
    }, []) //will only execute on first render

    function checkBoxChanged(e: React.ChangeEvent<HTMLInputElement>, idx: number) {
        //const allowed = shipsAllowed //will not work... omg... https://stackoverflow.com/a/72950842/9375488 It might appear that the obj was modified, but react will see that the obj reference is the same and ignore it...
        //const allowed = JSON.parse(JSON.stringify(shipsAllowed)) as ShipSettings[] //the 'as' is neccessary for using .forEach
        const allowed = structuredClone(shipsAllowed) //https://stackoverflow.com/a/23481096/9375488 or use spread operator
        allowed.forEach((value, index) => {
            if (index == idx) value.isChecked = e.target.checked
        })
        setShipsAllowed(allowed)
        console.log(allowed)
    }

    function dropDownShipQuantityChanged(e: React.ChangeEvent<HTMLSelectElement>, idx: number) {
        const allowed = structuredClone(shipsAllowed)
        allowed.forEach((value, index) => {
            if (index == idx) value.quantity = new Number(e.target.value).valueOf()
        })
        setShipsAllowed(allowed)
        console.log(allowed)
    }

    function dropDownShotsChanged(e: React.ChangeEvent<HTMLSelectElement>) {
        setShotsPerRound(e.target.selectedIndex + 1) //only fits for this case. Proper alternative would be: e.target.options[e.target.selectedIndex].value
    }

    function onChangeSetupTime(e: React.ChangeEvent<HTMLInputElement>) {
        setSetupTime(Utils.timeStringToTime(e.target.value))
        setTimeout(() => {
            if (!Utils.isValidTime(Utils.timeStringToTime(e.target.value))) {
                setSetupTime(Utils.secondsToTime(Game.vals.defaultDurationS))
            }
        }, 1000)
    }

    function onChangeRoundTime(e: React.ChangeEvent<HTMLInputElement>) {
        setRoundTime(Utils.timeStringToTime(e.target.value))
        setTimeout(() => {
            if (!Utils.isValidTime(Utils.timeStringToTime(e.target.value))) {
                setRoundTime(Utils.secondsToTime(Game.vals.defaultDurationS))
            }
        }, 1000)
    }

    function tryCreateGame(){
        if(lobbyName=="") {
            const lobby = document.getElementById('lobbyName')
            lobby.scrollIntoView()
            lobby.focus()
            lobby.setAttribute("placeholder", "please provide a lobby name")
            return
        }
        const rules = new Body.RulesRequestSegment(columnDim, rowDim, shotsPerRound, 
            shipsAllowed.map(value => {
                if(value.isChecked) return new Body.ShipsTypesAndQuantitySegment(value.shipType, value.quantity)
            }).filter(value => value!=null) , setupTime.toSeconds(), roundTime.toSeconds(), doAllShipTypesNeedToBeInserted
        )
        const body = new Body.CreateGameRequest(lobbyName, rules)
        console.log(JSON.stringify(body))

        async function doFetch(){ //TODO: must handle error of user already in game per example
            const response = await UtilsFetch.doFetch(postCreateGame_URI, "POST", body)
            if(response!=null){
                const gameCreated = Body.responseToObj(Body.GameCreatedResponse, response)
                setGameCreated(gameCreated)
                setIsWaitingForGuest(true)
                console.log(response) //TODO: store returned information
            }
        }
        doFetch()
    }

    if (!Utils.isLoggedIn()) {
        return <ReactRouter.Navigate to="/login" replace />
        //navigate("/login") //will not work
    } else {
        const shipsSettings = shipsAllowed.map((value, index) => {
            return (
                <div key={index}>
                    <label>{value.shipType}</label>
                    <input type="checkbox" defaultChecked={shipsAllowed[index].isChecked} onChange={(event) => checkBoxChanged(event, index)}/>
                    <select disabled={!(shipsAllowed[index].isChecked)} defaultValue={value.quantity} onChange={(event) => dropDownShipQuantityChanged(event, index)}>
                        {createNumericOptions(Game.vals.MIN_NUM_OF_SHIPS_W_SAME_TYPE, Game.vals.MAX_NUM_OF_SHIPS_W_SAME_TYPE)}
                    </select>
                    <br/>
                </div>)
        })
        
        return (
            <div className="basicContainer" id="setupGame">
                <form onSubmit={ (e) => { e.preventDefault() }}>
                
                <h2>Lobby name</h2>
                <input type="text" id="lobbyName" onChange={(event) => setLobbyName(event.target.value)} placeholder="new lobby" autoComplete="off"/>

                <h2>Rules</h2>
                <label>Board Dimension</label>
                <div className='simplerBasicContainer'>
                    <div style={{ position: 'relative' }}>
                        <input type="range" id="rows" min={Game.vals.MIN_BOARD_SIZE} max={Game.vals.MAX_BOARD_SIZE} defaultValue={Game.vals.defaultBoardDimension} className="rangeSlider vertical" onChange={(event) =>
                            setRowDim(Number(event.target.value))
                        }/>
                    </div>
                    <input type="range" id="columns" min={Game.vals.MIN_BOARD_SIZE} max={Game.vals.MAX_BOARD_SIZE} defaultValue={Game.vals.defaultBoardDimension} className="rangeSlider" onChange={(event) =>
                         setColumnDim(Number(event.target.value))
                    }/>
                    <br/>
                    <label>{rowDim} x {columnDim}</label>
                </div>

                <b>Max shots per round? </b>
                <select defaultValue={shotsPerRound} onChange={(event) => dropDownShotsChanged(event)}>
                    {createNumericOptions(Game.vals.MIN_SHOTS_PER_ROUND, Game.vals.MAX_SHOTS_PER_ROUND)}
                </select>
                <br/><br/>

                <b>Ship types allowed: </b>
                <br/>
                {shipsSettings}
                <br/>

                <b>Do all ships need to be inserted? </b>
                <select defaultChecked={doAllShipTypesNeedToBeInserted} onChange={() => setdoAllShipTypesNeedToBeInserted(!doAllShipTypesNeedToBeInserted)}>
                    <option>No</option>
                    <option>Yes</option>
                </select>
                <br/><br/>

                <b>Setup time: </b>
                <div className="hide">[{minDuration}-{maxDuration}]</div>
                <input type='time'min={minDuration} max={maxDuration}
                    value={setupTime.toString()} onChange={(event) => onChangeSetupTime(event)}/>
                <span className="validity"></span>
                <br/><br/>

                <b>Round time: </b>
                <div className="hide">[{minDuration}-{maxDuration}]</div>
                <input type='time' min={minDuration} max={maxDuration}
                    value={roundTime.toString()} onChange={(event) => onChangeRoundTime(event)}/>
                <span className="validity"></span>
                <br/><br/>

                <button type="submit" className='highlighted' onClick={() => tryCreateGame()}>Create</button>
                </form>
                {isWaitingForGuest ? 
                    <isWaitingForGuestCtx.Provider value={{setIsWaitingForGuest: (isWaitingForGuest: boolean) => setIsWaitingForGuest(isWaitingForGuest), gameCreated: gameCreated}}>
                        <WaitingForGuest/>
                    </isWaitingForGuestCtx.Provider> 
                : <></>}
            </div>
        )
    }
}

function createNumericOptions(min: number, max: number): Array<JSX.Element> {
    const options = Array<JSX.Element>()
    for (let i = min; i <= max; i++) {
        options.push(<option key={i}>{i}</option>)
    }
    return options
}

type IsWaitingForGuest = {
    setIsWaitingForGuest: (isWaitingForGuest: boolean) => void
    gameCreated: Body.GameCreatedResponse
}

export const isWaitingForGuestCtx = React.createContext<IsWaitingForGuest>({
    setIsWaitingForGuest: (isWaitingForGuest: boolean) => {}, //default value
    gameCreated: undefined
})
