package br.uff.ic

import com.arangodb.ArangoDB
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.litote.kmongo.*
import com.google.inject.Binder
import com.google.inject.Inject
import com.google.inject.name.Named
import com.mongodb.MongoClient
import com.typesafe.config.Config
import org.jooby.Env
import org.jooby.Err
import org.jooby.Jooby
import org.jooby.json.Jackson
import org.jooby.mvc.*
import com.arangodb.Protocol
import com.arangodb.entity.DocumentField
import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import javax.inject.Provider

@Path("/v1/students")
class StudentController {

    lateinit var client: MongoClient

    private val studentArangoDBDataSource: StudentArangoDBDataSource = StudentArangoDBDataSource(
        ArangoDB.Builder().host("localhost",8529)
            .user("root")
            .password("9PljHcLjlRUZX6cy")
            .build(),
        "sti")


    @GET
    @Path("/:card-nfc-id")
    fun get(@Named("card-nfc-id") cardNfcId: String): Map<String, Any?> {
        return studentArangoDBDataSource.retrieve(cardNfcId).toMap()

    }

    data class Money(
        val uffFunds: Double = 0.0,
        val rioCardFunds: Double = 0.0
    )

    @PATCH
    @Path("/:card-nfc-id")
    fun update(@Named("card-nfc-id") cardNfcId: String, @Body money: Money) {
        val toBeUpdated = studentArangoDBDataSource.retrieve(cardNfcId).copy(
            uffFunds = money.uffFunds
        )
        studentArangoDBDataSource.update(cardNfcId,toBeUpdated)
    }

    @POST
    fun create(@Body student: Student) {
        studentArangoDBDataSource.create(student)
    }

    @DELETE
    @Path("/:card-nfc-id")
    fun delete(@Named("card-nfc-id") cardNfcId: String) {
        studentArangoDBDataSource.delete(cardNfcId)
    }
}


data class Student(
    val name: String,
    val picture: String,
    @DocumentField(DocumentField.Type.KEY)
    @JsonAlias("_key")
    val cardNfcId: String,
    val uffRegistrationNumber: String,
    val course: String,
    val expiresAt: String,
    val uffFunds: Double = 0.0,
    val rioCardFunds: Double = 0.0
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "name" to name,
            "picture" to picture,
            "cardNfcId" to cardNfcId,
            "uffRegistrationNumber" to uffRegistrationNumber,
            "expiresAt" to expiresAt,
            "course" to course,
            "uffFunds" to uffFunds,
            "rioCardFunds" to rioCardFunds
        )
    }
}

object StiThatWorks : Jooby.Module {
    override fun configure(env: Env, conf: Config, binder: Binder) {
        binder.bind(MongoClient::class.java).toProvider(Provider {
            KMongo.createClient()
        })
    }

    @JvmStatic
    fun main(args: Array<String>) {
        org.jooby.run("port=${args.getOrNull(0) ?: "8878"}") {
            use(Jackson().doWith { mapper ->
                mapper.registerKotlinModule()
                mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            })
            use(StudentController::class.java)
        }
    }
}