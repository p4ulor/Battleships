import * as React from 'react'
import * as ReactRouter from "react-router-dom"

import * as UtilsFetch from '../utils/Utils-Fetch'
import * as Utils from '../utils/Utils'
import * as Body from '../utils/Req-Res-Bodies'
import * as SFX from '../utils/SFXManager'

import { isWaitingForGuestCtx } from '../Pages/SetupGame'

const getUpdateGameStatus_URI = (gameID: number) => { return `/play/update/${gameID}` }
const deleteCreatedLobby_URI = "/setup/lobby"

const updateIntervalSeconds = 3
let guestJoined2 = "" //note, values stored here, will be saved between route navigation! This value will be stored throught the app entire use, unless a refresh is made! When you do it like this, you need to clear it on unmounting
const waitingSFX = SFX.waitingForGuest()
export function WaitingForGuest(){
    const ctx = React.useContext(isWaitingForGuestCtx)
    const navigate = ReactRouter.useNavigate()

    const [isUpdating, setIsUpdating] = React.useState(true) //for controlling the fetching
    const [isFetching, setIsFetching] = React.useState(false) //for user feedback
    const [countDown, setCountdown] = React.useState(updateIntervalSeconds) //for user feedback

    const [guestJoined, setGuestJoined] = React.useState("")
    
    React.useEffect(() => {
        function update() {
            console.log("Will update")
            async function fetchUpdate(){
                setIsFetching(true)
                const jsonRsp = await UtilsFetch.doFetch(getUpdateGameStatus_URI(ctx.gameCreated.gameID), "GET", null)
                const gameInfo = Body.responseToObj(Body.GetGameInformation, jsonRsp)
                if(gameInfo.opponentName!=null) {
                    setIsUpdating(false)
                    console.log("Guest -> "+gameInfo.opponentName)
                    setGuestJoined(gameInfo.opponentName)
                    guestJoined2 = gameInfo.opponentName
                    navigate("/setupships", {state: new Utils.LocStateSetup(gameInfo.opponentName, true, ctx.gameCreated)})
                }
                setIsFetching(false)
                setCountdown(updateIntervalSeconds)
            }
            fetchUpdate()
        }

        function decrementCounter(){
            if(!isFetching){ //while fetching, the counter stays at 0
                if(countDown==0) update()
                else setCountdown(countDown-1)
            }
        }

        const timeout = isUpdating ? setTimeout(decrementCounter, 1000) : null

        if(isUpdating) console.log("Is updating")
        else console.log("updating cleared")

        return () => { 
            //console.log("useEffect1 -> WaitingForGuest will unmount") //it will unmount on each Timeout execution
            console.log("guestJoined(useffect1)="+guestJoined)
            if(timeout) clearTimeout(timeout)
        }
    }) //this useEffect will run on any modification of states (the main trigger is the countDown)

    React.useEffect(() => {
        /* console.log("Ran WaitingForGuest useEffect2") */
        const thisElement = document.getElementById("WaitingForGuest")
        thisElement.setAttribute("style", `pointer-events: auto; cursor: wait`)

        const listener = (event: MouseEvent) => { //it's acceptable to consult the DOM w/ this, but never change the DOM directly!
            const isClickInside = thisElement.contains(event.target as Node)
            console.log("is click inside WaitingForGuest: "+isClickInside)
            if(!isClickInside) {
                ctx.setIsWaitingForGuest(false) //closes this Component which will also delete the lobby given this useEffect's return
                setIsUpdating(false) //I guess this call is worthless?
                alert("Lobby was deleted")
            } 
        }

        setTimeout(() => { //because pressing 'enter' counts as click... so it auto closes...
            document.body.addEventListener("click", listener)
            waitingSFX.play()
        }, 300)
        const guest = guestJoined
        return () => {
            console.log("useEffect2 -> WaitingForGuest will unmount")
            document.body.removeEventListener("click", listener)

            async function deleteLobby(){
                console.log("Deleting lobby...")
                const response = await UtilsFetch.doFetch(deleteCreatedLobby_URI, "DELETE", null)
                console.log("Delete response="+response)
            }
            
            console.log("guestJoined(useffect2)="+guestJoined) //will not work, it will have the old value...
            console.log("guestJoined2="+guestJoined2) //WORKS, the state is instanstly accessible
            if(guestJoined2=="") deleteLobby()
            waitingSFX.stop()
            guestJoined2 = ""
        }
    }, []) //this useEffect will only run once. And the unmount will only run once too

    return(
        <div id="WaitingForGuest" className="overLappingContainerFixed">
            <h2>Waiting for guest</h2>
            <p>Updating in {countDown}s</p>
            {isFetching ? <p>Fetching...</p> : <p>No guest joined yet</p>}
        </div>
    )
}
