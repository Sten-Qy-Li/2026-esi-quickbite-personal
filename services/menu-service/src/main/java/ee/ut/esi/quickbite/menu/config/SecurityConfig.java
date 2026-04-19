package ee.ut.esi.quickbite.menu.config;

import ee.ut.esi.quickbite.menu.security.JwtAuthFilter;
import ee.ut.esi.quickbite.menu.security.RestAuthEntryPoints;
import ee.ut.esi.quickbite.menu.security.SecurityRoles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RestAuthEntryPoints authEntryPoints;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, RestAuthEntryPoints authEntryPoints) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.authEntryPoints = authEntryPoints;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authEntryPoints.unauthorizedEntryPoint())
                .accessDeniedHandler(authEntryPoints.forbiddenHandler()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/actuator/health", "/actuator/info",
                    "/v3/api-docs/**", "/v3/api-docs.yaml",
                    "/swagger-ui/**", "/swagger-ui.html"
                ).permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Public browse endpoints per 0010 §6 / §8.
                .requestMatchers(HttpMethod.GET, "/restaurants/*/menu-items").permitAll()
                .requestMatchers(HttpMethod.GET, "/menu-items/{id:[0-9a-fA-F-]+}").permitAll()
                // Batch validate: any valid token (customer/owner/driver/admin/service).
                .requestMatchers(HttpMethod.POST, "/menu-items/validate").authenticated()
                // Mutations: restaurant owner or admin.
                .requestMatchers(HttpMethod.POST,   "/restaurants/*/menu-items")
                    .hasAnyRole(SecurityRoles.RESTAURANT_OWNER, SecurityRoles.ADMIN)
                .requestMatchers(HttpMethod.PUT,    "/menu-items/*")
                    .hasAnyRole(SecurityRoles.RESTAURANT_OWNER, SecurityRoles.ADMIN)
                .requestMatchers(HttpMethod.DELETE, "/menu-items/*")
                    .hasAnyRole(SecurityRoles.RESTAURANT_OWNER, SecurityRoles.ADMIN)
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Browser-visible origins that may call the service directly (dev)
        // or via the gateway. The compose frontend is served by nginx on
        // :80 (container) / :8090 (host). The Vue CLI dev server is
        // :8090. Gateway itself is :8080.
        config.setAllowedOrigins(List.of(
            "http://localhost",
            "http://localhost:5173",
            "http://localhost:8080",
            "http://localhost:8090"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
