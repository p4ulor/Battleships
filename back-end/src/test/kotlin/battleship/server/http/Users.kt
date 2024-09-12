package battleship.server.http

import battleship.server.utils.CreateUserRequest
import battleship.server.utils.LoginUserRequest
import battleship.server.utils.UserTokenAndIDResponse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.DependsOn
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UsersTests {

    @LocalServerPort
    val port: Int = 0

    @DependsOn(value = ["port"])
    fun utils() = HttpUtils("http://localhost:$port")

    @Test
    fun `create a user`(){ //integrados
        val u = utils()
        val res = u.httpReqWrite(
            "/users/newuser",
            jacksonObjMapper.writeValueAsString(CreateUserRequest("goncalo123", "goncalo222@gmail.com", "gon"))
        )
        assertDoesNotThrow { jacksonObjMapper.readValue(res.body(), UserTokenAndIDResponse::class.java) }
        assertTrue { u.isContentType(res, "application/json") }
        assertEquals(200, res.statusCode())
    }

    @Test
    fun `login user`(){
        val u = utils()
        val res = u.httpReqWrite(
            "/users/login",
            jacksonObjMapper.writeValueAsString(LoginUserRequest("paulo", "ay"))
        )
        assertDoesNotThrow { jacksonObjMapper.readValue(res.body(), UserTokenAndIDResponse::class.java) }
        assertTrue { u.isContentType(res, "application/json") }
        assertEquals(200, res.statusCode())
    }

}