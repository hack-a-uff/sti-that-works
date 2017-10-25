package br.uff.ic


import com.arangodb.ArangoDB
import com.arangodb.model.DocumentCreateOptions
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlin.reflect.KClass

abstract class ArangoDBDataSource(
    private val mapper: ObjectMapper,
    private val arangoDB: ArangoDB,
    private val databaseName: String
){
     fun retrieveWith(key: String, arguments: Map<String, Any>): List<Student> {
        val argList = (arguments.map { "a.${it.key} == @${it.key}" } + "a._key == @key").joinToString(" AND ")
        with(arangoDB.db(databaseName)) {
            val cursor = query("""
                    FOR a IN ${Student::class.simpleName}
                        FILTER $argList
                        RETURN a
                """,
                arguments + mapOf("key" to key),
                null,
                String::class.java
            )
            if (cursor.hasNext()) {
                return cursor.asListRemaining().map { mapper.readValue(it, Student::class.java) }
            }
            return listOf()
        }
    }

     fun exists(key: String): Boolean {
        with(arangoDB.db(databaseName).collection(Student::class.simpleName)) {
            return documentExists(key)
        }
    }

     fun close() {
        arangoDB.shutdown()
    }

     fun delete(key: String) {
        with(arangoDB.db(databaseName).collection(Student::class.simpleName)) {
            deleteDocument(key)
        }
    }

     fun overwriteBy(entity: Student, key: String) {
        with(arangoDB.db(databaseName).collection(entity::class.java.simpleName)) {
            replaceDocument(key, entity)
        }
    }

     fun update(key: String, entity: Student) {
        with(arangoDB.db(databaseName).collection(entity::class.java.simpleName)) {
            updateDocument(key, entity)
        }
    }

     fun retrieve(key: String): Student {
         with(arangoDB.db(databaseName).collection(Student::class.java.simpleName)) {
             return mapper.readValue(getDocument(key, String::class.java), Student::class.java)
         }
     }



     fun create(entity: Student) {
        with(arangoDB.db(databaseName).collection(entity::class.java.simpleName)) {
            insertDocument(entity, DocumentCreateOptions())
        }
    }
}


private fun ObjectMapper.configure(): ObjectMapper {
    registerKotlinModule()
    propertyNamingStrategy = PropertyNamingStrategy.LOWER_CAMEL_CASE
    setSerializationInclusion(JsonInclude.Include.NON_NULL)
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    return this
}

fun arangoMapper(): ObjectMapper {
    return ObjectMapper().configure()
}

class StudentArangoDBDataSource(arangoDB: ArangoDB, databaseName: String) : ArangoDBDataSource(arangoMapper(), arangoDB, databaseName)