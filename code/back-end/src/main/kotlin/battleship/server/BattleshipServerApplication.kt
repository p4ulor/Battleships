package battleship.server

import battleship.server.model.Author
import battleship.server.model.ServerInfo
import battleship.server.utils.NotFoundException
import battleship.server.utils.pl
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.postgres.PostgresPlugin
import org.jdbi.v3.sqlobject.SqlObjectPlugin
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpStatus
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.util.ContentCachingRequestWrapper
import java.io.IOException
import java.sql.SQLException
import java.util.*
import java.util.function.Supplier
import java.util.stream.Collectors
import java.util.stream.Stream
import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@SpringBootApplication(
    exclude = [
        SecurityAutoConfiguration::class, //turns off weird login page
        /*DataSourceAutoConfiguration::class*/ //Putting this solved error 'Failed to configure a DataSource'. Turns out it was caused by havint the dependency 'spring-boot-starter-jdbc' https://stackoverflow.com/q/51221777/9375488    https://stackoverflow.com/q/28042426/9375488     https://www.baeldung.com/spring-boot-failed-to-configure-data-source https://javarevisited.blogspot.com/2019/04/spring-boot-error-error-creating-bean.html
    ],
    //scanBasePackages = ["battleship.server.controllers"] //Tells spring what packages to look for annonations I think
)

class BattleshipServerApplication {

    /**
     * A method annotated with @Bean defines a method that should return an object. This object is injected into
     * fields that have the same name and type as the Bean, and these fields should belong to classes with other Spring
     * annotations, like @Component per example
     * If some other parameter has the same name and is of the same type of the @Bean, the injection will be successful
     * https://docs.spring.io/spring-javaconfig/docs/1.0.0.M4/reference/html/ch02s02.html
     */
    @Bean //we can call this annotation with (name= ["jdbi"]) or  (name= arrayOf("jdbi")) to say the name of the @Bean by default it will be the name of the method
    fun jdbi() : Jdbi {
        pl("JDBI bean started")
        if(dataIsInMemory) return Jdbi.create("")
        if(useEnvironmentVariable){
            val environmentVariable = System.getenv("JDBC_DATABASE_URL")
            if(environmentVariable.isEmpty()) {
                pl("You don't have the environment variable JDBC_DATABASE_URL ...")
                pl("Will use yourJDBCURL=$yourJDBC_URL")
            } else {
                yourJDBC_URL = environmentVariable
            }
        }
        pl("dataSource URL=$yourJDBC_URL")

        val postgresDataSource = PGSimpleDataSource()
        postgresDataSource.setUrl(yourJDBC_URL)
        try{
            pl("Will wait 30s for the database connection validation...")
            if(postgresDataSource.connection.isValid(30)) pl("Connection successful")
        } catch (sqle: SQLException){
            pl("Connection failed because -> $sqle.\n\n stackTrace -> ${sqle.stackTrace.toList()}\n\n")
            pl("The database server may have not started yet")
            pl("Will use memory as data source")
            dataIsInMemory = true
            return Jdbi.create("")
        }

        val jdbi = Jdbi.create(postgresDataSource).apply{
            installPlugin(KotlinPlugin()) //https://jdbi.org/#_resultset_mapping
            installPlugin(PostgresPlugin()) //https://jdbi.org/#_postgresql
            installPlugin(SqlObjectPlugin()) //https://jdbi.org/#_declarative_api
        }

        try{
            jdbi.useHandle<Exception> {
                pl("Username acessing DB ->"+it.connection.metaData.userName.toString())
                pl("Tables found:")
                val rs = it.connection.metaData.getTables(null, null, "%", arrayOf("TABLE")) // alternative:  it.queryMetadata { it.getTables() }
                while(rs.next()){
                    pl(rs.getString("TABLE_NAME"))
                }
            }
        } catch (e: Exception) { pl("DB analysis exception -> $e")}

        //I choose to leave the database creation on docker's container's creation (at least for now because I want to try here too. See docker-compose.yml for more info

        return jdbi
    }

