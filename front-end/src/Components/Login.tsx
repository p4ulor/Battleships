import * as React from 'react'

import * as UtilsFetch from '../utils/Utils-Fetch'
import * as Utils from '../utils/Utils'
import * as Body from '../utils/Req-Res-Bodies'
import * as SFX from '../utils/SFXManager'

import { userNameContext } from '../Pages/Home'

const postRegister_URI = "/users/newuser"
const postLogin_URI = "/users/login"

export function LoginButton(){
    const [isDialogOpen, setDialogOpen] = React.useState(false)
    const [loginButtonText, setLoginButtonText] = React.useState(Utils.isLoggedIn() ? "Logout" : "Login")
    const ctx = React.useContext(userNameContext)

    function open(){
        setDialogOpen(true)
    }

    function close(){
        setDialogOpen(false)

        //https://stackoverflow.com/questions/63518293/animation-for-react-select-menu-open-close inspired by this but altered because the answer has an error. You can only do "classList.add" on HTML Element not on a Node
        const loginDialogBox = document.querySelector(".overLappingContainer") as HTMLElement //https://developer.mozilla.org/en-US/docs/Web/API/Document/querySelector#finding_the_first_element_matching_a_class
        const parent = loginDialogBox.parentElement
        loginDialogBox.classList.add("overLappingContainer-closed")
        let clone = loginDialogBox.cloneNode(true)
        clone.addEventListener("animationend", () => {
            parent.removeChild(clone)
        })
        parent.appendChild(clone)
    }

    function setIsLoggedIn(isLoggedIn: boolean){
        //console.log(`setIsLoggedIn -> ${isLoggedIn}`)
        if(isLoggedIn) setLoginButtonText("Logout")
        else setLoginButtonText("Login")
    }

    function decide() : JSX.Element {
        let login: JSX.Element = null
        if(isDialogOpen) login = <LoginDialogBox onClose={close} setIsLoggedIn={setIsLoggedIn}/>
        console.log(`isDialogOpen -> ${isDialogOpen}`)
        
        return(
        <div>
            <button className="login" style={{
                float:"right",
                marginLeft: 10
                }}
                onClick={(e) => { 
                    SFX.click()
                    console.log(`Loggin button onClick: is logged in -> ${Utils.isLoggedIn()}`)
                    if(Utils.isLoggedIn()) { //then, logout
                        ctx.myUserName = undefined
                        setLoginButtonText("Login")
                        Utils.deleteCookies() 
                    }
                    else open() 
                }}
                onMouseEnter={SFX.hover}>
                {loginButtonText}
            </button>
            {login}
        </div>
        )
    }

    return(decide())
}

