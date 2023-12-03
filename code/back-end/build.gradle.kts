import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.7.3"
    id("io.spring.dependency-management") version "1.0.13.RELEASE"
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.spring") version "1.6.21"
    application //Hmm
}

group = "battleship"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    //Dependencies that came with Spring Initializr:
    implementation("org.springframework.boot:spring-boot-starter-web") //responsible for providing the static content to the client
    //implementation("org.springframework.boot:spring-boot-starter-jdbc") //putting this in comment solved "Failed to Configure a DataSource" alternative: in @SpringBootApplication (exclude = [DataSourceAutoConfiguration::class])
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    //Added dependencies:
    implementation("org.springframework.boot:spring-boot-starter-security") //For password hashing
	implementation("org.springframework.boot:spring-boot-starter-validation:2.7.4") //A library used for Validating our request bodies
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.+") //To convert objects to and from json, used to store lists in the DB

    // JDBI and postgresql
    implementation("org.jdbi:jdbi3-core:3.34.0")
    implementation("org.jdbi:jdbi3-postgres:3.34.0")
    implementation("org.jdbi:jdbi3-kotlin:3.34.0")
    implementation("org.jdbi:jdbi3-kotlin-sqlobject:3.34.0") //https://jdbi.org/#_sqlobject
    implementation("org.postgresql:postgresql:42.5.0") //for: PGSimpleDataSource

    //For integrated tests:
    testImplementation(kotlin("test"))
    //In order to create integrated tests that work w/ Spring
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
