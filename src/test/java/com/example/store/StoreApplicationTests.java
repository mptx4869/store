package com.example.store;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StoreApplicationTests {

	// @Autowired
	// TestRestTemplate restTemplate;

	@Test
	void contextLoads() {
		// ResponseEntity<String> response = restTemplate.getForEntity("/book", String.class);
		// assertThat(response.getBody()).isEqualTo("something");
		// assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

}
