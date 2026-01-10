package com.generator.generator.config;

import com.generator.generator.entity.Role;
import com.generator.generator.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        if (roleRepository.findByName(Role.RoleName.ROLE_USER).isEmpty()) {
            Role userRole = Role.builder()
                    .name(Role.RoleName.ROLE_USER)
                    .description("Default user role")
                    .build();
            roleRepository.save(userRole);
        }

        if (roleRepository.findByName(Role.RoleName.ROLE_ADMIN).isEmpty()) {
            Role adminRole = Role.builder()
                    .name(Role.RoleName.ROLE_ADMIN)
                    .description("Administrator role")
                    .build();
            roleRepository.save(adminRole);
        }
    }
}

