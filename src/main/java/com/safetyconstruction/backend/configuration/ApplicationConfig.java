// File: src/main/java/com/safetyconstruction/backend/configuration/ApplicationConfig.java
package com.safetyconstruction.backend.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class ApplicationConfig {

    /**
     * Di chuyển bean PasswordEncoder vào đây
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    // (Bạn cũng có thể đặt các bean chung chung khác ở đây)
}
