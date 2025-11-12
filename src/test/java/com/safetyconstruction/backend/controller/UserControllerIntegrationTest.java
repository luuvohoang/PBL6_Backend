package com.safetyconstruction.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.safetyconstruction.backend.dto.request.UserCreationRequest;
import com.safetyconstruction.backend.dto.request.UserUpdateRequest;
import com.safetyconstruction.backend.entity.User;
import com.safetyconstruction.backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.stream.Stream;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class UserControllerIntegrationTest {

    @Container
    static final MySQLContainer<?> MY_SQL_CONTAINER = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MY_SQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", MY_SQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", MY_SQL_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "true");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Clear database before each test
        userRepository.deleteAll();

        // Configure ObjectMapper for Java 8 time
        objectMapper.registerModule(new JavaTimeModule());
    }

    // ========== CREATE USER TESTS ==========

    @DisplayName("Create User - Parameterized Tests from Excel")
    @ParameterizedTest(name = "{0}")
    @MethodSource("com.safetyconstruction.backend.util.UserServiceExcelProvider#createUserProvider")
    @WithMockUser(roles = "ADMIN")
    void createUser_ParameterizedTests(String testName, UserCreationRequest request, String expectedError) throws Exception {
        String content = objectMapper.writeValueAsString(request);

        var resultActions = mockMvc.perform(MockMvcRequestBuilders.post("/api/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(content));

        if (expectedError == null) {
            // Expect success
            resultActions
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(1000))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.name").value(request.getName()));
        } else {
            // Expect error
            switch (expectedError) {
                case "USER_EXISTED":
                    resultActions.andExpect(MockMvcResultMatchers.status().isBadRequest());
                    break;
                case "INVALID_EMAIL":
                    resultActions.andExpect(MockMvcResultMatchers.status().isBadRequest());
                    break;
                default:
                    resultActions.andExpect(MockMvcResultMatchers.status().is4xxClientError());
            }
        }

        log.info("Test '{}' completed - Expected error: {}", testName, expectedError);
    }

    // ========== UPDATE USER TESTS ==========

    @DisplayName("Update User - Parameterized Tests from Excel")
    @ParameterizedTest(name = "{0}")
    @MethodSource("com.safetyconstruction.backend.util.UserServiceExcelProvider#updateUserProvider")
    @WithMockUser(roles = "ADMIN")
    void updateUser_ParameterizedTests(String testName, String userId, UserUpdateRequest request, String expectedError) throws Exception {
        // First create a user to update (if not testing user not found scenario)
        if (!"USER_NOT_EXISTED".equals(expectedError)) {
            User existingUser = User.builder()
                    .id(userId)
                    .name("existingUser")
                    .email("existing@email.com")
                    .password("encodedPassword")
                    .build();
            userRepository.save(existingUser);
        }

        String content = objectMapper.writeValueAsString(request);

        var resultActions = mockMvc.perform(MockMvcRequestBuilders.put("/api/users/{userId}", userId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(content));

        if (expectedError == null) {
            resultActions
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(1000));
        } else {
            switch (expectedError) {
                case "USER_NOT_EXISTED":
                    resultActions.andExpect(MockMvcResultMatchers.status().isNotFound());
                    break;
                default:
                    resultActions.andExpect(MockMvcResultMatchers.status().is4xxClientError());
            }
        }
    }

    // ========== DELETE USER TESTS ==========

    @DisplayName("Delete User - Parameterized Tests from Excel")
    @ParameterizedTest(name = "{0}")
    @MethodSource("com.safetyconstruction.backend.util.UserServiceExcelProvider#deleteUserProvider")
    @WithMockUser(roles = "ADMIN")
    void deleteUser_ParameterizedTests(String testName, String userId, String expectedError) throws Exception {
        // Create user first if we're not testing error scenario
        if (expectedError == null) {
            User user = User.builder()
                    .id(userId)
                    .name("userToDelete")
                    .email("delete@email.com")
                    .password("encodedPassword")
                    .build();
            userRepository.save(user);
        }

        var resultActions = mockMvc.perform(MockMvcRequestBuilders.delete("/api/users/{userId}", userId)
                .with(csrf()));

        if (expectedError == null) {
            resultActions.andExpect(MockMvcResultMatchers.status().isOk());
        } else {
            resultActions.andExpect(MockMvcResultMatchers.status().is4xxClientError());
        }
    }

    // ========== GET MY INFO TESTS ==========

    @DisplayName("Get My Info - Parameterized Tests from Excel")
    @ParameterizedTest(name = "{0}")
    @MethodSource("com.safetyconstruction.backend.util.UserServiceExcelProvider#getMyInfoProvider")
    @WithMockUser(username = "loggedInUser", roles = "USER")
    void getMyInfo_ParameterizedTests(String testName, String loggedInUser, String expectedName, String expectedError) throws Exception {
        // Setup test data based on the scenario
        if ("USER_NOT_EXISTED".equals(expectedError)) {
            // Don't create user - simulate user not found
        } else {
            User user = User.builder()
                    .id("user-123")
                    .name(loggedInUser)
                    .email(loggedInUser + "@email.com")
                    .password("encodedPassword")
                    .build();
            userRepository.save(user);
        }

        var resultActions = mockMvc.perform(MockMvcRequestBuilders.get("/api/users/my-info")
                .with(csrf()));

        if (expectedError == null) {
            resultActions
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(1000))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.name").value(expectedName));
        } else {
            resultActions.andExpect(MockMvcResultMatchers.status().isNotFound());
        }
    }

    // ========== GET USER BY ID TESTS ==========

    @DisplayName("Get User By ID - Parameterized Tests from Excel")
    @ParameterizedTest(name = "{0}")
    @MethodSource("com.safetyconstruction.backend.util.UserServiceExcelProvider#getUserProvider")
    void getUserById_ParameterizedTests(String testName, String userId, String loggedInUser,
                                        String userRoles, String expectedUserName, String expectedError) throws Exception {
        // Setup test user in database
        if (!"USER_NOT_FOUND".equals(expectedError)) {
            User user = User.builder()
                    .id(userId)
                    .name(expectedUserName)
                    .email(expectedUserName + "@email.com")
                    .password("encodedPassword")
                    .build();
            userRepository.save(user);
        }

        // Setup security based on roles
        String[] roles = userRoles.split(",");
        String roleString = String.join(",", roles);

        var resultActions = mockMvc.perform(MockMvcRequestBuilders.get("/api/users/{userId}", userId)
                .with(csrf())
                .with(request -> {
                    request.setRemoteUser(loggedInUser);
                    return request;
                }));

        if (expectedError == null) {
            resultActions
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(1000))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.name").value(expectedUserName));
        } else {
            switch (expectedError) {
                case "USER_NOT_FOUND":
                    resultActions.andExpect(MockMvcResultMatchers.status().isNotFound());
                    break;
                case "ACCESS_DENIED":
                    resultActions.andExpect(MockMvcResultMatchers.status().isForbidden());
                    break;
                default:
                    resultActions.andExpect(MockMvcResultMatchers.status().is4xxClientError());
            }
        }
    }

    // ========== GET ALL USERS TESTS ==========

    @DisplayName("Get All Users - Admin Access")
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_AdminAccess_Success() throws Exception {
        // Create some test users
        User user1 = User.builder()
                .id("user-1")
                .name("user1")
                .email("user1@email.com")
                .password("encodedPassword")
                .build();

        User user2 = User.builder()
                .id("user-2")
                .name("user2")
                .email("user2@email.com")
                .password("encodedPassword")
                .build();

        userRepository.saveAll(List.of(user1, user2));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/users")
                        .with(csrf()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(1000))
                .andExpect(MockMvcResultMatchers.jsonPath("$.result.length()").value(2));
    }

    @DisplayName("Get All Users - Non-Admin Access Denied")
    @WithMockUser(roles = "USER")
    void getAllUsers_NonAdmin_AccessDenied() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/users")
                        .with(csrf()))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    // ========== HELPER METHODS ==========

    private User createTestUser(String id, String name, String email) {
        User user = User.builder()
                .id(id)
                .name(name)
                .email(email)
                .password("encodedPassword")
                .build();
        return userRepository.save(user);
    }
}