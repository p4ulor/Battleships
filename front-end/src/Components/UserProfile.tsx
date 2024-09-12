import * as React from 'react'
import * as ReactRouter from "react-router-dom"

import * as UtilsFetch from '../utils/Utils-Fetch'
import * as Body from '../utils/Req-Res-Bodies'

const getUserProfilePage_URI = (id?: string) => {
    return `/users/${id}`
}

export function UserProfile(){
    const navigate = ReactRouter.useNavigate()
    const location = ReactRouter.useLocation() //use this or params?
    const {id: pathParamID} = ReactRouter.useParams()
    const [userProfile, setUserProfile] = React.useState<Body.UserProfileResponse>(undefined)
    const [gotError, setGotError] = React.useState(false) //most likely because user is not logged in

    React.useEffect(() => {
        setUserProfile(undefined) //Makes "Loading appear when changing from some user profile to "my" user profile
        async function doFetch(uri: string){
            const jsonRsp = await UtilsFetch.doFetch(uri, "GET", null, null)
            if(jsonRsp){ //if it's not null or undefined
                const response = Body.responseToObj(Body.UserProfileResponse, jsonRsp)
                console.log(`response obtained -> ${JSON.stringify(response)}`)
                setUserProfile(response)
            } else {
                setGotError(true)
            }
        }

        if(pathParamID!=undefined){
            console.log("will try getting userprofile id="+pathParamID)
            doFetch(getUserProfilePage_URI(pathParamID))
        }

        else doFetch(getUserProfilePage_URI("me"))
        
        //adding 'pathParamID' aids in the re-rendering when I'm at playerhub/5 and then try to go to playhub/me
    }, [pathParamID]) //putting [userProfile] will provoke an infinite number of calls!!!!!!!!!!!

    function render(){
        let userProfileData: JSX.Element =<h2>Loading...</h2>
        if(gotError) {
            userProfileData=
                <div>
                    <p>You are not logged in</p>
                    <button className='highlighted' onClick={ () => navigate("/login")}>Log in</button>
                </div>
        }
        else if(userProfile){
            userProfileData=
                <div>
                    <h2>Username: {userProfile.name}</h2>
                    <h2>Wins: {userProfile.winCount}</h2>
                    <h2>Play count: {userProfile.playCount}</h2>
                </div>
        }
        return(
            <div className="basicContainer">
                {userProfileData}
            </div>
        )
    }

    return render()
}