    @Bean
    fun corsConfigurer(): WebMvcConfigurer? {
        return object : WebMvcConfigurer { //Solves problem: "Access to fetch at 'http://localhost:9000/ (...)' from origin 'http://localhost:8080' has been locked by CORS policy (...)" See-> https://learn.microsoft.com/en-us/answers/questions/1026379/blocked-by-cors-policy-react-native-aspnet-core-ap.html  https://stackoverflow.com/q/43871637/9375488 Solutions obtained-> https://spring.io/guides/gs/rest-service-cors/ https://www.baeldung.com/spring-cors
            override fun addCorsMappings(registry: CorsRegistry) { //This is only needed when running the webpack server and doing the fetch requests. More explanations: https://javascript.info/fetch-crossorigin The alternative to this would be to configure webPack server (but it's just a dev only/temp/workaround solution) https://stackoverflow.com/a/44748420/9375488
                registry.addMapping("/**").
                allowedHeaders("*").
                allowedMethods("*"). //allows all HTTP methods, https://spring.io/blog/2015/06/08/cors-support-in-spring-framework#:~:text=By%20default%20all%20origins%20and%20GET%2C%20HEAD%20and%20POST%20methods%20are%20allowed.
                allowedOrigins("*").allowCredentials(true). //allows cookies
                allowedOrigins("http://localhost:8080", "http://localhost:8081", "http://localhost:9000") //webpack server's port
            }

            override fun addInterceptors(registry: InterceptorRegistry) { //https://www.baeldung.com/spring-mvc-handlerinterceptor
                registry.addInterceptor(object: HandlerInterceptor{ // https://stackoverflow.com/a/53986465/9375488
                    override fun preHandle(req: HttpServletRequest, r: HttpServletResponse, handler: Any): Boolean {
 /*                       if ("POST".equals(req.method, ignoreCase = true)){ //I tried cloning the inputStream to log the body, but it didnt really work out https://stackoverflow.com/q/7318632/9375488 https://stackoverflow.com/q/8100634/9375488  https://stackoverflow.com/q/24474838/9375488 https://stackoverflow.com/q/23860533/9375488 https://mkyong.com/java8/java-stream-has-already-been-operated-upon-or-closed/
                            val streamSupplier =
                                Supplier {
                                    Stream.of(
                                        req.inputStream
                                    )
                                }
                            try {
                                val result1 = Scanner(streamSupplier.get().findAny().get(), "UTF-8").useDelimiter("\\A").next()
                                println("Body=${result1}")
                            } catch (e: Exception){println(e)}
                            //req.inputStream.copyTo(System.out)
                            ContentCachingRequestWrapper(req).getInputStream().copyTo(System.out)
                        }*/

                        pl("Log: Method=[${req.method}] URI=[${req.requestURI}] Params=${req.parameterMap.toList()}")
                        //pl("Waiting $artificialLagInMilliSeconds ms for front-end 'loading' appearance")
                        Thread.sleep(artificialLagInMilliSeconds.toLong())
                        return true
                    }
                })
            }
        }
    }

