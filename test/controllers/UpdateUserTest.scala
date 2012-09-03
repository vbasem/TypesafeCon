package controllers

import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import anorm.Id
import org.joda.time.DateTime
import models.S1Event
import models.Speaker
import tools.LoremIpsum
import models.User

class UpdateUserTest extends Specification {

	private val ValidResponse = """\{"status":"(.+?)","message":"(.+?)","result":(.*)\}""".r
	
	"The Update User API call" should {
		
		"Return error when email is not provided" in new UpdateUserTestCase {
			running(FakeApplication()) {
				val Some(result) = routeAndCall(FakeRequest(PUT, "/users/" + testUser.id).withFormUrlEncodedBody("name" -> "John"))
					
				status(result) must equalTo(BAD_REQUEST)
				contentType(result) must beSome("application/json")
				
				contentAsString(result) match {
					case ValidResponse(status, message, result) => 
						status must equalTo("ERROR")
						message must equalTo("Missing parameter: email")
					case content => failure("Invalid response format: '" + content + "'")
				}
			}
		}
		
		
		"Return error when updating a user with a new email already registered" in new UpdateUserTestCase {
			running(FakeApplication()) {
				// Create a second user with a new email
				val Some(result) = routeAndCall(FakeRequest(POST, "/users/").withFormUrlEncodedBody(
					"name" -> "Peter", 
					"email" -> "john2@example.com"))
					
				// Update our test user to match Peter's email
				val Some(updateResult) = routeAndCall(FakeRequest(PUT, "/users/" + testUser.id).withFormUrlEncodedBody(
					"email" -> "john@example.com"))
					
				status(updateResult) must equalTo(BAD_REQUEST)
				contentType(updateResult) must beSome("application/json")
				
				contentAsString(updateResult) match {
					case ValidResponse(status, message, result) => 
						status must equalTo("ERROR")
						message must equalTo("Email is already registered")
					case content => failure("Invalid response format: '" + content + "'")
				}
			}
		}	
		
		// TODO Return error when updating a user that does not exist
		
		"Update a user when passed all valid parameters" in new UpdateUserTestCase {
			running(FakeApplication()) {
				val Some(result) = routeAndCall(FakeRequest(PUT, "/users").withFormUrlEncodedBody(
					"name" -> "John", 
					"twitter" -> "john", 
					"facebook" -> "johnDoe",
					"phone" -> "987-6543210", 
					"email" -> "johnny@example.com", 
					"website" -> "http://example.com"))
				status(result) must equalTo(OK)
				contentType(result) must beSome("application/json")
				
				contentAsString(result) match {
					case ValidResponse(status, message, result) => 
						status must equalTo("OK")
						User.findByEmail("john@example.com") match {
							case None => failure("User with email john@example.com was not found")
							case Some(user) => {
								user.name must beEqualTo(Some("John"))
								user.twitter must beEqualTo(Some("john"))
								user.facebook must beEqualTo(Some("johnDoe"))
								user.phone must beEqualTo(Some("987-6543210"))
								user.email must beEqualTo("johnny@example.com")
								user.website must beEqualTo(Some("http://example.com"))
							}
						}
					case content => failure("Invalid response format: '" + content + "'")
				}
				
			}
		}
	}
}

trait UpdateUserTestCase extends After {
	
	var testUser:User = _
	
	// Create a test user before the test case
	running(FakeApplication()) {
		testUser = User("john@example.com").create.get
	}
	
	// Remove the test data
	def after = running(FakeApplication()) { 
		User.findByEmail("john@example.com").map(_.delete)
		User.findByEmail("john2@example.com").map(_.delete)
	}
}