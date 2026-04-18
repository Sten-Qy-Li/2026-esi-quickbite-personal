package ee.ut.esi.quickbite.restaurant.config;

import ee.ut.esi.quickbite.restaurant.security.JwtAuthFilter;
import ee.ut.esi.quickbite.restaurant.security.RestAuthEntryPoints;
import ee.ut.esi.quickbite.restaurant.security.SecurityRoles;
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
                // Public browse endpoints per 0010 §6.
                .requestMatchers(HttpMethod.GET, "/restaurants").permitAll()
                .requestMatchers(HttpMethod.GET, "/restaurants/{id:[0-9a-fA-F-]+}").permitAll()
                // Availability: any valid token (customer/owner/driver/admin/service) per 0010 §8.
                .requestMatchers(HttpMethod.GET, "/restaurants/*/availability").authenticated()
                // Mutations: restaurant owner or admin.
                .requestMatchers(HttpMethod.POST,   "/restaurants")
                    .hasAnyRole(SecurityRoles.RESTAURANT_OWNER, SecurityRoles.ADMIN)
                .requestMatchers(HttpMethod.PUT,    "/restaurants/*")
                    .hasAnyRole(SecurityRoles.RESTAURANT_OWNER, SecurityRoles.ADMIN)
                .requestMatchers(HttpMethod.PATCH,  "/restaurants/*/status")
                    .hasAnyRole(SecurityRoles.RESTAURANT_OWNER, SecurityRoles.ADMIN)
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:8080"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
