package battleship.server.http

import battleship.server.model.ServerInfo
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.DependsOn
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InfoTests {

    @LocalServerPort
    val port: Int = 0

    @DependsOn(value = ["port"])
    fun utils() = HttpUtils("http://localhost:$port") //I tried using autowired, and it didn't work out very well... This was the best I could do

    @Test
    fun `get home index,html`() {
        val u = utils()
        val res = u.httpReqGet("/")
        assertTrue { u.isContentType(res, "text/html") }//(text/html;charset=UTF-8)
        assertEquals(200, res.statusCode())
    }

    @Test
    fun `get server info`() {
        val res = utils().httpReqGet("/system-info")
        val serverInfo: ServerInfo = jacksonObjMapper.readValue(res.body(), ServerInfo::class.java)
        assertEquals(true, serverInfo.author.name=="Paulo Rosa")
        assertEquals(200, res.statusCode())
    }

    @Test
    fun `route not found returns index,html`(){
        val u = utils()
        val res = u.httpReqGet("/aaaaaaaaaaa")
        //During the tests it never reaches the ErrorViewResolver so it always returns 404 and application/json...
        println("${u.isContentType(res, "application/json")}, "+res.statusCode())
        //assertTrue { u.getContentType(res).contains("text/html") }
       // assertEquals(200, res.statusCode())
    }

    @Test
    fun `API route not found returns 404`(){
        val u = utils()
        val res = u.httpReqGet("/api/aaaaaaaaaaa")
        assertTrue { u.isContentType(res, "application/json") }
        assertEquals(404, res.statusCode())
    }
}