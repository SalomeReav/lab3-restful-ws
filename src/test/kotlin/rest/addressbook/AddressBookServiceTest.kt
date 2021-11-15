package rest.addressbook

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.*
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import java.net.URI

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class AddressBookServiceTest {

    @LocalServerPort
    var port = 0

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @BeforeEach
    fun cleanRepository() {
        addressBook.clear()
    }

    @Test
    fun serviceIsAlive() {
        // Request the address book
        val response = restTemplate.getForEntity("http://localhost:$port/contacts", Array<Person>::class.java)
        assertEquals(200, response.statusCode.value())
        assertEquals(0, response.body?.size)

        //////////////////////////////////////////////////////////////////////
        // Verify that GET /contacts is well implemented by the service, i.e
        // complete the test to ensure that it is safe and idempotent
        //////////////////////////////////////////////////////////////////////

        //it will be safe and inmpotent if the server didn't change the elements of the 
        // list before the Get is done. 
        assertEquals(0,addressBook.personList.size)
    }

    @Test
    fun createUser() {

        // Prepare data
        val juan = Person(name = "Juan")
        val juanURI: URI = URI.create("http://localhost:$port/contacts/person/1")

        // Create a new user
        var response = restTemplate.postForEntity("http://localhost:$port/contacts", juan, Person::class.java)

        assertEquals(201, response.statusCode.value())
        assertEquals(juanURI, response.headers.location)
        assertEquals(MediaType.APPLICATION_JSON, response.headers.contentType)
        var juanUpdated = response.body
        assertEquals(juan.name, juanUpdated?.name)
        assertEquals(1, juanUpdated?.id)
        assertEquals(juanURI, juanUpdated?.href)

        // Check that the new user exists
        response = restTemplate.getForEntity(juanURI, Person::class.java)

        assertEquals(200, response.statusCode.value())
        assertEquals(MediaType.APPLICATION_JSON, response.headers.contentType)
        juanUpdated = response.body
        assertEquals(juan.name, juanUpdated?.name)
        assertEquals(1, juanUpdated?.id)
        assertEquals(juanURI, juanUpdated?.href)

        //////////////////////////////////////////////////////////////////////
        // Verify that POST /contacts is well implemented by the service, i.e
        // complete the test to ensure that it is not safe and not idempotent
        //////////////////////////////////////////////////////////////////////

        //test it is not safe
        // after creation of the new user, the size of the list has incremented by 1 
        assertEquals(1,addressBook.personList.size)

        //test it is not idemponten, that meanse if you create again the same user with
        // the same data, it will create a new user incrementing by 1 again list's size. 
        val juanURI2: URI = URI.create("http://localhost:$port/contacts/person/2") 
        response = restTemplate.postForEntity("http://localhost:$port/contacts", juan, Person::class.java)

        assertEquals(201, response.statusCode.value())
        assertEquals(juanURI2, response.headers.location)
        assertEquals(MediaType.APPLICATION_JSON, response.headers.contentType)
        juanUpdated = response.body
        assertEquals(juan.name, juanUpdated?.name)
        assertEquals(2, juanUpdated?.id)
        assertEquals(juanURI, juanUpdated?.href)

        assertEquals(2,addressBook.personList.size)
    }

    @Test
    fun createUsers() {
        // Prepare server
        val salvador = Person(id = addressBook.nextId(), name = "Salvador")
        addressBook.personList.add(salvador)

        // Prepare data
        val juan = Person(name = "Juan")
        val juanURI = URI.create("http://localhost:$port/contacts/person/2")
        val maria = Person(name = "Maria")
        val mariaURI = URI.create("http://localhost:$port/contacts/person/3")

        // Create a new user
        var response = restTemplate.postForEntity("http://localhost:$port/contacts", juan, Person::class.java)
        assertEquals(201, response.statusCode.value())
        assertEquals(juanURI, response.headers.location)

        // Create a second user
        response = restTemplate.postForEntity("http://localhost:$port/contacts", maria, Person::class.java)
        assertEquals(201, response.statusCode.value())
        assertEquals(mariaURI, response.headers.location)
        assertEquals(MediaType.APPLICATION_JSON, response.headers.contentType)

        var mariaUpdated = response.body
        assertEquals(maria.name, mariaUpdated?.name)
        assertEquals(3, mariaUpdated?.id)
        assertEquals(mariaURI, mariaUpdated?.href)

        // Check that the new user exists
        response = restTemplate.getForEntity(mariaURI, Person::class.java)

        assertEquals(200, response.statusCode.value())
        assertEquals(MediaType.APPLICATION_JSON, response.headers.contentType)
        mariaUpdated = response.body
        assertEquals(maria.name, mariaUpdated?.name)
        assertEquals(3, mariaUpdated?.id)
        assertEquals(mariaURI, mariaUpdated?.href)

        //////////////////////////////////////////////////////////////////////
        // Verify that GET /contacts/person/3 is well implemented by the service, i.e
        // complete the test to ensure that it is safe and idempotent
        //////////////////////////////////////////////////////////////////////

        //test is safe and idempotent. list size must stay the same after create a new user 

        val sizeBefore = addressBook.personList.size
        // create again the same user MAria 
        response = restTemplate.getForEntity(mariaURI, Person::class.java)

        assertEquals(200, response.statusCode.value())
        assertEquals(MediaType.APPLICATION_JSON, response.headers.contentType)
        mariaUpdated = response.body
        assertEquals(maria.name, mariaUpdated?.name)
        assertEquals(3, mariaUpdated?.id)
        assertEquals(mariaURI, mariaUpdated?.href)

        val sizeAfter= addressBook.personList.size

        //check before and after size are equals
        assertEquals(sizeBefore,sizeAfter)
    }

    @Test
    fun listUsers() {

        // Prepare server
        val salvador = Person(name = "Salvador", id = addressBook.nextId())
        val juan = Person(name = "Juan", id = addressBook.nextId())
        addressBook.personList.add(salvador)
        addressBook.personList.add(juan)

        // Test list of contacts
        val response = restTemplate.getForEntity("http://localhost:$port/contacts", Array<Person>::class.java)
        assertEquals(200, response.statusCode.value())
        assertEquals(MediaType.APPLICATION_JSON, response.headers.contentType)
        assertEquals(2, response.body?.size)
        assertEquals(juan.name, response.body?.get(1)?.name)

        //////////////////////////////////////////////////////////////////////
        // Verify that GET /contacts is well implemented by the service, i.e
        // complete the test to ensure that it is safe and idempotent
        //////////////////////////////////////////////////////////////////////

        // list size doesn't change after do the GET 

        val sizeBefore = addressBook.personList.size
        response2 = restTemplate.getForEntity("http://localhost:$port/contacts", Array<Person>::class.java)
        assertEquals(200, response2.statusCode.value())
        assertEquals(MediaType.APPLICATION_JSON, response2.headers.contentType)
        assertEquals(2, response2.body?.size)
        assertEquals(juan.name, response2.body?.get(1)?.name)
        assertEquals(salvador.name, response2.body?.get(0)?.name)

        val sizeAfter = addressBook.personList.size

        assertEquals (sizeBefore,sizeAfter)
    }

    @Test
    fun updateUsers() {
        // Prepare server
        val salvador = Person(name = "Salvador", id = addressBook.nextId())
        val juan = Person(name = "Juan", id = addressBook.nextId())
        val juanURI = URI.create("http://localhost:$port/contacts/person/2")
        addressBook.personList.add(salvador)
        addressBook.personList.add(juan)

        // Update Maria
        val maria = Person(name = "Maria")

        var response = restTemplate.exchange(juanURI, HttpMethod.PUT, HttpEntity(maria), Person::class.java)
        assertEquals(204, response.statusCode.value())

        // Verify that the update is real
        response = restTemplate.getForEntity(juanURI, Person::class.java)
        assertEquals(200, response.statusCode.value())
        assertEquals(MediaType.APPLICATION_JSON, response.headers.contentType)
        val updatedMaria = response.body
        assertEquals(maria.name, updatedMaria?.name)
        assertEquals(2, updatedMaria?.id)
        assertEquals(juanURI, updatedMaria?.href)

        // Verify that only can be updated existing values
        restTemplate.execute("http://localhost:$port/contacts/person/3", HttpMethod.PUT,
            {
                it.headers.contentType = MediaType.APPLICATION_JSON
                ObjectMapper().writeValue(it.body, maria)
            },
            { assertEquals(404, it.statusCode.value()) }
        )

        //////////////////////////////////////////////////////////////////////
        // Verify that PUT /contacts/person/2 is well implemented by the service, i.e
        // complete the test to ensure that it is idempotent but not safe
        //////////////////////////////////////////////////////////////////////

        var response2 = restTemplate.exchange(juanURI, HttpMethod.PUT, HttpEntity(maria), Person::class.java)
        assertEquals(204, response2.statusCode.value())

        response2 = restTemplate.getForEntity(juanURI, Person::class.java)
        assertEquals(200, response2.statusCode.value())
        assertEquals(MediaType.APPLICATION_JSON, response2.headers.contentType)
        val updatedMaria2 = response2.body
        assertEquals(maria.name, updatedMaria2?.name)
        assertEquals(2, updatedMaria2?.id)
        assertEquals(juanURI, updatedMaria2?.href)
    }

    @Test
    fun deleteUsers() {
        // Prepare server
        val salvador = Person(name = "Salvador", id = addressBook.nextId())
        val juan = Person(name = "Juan", id = addressBook.nextId())
        val juanURI = URI.create("http://localhost:$port/contacts/person/2")
        addressBook.personList.add(salvador)
        addressBook.personList.add(juan)


        val sizeBefore = addressBook.personList.size
        // Delete a user
        restTemplate.execute(juanURI, HttpMethod.DELETE, {}, { assertEquals(204, it.statusCode.value()) })

        val sizeAfter = addressBook.personList.size
        // Verify that the user has been deleted
        restTemplate.execute(juanURI, HttpMethod.GET, {}, { assertEquals(404, it.statusCode.value()) })

        //////////////////////////////////////////////////////////////////////
        // Verify that DELETE /contacts/person/2 is well implemented by the service, i.e
        // complete the test to ensure that it is idempotent but not safe
        //////////////////////////////////////////////////////////////////////

        assertNotEquals(sizeBefore,sizeAfter)
        restTemplate.execute(juanURI, HttpMethod.DELETE, {}, { assertEquals(204, it.statusCode.value()) })

        

    }

    @Test
    fun findUsers() {
        // Prepare server
        val salvador = Person(name = "Salvador", id = addressBook.nextId())
        val juan = Person(name = "Juan", id = addressBook.nextId())
        val salvadorURI = URI.create("http://localhost:$port/contacts/person/1")
        val juanURI = URI.create("http://localhost:$port/contacts/person/2")
        addressBook.personList.add(salvador)
        addressBook.personList.add(juan)

        // Test user 1 exists
        var response = restTemplate.getForEntity(salvadorURI, Person::class.java)
        assertEquals(200, response.statusCode.value())
        assertEquals(MediaType.APPLICATION_JSON, response.headers.contentType)
        var person = response.body
        assertEquals(salvador.name, person?.name)
        assertEquals(salvador.id, person?.id)
        assertEquals(salvador.href, person?.href)

        // Test user 2 exists
        response = restTemplate.getForEntity(juanURI, Person::class.java)
        assertEquals(200, response.statusCode.value())
        assertEquals(MediaType.APPLICATION_JSON, response.headers.contentType)
        person = response.body
        assertEquals(juan.name, person?.name)
        assertEquals(juan.id, person?.id)
        assertEquals(juan.href, person?.href)

        // Test user 3 doesn't exist
        restTemplate.execute("http://localhost:$port/contacts/person/3", HttpMethod.GET, {}, { assertEquals(404, it.statusCode.value()) })
    }

}
