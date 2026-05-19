package com.lianzhi.wms.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {
  private static final String BEARER_PREFIX = "Bearer ";

  private final TokenAuthenticationService tokenAuthenticationService;

  public BearerTokenAuthenticationFilter(TokenAuthenticationService tokenAuthenticationService) {
    this.tokenAuthenticationService = tokenAuthenticationService;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {
    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      String header = request.getHeader(HttpHeaders.AUTHORIZATION);
      if (header != null && header.startsWith(BEARER_PREFIX)) {
        String token = header.substring(BEARER_PREFIX.length()).trim();
        tokenAuthenticationService.authenticate(token).ifPresent(principal -> {
          UsernamePasswordAuthenticationToken authentication =
              new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
          authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(authentication);
        });
      }
    }

    filterChain.doFilter(request, response);
  }
}