    fun extractPostRequestBody(request: HttpServletRequest): String? {
        if ("POST".equals(request.method, ignoreCase = true)) {
            var s: Scanner? = null
            try {
                s = Scanner(request.inputStream, "UTF-8").useDelimiter("\\A")
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return if (s?.hasNext() == true) s?.next() else ""
        }
        return ""
    }

    //@Bean //No longer in use //Acomplishes the same goal as 'historyApiFallback: true' in webpack.config.js I chose to config this in the server rather than in nginx because it's easier here AFAIK and IMO
    fun configHistoryCallBack() : WebMvcConfigurer { //https://steve-mu.medium.com/implement-history-fall-back-in-spring-boot-dd7636f75dea
        return object : WebMvcConfigurer {
            override fun addViewControllers(registry: ViewControllerRegistry) {
                //If the URL doesn't contain /api/, forward to index.html
                registry.addViewController("^((?!/api\\/).)*\$").setViewName("forward:/index.html")

                // Matches all routes up that have exactly 1 route level
                //registry.addViewController("/{x:[\\w\\-]+}").setViewName("forward:/index.html")

                // Matches all routes up that have exactly 2 route levels, per example, if you remove "/{y:[\w\-]+}", you wont be able to access directly to http://localhost:9000/playerhub/me
                //registry.addViewController("/{x:[\\w\\-]+}/{y:[\\w\\-]+}").setViewName("forward:/index.html")

                // Multi-level directory path, need to exclude "api" on the first part of the path
                //registry.addViewController("/{x:^(?!api$).*$}/**/{y:[\\w\\-]+}").setViewName("forward:/index.html")
            }
        }
    }

    //Alternative to what's above, and this 1 provides more info
    @Bean //fallbacks 404's to front-end's 404, not .html pages in resources/static/errors/404.html
    fun redirectToFrontEndOn404(): ErrorViewResolver? { //🙏 https://gist.github.com/srikarn/e89cbb459c454754654b1975e8ca50c0 🙏
        return ErrorViewResolver { request: HttpServletRequest?, status: HttpStatus, model: Map<String?, Any?>? ->
            val path = model?.get("path").toString()
            if (status == HttpStatus.NOT_FOUND && path.startsWith("/api/")) {
                pl("API path not found")
                throw NotFoundException("The /api doesn't contain this route", path)
            } //I need to insert path otherwise it will always be "/error"...
            else {
                pl("Returning index.html")
                // A file under the directory set at spring.web.resources.static-location should be provided
                ModelAndView("forward:/index.html", emptyMap<String, Any>(), HttpStatus.OK)
            }
        }
    }
}

const val artificialLagInMilliSeconds = 500
var dataIsInMemory = true
const val enableDebugPrints = true
//                  optional w/ port->hostname:5740 (it automatically defaults to 5740 or some other port that was chosen when installing postgres)
//   scheme AKA connection protocol:// hostname|address / databasename ? params (self-explanatory)
var yourJDBC_URL = "jdbc:postgresql://localhost/postgres?user=postgres&password=MYDB" //I put this here instead of just using the Environment Variable because it's faster and easier to  edit
//                                 127.0.0.1 also works
/* https://www.postgresql.org/docs/7.4/jdbc.html      https://jdbc.postgresql.org/    https://jdbc.postgresql.org/documentation/use/
 The URL contains 'jdbc:posgresql' in the beginning because postgres uses it as driver. JDBI can interact with it because JDBI is built on top of jdbc.
 The PostgreSQL JDBC Driver allows Java programs to connect to a PostgreSQL database using standard, database independent Java code. pgJDBC is an open source JDBC driver written in Pure Java (Type 4), and communicates in the PostgreSQL native network protocol
 */

/**
 * if false, it will use [yourJDBCURL]
 */
var useEnvironmentVariable = false

fun main(args: Array<String>) {
    pl("main started")
    pl("args -> ${args.toList()}")
    if (args.isNotEmpty() && args[0] == "postgres"){
        dataIsInMemory = false
        if(try { args[1].isNotEmpty() } catch (e: Exception) { false }){
            useEnvironmentVariable = false
            //yourJDBC_URL = args1
            pl("Datasource was chosen to be postgres with JDBC URL -> ${args[1]}")
        }
        else {
            useEnvironmentVariable = true
            pl("Datasource was chosen to be postgres with environment variable")
        }
    }
    else pl("Data source will be from memory")

    runApplication<BattleshipServerApplication>(*args)
}

val serverInfo = ServerInfo("1.0.0", Author("Paulo Rosa", "44873@email.com", 44873))
