plugins {
    `java-library`
    id("com.apollographql.apollo").version("2.5.11")
}

dependencies {
    api(project(":ontrack-json"))

    api("com.apollographql.apollo:apollo-runtime:2.5.11")
    api("com.apollographql.apollo:apollo-coroutines-support:2.5.11")
    implementation("org.apache.httpcomponents:httpclient:4.5.13")
    implementation("org.springframework.boot:spring-boot-starter-web")
}

//
//val downloadGraphQL by tasks.registering {
//    doLast {
//        val dir = "src/main/graphql/net/nemerosa/ontrack/kdsl/connector/graphql/queries"
//        project.mkdir(dir)
//        ant.withGroovyBuilder {
//            "get"(
//                "src" to "http://localhost:8080/schema/ontrack-pro.json",
//                "dest" to "$dir/schema.json"
//            )
//        }
//    }
//}