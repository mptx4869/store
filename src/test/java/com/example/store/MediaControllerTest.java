package com.example.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.example.store.dto.LoginRequest;
import com.example.store.dto.MediaDeleteResponse;
import com.example.store.dto.MediaUploadResponse;
import com.example.store.model.Role;
import com.example.store.model.User;
import com.example.store.repository.RoleRepository;
import com.example.store.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MediaControllerTest {
    
    // Test constants
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String CUSTOMER_USERNAME = "customer";
    private static final String CUSTOMER_PASSWORD = "customer123";
    private static final String TEST_IMAGE_FILENAME = "test-image.jpg";
    private static final String UPLOAD_ENDPOINT = "/media/upload";
    private static final String MEDIA_ENDPOINT = "/media/";
    
    // JPEG file header bytes
    private static final byte[] JPEG_HEADER = new byte[] {
        (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
        0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
        0x01, 0x01, 0x00, 0x48, 0x00, 0x48, 0x00, 0x00,
        (byte) 0xFF, (byte) 0xD9
    };

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SetUpTest setUpTest;

    private String adminToken;
    private String customerToken;
    private Path uploadDir;

    @BeforeEach
    void setUp() {
        setUpTest.setUp();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        // Create roles
        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        adminRole.setDescription("Administrator");
        roleRepository.save(adminRole);

        Role customerRole = new Role();
        customerRole.setName("CUSTOMER");
        customerRole.setDescription("Customer");
        roleRepository.save(customerRole);

        // Create admin user
        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@test.com");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setRole(adminRole);
        admin.setStatus("ACTIVE");
        userRepository.save(admin);

        // Create customer user
        User customer = new User();
        customer.setUsername("customer");
        customer.setEmail("customer@test.com");
        customer.setPasswordHash(passwordEncoder.encode("customer123"));
        customer.setRole(customerRole);
        customer.setStatus("ACTIVE");
        userRepository.save(customer);

        // Get tokens
        adminToken = loginAndGetToken("admin", "admin123");
        customerToken = loginAndGetToken("customer", "customer123");

        // Setup upload directory
        uploadDir = Paths.get("uploads").toAbsolutePath().normalize();
    }

    @AfterEach
    void cleanup() throws IOException {
        // Clean up uploaded test files
        if (Files.exists(uploadDir)) {
            Files.list(uploadDir)
                .filter(path -> path.getFileName().toString().startsWith("test-"))
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        // Ignore
                    }
                });
        }
    }

    @Test
    void shouldUploadImageAsAdmin() {
        ByteArrayResource fileResource = createImageResource(TEST_IMAGE_FILENAME);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = createUploadRequest(fileResource, adminToken);

        ResponseEntity<MediaUploadResponse> response = restTemplate.postForEntity(
            UPLOAD_ENDPOINT, requestEntity, MediaUploadResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUrl()).isNotNull();
        assertThat(response.getBody().getUrl()).contains(MEDIA_ENDPOINT);
        assertThat(response.getBody().getMessage()).isEqualTo("File uploaded successfully");
    }

    @Test
    void shouldDenyUploadForCustomer() {
        ByteArrayResource fileResource = createImageResource(TEST_IMAGE_FILENAME);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = createUploadRequest(fileResource, customerToken);

        ResponseEntity<MediaUploadResponse> response = restTemplate.postForEntity(
            UPLOAD_ENDPOINT, requestEntity, MediaUploadResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldDenyUploadWithoutToken() {
        ByteArrayResource fileResource = createImageResource(TEST_IMAGE_FILENAME);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = createUploadRequest(fileResource, null);

        ResponseEntity<MediaUploadResponse> response = restTemplate.postForEntity(
            UPLOAD_ENDPOINT, requestEntity, MediaUploadResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldRejectEmptyFile() {
        ByteArrayResource emptyResource = new ByteArrayResource(new byte[0]) {
            @Override
            public String getFilename() {
                return "empty.jpg";
            }
        };
        
        HttpEntity<MultiValueMap<String, Object>> requestEntity = createUploadRequest(emptyResource, adminToken);

        ResponseEntity<MediaUploadResponse> response = restTemplate.postForEntity(
            UPLOAD_ENDPOINT, requestEntity, MediaUploadResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Please select a file to upload");
    }

    @Test
    void shouldAllowPublicAccessToUploadedImage() {
        // Upload image
        ByteArrayResource fileResource = createImageResource("test-public.jpg");
        HttpEntity<MultiValueMap<String, Object>> uploadRequest = createUploadRequest(fileResource, adminToken);
        ResponseEntity<MediaUploadResponse> uploadResponse = restTemplate.postForEntity(
            UPLOAD_ENDPOINT, uploadRequest, MediaUploadResponse.class);

        String fileUrl = uploadResponse.getBody().getUrl();
        String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);

        // Try to access without token (should work)
        ResponseEntity<byte[]> downloadResponse = restTemplate.getForEntity(
            MEDIA_ENDPOINT + fileName, byte[].class);

        assertThat(downloadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(downloadResponse.getHeaders().getContentType().toString()).contains("image");
    }

    @Test
    void shouldDeleteImageAsAdmin() {
        // Upload image
        ByteArrayResource fileResource = createImageResource("test-delete.jpg");
        HttpEntity<MultiValueMap<String, Object>> uploadRequest = createUploadRequest(fileResource, adminToken);
        ResponseEntity<MediaUploadResponse> uploadResponse = restTemplate.postForEntity(
            UPLOAD_ENDPOINT, uploadRequest, MediaUploadResponse.class);

        String fileUrl = uploadResponse.getBody().getUrl();
        String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);

        // Delete image
        HttpHeaders deleteHeaders = new HttpHeaders();
        deleteHeaders.setBearerAuth(adminToken);
        HttpEntity<Void> deleteRequest = new HttpEntity<>(deleteHeaders);
        
        ResponseEntity<MediaDeleteResponse> deleteResponse = restTemplate.exchange(
            MEDIA_ENDPOINT + fileName, HttpMethod.DELETE, deleteRequest, MediaDeleteResponse.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deleteResponse.getBody()).isNotNull();
        assertThat(deleteResponse.getBody().getMessage()).isEqualTo("File deleted successfully");

        // Verify file is deleted
        ResponseEntity<byte[]> getResponse = restTemplate.getForEntity(
            MEDIA_ENDPOINT + fileName, byte[].class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldDenyDeleteForCustomer() {
        // Upload as admin
        ByteArrayResource fileResource = createImageResource("test-customer-delete.jpg");
        HttpEntity<MultiValueMap<String, Object>> uploadRequest = createUploadRequest(fileResource, adminToken);
        ResponseEntity<MediaUploadResponse> uploadResponse = restTemplate.postForEntity(
            UPLOAD_ENDPOINT, uploadRequest, MediaUploadResponse.class);

        String fileUrl = uploadResponse.getBody().getUrl();
        String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);

        // Try to delete as customer
        HttpHeaders deleteHeaders = new HttpHeaders();
        deleteHeaders.setBearerAuth(customerToken);
        HttpEntity<Void> deleteRequest = new HttpEntity<>(deleteHeaders);
        
        ResponseEntity<MediaDeleteResponse> deleteResponse = restTemplate.exchange(
            MEDIA_ENDPOINT + fileName, HttpMethod.DELETE, deleteRequest, MediaDeleteResponse.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // Helper methods
    
    private String loginAndGetToken(String username, String password) {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);

        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
            "/login", loginRequest, LoginResponse.class);
        return response.getBody().getToken();
    }
    
    private ByteArrayResource createImageResource(String filename) {
        return new ByteArrayResource(JPEG_HEADER) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }
    
    private HttpEntity<MultiValueMap<String, Object>> createUploadRequest(
            ByteArrayResource fileResource, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);

        return new HttpEntity<>(body, headers);
    }
    
    // Inner class for login response
    private static class LoginResponse {
        private String token;
        
        public String getToken() {
            return token;
        }
        
        public void setToken(String token) {
            this.token = token;
        }
    }
}
