package battleship.server.utils.hypermedia


enum class SirenClasses { //https://github.com/kevinswiber/siren#class-3
    collection, //property classes in siren should have at the last position either 1 or the other (not both) collection OR item to indicate if the object is a list or a single object
    item,
    player,
    in_game_player,

}

enum class SirenActionMethods { //https://github.com/kevinswiber/siren#method
    GET,
    PUT,
    POST
}

enum class SirenFields { //https://github.com/kevinswiber/siren#type-3
    hidden,
    text,
    search,
    tel,
    url,
    email,
    password,
    datetime,
    date,
    month,
    week,
    time,
    datetime_local,
    number,
    range,
    color,
    checkbox,
    radio,
    file,
    graph
}
