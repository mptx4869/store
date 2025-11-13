package com.example.store;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import com.example.store.model.User;
import com.example.store.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserTest {
     @Autowired
    UserRepository userRepository;

	@Test
    public void testLoadDatabase(){
        User user = userRepository.findByUsername("nhanhoa");
        assertThat(user).isNotNull();
        assertThat(user.getRole_id()).isEqualTo(2);
    }
   

    @Test
    @DirtiesContext
    @Transactional
    public void testDeleteUser(){

        User newUser = new User("newUser","password","newUser@example.com",2);
        userRepository.save(newUser);

        User user = userRepository.findByUsername("newUser");
        assertThat(user).isNotNull();
        userRepository.deleteByUsername(user.getUsername());

        user = userRepository.findByUsername("newUser");
        assertThat(user).isNull();
    }
    
    @Test
    @DirtiesContext
    public void testCreateUser(){
        User newUser = new User("createUser","password","createUser@example.com",2);
        userRepository.save(newUser);

        User user = userRepository.findByUsername("createUser");
        assertThat(user).isNotNull();
        assertThat(user.getEmail()).isEqualTo("createUser@example.com");
    }

}
