package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // disable CSRF for APIs
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // stateless
            .authorizeHttpRequests(auth -> auth
//                .requestMatchers("/auth/**").permitAll() 
            		.requestMatchers(
            				"/auth/**",
            				"/auth/login/send-otp/**",
            				"/auth/updated/save",
            			    "/auth/login/**",
            			    "/auth/register/**",
            			    "/auth/otp/**","/auth/check-email/{email}",
            			    "/manageusers/searchAndsorting/getall/**",
            			    "/auth/updated/save/**"
//            			    "/auth/getall/privileges"
            			).permitAll()

                .anyRequest().authenticated()              // all others require JWT
            )
            
            .formLogin(form -> form.disable())           // disable form login
            .httpBasic(httpBasic -> httpBasic.disable()); // disable basic auth
        return http.build();
    }
}
