// File: src/test/java/com/safetyconstruction/backend/util/TestDataInitializer.java
package com.safetyconstruction.backend.util;

import com.safetyconstruction.backend.entity.Role;
import com.safetyconstruction.backend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TestDataInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(ApplicationArguments args) {
        // Chỉ tạo roles nếu chưa tồn tại
        if (roleRepository.findByName("USER").isEmpty()) {
            Role userRole = Role.builder()
                    .name("USER")
                    .description("Default user role")
                    .build();
            roleRepository.save(userRole);
        }

        if (roleRepository.findByName("ADMIN").isEmpty()) {
            Role adminRole = Role.builder()
                    .name("ADMIN")
                    .description("Administrator role")
                    .build();
            roleRepository.save(adminRole);
        }


    }
}