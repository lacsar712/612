package com.lianzhi.wms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lianzhi.wms.security.BearerTokenAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  private final BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter;
  private final ObjectMapper objectMapper;

  public SecurityConfig(BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter,
                        ObjectMapper objectMapper) {
    this.bearerTokenAuthenticationFilter = bearerTokenAuthenticationFilter;
    this.objectMapper = objectMapper;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .sessionManagement(configurer -> configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers("/auth/login", "/error", "/actuator/health").permitAll()
            .anyRequest().authenticated()
        )
        .exceptionHandling(configurer -> configurer
            .authenticationEntryPoint((request, response, ex) -> writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                "未登录或登录已失效"))
            .accessDeniedHandler((request, response, ex) -> writeJson(response, HttpServletResponse.SC_FORBIDDEN,
                "无权限执行当前操作"))
        )
        .addFilterBefore(bearerTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  private void writeJson(HttpServletResponse response, int status, String message) throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    objectMapper.writeValue(response.getWriter(), Map.of("message", message));
  }
}
