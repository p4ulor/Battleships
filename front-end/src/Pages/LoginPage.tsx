import * as React from 'react'
import * as ReactRouter from "react-router-dom"

import * as Utils from '../utils/Utils'

import { LoginDialogBox } from '../Components/Login'

//I made this page just to make things differents (by not always using the Login button in home). I make use of this when a user tries to perform an action that requires being logged in
export function LoginPage(){
    const navigate = ReactRouter.useNavigate()
    const location = ReactRouter.useLocation()
    const {returnurl} = ReactRouter.useParams() //TODO: to return to exact previous url, not just the latest one
    if(Utils.isLoggedIn()){
        return(
            <div className='basicContainer'> 
                <h1>You are already logged in</h1>
                <button className='highligthed' onClick={() => navigate("/")}>Go to homepage</button> 
                <button className='highligthed' onClick={() => { Utils.deleteCookies(); navigate("/") }}>Logout</button>
            </div>
        )
    }
    
    return(<LoginDialogBox onClose={Utils.previousPageIfPossible(navigate)} setIsLoggedIn={()=>{}}/>)
}