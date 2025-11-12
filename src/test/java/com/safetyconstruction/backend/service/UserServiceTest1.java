package com.safetyconstruction.backend.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.safetyconstruction.backend.dto.request.UserCreationRequest;
import com.safetyconstruction.backend.dto.response.UserResponse;
import com.safetyconstruction.backend.entity.User;
import com.safetyconstruction.backend.exception.AppException;
import com.safetyconstruction.backend.repository.UserRepository;

@SpringBootTest
@TestPropertySource("/test.properties")
public class UserServiceTest1 {
    @Autowired
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    private UserCreationRequest userCreationRequest;
    private UserResponse userResponse;
    private User user;

    @BeforeEach
    void initData() {
        userCreationRequest = UserCreationRequest.builder()
                .name("testuser")
                .password("password")
                .email("hoang@gmail.com")
                .build();

        userResponse = UserResponse.builder().id("1").name("testuser").build();

        user = User.builder().id("1").name("testuser").build();
    }

    @Test
    void createUser_validRequest_success() {
        // GIVEN
        when(userRepository.existsByName(anyString())).thenReturn(false);
        when(userRepository.save(any())).thenReturn(user);

        // WHEN
        var response = userService.createUser(userCreationRequest);
        // THEN

        Assertions.assertThat(response.getId()).isEqualTo("1");
        Assertions.assertThat(response.getName()).isEqualTo("testuser");
    }

    @Test
    void createUser_userExisted_fail() {
        // GIVEN
        when(userRepository.existsByName(anyString())).thenReturn(true);

        // WHEN
        var exception = assertThrows(AppException.class, () -> userService.createUser(userCreationRequest));

        // THEN
        Assertions.assertThat(exception.getErrorCode().getCode()).isEqualTo(1002);
    }

    @Test
    @WithMockUser(username = "testuser")
    void getMyInfo_valid_success() {
        when(userRepository.findByName(anyString())).thenReturn(Optional.of(user));

        var response = userService.getMyInfo();

        Assertions.assertThat(response.getName()).isEqualTo("testuser");
        Assertions.assertThat(response.getId()).isEqualTo("1");
    }

    @Test
    @WithMockUser(username = "testuser")
    void getMyInfo_userNotFound_error() {
        when(userRepository.findByName(anyString())).thenReturn(Optional.ofNullable(null));

        // WHEN
        var exception = assertThrows(AppException.class, () -> userService.getMyInfo());

        Assertions.assertThat(exception.getErrorCode().getCode()).isEqualTo(1005);
    }
}
