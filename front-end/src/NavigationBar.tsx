import * as React from 'react'
import * as ReactRouter from "react-router-dom"

export function NavigationBar(){
    return (
        <nav id="mainNav">
            <ul>
                <li><ReactRouter.Link to="/">Home</ReactRouter.Link></li>
                <li><ReactRouter.Link to="/play">Play</ReactRouter.Link></li>
                <li><ReactRouter.Link to="/playerhub">Player Hub</ReactRouter.Link></li>
                <li><ReactRouter.Link to="/about">About</ReactRouter.Link></li>
            </ul>
        </nav>
    )
}

let prevScrollpos = window.pageYOffset
window.onscroll = function() {
    let currentScrollPos = window.pageYOffset
    if(currentScrollPos==0) document.getElementById("mainNav").style.top = "0"
    if (prevScrollpos > currentScrollPos) document.getElementById("mainNav").style.top = "0"
    else document.getElementById("mainNav").style.top = "-50px"
    prevScrollpos = currentScrollPos
}
