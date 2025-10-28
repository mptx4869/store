package com.example.store;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.store.model.User;
import com.example.store.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StoreApplicationTests {

    @Autowired
    UserRepository userRepository;

	@Test
    public void testLoadDatabase(){
        User user = userRepository.findByUsername("nhanhoa");
        assertThat(user).isNotNull();
        assertThat(user.getRole()).isEqualTo("USER");
    }
}
