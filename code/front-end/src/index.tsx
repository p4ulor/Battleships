import * as React from 'react' //"Used" in places where we put HTML elements mixed with JS. See lines in 3123 'react'. search for keywords 'declare global'
import * as ReactDOM from 'react-dom/client'
import * as ReactRouter from "react-router-dom"

import { NavigationBar } from './NavigationBar'
import { OpenGames } from './Pages/Play'
import { PlayerHub } from './Pages/PlayerHub'
import { LoginPage } from './Pages/LoginPage'
import { Home } from './Pages/Home'
import { About } from './Pages/About'
import { NotFound } from './Pages/NotFound'
import { SetupGame } from './Pages/SetupGame'
import { SetupShips } from './Pages/SetupShips'
import { InGame } from './Pages/InGame'

import { Rankings } from './Components/Rankings'
import { UserProfile } from './Components/UserProfile'

import '../public/css/styles.css' //or name the .css file like index.css (so webpack uses automaticaly) and I won't need to reference it in the file

const root = ReactDOM.createRoot(document.getElementById('root')) 
root.render(
    <div id="react">
        <ReactRouter.BrowserRouter>
            <NavigationBar/> {/* NavigationBar must be inside BrowserRouter, or it doesnt work because it uses react-router-dom.Link */}
            <ReactRouter.Routes>
                <ReactRouter.Route path="/" element={<Home/>}/>
                <ReactRouter.Route path="/play" element={<OpenGames/>}>
                    <ReactRouter.Route path="setup" element={<SetupGame/>}/> {/* path="setup" == path="/play/setup"  */}
                </ReactRouter.Route>
                <ReactRouter.Route path="/setupships" element={<SetupShips/>}/>
                <ReactRouter.Route path="/ingame" element={<InGame/>}/>
                <ReactRouter.Route path="/playerhub" element={<PlayerHub/>}>
                    <ReactRouter.Route path="/playerhub/me" element={<UserProfile/>}/>
                    <ReactRouter.Route path="/playerhub/ranking" element={<Rankings/>}/>
                    <ReactRouter.Route path="/playerhub/:id" element={<UserProfile/>}/>
                </ReactRouter.Route>
                <ReactRouter.Route path="/about" element={<About/>}/>
                <ReactRouter.Route path="/login" element={<LoginPage/>}/>
                <ReactRouter.Route path="/index.html" element={<Redirect path={"/"}/>}/> {/* 😈 actually needed when accessing http://localhost:9000/ (or it reaches "*" which redirects to the 404 page) (when accessing the page through the server's web-static-locations, not using webpack server). The redirect is just to replace the use of .htaccess (which I couldn't make it work) to remove the index.html in the URL */}
                <ReactRouter.Route path="*" element={<NotFound/>}/>
            </ReactRouter.Routes>
        </ReactRouter.BrowserRouter>
    </div>
)

function Redirect({ path } : { path: string }){
    return <ReactRouter.Navigate to={path} replace/>
}

window.onload = (event) =>{
    console.log('Page Loaded')
}
