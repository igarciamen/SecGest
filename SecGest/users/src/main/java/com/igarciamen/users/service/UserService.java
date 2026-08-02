package com.igarciamen.users.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.igarciamen.users.enums.ERole;
import com.igarciamen.users.model.Role;
import com.igarciamen.users.model.User;
import com.igarciamen.users.repository.RoleRepository;
import com.igarciamen.users.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder encoder;

    public UserService(UserRepository userRepo,
                       RoleRepository roleRepo,
                       PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.encoder  = encoder;
    }

      public User registerUser(String username, String email, String rawPassword) {

        if (userRepo.existsByUsername(username)) {
            throw new IllegalArgumentException("User already exists: " + username);
        }
        if (userRepo.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is in use: " + email);
        }

        String encoded = encoder.encode(rawPassword);
        User user = new User(username, email, encoded);

        Role roleUser = roleRepo.findByName(ERole.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("ROLE_USER does not exist"));
        user.getRoles().add(roleUser);

        return userRepo.save(user);
    }

    public User findByUsername(String username) {
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    public User findByUsernameOrEmail(String login) {
        return userRepo.findByUsernameOrEmail(login, login)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + login));
    }

    public User findById(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }
}