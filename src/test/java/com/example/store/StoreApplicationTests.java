package com.example.store;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import com.example.store.dto.LoginRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StoreApplicationTests {

	@Autowired
	TestRestTemplate restTemplate;

	@Test
	void testLoginExitingUser(){
		RequestEntity<LoginRequest> request = RequestEntity
			.post("/login")
			.body(new LoginRequest("nhanhoa", "123456"));
		
		ResponseEntity<String> response = restTemplate.exchange(request, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

	}

	@Test
	void testLoginNonExitingUser(){
		RequestEntity<LoginRequest> request = RequestEntity
			.post("/login")
			.body(new LoginRequest("nonexist", "123456"));

		ResponseEntity<String> response = restTemplate.exchange(request, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	@DirtiesContext
	void checkRegisterNewUser(){
		RequestEntity<LoginRequest> request = RequestEntity
			.post("/register")
			.body(new LoginRequest("nhanhoa", "123456"));

		ResponseEntity<String> response = restTemplate.exchange(request, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		RequestEntity<LoginRequest> loginRequest = RequestEntity
			.post("/login")
			.body(new LoginRequest("nhanhoa", "123456"));

		ResponseEntity<String> loginResponse = restTemplate.exchange(loginRequest, String.class);
		assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);


	}
}
