package com.bobbuy;

import com.bobbuy.security.RoleInjectionFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final RoleInjectionFilter roleInjectionFilter;

    public SecurityConfig(RoleInjectionFilter roleInjectionFilter) {
        this.roleInjectionFilter = roleInjectionFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .addFilterBefore(roleInjectionFilter, AnonymousAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/health/**").permitAll()
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
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
