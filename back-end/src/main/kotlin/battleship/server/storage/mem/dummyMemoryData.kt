package battleship.server.storage.mem

import battleship.server.model.*

val users = mutableListOf(
    User(1, "goncalo","goncalo@gmail.com",
         "0f165f7e869c8d6f08ae67b14ba0cf3f23fa76bed822c369", //passoword=1234. INGAME
         "8304c3c9-c4ca-4b1a-848c-74b5f415a62f",
         3, 1),
    User(2, "pedro", "pedro@gmail.com",
         "e5bc22d238c73b81fd8f36ef38aadde3149eb160fc3d2f51", //password=4321. INGAME
         "c9c49334-b452-11ec-b909-0242ac120002",
         1, 3),
    User(3, "paulo", "paulo@gmail.com",
         "85b082f620662a8946b16999652a57aa60ad9ab129cb3e01", //password=ay. INGAME
         "d83a659c-b452-11ec-b909-0242ac120002",
         2, 2),
    User(4, "chico", "chico@gmail.com",
         "4f160128a389edda5cb7edb9e6351c90d11fce4d96e3cd45", //passwords are equal to the names from here down. INGAME
         "359fcc7e-3c63-4258-8840-30e408494ba0",
         1, 0),
    User(5, "joao", "joao@gmail.com",
         "81808e683d1b3d3aa6b44dac97c60d7ed969a187dfd6589c", //INGAME
         "4529bd41-9b91-4a53-b191-02750487af86",
         0, 1),
    User(6, "samuel", "samuel@gmail.com",
         "f2b0dfff4d87c6dc5460de35e298c4b1109490dba6038b59", //INGAME
         "d053910e-6628-4608-9750-abc7cef4abb3"),
    User(7, "carlos", "carlos@gmail.com",
        "891f9b07bc5e960b5be0c2bf50c04655975ff91af2a31915", //INGAME
        "7e5d9c41-de11-4713-aac2-e0ab3d82f85c"),
    User(8, "francisco", "francisco@gmail.com",
        "545fb057d10c17c2a8e7a0d4a892b0d5e1b64dd580bc42af", //not in any game
        "a5a39533-b186-4fd5-b6cf-29296d6a33f6"),
    User(9, "rogerio", "rogerio@gmail.com",
        "4ab0fcd6eb291b18f10ad666e7cba5f9b1c2459a42c98ae6", //not in any game
        "061bebd1-0d4b-43e4-b2d2-34f1be9e0cab"),
    User(10, "rita", "rita@gmail.com",
        "f4f359fa54779637955e161e2f137c7b7969f0f10f172933", //INGAME
        "8053d861-7bb8-497b-be12-da26eaa0d9ba"),
    User(11, "filipa", "filipa@gmail.com",
        "cae836ca96e05bdf0b35535f5faaf3c49a26eb45d5c9534b", //INGAME
        "2f9266c0-c74e-4fbf-9818-2d7afea356a9"),
    User(12, "sandra", "sandra@gmail.com",
        "6e0c123d9b396ee5b65d66c5fec04f884d8c0d9d9e3d2c74", //INGAME
        "812c5f8a-19ad-4a60-9e43-712e4e1caadb"),
    User(13, "joana", "joana@gmail.com",
        "ebbbd96634024affec43c5ad1deaed4105c313686b56661e", //INGAME
        "8d3506ef-b773-4f96-9dbc-1c880d0e05d1"),
    User(14, "miguel", "miguel@gmail.com",
        "f76954a5dc74ee01db73b81205e6d1dcfd3d34d7c0e3be42", //not in any game
        "6dd0353d-695e-47e8-9d7f-89e4068cb69a"),
    User(15, "alexandre", "alexandre@gmail.com",
        "46c3aa658cbe8b4fc4b0f05f50dc5b6d34df32bf670caf9d", //not in any game
        "6fe0068f-70ed-4ef5-9be1-b47f461f37f8"),
    User(16, "diogo", "diogo@gmail.com",
        "278e288c1b405d561958b19edc18ab241b61b6fdc5e48676", //not in any game
        "60d77658-05a6-40a4-8502-02b15581cdaf")
)

val ship1 = Ship(ShipType.Destroyer, Position(1,1, Entity.SHIP), Direction.UP).also {
    it.positionsOccupying.add(Position(1, 1, Entity.SHIP))
    it.positionsOccupying.add(Position(1, 2, Entity.SHIP))
}

val ship2 = Ship(ShipType.Submarine, Position(3,1, Entity.SHIP), Direction.RIGHT).also {
    it.positionsOccupying.add(Position(3, 1, Entity.SHIP))
    it.positionsOccupying.add(Position(2, 1, Entity.SHIP))
    it.positionsOccupying.add(Position(1, 1, Entity.SHIP))
}

val games = mutableListOf(
    Game(1, "gonçalo e pedro", 1, 2, gameStatus = GameStatus.HOST_TURN, isHostReady = true, isGuestReady = true,
        hostShips = mutableListOf(ship1),
        guestShips = mutableListOf(ship2)
    ),
    Game(2, "paulo lobby", 3),
    Game(3, "chico lobby", 4),
    Game(7, "carlos lobby", 7),
    Game(4, "joao e samuel", 5, 6, gameStatus = GameStatus.SHIPS_SETUP, isHostReady = true, isGuestReady = true),
    Game(5, "rita e filipa", 10, 11, Rules(shipsAllowed = defaultShipTypes.toMutableList().subList(3,5)), gameStatus = GameStatus.SHIPS_SETUP),

    Game(6, "sandra e joana", 12, 13, isHostReady = true, isGuestReady = true, gameStatus = GameStatus.SHIPS_SETUP).also {
        //it.setShips()
    }
)

//starting ID's from 1 to match with posgresql (because the value must be 1 or greater)
fun nextUserID() = users.size+1
fun nextGameID() = games.size+1
