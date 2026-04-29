package com.bobbuy;

import com.bobbuy.security.RoleInjectionFilter;
import com.bobbuy.security.InternalServiceTokenFilter;
import com.bobbuy.security.TokenAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final InternalServiceTokenFilter internalServiceTokenFilter;
    private final TokenAuthenticationFilter tokenAuthenticationFilter;
    private final RoleInjectionFilter roleInjectionFilter;
    private final List<String> allowedOriginPatterns;

    public SecurityConfig(InternalServiceTokenFilter internalServiceTokenFilter,
                          TokenAuthenticationFilter tokenAuthenticationFilter,
                          RoleInjectionFilter roleInjectionFilter,
                          @Value("${bobbuy.security.cors.allowed-origin-patterns:http://localhost,http://127.0.0.1,http://localhost:4173,http://127.0.0.1:4173,http://localhost:5173,http://127.0.0.1:5173}") String allowedOriginPatterns) {
        this.internalServiceTokenFilter = internalServiceTokenFilter;
        this.tokenAuthenticationFilter = tokenAuthenticationFilter;
        this.roleInjectionFilter = roleInjectionFilter;
        this.allowedOriginPatterns = Stream.of(allowedOriginPatterns.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .toList();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler((request, response, accessDeniedException) -> response.sendError(HttpStatus.FORBIDDEN.value()))
            )
            .addFilterBefore(internalServiceTokenFilter, AnonymousAuthenticationFilter.class)
            .addFilterBefore(tokenAuthenticationFilter, AnonymousAuthenticationFilter.class)
            .addFilterBefore(roleInjectionFilter, AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/health/**").permitAll()
                .requestMatchers("/internal/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                .requestMatchers("/ws", "/ws/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/procurement/*/ledger").hasAnyRole("CUSTOMER", "AGENT")
                .requestMatchers(HttpMethod.POST, "/api/procurement/*/ledger/*/confirm").hasAnyRole("CUSTOMER", "AGENT")
                .requestMatchers("/api/procurement/**").hasRole("AGENT")
                .requestMatchers("/api/users/**").hasRole("AGENT")
                .requestMatchers("/api/audit-logs/**").hasRole("AGENT")
                .requestMatchers("/api/metrics/**").hasRole("AGENT")
                .requestMatchers(HttpMethod.PATCH, "/api/mobile/products/**").hasRole("AGENT")
                .requestMatchers("/api/financial/audit/**").hasRole("AGENT")
                .requestMatchers(HttpMethod.POST, "/api/trips/**").hasRole("AGENT")
                .requestMatchers(HttpMethod.PUT, "/api/trips/**").hasRole("AGENT")
                .requestMatchers(HttpMethod.PATCH, "/api/trips/**").hasRole("AGENT")
                .requestMatchers(HttpMethod.DELETE, "/api/trips/**").hasRole("AGENT")
                .requestMatchers(HttpMethod.GET, "/api/orders/**").hasAnyRole("CUSTOMER", "AGENT", "MERCHANT")
                .requestMatchers(HttpMethod.POST, "/api/orders").hasRole("AGENT")
                .requestMatchers(HttpMethod.PUT, "/api/orders/**").hasRole("AGENT")
                .requestMatchers(HttpMethod.DELETE, "/api/orders/**").hasRole("AGENT")
                .requestMatchers(HttpMethod.PATCH, "/api/orders/**").hasRole("AGENT")
                .anyRequest().authenticated()
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(allowedOriginPatterns.isEmpty() ? Arrays.asList("http://localhost", "http://127.0.0.1") : allowedOriginPatterns);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("X-Trace-Id"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
