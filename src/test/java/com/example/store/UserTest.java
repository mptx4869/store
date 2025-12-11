package com.example.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import com.example.store.model.Role;
import com.example.store.model.User;
import com.example.store.repository.RoleRepository;
import com.example.store.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserTest {
    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    private Role customerRole;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role admin = new Role();
        admin.setName("ADMIN");
        roleRepository.save(admin);

        customerRole = new Role();
        customerRole.setName("CUSTOMER");
        customerRole = roleRepository.save(customerRole);

        User baselineUser = User.builder()
            .username("nhanhoa")
            .passwordHash("encoded")
            .email("hoa@example.com")
            .role(customerRole)
            .build();
        userRepository.save(baselineUser);
    }

	@Test
    public void testLoadDatabase(){
        User user = userRepository.findByUsername("nhanhoa").orElse(null);
        assertThat(user).isNotNull();
        assertThat(user.getRole()).isNotNull();
        assertThat(user.getRole().getName()).isEqualTo("ROLE_CUSTOMER");
    }
   

    @Test
    @DirtiesContext
    @Transactional
    public void testDeleteUser(){
        User newUser = User.builder()
            .username("newUser")
            .passwordHash("password")
            .email("newUser@example.com")
            .role(customerRole)
            .build();
        userRepository.save(newUser);

        User user = userRepository.findByUsername("newUser").orElse(null);
        assertThat(user).isNotNull();
        userRepository.deleteByUsername(user.getUsername());
        user = userRepository.findByUsername("newUser").orElse(null);
        assertThat(user).isNull();
    }
    
    @Test
    @DirtiesContext
    public void testCreateUser(){
        User newUser = User.builder()
            .username("createUser")
            .passwordHash("password")
            .email("createUser@example.com")
            .role(customerRole)
            .build();
        userRepository.save(newUser);

        User user = userRepository.findByUsername("createUser").orElse(null);
        assertThat(user).isNotNull();
        assertThat(user.getEmail()).isEqualTo("createUser@example.com");
    }

}
