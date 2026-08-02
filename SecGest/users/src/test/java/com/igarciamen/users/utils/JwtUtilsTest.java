package com.igarciamen.users.utils;

import com.igarciamen.users.enums.ERole;
import com.igarciamen.users.model.Role;
import com.igarciamen.users.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private static final String SECRET =
            "test-secret-key-for-jwt-1234567890-abcdefghijklmnopqrstuvwxyz";
    private static final int EXPIRATION_MS = 86_400_000; // 24 horas

    @Test
    void generateJwtToken_contieneLosClaimsEsperados() {
        JwtUtils jwtUtils = new JwtUtils(SECRET, EXPIRATION_MS);

        User user = new User("isabel", "isabel@admin.local", "hashed");
        user.setId(1L);
        user.getRoles().add(new Role(ERole.ROLE_ADMIN));

        String token = jwtUtils.generateJwtToken(user);

        assertNotNull(token);
        // Un JWT tiene tres partes separadas por puntos: header.payload.signature
        assertEquals(3, token.split("\\.").length);

        Claims claims = parse(token);
        assertEquals("isabel", claims.getSubject());
        assertEquals(1, ((Number) claims.get("userId")).intValue());

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.get("roles");
        assertTrue(roles.contains("ROLE_ADMIN"));

        System.out.println("=== Test 1: claims del token ===");
        System.out.println("Token generado: " + token);
        System.out.println("subject : " + claims.getSubject());
        System.out.println("userId  : " + claims.get("userId"));
        System.out.println("roles   : " + roles);
    }

    @Test
    void generateJwtToken_fijaEmisionYExpiracion() {
        JwtUtils jwtUtils = new JwtUtils(SECRET, EXPIRATION_MS);

        User user = new User("marco", "marco@mail.com", "hashed");
        user.setId(2L);
        user.getRoles().add(new Role(ERole.ROLE_ADMIN));

        String token = jwtUtils.generateJwtToken(user);
        Claims claims = parse(token);

        Date issuedAt = claims.getIssuedAt();
        Date expiration = claims.getExpiration();
        assertNotNull(issuedAt);
        assertNotNull(expiration);

        // La expiracion debe ser aproximadamente 24 h despues de la emision.
        long diff = expiration.getTime() - issuedAt.getTime();
        assertTrue(Math.abs(diff - EXPIRATION_MS) <= 1000);
        assertTrue(expiration.after(new Date()));

        System.out.println("=== Test 2: emision y expiracion ===");
        System.out.println("Emitido en : " + issuedAt);
        System.out.println("Expira en  : " + expiration);
        System.out.println("Duracion   : " + (diff / 1000 / 60 / 60) + " horas");
    }

    @Test
    void generateJwtToken_firmaInvalidaConOtraClave() {
        JwtUtils jwtUtils = new JwtUtils(SECRET, EXPIRATION_MS);
        User user = new User("lucia", "lucia@mail.com", "hashed");
        user.setId(3L);
        user.getRoles().add(new Role(ERole.ROLE_USER));

        String token = jwtUtils.generateJwtToken(user);

        // Con un secreto distinto, la verificacion de la firma debe fallar.
        SecretKey otraClave = Keys.hmacShaKeyFor(
                "otra-clave-totalmente-distinta-1234567890-abcdefgh".getBytes(StandardCharsets.UTF_8));
        Exception ex = assertThrows(Exception.class, () ->
                Jwts.parser().verifyWith(otraClave).build().parseSignedClaims(token));

        System.out.println("=== Test 3: firma invalida con otra clave ===");
        System.out.println("Con la clave equivocada se rechaza el token: "
                + ex.getClass().getSimpleName());
    }

    private Claims parse(String token) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
