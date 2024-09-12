import * as React from 'react'
import * as ReactRouter from "react-router-dom"

import * as UtilsFetch from '../utils/Utils-Fetch'
import * as Utils from '../utils/Utils'
import * as Body from '../utils/Req-Res-Bodies'
import * as SFX from '../utils/SFXManager'

const getOpenGames_URI = "/setup/opengames"
const postJoinGame_URI = "/setup/joingame"
let isFetching = false
export function OpenGames(){
    const navigate = ReactRouter.useNavigate()
    const location = ReactRouter.useLocation()
    const [gamesList, setGamesList] = React.useState(Array<Body.GamesArrayResponse>())
    const [isFetching, setIsFetching] = React.useState(false)

    React.useEffect(() => { 
        getOpenGames()
    }, []) //the dependency [] makes so it only runs and renders once

    function tryCreateGame(){
        if(!Utils.isLoggedIn()){
            alert("You need to login to perform this action")
            navigate("/login")
        } else {
            console.log("You can create a game")
            if(location.pathname=="/play/setup") document.getElementById('setupGame').scrollIntoView()
            else navigate("/play/setup")
        }
    }

    function getOpenGames(){
        async function doFetch(){
            setIsFetching(true)
            const GamesArrayResponse = await UtilsFetch.doFetch(getOpenGames_URI, "GET")
            if(GamesArrayResponse){
                const gamesObjList: any = Body.responseToObj(Body.GamesArrayResponse, GamesArrayResponse)
                const gamesList = Body.convertEnumerableObjToArrayOfBodies<Body.GamesArrayResponse>(gamesObjList)
                /* console.log(`games obtained -> ${JSON.stringify(gamesObjList)}`) */
                console.log(`list games obtained -> ${JSON.stringify(gamesList)}`)
                setGamesList(gamesList)
            }
            setIsFetching(false)
        }
        doFetch()
    }

    function tryJoinGame(id: number, opponentName: string){ //I have
        console.log("clicked to join "+id)
        if(!Utils.isLoggedIn()){
            alert("You need to login to perform this action")
            navigate("/login")
        } else {
            console.log("You may join this game")

            const joinGame = async function(){ //TODO: must handle error
                const jsonRsp = await UtilsFetch.doFetch(postJoinGame_URI, "PUT", new Body.JoinGameRequest(id))
                const gameJoinedInto = Body.responseToObj(Body.PlayersMatchedResponse, jsonRsp)
                console.log(jsonRsp)
                if(jsonRsp!=null) {
                    //TODO: store returned rules
                    navigate("/setupships", {state: new Utils.LocStateSetup(opponentName, false, new Body.GameCreatedResponse(id, gameJoinedInto.rules))}) //used an value injected between navigation
                }
            }
            joinGame()
        }
    }
   
    function render(){
        let listOfGames = Array<JSX.Element>()
        if(isFetching) listOfGames.push(<h2 key='0'>Loading...</h2>)
        else if(gamesList.length==0) listOfGames.push(<h2 key='0'>No games yet</h2>)
        else {
            listOfGames.push(<h2 key='0'>Open games</h2>)
            
            gamesList.map(game => {
                listOfGames.push(
                    <li key={game.gameID}>
                        <button className='openGame' onClick={() => tryJoinGame(game.gameID, game.hostName)} onMouseEnter={SFX.hover}>
                            {game.lobbyName}
                        </button>
                    </li>
                )
            })
        }

        return(
            <div style={{textAlign: 'center'}}>
                <button className='highlighted' onClick={() => tryCreateGame()}>Create a game</button>
                <button className='highlighted' onClick={() => getOpenGames()}>Refresh</button>
                <div className="openGames">
                    <ul>
                        {listOfGames}
                    </ul>
                </div>
            <ReactRouter.Outlet/>
            </div>
        )
    }
    
    return render()
}