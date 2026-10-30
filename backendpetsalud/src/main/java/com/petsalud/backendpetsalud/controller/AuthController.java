package com.petsalud.backendpetsalud.controller;
import com.petsalud.backendpetsalud.dto.*;
import com.petsalud.backendpetsalud.entity.*;
import com.petsalud.backendpetsalud.service.UsuarioService;
import com.petsalud.backendpetsalud.security.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import java.util.*;
@RestController
public class AuthController {
 private final UsuarioService users; private final AuthenticationManager auth; private final JwtUtil jwt;
 public AuthController(UsuarioService users,AuthenticationManager auth,JwtUtil jwt){this.users=users;this.auth=auth;this.jwt=jwt;}
 @PostMapping({"/api/auth/registro","/api/usuarios/registro"})
 public ResponseEntity<?> register(@Valid @RequestBody RegistroRequest r){
  Usuario u=users.registrarUsuario(Usuario.builder().email(r.email().strip().toLowerCase(Locale.ROOT)).password(r.password()).nombre(r.nombre()).apellido(r.apellido()).build());
  return ResponseEntity.status(201).body(Map.of("id",u.getId(),"email",u.getEmail(),"rol",u.getRol()));
 }
 @PostMapping({"/api/auth/login","/api/login"})
 public Map<String,Object> login(@Valid @RequestBody LoginRequest r){
  var result=auth.authenticate(new UsernamePasswordAuthenticationToken(r.email().strip().toLowerCase(Locale.ROOT),r.password()));
  return Map.of("access_token",jwt.generate((UsuarioDetails)result.getPrincipal()),"token_type","Bearer");
 }
 @GetMapping("/api/auth/me") public Map<String,Object> me(Authentication a){var u=(UsuarioDetails)a.getPrincipal();return Map.of("id",u.getId(),"email",u.getUsername(),"roles",u.getAuthorities());}
}
