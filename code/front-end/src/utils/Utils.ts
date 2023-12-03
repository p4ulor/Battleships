import * as Bodies from './Req-Res-Bodies'
import * as ReactRouter from "react-router-dom"

import * as Game from '../Components/GameObjs'
import * as Body from '../utils/Req-Res-Bodies'

export class Time {
    m: number
    s: number
    toSeconds: Function
    toString: Function
    constructor(m: number, s: number) {
        if(m < 0 || s < 0) {
            //throw new Error("Bad input in Time class")
            m = 0; s = 0
        }
        this.m = m; this.s = s
        this.toSeconds = function toSeconds() : number {
            if(this.m==0) return this.s
            return this.m * 60 + this.s
        }
        this.toString = function toString() : string {
            let mz = ""; let sz = ""
            if (this.s / 10 < 1) sz = "0"
            if (this.m / 10 < 1) mz = "0"
            return `${mz}${m}:${sz}${s}`
        }
    }
}

export function timeStringToTime(time: string) : Time {
    const [m, s] = time.split(":")
    const minutes = new Number(m).valueOf()
    const seconds = new Number(s).valueOf()
    if(isNaN(minutes)) throw new Error("Minutes is not a number")
    if(isNaN(seconds)) throw new Error("Seconds is not a number")
    return new Time(minutes, seconds)
}

export function secondsToTime(n: number) : Time {
    let m = 0; let s = 0
    s = n % 60
    n = n - s
    if (n > 0) { //minutes
        n = n / 60
        m = n % 60
        n = n - m
    }

    let mz = ""; let sz = ""
    if (s / 10 < 1) sz = "0"
    if (m / 10 < 1) mz = "0"
    //console.log(`${mz}${m}:${sz}${s}`)
    return new Time(m, s)
}

export function isValidTime(t: Time) : boolean {
    console.log(t.toSeconds())
    if(t.toSeconds() < Game.vals.MIN_DURATION_S) return false
    if(t.toSeconds() > Game.vals.MAX_DURATION_S) return false
    return true
}

export function log(...args: any[]) {
    for (let i = 0; i < args.length; i++) {
        console.log(args[i])
    }
}

// Token / sessionStorage

export class UserData {
    userID: number
    constructor(userID: number) {
        this.userID = userID
    }
}

export function deleteCookies() {
    while(document.cookie!=''){  //clear all cookies non-HttpOnly
        document.cookie = document.cookie + '=;expires=Thu, 01 Jan 1970 00:00:01 GMT;'
    }
}

export function isLoggedIn() { //Doing it like this for now or I would need to refactor many stuff. Is this a scuffed solution?
    return getCookie("token")!=null
}

function getCookie(cname: string) : string | null { //https://www.w3schools.com/js/js_cookies.asp#:~:text=path%3D/%22%3B%0A%7D-,function%20getCookie(cname),-%7B%0A%C2%A0%C2%A0let
    let name = cname + "="
    let ca = document.cookie.split(';');
    for(let i = 0; i < ca.length; i++) {
        let c = ca[i]
        while (c.charAt(0) == ' ') {
        c = c.substring(1)
        }
        if (c.indexOf(name) == 0) {
        return c.substring(name.length, c.length)
        }
    }
    return null
}

// HTML Utils

const root = document.getElementById("root")

export type Cursor = "wait" | "not-allowed" | "auto"

export function setBackgroundUnclickable(exceptThisElement: HTMLElement, cursor: Cursor = "auto"){
    const react = document.getElementById("react") //contrary to root, this needs to be obtained everytime!...
    root.setAttribute("style", "cursor: not-allowed") //https://stackoverflow.com/a/46665946/9375488
    react.setAttribute("style", "pointer-events: none") //https://stackoverflow.com/a/18083136/9375488 
    exceptThisElement.setAttribute("style", `pointer-events: auto; cursor: ${cursor}`) //in order to override the parent's setted styles
}

export function resetClickable(){
    const react = document.getElementById("react")
    root.removeAttribute("style")
    react.removeAttribute("style")
}

export function previousPageIfPossible(navigate: ReactRouter.NavigateFunction) : Function { //this will not work very well... document.referrer only stores the previous hostname https://stackoverflow.com/a/3528331/9375488
    const navHome = history.length==2 //this isn't perfect, if a user routes throught different pages, and goes back to the not found page and clicks "back" it will go to the new tab. There's this complication/impossibility for privacy reasons
    return () => navHome ? navigate("/") : navigate(-1)
}

export class LocStateSetup { //used as a carrier of data between the navigation of Play.tsx to SetupShips.tsx
    opponentName: string
    amIHost: boolean
    gameData: Body.GameData
    constructor(opponentName: string, amIHost: boolean, gameData: Body.GameCreatedResponse){
        this.opponentName = opponentName; this.amIHost = amIHost; this.gameData = gameData
    }
}

import { ShipSelection } from '../Pages/SetupShips'
export class LocStateInGame {
    opponentName: string
    myShips: Array<ShipSelection>
    columnDim: number
    rowDim: number
    amIHost: boolean
    gameData: Body.GameData
    constructor(opponentName: string, myShips: Array<ShipSelection>, amIHost: boolean, gameData: Body.GameData){
        this.opponentName = opponentName; this.myShips = myShips; this.amIHost = amIHost; this.gameData = gameData
    }
}
