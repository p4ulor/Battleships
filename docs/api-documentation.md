# API Documentation
Simpler documentation that was allowed to do at the time instead of using [Swagger UI](https://swagger.io/tools/swagger-ui/), like it was done for the *Software Lab* in the [Sports Manager](https://github.com/p4ulor/Sports-Manager) project

## GET - /system-info
```
ServerInfo(version=0.0.1, author=Author(name=Paulo Rosa, email=a44873@alunos.isel.pt, id=44873))
```
___
# /users
## POST - /users/newuser
### Request
```json
"name": "newusername",
"email": "newuser@gmail.com",
"password": "newpassword"
```

### Response
```json
"token": "061bebd1-0d4b-43e4-b2d2-34f1be9e0cab"
```
___
## GET - /users/{id}
### Response
```json
"id": 3,
"name": "user",
"playCount": 0,
"winCount":  0
```
___
## POST - /users/login
### Request
```json
"emailOrName": "newusername",
"password": "newpassword"
```

### Response
```json
"token": "061bebd1-0d4b-43e4-b2d2-34f1be9e0cab"
```
___
## GET - /users/ranking/{scheme}
- If {scheme}==played, orders players by games played, else orders by wins
- Accepts {paging} and {limit} query params
### Response
```json
[
    {
        "id": 2,
        "name": "pedro",
        "playCount": 1,
        "winCount": 3
    },
    {
        "id": 3,
        "name": "paulo",
        "playCount": 2,
        "winCount": 2
    },
    {
        "id": 5,
        "name": "joao",
        "playCount": 0,
        "winCount": 1
    }
]
```
___
# /setup
## POST - /setup/newgame
- Requires valid Bearer token
- Lobby name is the only mandatory param. The others can be null and the server will use default values. But if the values the user inserted are wrong, there will be a reply indicating that it's a bad request
### Request
```json
 "lobbyName": "newlobby",
   "rules": {
       "columnDim": 2,
       "rowDim": 2,
       "shotsPerRound": 1,
       "shipsAllowed": [
            {
                "shipType": "Submarine",
                "quantityAllowed": 1
            }, 
            {
                "shipType": "Carrier",
                "quantityAllowed": 5
            },
       ],
       "setupTime": 5,
       "timeToMakeMove": 5
       "doAllShipTypesNeedToBeInserted": false
   }
```

### Response
- The value of the dimension has more priority than the sizes of the ships
```json
"gameID": 1,
"rules": {
    "dimension": {
        "columnDim": 2,
        "rowDim": 2,
    },
    "shotsPerRound": 1,
    "shipsAllowed": [
        {
            "shipType": "Submarine",
            "quantityAllowed": 1,
        }
    ],
    "setupTime": 5,
    "timeToMakeMove": 5,
    "doAllShipTypesNeedToBeInserted": false
}
```
___

## GET - /setup/opengames
- Accepts {paging} and {limit} query params
### Response
```json
[
    {
        "gameID": 2,
        "lobbyName": "host waiting for guest"
    }
]
```
___

## PUT - /setup/joingame
- Requires Bearer token
### Request
```json
"gameID": 1
```

### Response 
```json
"opponentName": "host name",
"rules": {
    "dimension": {
        "columnDim": 2,
        "rowDim": 2,
    },
    "shotsPerRound": 1,
    "shipsAllowed": [
        {
            "shipType": "Submarine",
            "quantityAllowed": 1,
        }
    ],
    "setupTime": 5,
    "timeToMakeMove": 5,
    "doAllShipTypesNeedToBeInserted": false
}
```
___
## POST /setup/board
- Requires token
- Will search for a on going game the user is on
- If setReady is not present it will be true by default
### Request
```json
"ships": [
        {
            "shipType": "Destroyer",
            "head": {
                "column": 1,
                "row": 1
            },
            "direction": "Up"
        }
    ], 
"setReady": = true
```

### Response

```json
"canAddMore": false,
    "shipsBuilt": [
        {
            "shipType": "Destroyer",
            "head": {
                "column": 1,
                "row": 1,
                "entity": "SHIP"
            },
            "direction": "UP",
            "positionsOccupying": [
                {
                    "column": 1,
                    "row": 1,
                    "entity": "SHIP"
                },
                {
                    "column": 1,
                    "row": 2,
                    "entity": "SHIP"
                }
            ]
        }
    ],
    "shipTypesAndQuantityLeft": []
```
___
# /play
## POST /play/shoot
### Request 
```json
"position": {
        "column": 1,
        "row": 2
    }
```

### Response
```json
"position": {
        "column": 1,
        "row": 2
    },
"shotResult": "MISS"
```
___
# GET - /play/update
- Requires Bearer token
- Gets data the user is authorized to see about the game he's playing at
- Used when a guest joins, and the host user can know the apponent's name. Also, during gameplay, each one can know the changes of states of the game and the result of the apponent's on each other ships
### Response
```json
 "opponentName": "miguel",
    "gameStatus": "GUEST_TURN",
    "stateOfMyShips": [
        {
            "shipType": "Destroyer",
            "head": {
                "column": 1,
                "row": 1,
                "entity": "SHIP"
            },
            "direction": "UP",
            "positionsOccupying": [
                {
                    "column": 1,
                    "row": 1,
                    "entity": "SHIP"
                },
                {
                    "column": 1,
                    "row": 2,
                    "entity": "SHIP"
                }
            ]
        }
    ]
```
___
## GET - /play/update?gameID=1
- Gets game that is finished, specially meant for the players involved
### Response
```json
"id": 1,
"lobbyName": "in da game",
"hostID": 16,
"guestID": 8,
"rules": {
    "dimension": {
        "columnDim": 2,
        "rowDim": 2,
        "numOfPositions": 4
    },
    "shotsPerRound": 1,
    "shipsAllowed": [
        {
            "shipType": "Destroyer",
            "quantityAllowed": 1
        }
    ],
    "setupTime": 5,
    "timeToMakeMove": 5,
    "doAllShipTypesNeedToBeInserted": false
},
"gameCreationDateTime": "2022-11-03T13:44:46.7703773",
"isHostReady": true,
"isGuestReady": true,
"gameStatus": "WINNER_IS_HOST",
"hostShips": [
    {
        "shipType": "Destroyer",
        "head": {
            "column": 2,
            "row": 1,
            "entity": "SHIP"
        },
        "direction": "UP",
        "positionsOccupying": [
            {
                "column": 2,
                "row": 1,
                "entity": "SHIP"
            },
            {
                "column": 2,
                "row": 2,
                "entity": "SHIP"
            }
        ]
    }
],
"hostShots": [
    {
        "position": {
            "column": 1,
            "row": 1,
            "entity": "WATER"
        },
        "result": "HIT"
    },
    {
        "position": {
            "column": 1,
            "row": 2,
            "entity": "WATER"
        },
        "result": "HIT"
    }
],
"guestShips": [
    {
        "shipType": "Destroyer",
        "head": {
            "column": 1,
            "row": 1,
            "entity": "SHIP"
        },
        "direction": "UP",
        "positionsOccupying": [
            {
                "column": 1,
                "row": 1,
                "entity": "DAMAGED"
            },
            {
                "column": 1,
                "row": 2,
                "entity": "DAMAGED"
            }
        ]
    }
],
"guestShots": [
    {
        "position": {
            "column": 1,
            "row": 1,
            "entity": "WATER"
        },
        "result": "MISS"
    }
]
```

## PUT /play/quit/
- Requires token
- Quit game a user is playing at
- Returns ok on success