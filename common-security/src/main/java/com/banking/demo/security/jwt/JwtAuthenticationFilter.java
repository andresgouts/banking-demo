package com.banking.demo.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        try {
            if (jwtService.isTokenValid(jwt)) {
                String subject = jwtService.extractSubject(jwt);

                if (subject != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    List<String> roles = jwtService.extractRoles(jwt);
                    List<SimpleGrantedAuthority> authorities = (roles != null && !roles.isEmpty())
                            ? roles.stream().map(SimpleGrantedAuthority::new).toList()
                            : Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));

                    log.debug("Token JWT valido autenticado para usuario [{}] con roles {} en {}",
                            subject, authorities, request.getRequestURI());
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            subject,
                            null,
                            authorities
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } else {
                log.warn("Token JWT invalido o expirado para peticion a {}", request.getRequestURI());
            }
        } catch (Exception e) {
            log.warn("Error al procesar token JWT en {}: {}", request.getRequestURI(), e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