export function LoginDialogBox(
    {onClose, setIsLoggedIn} : { //These are read only attribute params (that are set during element creation), do not use these to store mutable state. The alternative to this would be using React.useContext
        onClose: Function,
        setIsLoggedIn: Function
    }){

    const ctx = React.useContext(userNameContext)
    const [isSubmitButtonDisabled, setIsSubmitButtonDisabled] = React.useState(true)

    const userName = React.useRef(undefined) //The alternative to this would be to use onChange and affect the variables in a useState hook-variable, but it would cause a re-render unless controlled by a dependency/condition https://bobbyhadz.com/blog/react-get-input-value https://beta.reactjs.org/apis/react/useRef
    const email = React.useRef(undefined) // when the value changes, it doesn't cause a re-render
    const password = React.useRef(undefined)
    
    const [registerMode, setRegisterMode] = React.useState(false)
    const [boxHeaderText, setBoxHeaderText] = React.useState("Login")

    React.useEffect(() => {
        const thisElement = document.getElementById("LoginDialogBox")
        //window.getComputedStyle(thisElement).getPropertyValue('z-index')

        Utils.setBackgroundUnclickable(thisElement)
        
        const listener = (event: MouseEvent) => {
            const isClickInside = thisElement.contains(event.target as Node) //https://stackoverflow.com/a/28432139/9375488 https://stackoverflow.com/a/71193834/9375488
            console.log("is click inside LoginDialogBox: "+isClickInside)
        }

        document.body.addEventListener("click", listener)

        return () => {
            console.log('LoginDialogBox will unmount')
            document.body.removeEventListener("click", listener)
            Utils.resetClickable()
        }
    }, [])

    React.useEffect(() => {
        autoSelectFirstInput()
    }, [registerMode])

    function autoSelectFirstInput(){ //https://stackoverflow.com/a/10894719/9375488
        document.getElementById("LoginDialogBox").getElementsByTagName("input")[0].select()
    }

    function tryLogin(){ //the form automatically sees if the inputs are filled
        Utils.log(userName.current.value, password.current.value)
        setBoxHeaderText("Loading")
        async function doFetch(){
            const jsonRsp = await UtilsFetch.doFetch(postLogin_URI, "POST", new Body.LoginUserRequest(userName.current.value, password.current.value))
            if(jsonRsp){
                setBoxHeaderText("Success")
                setIsLoggedIn(true)
                ctx.myUserName = userName.current.value
                setTimeout(() => { //necessary or 'Success' text change (if performed) wont show before the closing animation
                    onClose()
                }, 1000)
            } else setBoxHeaderText("Try again")
        }
        doFetch()
    }

    function tryRegister(){
        setBoxHeaderText("Loading")
        Utils.log(userName.current.value, email.current.value, password.current.value)
        async function doFetch(){
            const jsonRsp = await UtilsFetch.doFetch(postRegister_URI, "POST", new Body.CreateUserRequest(userName.current.value, email.current.value, password.current.value))
            if(jsonRsp){ //if it's not null or undefined
                setBoxHeaderText("Registered")
                setIsLoggedIn(true)
                ctx.myUserName = userName.current.value
                setTimeout(() => { //necessary or 'Success' text change (if performed) wont show before the closing animation
                    onClose()
                }, 1000)
            } else setBoxHeaderText("Try again")
            
        }
        doFetch()
    }

    function cleanUpValues(){ //set to "" because, otherwise, it shows "undefined" on the fade out animation-transition
        userName.current.value = ""
        if(registerMode) email.current.value = ""
        password.current.value = ""
    }

    function areFieldsValid() : boolean {
        if(registerMode){
            if(email.current.value==0 || !new String(email.current.value).includes("@")) return false
        }
        if(userName.current.value.length==0 || password.current.value.length==0) return false
        return true
    }

    function handleUserInput (e: React.ChangeEvent<HTMLInputElement>) {
        /* const name = e.target.name; const value = e.target.value */
        if(areFieldsValid()) setIsSubmitButtonDisabled(false)
        else setIsSubmitButtonDisabled(true)
    }

    function sortOutReturn() : JSX.Element{ //https://stackoverflow.com/a/64962453/9375488 https://learnetto.com/blog/react-form-validation
        console.log("LoginDialogBox return ran")
        let registerOrLoginBox_name_or_email: JSX.Element = null
        if(registerMode) registerOrLoginBox_name_or_email = 
        <>
            <b>Username</b>
            <input type="text" placeholder="Enter Username" name="username" ref={userName} onChange={(e) => handleUserInput(e)}/>
            <b>Email</b>
            <input type="text" placeholder="Enter Email" name="email" ref={email} onChange={(e) => handleUserInput(e)}/>
        </>

        else registerOrLoginBox_name_or_email = 
        <>
            <b>Username or Email</b>
            <input type="text" placeholder="Username/email" name="username" title="email or password" ref={userName} onChange={(e) => handleUserInput(e)}/> {/* Adding atribute 'name' or 'id' will allow caching of previously insered values (and will show small box when selecting the input box) of associated inputs with same name or id */}
        </>

        let changeTologinOrRegisterButton: JSX.Element
        if(registerMode) changeTologinOrRegisterButton = <button type="button" className="aside" onClick={ () => { setRegisterMode(false) } } > Login </button> 
        else changeTologinOrRegisterButton = <button type="button" className="aside" onClick={ () => { setRegisterMode(true) } } > Register </button> 

        let loginOrRegisterButton: JSX.Element
        if(registerMode) loginOrRegisterButton = <button type="submit" disabled={isSubmitButtonDisabled} onClick={ (e)=> {e.preventDefault(); tryRegister()}}> Register </button> //Note: preventDefault() will also make typing the property 'required' in <inputs> to not take effect. So the mini-box with the exclamation point saying 'please fill out this field' will not show
        else loginOrRegisterButton = <button type="submit" disabled={isSubmitButtonDisabled} onClick={ (e)=> {e.preventDefault(); tryLogin() }}> Login </button> //the 'type="submit"' tells the browser that this is the form's "handler" for submitting the input. The 'e.preventDefault()' prevents the default behaviour of the page being reloaded once the fetch request or a submit is executed.
        return(
            <div className="overLappingContainer" id="LoginDialogBox">
                <form onSubmit={ (e) => { e.preventDefault() }}>
                    <h1>{boxHeaderText}</h1>
                    
                    {registerOrLoginBox_name_or_email}
                    
                    <b>Password</b>
                    <input type="password" placeholder="Enter Password" name="password" ref={password} onChange={(e) => handleUserInput(e)}/>
    
                    {loginOrRegisterButton}
                    <button type="button" onClick={ ()=> { cleanUpValues(); onClose() }}>Close</button>
                    {changeTologinOrRegisterButton}
                    {/* <Dialog isOpen={true} onClose={() => {console.log("atribute onClose")}}></Dialog> */}
                </form>
            </div>
        )
    }

    return(sortOutReturn())
}

//DEMONSTRATION / EXPERIMENTS:

interface DialogProps {
    isOpen: boolean,
    onClose?: Function
    //children: /React.ReactNode /any
}

/* Uncommenting: 'children: any' will allow doing:
<Dialog ... >
          <elements> ...  or plain text
</Dialog>
*/

//https://stackoverflow.com/questions/39123667/react-typescript-extending-a-component-with-additional-properties
//https://stackoverflow.com/questions/44707634/typesscript-wrongly-assumes-that-property-is-read-only
//             Properties/custom-HTML Atributes | State (internal variables)
class Dialog extends React.Component<DialogProps, DialogProps> { //Alternative way of creating components
    constructor(props: DialogProps) {
        super(props)
        this.state = {
            isOpen: false,
            onClose: () => {
                console.log("state-function onClosed")
            }
        }
    }
    
    switch(){
        console.log("Dialog called close()")
        this.setState({
            isOpen: !this.state.isOpen
        })
    }

    render() { //extending from Component allows us to declare functions like this apparently
        let dialog = (
            <div className="overLappingContainer">
                <button className="xbutton" onClick={() => { this.state.onClose(); this.switch(); this.props.onClose() }}>
                    x
                </button>
            </div>
        )

        if (! this.state.isOpen) {
            dialog = (
                <div className="overLappingContainer">
                    <button className="xbutton" onClick={() => { this.state.onClose(); this.switch(); this.props.onClose()}}>
                        O
                    </button>
                </div>
            )
        }
        return (
            <div>
                {dialog}
            </div>
        );
    }
}

