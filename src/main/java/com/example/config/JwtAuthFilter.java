package com.example.config;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.serviceImpl.JwtServiceImpl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtServiceImpl jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip public endpoints
        if (path.startsWith("/auth/login") ||
            path.startsWith("/auth/register") ||
            path.startsWith("/auth/login/send-otp") ||
            path.startsWith("/auth/check-token") ||
            path.startsWith("/auth/validate-token")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Missing Authorization header\"}");
            return;
        }

        String token = authHeader.substring(7).trim();

        if (!jwtService.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Invalid or expired token\"}");
            return;
        }

        // Extract email and roles from JWT
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(jwtService.getSecret().getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody();

        String email = claims.getSubject();
        if (email == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"JWT missing subject\"}");
            return;
        }

        // Roles stored as List<String> in claim "roles"
        List<String> roles = claims.get("roles", List.class);
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (roles != null) {
            roles.forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r.toUpperCase())));
        }

        // Set Authentication in SecurityContext
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(email, null, authorities);
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }
}
