package com.shiftleft.hub.config;

import com.shiftleft.hub.common.domain.WorkspaceContextHolder;
import com.shiftleft.hub.user.domain.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RateLimitingFilter rateLimitingFilter;

    /**
     * Provides the BCrypt password encoder.
     *
     * @return the password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Creates a new SecurityConfig.
     *
     * @param userRepository       the user repository
     * @param jwtService           the JWT service
     * @param rateLimitingFilter   the rate limiting filter
     */
    public SecurityConfig(UserRepository userRepository, JwtService jwtService,
            RateLimitingFilter rateLimitingFilter) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.rateLimitingFilter = rateLimitingFilter;
    }

    /**
     * Configures the HTTP security filter chain.
     *
     * @param http the HttpSecurity to configure
     * @return the built SecurityFilterChain
     * @throws Exception on configuration error
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // CSRF disabled: stateless JWT API. Mitigated by SameSite=None cookies with HTTPS (cross-site production).
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000))
                .contentTypeOptions(Customizer.withDefaults())
                .frameOptions(frame -> frame.deny())
            )
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/articles/**").permitAll()
                .requestMatchers("/api/ai/**").authenticated()
                .requestMatchers("/api/tickets/**").authenticated()
                .requestMatchers("/api/agent/**").hasAnyRole("AGENT", "ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter(),
                UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Value("${app.cors.allowed-origins:http://localhost:4200}")
    private String[] allowedOrigins;

    /**
     * Configures CORS settings for the application.
     *
     * @return the CORS configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of(allowedOrigins));
        config.setAllowedMethods(
            List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(
            List.of("Content-Type", "Authorization", "X-Workspace-Id", "Accept", "X-Requested-With"));

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Creates a filter that reads the JWT from a cookie and sets the security context.
     *
     * @return the JWT authentication filter
     */
    @Bean
    public OncePerRequestFilter jwtAuthenticationFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    FilterChain filterChain)
                    throws ServletException, IOException {

                Cookie[] cookies = request.getCookies();
                if (cookies != null) {
                    String accessToken = Arrays.stream(cookies)
                        .filter(c -> "access_token".equals(c.getName()))
                        .map(Cookie::getValue)
                        .findFirst()
                        .orElse(null);

                    if (accessToken != null
                            && jwtService.isTokenValid(accessToken)) {
                        try {
                            var userId = jwtService.extractUserId(accessToken);
                            var user = userRepository.findById(userId);
                            if (user.isPresent()) {
                                var u = user.get();
                                var principal = new User(
                                    u.getEmail(), "",
                                    List.of(new SimpleGrantedAuthority(
                                        u.getRole().name())));
                                SecurityContextHolder.getContext()
                                    .setAuthentication(
                                        new UsernamePasswordAuthenticationToken(
                                            principal, null,
                                            principal.getAuthorities()));
                                UUID workspaceId = jwtService.extractWorkspaceId(accessToken);
                                if (workspaceId != null) {
                                    WorkspaceContextHolder.setCurrentWorkspaceId(workspaceId);
                                }
                            } else {
                                log.debug("JWT user not found in DB — clearing auth context");
                                SecurityContextHolder.clearContext();
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.getWriter().write("{\"error\":\"Session expired — please log in again\"}");
                                return;
                            }
                        } catch (Exception e) {
                            log.warn("JWT validation failed", e);
                            SecurityContextHolder.clearContext();
                        }
                    }
                }

                try {
                    filterChain.doFilter(request, response);
                } finally {
                    WorkspaceContextHolder.clear();
                }
            }
        };
    }
}
