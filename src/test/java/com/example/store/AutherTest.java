package com.example.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.http.HttpHeaders;

import com.example.store.dto.LoginRequest;
import com.example.store.dto.RegisterRequest;
import com.example.store.model.Role;
import com.example.store.model.User;
import com.example.store.repository.RoleRepository;
import com.example.store.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.store.SetUpTest;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AutherTest {
    @Autowired
	TestRestTemplate restTemplate;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    private Role customerRole;

    @Autowired
    SetUpTest setUpTest;

    @BeforeEach
    void setUp() {
        // ensure deterministic IDs for roles and users for each test
        setUpTest.setUp();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        adminRole.setDescription("Administrator role");
        roleRepository.save(adminRole);

        customerRole = new Role();
        customerRole.setName("CUSTOMER");
        customerRole.setDescription("Default customer role");
        customerRole = roleRepository.save(customerRole);

        User baseUser = User.builder()
            .username("nhanhoa")
            .passwordHash(passwordEncoder.encode("123456"))
            .email("hoa@example.com")
            .role(customerRole)
            .status("ACTIVE")
            .build();
        userRepository.save(baseUser);

        User inactiveUser = User.builder()
            .username("inactiveUser")
            .passwordHash(passwordEncoder.encode("123456"))
            .email("inactive@example.com")
            .role(customerRole)
            .status("INACTIVE")
            .build();
        userRepository.save(inactiveUser);
    }

    @Test
    void shouldSuccessContextLoad() {
        assertThat(userRepository.findByUsername("nhanhoa")).isNotNull();
    }

	@Test
	void testLoginExitingUser(){
		RequestEntity<LoginRequest> request = RequestEntity
			.post("/login")
			.body(new LoginRequest("nhanhoa", "123456"));
		
		ResponseEntity<String> response = restTemplate.exchange(request, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

	}
    @Test 
    void testLoginInactiveUser(){
        RequestEntity<LoginRequest> request = RequestEntity
            .post("/login")
            .body(new LoginRequest("inactiveUser", "123456"));

        ResponseEntity<String> response = restTemplate.exchange(request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).contains("User is not active");
    }
	@Test
	void testLoginNonExitingUser(){
		RequestEntity<LoginRequest> request = RequestEntity
			.post("/login")
			.body(new LoginRequest("nonexist", "123456"));

		ResponseEntity<String> response = restTemplate.exchange(request, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

    @Test void shouldNotAcceptWrongPassword(){
        RequestEntity<LoginRequest> request = RequestEntity
            .post("/login")
            .body(new LoginRequest("nhanhoa", "wrongpassword"));
        
        ResponseEntity<String> response = restTemplate.exchange(request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).contains("Bad credentials");
        
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
	void testRegisterNewUser(){
		RequestEntity<RegisterRequest> request = RequestEntity
			.post("/register")
			.body(new RegisterRequest("newUser", "newpassword","new`@gmail.com", customerRole.getName()));

		ResponseEntity<String> response = restTemplate.exchange(request, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
	}

    @Test
    @DirtiesContext
    void testRegisterDuplicateUsernameOrEmail(){

        // duplicate username
        RequestEntity<RegisterRequest> request = RequestEntity
            .post("/register")
			.body(new RegisterRequest("nhanhoa", "newpassword","newuser@gmail.com",customerRole.getName()));

        ResponseEntity<String> response = restTemplate.exchange(request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("Username already exists");

        // duplicate email
        request = RequestEntity
            .post("/register")
			.body(new RegisterRequest("duplicateEmail", "newpassword","hoa@example.com",customerRole.getName()));

        response = restTemplate.exchange(request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("Email already exists");
    }

    @Test
    void testValidationEmptyRegister(){

        // empty username
        RequestEntity<RegisterRequest> request = RequestEntity
            .post("/register")
			.body(new RegisterRequest("", "newpassword","new@gmail.com",customerRole.getName()));
        
        ResponseEntity<String> response = restTemplate.exchange(request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Username cannot be empty");
    
        // empty password
        request = RequestEntity
            .post("/register")
			.body(new RegisterRequest("newUser", "","new@gmail.com",customerRole.getName()));
        response = restTemplate.exchange(request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Password cannot be empty");

        // empty email
        request = RequestEntity
            .post("/register")
			.body(new RegisterRequest("newUser", "newpassword","",customerRole.getName()));
        response = restTemplate.exchange(request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Email cannot be empty");

        // empty role
        request = RequestEntity
            .post("/register")
            .body(new RegisterRequest("newUser", "newpassword","new@gmail.com",null));
        response = restTemplate.exchange(request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("RoleName cannot be empty");

    }

    @Test
    @DirtiesContext
    void testDeleteExistingUser(){
        RequestEntity<LoginRequest> loginRequest = RequestEntity
            .post("/login")
            .body(new LoginRequest("nhanhoa","123456"));
        
        ResponseEntity<Map> loginResponse = restTemplate
            .exchange(loginRequest, Map.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth((String)loginResponse.getBody().get("token"));
        
        RequestEntity<Void> request = RequestEntity
            .delete("/user/{userName}", "nhanhoa")
            .headers(headers)
            .build();

        ResponseEntity<String> response = restTemplate.exchange(request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        loginRequest = RequestEntity
            .post("/login")
            .body(new LoginRequest("nhanhoa","123456"));

        ResponseEntity<String> failedLoginResponse = restTemplate
            .exchange(loginRequest, String.class);

        assertThat(failedLoginResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
