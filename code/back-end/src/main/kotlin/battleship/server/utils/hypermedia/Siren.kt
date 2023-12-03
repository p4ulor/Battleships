// !!! Original by Paulo Pereira -> https://github.com/isel-leic-pdm/2223I-LEIC51D/blob/18acb661b605fc6095eb408acc2c1ffa689b9210/QuoteOfDay/app/src/main/java/isel/pdm/demos/quoteofday/utils/hypermedia/siren.kt
package battleship.server.utils.hypermedia

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity

private const val APPLICATION_TYPE = "application"
private const val SIREN_SUBTYPE = "vnd.siren+json"
const val SirenMediaType = "$APPLICATION_TYPE/$SIREN_SUBTYPE"

data class SirenEntity<T>(
    @JsonProperty("class") //renames the property name to "class" on json serialization
    val clas: List<SirenClasses>? = null,
    val properties: T? = null,
    val entities: List<SirenSubEntity<Any>>? = null,
    val links: List<Link>? = null,
    val actions: List<Action>? = null,
    val title: String? = null
) {
    data class SirenSubEntity<T>(
        val rel: List<SirenClasses>,
        @JsonProperty("class")
        val clas: List<String>? = null,
        val properties: T? = null,
        val entities: List<SirenSubEntity<T>>? = null,
        val links: List<Link>? = null,
        val actions: SirenActionMethods? = null,
        val title: String? = null
    )

    data class Action( //represent actions that are included in a siren entity
        val name: String,
        val href: String,
        val title: String? = null,
        @JsonProperty("class")
        val clas: List<String>? = null,
        val method: SirenActionMethods? = null,
        val type: MediaType? = null,
        val fields: List<Field>? = null
    ) {
        data class Field( //Represents action's fields
            val name: String,
            val type: SirenFields? = SirenFields.text,
            val value: String? = null,
            val title: String? = null
        )
    }
    data class Link( //represent links as they are represented in Siren.
        val rel: List<String>,
        val href: String,
        val title: String? = null,
        val type: MediaType? = null
    )
    fun selfLink(uri: String) = Link(rel = listOf("self"), href = uri)
}

fun <T> sirenResponse(body: SirenEntity<T>): ResponseEntity<Any> =
     ResponseEntity.status(200)
    .contentType(MediaType.parseMediaType(SirenMediaType))
    .body(body)
