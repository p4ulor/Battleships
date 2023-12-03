package battleship.server.http

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.test.web.reactive.server.WebTestClient
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*

val jacksonObjMapper = ObjectMapper().registerKotlinModule()

class HttpUtils(private val baseURL: String){
    init { println("Base url = $baseURL") }

    val uri: (path: String) -> URI = { path ->
        if(path.first()!='/') throw IllegalArgumentException("Path must start with '/'")
        URI("${baseURL}$path")
    }

    fun springReqGet(path: String): WebTestClient.BodyContentSpec {
        val client = WebTestClient.bindToServer().baseUrl(baseURL).build()
        return client.get().uri(uri(path))
            .exchange()
            //.expectStatus().isOk
            .expectBody()
        //.jsonPath("links[0].rel").isEqualTo("self")
        //.expectStatus().value(equalTo(500))
        //            .expectHeader().doesNotExist("Content-Type")
        //            .expectStatus().isCreated
        //            .expectHeader().value("location") {
        //                assertTrue(it.startsWith("/users/"))
        //            }
    }

    fun springReqWrite(path: String, body: String, method: Method = Method.POST) : WebTestClient.ResponseSpec {
        var client = WebTestClient.bindToServer().baseUrl(baseURL).build()
        var c = when(method){
            Method.PUT -> client.put()
            Method.POST -> client.post()
            else -> null
        } ?: return client.delete().uri(path).exchange()
        return c.uri(path).bodyValue(body).exchange()
    }

    fun httpReqGet(path: String): HttpResponse<String> {
        val client: HttpClient = HttpClient.newHttpClient()
        return client.send(
            HttpRequest.newBuilder().uri(uri(path)).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        )
    }

    fun httpReqWrite(path: String, body: String, method: Method = Method.POST): HttpResponse<String> {
        val client: HttpClient = HttpClient.newHttpClient()
        var req = HttpRequest.newBuilder().uri(uri(path)).setHeader("Content-type", "application/json")
        if(method==Method.DELETE) req.DELETE()
        if(method==Method.PUT) req.PUT(HttpRequest.BodyPublishers.ofString(body)) //https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpRequest.BodyPublishers.html
        else req.POST(HttpRequest.BodyPublishers.ofString(body))
        return client.send(req.build(), HttpResponse.BodyHandlers.ofString())
    }

    fun isContentType(res: HttpResponse<String>, value: String) : Boolean {
        return (res.headers().firstValue("Content-Type") as Optional).get().contains(value) //im using contains to be more flexible
    }
}

enum class Method {
    POST, PUT, DELETE
}
