import * as React from 'react'
import * as ReactRouter from "react-router-dom"

import * as UtilsFetch from '../utils/Utils-Fetch'
import * as Body from '../utils/Req-Res-Bodies'

const getRankingWins_URI = "/users/ranking"
const getRankingPlayCount_URI = "/users/ranking/played"

export function Rankings(){
    const navigate = ReactRouter.useNavigate()
    const [playerList, setPlayerList] = React.useState(Array<Body.UserProfileResponse>)
    const [orderByWins, setOrderByWins] = React.useState(true)
    const [page, setPage] = React.useState(1)
    const [isFetching, setIsFetching] = React.useState(false)
    const [doTheFetch, setDoTheFetch] = React.useState(false)

    React.useEffect(() => {
        setDoTheFetch(true)
    }, [])

    React.useEffect(() => { 
        console.log(doTheFetch)
        if(!doTheFetch) return
        async function doFetch(){
            setIsFetching(true)
            const URI = orderByWins ? getRankingWins_URI : getRankingPlayCount_URI
            const RankingsArrayResponse = await UtilsFetch.doFetch(URI, "GET", null, true, `?limit=5&skip=${5*(page-1)}`)
            if(RankingsArrayResponse){
                const playersObjList: any = Body.responseToObj(Body.UserProfileResponse, RankingsArrayResponse)
                const playersList = Body.convertEnumerableObjToArrayOfBodies<Body.UserProfileResponse>(playersObjList)
                // console.log(`players obtained -> ${JSON.stringify(playersObjList)}`)
                console.log(`players-> ${JSON.stringify(playersList)}`)
                setPlayerList(playersList)
            }
            setIsFetching(false)
            setDoTheFetch(false)
        }
        doFetch()
    }, [doTheFetch]) //this component will only call useEffect when orderByWins and page is changed

    function changeOrderBy(){
        setOrderByWins(!orderByWins)
        setPlayerList(Array<Body.UserProfileResponse>())
        setDoTheFetch(true)
    }

    function render(){
        let listOfPlayers = Array<JSX.Element>()
        if(!isFetching) {
            if(playerList.length==0) listOfPlayers.push(<h3 key='1'>No further results</h3>)
            else {
                listOfPlayers = playerList.map(player => {
                    return <li key={player.id}>
                                <button className='square' onClick={() => { 
                                    navigate(`/playerhub/${player.id}`)
                                }}>{player.name} - {orderByWins ? <>{player.winCount} wins</> : <>{player.playCount} games</> }
                                </button>
                            </li>
                })
            }
        }
        return(
            <div>
                <input type="range" min={1} max={10} defaultValue={1} maxLength={100} className="rangeSlider" onChange={(event) => setPage(Number(event.target.value))} onMouseUp={() => setDoTheFetch(true) }/>
                <p>Page {page}</p>
                <br/>
                <button onClick={changeOrderBy}> {orderByWins ? "Order by plays" : "Order by wins" }</button>
                <ol>
                    {isFetching ? <h2 key='0'>Loading...</h2> : listOfPlayers}
                </ol>
            </div>
        )
    }

    return render()
}
