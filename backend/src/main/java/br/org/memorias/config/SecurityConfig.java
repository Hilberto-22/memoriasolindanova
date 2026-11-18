package br.org.memorias.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
class SecurityConfig {
        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public UserDetailsService users(@Value("${memorias.admin-user}") String name,
                        @Value("${memorias.admin-password}") String password,
                        PasswordEncoder encoder) {
                if (password.length() < 12) {
                        throw new IllegalStateException("Defina ADMIN_PASSWORD com pelo menos 12 caracteres no .env da raiz do projeto ou nas variáveis de ambiente.");
                }
                var admin = User.withUsername(name)
                                .password(encoder.encode(password))
                                .roles("ADMIN")
                                .build();
                return new InMemoryUserDetailsManager(admin);
        }

        @Bean
        public SecurityFilterChain http(HttpSecurity http) throws Exception {
                return http
                                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                                                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.GET, "/api/session").hasRole("ADMIN")
                                                .anyRequest().permitAll())
                                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response,
                                                failure) -> response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)))
                                .requestCache(cache -> cache.disable())
                                .formLogin(form -> form.loginPage("/entrar").loginProcessingUrl("/api/session")
                                                .successHandler((request, response, authentication) -> response
                                                                .setStatus(HttpServletResponse.SC_NO_CONTENT))
                                                .failureHandler((request, response, failure) -> response
                                                                .setStatus(HttpServletResponse.SC_UNAUTHORIZED)))
                                .logout(logout -> logout.logoutUrl("/api/logout")
                                                .deleteCookies("JSESSIONID")
                                                .logoutSuccessHandler((request, response, authentication) -> response
                                                                .setStatus(HttpServletResponse.SC_NO_CONTENT)))
                                .build();
        }
}
