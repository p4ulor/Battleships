//simplest way (storing in constant) to avoid doing GET requests everytime we play audio:
//const _click = setUpAudio(require('../../public/sfx/button-click.mp3')), 0.5)
//I tried making the requires lazy, but it didnt work when using a function, so it is like how it is (previously it looked simpler, but on the first request (and refresh), all sounds were sent to the client), see the commented out version of "setUpAudio()" near the end of this file

// Page sounds https://developer.mozilla.org/en-US/docs/Web/API/HTMLMediaElement
let _click: Sound
let _hover: Sound
let _waiting4guest: Sound
let _guestJoined: Sound
let _404: Sound

// Gaming sounds
let _localMiss: Sound
let _localFire: Sound
let _localFire2: Sound
let _localDamaged: Sound
let _localDamaged2: Sound
let _localDamagedSubmarine: Sound

let _distantMiss: Sound
let _distantHit: Sound
let _distantHit2: Sound

let _win: Sound
let _loss: Sound

// Page sounds
export function click() { 
    if(!_click) _click = setUpAudio(require('../../public/sfx/button-click.mp3'), 0.5)
    _click.play() 
}
export function hover() { 
    if(!_hover) _hover = setUpAudio(require('../../public/sfx/button-hover(GTA-SA).mp3'), 0.5)
    _hover.play() 
}

export function waitingForGuest(){
    return {
        play: () => {
            if(!_waiting4guest) _waiting4guest = setUpAudio(require('../../public/sfx/waiting4guest.ogg'), 0.5, true) //https://stackoverflow.com/a/57539551/9375488 https://forums.tumult.com/t/looping-sound-on-export-has-silent-gaps/16502/14 even then there's still a pause with .ogg
            _waiting4guest.play()
        },
        stop: () => {
            if(!_waiting4guest) return 
            _waiting4guest.stop()
        }
    }
}

export function guestJoined() {
    if(!_guestJoined) _guestJoined = setUpAudio(require('../../public/sfx/guest-joined(nextel-chirp-reverb).mp3'), 0.3)
    _guestJoined.play() 
}

export function s404() {
    if(!_404) _404 = setUpAudio(require('../../public/sfx/404.mp3'), 0.5) //vine boom sound effect ;D
    _404.play() 
}

// Gaming sounds
export function localMiss(){
    if(!_localMiss) _localMiss = setUpAudio(require('../../public/sfx/local-miss.mp3'), 0.5) 
    _localMiss.play() 
}
export function localFire(){ 
    if(randomBoolean()) {
        if(!_localFire) _localFire = setUpAudio(require('../../public/sfx/local-fire.mp3'), 0.3)
        _localFire.play()
    }
    else {
        if(!_localFire2) _localFire2 = setUpAudio(require('../../public/sfx/local-fire2.mp3'), 0.3)
        _localFire2.play()
    }
}
export function localDamaged(){ 
    if(randomBoolean()) {
        if(!_localDamaged) _localDamaged = setUpAudio(require('../../public/sfx/local-damaged.mp3'), 0.3)
        _localDamaged.play()
    }
    else {
        if(!_localDamaged2) _localDamaged2 = setUpAudio(require('../../public/sfx/local-damaged2.mp3'), 0.3)
        _localDamaged2.play() 
    }
}
export function localDamagedSubmarine(){
    if(!_localDamagedSubmarine) _localDamagedSubmarine = setUpAudio(require('../../public/sfx/local-damaged-submarine.mp3'), 0.5) //theres no localFire submarine because we cant control what ship shoots the shot
    _localDamagedSubmarine.play() 
}

export function distantMiss(){
    if(!_distantMiss) _distantMiss = setUpAudio(require('../../public/sfx/distant-miss.mp3'), 0.5)
    _distantMiss.play() 
}
export function distantHit(){ 
    if(randomBoolean()) {
        if(!_distantHit) _distantHit = setUpAudio(require('../../public/sfx/distant-hit.mp3'), 0.5)
        _distantHit.play()
    }
    else {
        if(!_distantHit2) _distantHit2 = setUpAudio(require('../../public/sfx/distant-hit2.mp3'), 0.5)
        _distantHit2.play()
    }
}

export function win(){
    if(!_win) _win = setUpAudio(require('../../public/sfx/win(kill-bill-siren).mp3'), 1)
    _win.play() 
}
export function loss(){
    if(!_loss) _loss = setUpAudio(require('../../public/sfx/loss(chucky-laugh).mp3'), 1)
    _loss.play() 
}

function setUpAudio(file: any, volume: number, loop: boolean = false){ //https://stackoverflow.com/a/14836099/9375488
    const sfx = new Audio(file)
    sfx.volume = volume
    sfx.currentTime
    sfx.loop = loop
    return {
        play: function(){
            sfx.pause(); sfx.currentTime = 0; sfx.play()
        },
        stop: function(){
            sfx.pause(); sfx.currentTime = 0;
        }
    }
}

//Lazy attempt, doesnt work because webpack adds some weird strings
/* function setUpAudioLazy(file: string, volume: number, loop: boolean = false){ //https://stackoverflow.com/a/14836099/9375488
    let hasBeenPlayed = false
    let sfx = )
    sfx.volume = volume
    sfx.currentTime
    sfx.loop = loop
    return {
        play: function(){
            if(!hasBeenPlayed) {
                sfx = require(`${file}`)) //required or it webpack says Critical dependency: the request of a dependency is an expression (because require needs some form of a fixed string)
                hasBeenPlayed = true
            }
            sfx.pause(); sfx.currentTime = 0; sfx.play()
        },
        stop: function(){
            if(hasBeenPlayed) {
                sfx.pause(); sfx.currentTime = 0;
            }
        }
    }
} */

//do stop all other audio before a sound plays function? or even a queue using the currentTime and duration?

function randomBoolean() {
    const random = Math.random()
    return Math.round(random*10)%2.0==0
}

interface Sound {
    play: () => void
    stop: () => void
}