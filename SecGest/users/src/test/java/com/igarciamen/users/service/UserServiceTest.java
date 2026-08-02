package com.igarciamen.users.service;

import com.igarciamen.users.enums.ERole;
import com.igarciamen.users.model.Role;
import com.igarciamen.users.model.User;
import com.igarciamen.users.repository.RoleRepository;
import com.igarciamen.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepo;
    @Mock private RoleRepository roleRepo;
    @Mock private PasswordEncoder encoder;

    @InjectMocks private UserService userService;

    @Test
    void registerUser_creaUsuarioConRolYPasswordCifrada() {
        when(userRepo.existsByUsername("pepe")).thenReturn(false);
        when(userRepo.existsByEmail("pepe@mail.com")).thenReturn(false);
        when(encoder.encode("123456")).thenReturn("HASH_BCRYPT");
        when(roleRepo.findByName(ERole.ROLE_USER)).thenReturn(Optional.of(new Role(ERole.ROLE_USER)));
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.registerUser("pepe", "pepe@mail.com", "123456");

        assertEquals("pepe", result.getUsername());
        assertEquals("HASH_BCRYPT", result.getPassword());
        assertEquals(1, result.getRoles().size());

        System.out.println("=== registerUser: alta correcta ===");
        System.out.println("Usuario creado      : " + result.getUsername());
        System.out.println("Password almacenada : " + result.getPassword() + " (cifrada, no en texto plano)");
        System.out.println("Roles asignados     : " + result.getRoles().size() + " (solo ROLE_USER; el registro publico ya no da ROLE_SELLER)");
    }

    @Test
    void registerUser_fallaSiUsuarioYaExiste() {
        when(userRepo.existsByUsername("pepe")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.registerUser("pepe", "x@mail.com", "123456"));

        verify(userRepo, never()).save(any());
        System.out.println("=== registerUser: usuario duplicado ===");
        System.out.println("Se rechaza el alta con el mensaje: " + ex.getMessage());
    }

    @Test
    void registerUser_fallaSiEmailEnUso() {
        when(userRepo.existsByUsername("pepe")).thenReturn(false);
        when(userRepo.existsByEmail("pepe@mail.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.registerUser("pepe", "pepe@mail.com", "123456"));

        verify(userRepo, never()).save(any());
        System.out.println("=== registerUser: email en uso ===");
        System.out.println("Se rechaza el alta con el mensaje: " + ex.getMessage());
    }

    @Test
    void findByUsernameOrEmail_devuelveUsuarioSiExiste() {
        User u = new User("ana", "ana@mail.com", "h");
        when(userRepo.findByUsernameOrEmail("ana", "ana")).thenReturn(Optional.of(u));

        User result = userService.findByUsernameOrEmail("ana");

        assertEquals("ana", result.getUsername());
        System.out.println("=== findByUsernameOrEmail: encontrado ===");
        System.out.println("Buscado 'ana' y encontrado el usuario: " + result.getUsername());
    }

    @Test
    void findByUsernameOrEmail_lanzaSiNoExiste() {
        when(userRepo.findByUsernameOrEmail("nadie", "nadie")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.findByUsernameOrEmail("nadie"));

        System.out.println("=== findByUsernameOrEmail: no encontrado ===");
        System.out.println("Mensaje de error: " + ex.getMessage());
    }

    @Test
    void findById_devuelveUsuarioSiExiste() {
        User u = new User("ana", "ana@mail.com", "h");
        when(userRepo.findById(5L)).thenReturn(Optional.of(u));

        User result = userService.findById(5L);

        assertEquals("ana", result.getUsername());
        System.out.println("=== findById: encontrado ===");
        System.out.println("Buscado id=5 y encontrado: " + result.getUsername());
    }

    @Test
    void findById_lanzaSiNoExiste() {
        when(userRepo.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.findById(99L));

        System.out.println("=== findById: no encontrado ===");
        System.out.println("Mensaje de error: " + ex.getMessage());
    }
}
