package battleship.server.services

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

class Users {
    @TestConfiguration
    class Config {
        @Bean @Primary
        fun jdbi(){
        }
    }

}