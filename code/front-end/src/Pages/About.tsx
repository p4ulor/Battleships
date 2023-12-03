import * as React from 'react'

import * as UtilsFetch from '../utils/Utils-Fetch'

const img = require('../../public/imgs/daw.png')

const getAbout_URI = "/system-info"

export function About(){
    const [aboutText, setAboutText] = React.useState(undefined)

    React.useEffect(() => {
        async function doFetch(){
            const serverInfo = await UtilsFetch.doFetch(getAbout_URI, "GET")
            setAboutText(`Version: ${serverInfo.version}\nAuthor: ${serverInfo.author.name}`)
        }
        doFetch()
    }, [])
    
    if(!aboutText) return(<h2 className='basicContainer'>Loading...</h2>)
    
    return(<>
        <p className="basicContainer">{aboutText}</p>

        <div className="basicContainer">
            <iframe width="640px" height="360px" src="https://www.youtube.com/embed/RY4nAyRgkLo" title="YouTube video player" frameBorder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowFullScreen>
            </iframe>
        </div>
        
        <img src={img} style={{
            border: "5px solid",
            borderBlockWidth: "5px",
            display: "block",
            marginLeft: "auto",
            marginRight: "auto",
            width: "50%",
            lineHeight: 10,
            borderRadius: '5px',
        }}/>
    </>)
}