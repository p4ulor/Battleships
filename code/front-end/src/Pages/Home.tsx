import * as React from 'react'

import { LoginButton } from '../Components/Login'

export function Home(){
    const ctx = React.useContext(userNameContext)

    const [counter, setCounter] = React.useState(getDate()) //or call directly using: import { useState } from 'react'
    const [isRunning, setIsRunning] = React.useState(true)
    const [loginButton] = React.useState(<LoginButton/>) //this will solely avoid the re-rendering of the LoginButton componenent each time the counter is incremented!

    function getDate() : string{
        const d = new Date()
        return `${d.toLocaleDateString()} - ${d.toLocaleTimeString()}`
    }

    React.useEffect(() => {
        const cleartimeout = refresh()
        return cleartimeout
    }, [isRunning]) 

    function refresh() : () => void {
        /* console.log("refresh called")  */

        function action() {
            /* console.log("action called")  */
            setCounter(getDate())
        }

        const timeout = (isRunning) ? setInterval(action, 1000) : null

        if(isRunning) console.log("Is running")
        else console.log("cleared")
        
        return () => {
            if(timeout) clearInterval(timeout);
        }
    }

    function start_stop(){
        setIsRunning(!isRunning)
    }

    return(
        <div>
            {loginButton} 
            <section id="homeImg"> 
                <h1>Welcome{ctx.myUserName==undefined ? "" : ` ${ctx.myUserName}`}</h1>
            </section>
            <div className="basicContainer" onClick={start_stop}>
                {counter}
            </div>
        </div>
    )
}

type UsernameContext = {
    myUserName: string
}

export const userNameContext = React.createContext<UsernameContext>({ //note, data stored in context is gone on a page refresh
    myUserName: undefined //default value
})