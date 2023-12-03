import * as React from 'react'

import * as Utils from '../utils/Utils'

let mousePosition: number = 0
let event: (mouseEvent: MouseEvent) => void
let timeout: NodeJS.Timeout
function Timer({ initTimeSeconds, onTimerReachesZero } : {initTimeSeconds: number, onTimerReachesZero: Function}){
    const [countDown, setCountdown] = React.useState(initTimeSeconds)
    React.useEffect(() => {
        console.log("rendered")
        return () => {
            console.log("Timer destructor called")
            clearTimeout(timeout)
        }
    }, [])

    React.useEffect(() => {
        console.log("initTimeSeconds updated! to "+initTimeSeconds)
        clearTimeout(timeout)
        setCountdown(initTimeSeconds)
    }, [initTimeSeconds])
    
    React.useEffect(() => {
        //console.log("countDown="+countDown)
        function decrementCounter(){
            if(countDown==0) {
                console.log("timer reached zero")
                onTimerReachesZero()
            }
            else setCountdown(countDown-1)
        }
        timeout = setTimeout(decrementCounter, 1000)
    }, [countDown])

    function resize(ev: MouseEvent, div: EventTarget & HTMLDivElement){
        //console.log(mousePosition)
        const dx = mousePosition - ev.x
        mousePosition = ev.x
        div.style.width = (parseInt(getComputedStyle(div, '').width) + dx) + "px"
    }

    function mouseDown(e: React.MouseEvent<HTMLDivElement, MouseEvent>){
        const div = e.currentTarget
        event = (mouseEvent: MouseEvent) => resize(mouseEvent, div)
        div.addEventListener("mousemove", event, false)
    }
    
    function getEvent() {
        return event
    }

    return (
        <div className="timer" id="right_panel" 
            onMouseDown={ (e) => mouseDown(e) } //https://stackoverflow.com/a/53220241/9375488
            onMouseUp={ (e) => {
                console.log("mouse lifted")
                //console.log(e.currentTarget.id)
                e.currentTarget.removeEventListener("mousemove", getEvent())
            }} 
            onMouseLeave={(e) => e.currentTarget.removeEventListener("mousemove", getEvent()) } /* complementary, even then, sometimes they get screwed up when moved quickly */
            onMouseEnter={(e) => e.currentTarget.removeEventListener("mousemove", getEvent()) }>
             
            <h5>Time to setup</h5>
            <h2>{Utils.secondsToTime(countDown.valueOf()).toString()}</h2>
        </div>
    )
}

function areEqual(prevProps: {initTimeSeconds: number, onTimerReachesZero: Function}, nextProps: {initTimeSeconds: number, onTimerReachesZero: Function}){
    return prevProps.initTimeSeconds==nextProps.initTimeSeconds
}

//export default React.memo(Timer, areEqual) //https://reactjs.org/docs/react-api.html#reactmemo
export default Timer //uncommented because I think this memo was causing problems and not unmounting past Timers
