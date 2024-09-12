import * as React from 'react'
import * as GameModel from './GameObjs'

function Square ( //A React Element (similar to the familiar HTML elements). Must start with uppercase letter and have a return of JSX
    {position, onClick} : { //defines HTML Attributes
        position: GameModel.Position,
        onClick?: Function
    }){
    
    const ay = () : void => {
        console.log("Clicked Square")
        position.entity.sfx()
    }

    return (
        <button className="square" onClick={(onClick) ? () => onClick(ay) : ay/* ()=>{} */}>
            {position.entity.name}
        </button>
    )
}

export function Board() {
    const [positions, setPositions] = React.useState<GameModel.Position[]>(() => { return initBoard() })

    /* const [dimColumn] = React.useState(3)
    const [dimRow] = React.useState(3) */

    function initBoard() : GameModel.Position[] {
        const dimColumn = 3
        const dimRow = 3
        const board =  Array<GameModel.Position>()
        let column = 0
        let row = 0
        for(var i = 0; i < dimColumn*dimRow; i++){
            board.push(new GameModel.Position(column, row, GameModel.Entity.WATER))
            if(dimColumn==column) column = 0
            if(dimRow==row) row = 0
        }
        console.log(`inited board -> ${JSON.stringify(board)}`)
        return board
    }

    const renderSquare = (i: number) => {
        return (
          <Square
            position={positions[i]}
            /* onClick={
                function() {
                console.log("ay")
                } 
            } */
          />
        );
    }
    
    return (
        <div>
        <div className="status">{status}</div>
        <div className="board-row">
            {renderSquare(0)}
            {renderSquare(1)}
            {renderSquare(2)}
        </div>
        <div className="board-row">
            {renderSquare(3)}
            {renderSquare(4)}
            {renderSquare(5)}
        </div>
        <div className="board-row">
            {renderSquare(6)}
            {renderSquare(7)}
            {renderSquare(8)}
        </div>
        </div>
    );
}
