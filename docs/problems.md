This document describes the various errors that can be returned from an HTTP request to my API.

This was part of the curricular subject's contents:
- https://labs.pedrofelix.org/notes/http/how-to-fail

# Request-Error

## NotFoundException
- When a user or game isn't found

## BadRequestException
- There can be multiple reasons for this error. It's mostly related to creating a gamee, setting up a board or shooting a shot

## AuthorizationException
When a user tries to access the data of a non-completed game

## ForbiddenException
You are not authorized to do an action regardless of authentication. There can be multiple reasons for this error.. Like inserting a ship type that is not included in the rules or try to shoot when it's not your turn. Joining a game that is already occupied. Trying to quit or shoot a shot on game that you're not part of, etc.

## ConflictException
- A request to create a user was made, but the email or name are already in use

## InternalServerErrorException
- An exception that wasn't checked has occured

# Body-Error

## BadRequestException: TypeMismatchException
When there are type mismatches in request bodies or invalid params or paths in URI's

## BadRequestException: MethodArgumentNotValidException
The request body contains values that are not valid and go against the rules and logic of the game

## BadRequestException: HttpMessageNotReadableException
When there are missing or invalid params

## Bad Request: HttpRequestMethodNotSupportedException
When you make a request using a POST to a path that should be called with a GET per example
```json
"title": "Bad Request: HttpRequestMethodNotSupportedException",
"detail": "Error=org.springframework.web.HttpRequestMethodNotSupportedException: Request method 'POST' not supported",
"objectsInvolved": null,
"type": "https://github.com/isel-leic-daw/2022-daw-leic52d-g05-1/tree/main/docs/problems.md#Body-Error",
"api_path": "/"
```

# Examples

## Tried creating game without token
```json
"title": "403 FORBIDDEN",
"detail": "Authorization header missing",
"objectsInvolved": null,
"type": "https://github.com/isel-leic-daw/2022-daw-leic52d-g05-1/tree/main/docs/problems.md#Request-Error",
"api_path": "/setup/newgame"
```

## Ship type doesn't exist
```json
"title": "400 BAD_REQUEST",
"detail": "ShipType doesn't exist. Ship type 'aaaaa' doesn't exist",
"objectsInvolved": null,
"type": "https://github.com/isel-leic-daw/2022-daw-leic52d-g05-1/tree/main/docs/problems.md#Request-Error",
"api_path": "/setup/newgame"
```

## Wrong passowrd
```json
"title": "403 FORBIDDEN",
"detail": "Wrong Password",
"objectsInvolved": null,
"type": "https://github.com/isel-leic-daw/2022-daw-leic52d-g05-1/tree/main/docs/problems.md#Request-Error",
"api_path": "/users/login"
```

## BoardSetupRequest is invalid
```json
"title": "MethodArgumentNotValidException",
"detail": "Error in the field(s):
{ships[0].head.column: 'null'}, Rejected because -> must not be null;
{ships[0]._isDirectionValid: 'false'}, Rejected because -> Valid values for field 'direction' (not case sensitive) -> UP, LEFT, RIGHT, DOWN;",
"objectsInvolved": null,
"type": "https://github.com/isel-leic-daw/2022-daw-leic52d-g05-1/tree/main/docs/problems.md#Request-Error",
"api_path": "/setup/board"
```

## Password field is missing
```json
"title": "Bad Request: HttpMessageNotReadableException",
"detail": "Missing or invalid param, Rejected because -> value failed for JSON property password due to missing (therefore NULL) value for creator parameter 'password' which is a non-nullable type",
"objectsInvolved": null,
"type": "https://github.com/isel-leic-daw/2022-daw-leic52d-g05-1/tree/main/docs/problems.md#Body-Error",
"api_path": "/users/newuser"
```

## A field in user login fails the regex
```json
"title": "Bad Request: MethodArgumentNotValidException",
"detail": "Error in the field(s): {emailOrName: ' '}, Rejected because -> Fields can't be empty or blank;",
"objectsInvolved": null,
"type": "https://github.com/isel-leic-daw/2022-daw-leic52d-g05-1/tree/main/docs/problems.md#Body-Error",
"api_path": "/users/login"
```

## Quit and not be part of any game
```json
"title": "403 FORBIDDEN",
"detail": "You Are Not Part Of Any On Going Game",
"objectsInvolved": null,
"type": "https://github.com/isel-leic-daw/2022-daw-leic52d-g05-1/tree/main/docs/problems.md#Request-Error",
"api_path": "/play/quit"
```

## Game not found
```json
"title": "404 NOT_FOUND",
"detail": "Game doesn't exist",
"objectsInvolved": null,
"type": "https://github.com/isel-leic-daw/2022-daw-leic52d-g05-1/tree/main/docs/problems.md#Request-Error",
"api_path": "/setup/joingame"
```

## User tried to submit board while in-game
```json
"title": "403 FORBIDDEN",
"detail": "You Can No Longer Change Your Board.",
"objectsInvolved": null,
"type": "https://github.com/isel-leic-daw/2022-daw-leic52d-g05-1/tree/main/docs/problems.md#Request-Error",
"api_path": "/setup/board"
```
