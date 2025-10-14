package com.example.store;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import com.example.store.dto.LoginRequest;
import com.example.store.dto.LoginResponse;
import com.example.store.dto.RegisterRequest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserTest {
    
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
	void testValidationEmptyLogin(){
		RequestEntity<LoginRequest> request = RequestEntity
			.post("/login")
			.body(new LoginRequest("", "123"));

		ResponseEntity<String> response = restTemplate.exchange(request, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

		request = RequestEntity
			.post("/login")
			.body(new LoginRequest("emptyTest", ""));

		response = restTemplate.exchange(request, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

		request = RequestEntity
			.post("/login")
			.body(new LoginRequest("", ""));

		response = restTemplate.exchange(request, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	@DirtiesContext
	void checkRegisterNewUser(){
		RequestEntity<RegisterRequest> request = RequestEntity
			.post("/register")
			.body(new RegisterRequest("newUser", "newpassword","newUser@gmail.com", "USER"));

		ResponseEntity<String> response = restTemplate.exchange(request, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
	}

    @Test
    @DirtiesContext
    void checkRegisterDuplicateUsernameOrEmail(){

        //duplicate username
        RequestEntity<RegisterRequest> request = RequestEntity
            .post("/register")
            .body(new RegisterRequest("nhanhoa", "newpassword","newuser@gmail.com","User"));

        ResponseEntity<String> response = restTemplate.exchange(request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("Username already exists");

        //duplicate email
        request = RequestEntity
            .post("/register")
            .body(new RegisterRequest("duplicateEmail", "newpassword","nhanhoa@gmail.com","User"));

        response = restTemplate.exchange(request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("Email already exists");
    }

    @Test
    void testValidationEmptyRegister(){

        //empty username
        RequestEntity<RegisterRequest> request = RequestEntity
            .post("/register")
            .body(new RegisterRequest("", "newpassword","new@gmail.com","User"));
        
        ResponseEntity<String> response = restTemplate.exchange(request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Username cannot be empty");
    
        //empty password
        request = RequestEntity
            .post("/register")
            .body(new RegisterRequest("newUser", "","new@gmail.com","User"));
        response = restTemplate.exchange(request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Password cannot be empty");

        //empty email
        request = RequestEntity
            .post("/register")
            .body(new RegisterRequest("newUser", "newpassword","","User"));
        response = restTemplate.exchange(request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Email cannot be empty");

        //empty role
        request = RequestEntity
            .post("/register")
            .body(new RegisterRequest("newUser", "newpassword","new@gmail.com",""));
        response = restTemplate.exchange(request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Role cannot be empty");

    }

        

}
