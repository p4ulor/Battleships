import * as React from 'react'
import * as ReactRouter from "react-router-dom"

import * as Utils from "../utils/Utils"
import * as sfx from '../utils/SFXManager'

export function NotFound(){
    const navigate = ReactRouter.useNavigate()
    console.log(navigate.name)
    sfx.s404()

    return(
        <div className='basicContainer'> 
            <h1>Page not found</h1>
            <button onClick={() => Utils.previousPageIfPossible(navigate)()}>Go back</button>
            <button className='highligthed' onClick={() => navigate("/")}>Go to homepage</button> 
        </div>
    )
}