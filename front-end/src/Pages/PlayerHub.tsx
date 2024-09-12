import * as React from 'react'
import * as ReactRouter from "react-router-dom"

export function PlayerHub(){
    const navigate = ReactRouter.useNavigate()
    const location = ReactRouter.useLocation()
    const [activeButtons, setActiveButtons] = React.useState([false, false])

    return(
        <div className="tabContainer">
            <button className= {activeButtons[0] ? 'square left active' : 'square left'} onClick={() => { 
                    navigate("/playerhub/ranking")
                    setActiveButtons([true, false])
                }
            }>Rankings</button>
            
            <button className={activeButtons[1] ? 'square right active' : 'square right'} onClick={() => {
                    navigate("/playerhub/me")
                    setActiveButtons([false, true])
                }
            }>Me</button>

            {location.pathname=='/playerhub' ? <h1>Consult player stats here </h1> : <></>}

            <ReactRouter.Outlet/>
        </div>
    ) //https://reactrouter.com/en/main/components/outlet
}