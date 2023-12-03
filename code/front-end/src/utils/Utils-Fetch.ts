//export const hostname = "https://battleshipp4ulor.azurewebsites.net" //for azure build
export const hostname = "http://localhost:8080"

type HTTPMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'

export function doFetch(url: string, method: HTTPMethod, requestBody?: object, doAlert: boolean = true, queryParams: string = "") { //TODO: Error handling
    const headers = new Headers()
    //headers.append("Authorization", `Bearer ${token}`)
    headers.append("Content-Type", "application/json")
/*  headers.append("access-control-expose-headers", "Set-Cookie")
    headers.append("Access-Control-Allow-Credentials", "true")
    headers.append("withCredentials", "true") */
    const x = fetch(hostname+url+queryParams, {
        method: method,
        headers: headers,
        credentials: 'include', //sends cookies or use 'same-origin' https://stackoverflow.com/a/34592377/9375488 https://github.com/github/fetch/issues/386
        body: (requestBody || method!="GET") ? JSON.stringify(requestBody) : null 
    }).then(async response => {
        if(!response.ok){
            return response.json().then(message => {
                let alertMsg = `Error ${response.status}`
                alertMsg = alertMsg.concat(`, ${message.detail}`)
                if(doAlert) alert(alertMsg)
                return null
            })
        }

        const txt = await response.clone().text() //doing .clone() avoids "stream already read" https://stackoverflow.com/a/54115314/9375488
        if(txt.length==0) {
            console.log("Response body is apparently empty")
            return true
        }
        
        let jsonObj
        try { jsonObj = await response.json() 
        } catch(e){
            console.log("Error converting to json->"+e+". doFetch will return undefined")
            jsonObj = undefined
        }
        return jsonObj
    }).catch(e => {
        alert("Fetch error ->"+e)
    })
    return x
}

export function getEventSourceStatus(eventSource: EventSource) {
    switch(eventSource.readyState) {
        case EventSource.CLOSED:
            return "Closed"
        case EventSource.CONNECTING:
            return "Connecting..."
        case EventSource.OPEN:
            return "Connected"
        default:
            return `Unknown (${eventSource.readyState})`
    }
}