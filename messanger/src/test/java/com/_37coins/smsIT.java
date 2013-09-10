package com._37coins;

import static com.jayway.restassured.RestAssured.given;

import org.junit.Test;

import com._37coins.resources.EnvayaSmsResource;

public class smsIT {
	public final String restUrl = "http://localhost:8082/msg/rest";
	
	@Test
	public void testEnvayaBalance() {
		given()
			.formParam("action", "incoming")
			.formParam("message", "balance")
			.formParam("test", "true")
			.formParam("phone_number", "010982392349")
			.formParam("from", "01027423984")
			.formParam("message_type","sms")
		.expect()
			.statusCode(200)
		.when()
			.post(restUrl + EnvayaSmsResource.PATH);
	}
	
	@Test
	public void testEnvayaHelp() {
		given()
			.formParam("action", "incoming")
			.formParam("message", "help")
			.formParam("test", "true")
			.formParam("phone_number", "010982392349")
			.formParam("from", "01027423984")
			.formParam("message_type","sms")
		.expect()
			.statusCode(200)
		.when()
			.post(restUrl + EnvayaSmsResource.PATH);
	}

}
