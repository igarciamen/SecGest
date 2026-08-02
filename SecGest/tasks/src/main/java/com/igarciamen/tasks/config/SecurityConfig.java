package com.igarciamen.tasks.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class SecurityConfig {

    // MISMO jwt.secret que el microservicio users: asi este servicio puede validar,
    // sin llamar a users, los tokens que users emitio en el login.
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html", "/webjars/**").permitAll()
                        // Notificacion de Redsys: la manda Redsys, no lleva (ni puede llevar)
                        // un JWT nuestro. Se declara publica y ANTES que la regla general de
                        // POST (que si no, la exigiria ROLE_USER como cualquier otro POST).
                        // La seguridad aqui no es el JWT, es la firma HMAC que valida el propio
                        // controller antes de procesar nada.
                        .requestMatchers(HttpMethod.POST, "/api/tasks/payments/redsys/notify").permitAll()
                        // Solo ROLE_USER puede solicitar tareas: el admin es la agencia que las
                        // gestiona (presupuesta/asigna en bloques siguientes), no quien las pide.
                        .requestMatchers(HttpMethod.POST, "/api/tasks/**").hasAuthority("ROLE_USER")
                        // Solo ROLE_ADMIN puede ver el listado de pendientes de presupuestar
                        // (de todos los clientes) y fijar el presupuesto de una tarea.
                        .requestMatchers(HttpMethod.GET, "/api/tasks/pending").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/tasks/all").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/tasks/metrics").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/tasks/calendar").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/tasks/*/budget").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/tasks/*/deliver").hasAuthority("ROLE_ADMIN")
                        // Aceptar, rechazar, pagar, confirmar y editar: solo ROLE_USER (la
                        // pertenencia a la tarea en si se comprueba en TaskService, no aqui).
                        .requestMatchers(HttpMethod.PUT, "/api/tasks/*/accept").hasAuthority("ROLE_USER")
                        .requestMatchers(HttpMethod.PUT, "/api/tasks/*/reject").hasAuthority("ROLE_USER")
                        .requestMatchers(HttpMethod.PUT, "/api/tasks/*/pay").hasAuthority("ROLE_USER")
                        .requestMatchers(HttpMethod.PUT, "/api/tasks/*/complete").hasAuthority("ROLE_USER")
                        .requestMatchers(HttpMethod.PUT, "/api/tasks/*").hasAuthority("ROLE_USER")
                        // /api/tasks/mine: cualquier autenticado (a un admin le devolvera vacio).
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())));
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder() {
        SecretKey key = new SecretKeySpec(
                jwtSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    private JwtAuthenticationConverter jwtAuthConverter() {
        JwtGrantedAuthoritiesConverter granted = new JwtGrantedAuthoritiesConverter();
        granted.setAuthoritiesClaimName("roles");
        granted.setAuthorityPrefix("");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(granted);
        return converter;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setExposedHeaders(List.of("Authorization"));
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}
