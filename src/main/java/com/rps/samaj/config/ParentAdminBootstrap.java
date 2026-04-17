package com.rps.samaj.config;

import com.rps.samaj.user.model.KycStatus;
import com.rps.samaj.user.model.User;
import com.rps.samaj.user.service.UserAccountProvisioner;
import com.rps.samaj.user.repository.UserRepository;
import com.rps.samaj.user.model.UserRole;
import com.rps.samaj.user.model.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Component
public class ParentAdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ParentAdminBootstrap.class);

    private final SamajProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserAccountProvisioner userAccountProvisioner;

    public ParentAdminBootstrap(
            SamajProperties properties,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            UserAccountProvisioner userAccountProvisioner
    ) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userAccountProvisioner = userAccountProvisioner;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByParentAdminIsTrueAndStatus(UserStatus.ACTIVE)) {
            return;
        }
        String email = properties.getBootstrap().getParentAdmin().getEmail();
        String password = properties.getBootstrap().getParentAdmin().getPassword();
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            return;
        }
        String normalized = email.trim().toLowerCase();
        if (userRepository.findByEmailIgnoreCase(normalized).isPresent()) {
            return;
        }
        UUID id = UUID.randomUUID();
        User admin = new User(
                id,
                normalized,
                null,
                passwordEncoder.encode(password),
                UserStatus.ACTIVE,
                UserRole.ADMIN
        );
        admin.setEmailVerified(true);
        admin.setPhoneVerified(false);
        admin.setParentAdmin(true);
        admin.setKycStatus(KycStatus.NONE);
        admin.setMetadata("{}");
        admin = userRepository.save(admin);
        userAccountProvisioner.ensureSidecars(admin);
        log.info("Bootstrapped parent admin for email {}", normalized);
    }
}
