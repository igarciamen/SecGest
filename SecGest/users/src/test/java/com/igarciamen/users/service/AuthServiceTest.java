package com.igarciamen.users.service;

import com.igarciamen.users.model.User;
import com.igarciamen.users.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserService userService;
    @Mock private JwtUtils jwtUtils;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private AuthService authService;

    @Test
    void authenticate_devuelveTokenConCredencialesValidas() {
        User user = new User("isabel", "isabel@admin.local", "HASH");
        when(userService.findByUsernameOrEmail("isabel")).thenReturn(user);
        when(passwordEncoder.matches("123456", "HASH")).thenReturn(true);
        when(jwtUtils.generateJwtToken(user)).thenReturn("token-xyz");

        String token = authService.authenticate("isabel", "123456");

        assertEquals("token-xyz", token);
        System.out.println("=== authenticate: credenciales correctas ===");
        System.out.println("Login 'isabel' + password correcta => token: " + token);
    }

    @Test
    void authenticate_passwordIncorrectaDevuelve401() {
        User user = new User("isabel", "isabel@admin.local", "HASH");
        when(userService.findByUsernameOrEmail("isabel")).thenReturn(user);
        when(passwordEncoder.matches("malisima", "HASH")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.authenticate("isabel", "malisima"));

        assertEquals(401, ex.getStatusCode().value());
        verify(jwtUtils, never()).generateJwtToken(any());
        System.out.println("=== authenticate: password incorrecta ===");
        System.out.println("Resultado: " + ex.getStatusCode().value() + " (no se genera token)");
    }

    @Test
    void authenticate_usuarioInexistenteDevuelve401() {
        when(userService.findByUsernameOrEmail("fantasma"))
                .thenThrow(new IllegalArgumentException("User not found: fantasma"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.authenticate("fantasma", "123456"));

        assertEquals(401, ex.getStatusCode().value());
        System.out.println("=== authenticate: usuario inexistente ===");
        System.out.println("Resultado: " + ex.getStatusCode().value()
                + " con el mismo mensaje generico 'Wrong user or password'");
    }
}
