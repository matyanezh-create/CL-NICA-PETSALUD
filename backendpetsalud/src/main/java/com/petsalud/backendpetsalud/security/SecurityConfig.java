package com.petsalud.backendpetsalud.security;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.http.HttpMethod;
@Configuration @EnableWebSecurity @EnableMethodSecurity
public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}
 @Bean AuthenticationManager authenticationManager(AuthenticationConfiguration c)throws Exception{return c.getAuthenticationManager();}
 @Bean SecurityFilterChain securityFilterChain(HttpSecurity http,JwtUtil jwt,UsuarioDetailsService users)throws Exception {
  return http.csrf(c->c.disable()).httpBasic(c->c.disable()).formLogin(c->c.disable()).logout(c->c.disable())
   .sessionManagement(c->c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .requestCache(c->c.disable()).userDetailsService(users)
   .exceptionHandling(c->c.authenticationEntryPoint((req,res,e)->{res.setStatus(401);res.setContentType("application/json");res.getWriter().write("{\"error\":\"Autenticacion requerida\"}");})
    .accessDeniedHandler((req,res,e)->{res.setStatus(403);res.setContentType("application/json");res.getWriter().write("{\"error\":\"Acceso denegado\"}");}))
   .authorizeHttpRequests(c->c
    .requestMatchers(HttpMethod.POST,"/api/auth/registro","/api/usuarios/registro","/api/auth/login","/api/login").permitAll()
    .requestMatchers("/api/auth/me").authenticated()
    .requestMatchers(HttpMethod.GET,"/api/productos/stock-bajo").hasAnyRole("EMPLEADO","GERENTE")
    .requestMatchers(HttpMethod.GET,"/api/productos","/api/productos/**").hasAnyRole("CLIENTE","EMPLEADO","GERENTE")
    .requestMatchers("/api/productos/**","/api/productos").hasRole("GERENTE")
    .requestMatchers("/api/usuarios","/api/usuarios/**","/api/mascotas/**").authenticated()
    .requestMatchers("/api/citas/**","/api/citas","/api/expedientes/**","/api/expedientes","/api/ventas/**","/api/ventas","/api/recompensas/**","/api/recompensas","/api/notificaciones/**","/api/notificaciones").hasAnyRole("EMPLEADO","GERENTE")
    .anyRequest().denyAll())
   .addFilterBefore(new JwtRequestFilter(jwt,users),UsernamePasswordAuthenticationFilter.class).build();
 }
}
