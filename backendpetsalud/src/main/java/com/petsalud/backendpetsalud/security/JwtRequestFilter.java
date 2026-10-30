package com.petsalud.backendpetsalud.security;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import io.jsonwebtoken.JwtException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;
public class JwtRequestFilter extends OncePerRequestFilter {
 private final JwtUtil jwt; private final UsuarioDetailsService users;
 public JwtRequestFilter(JwtUtil jwt, UsuarioDetailsService users){this.jwt=jwt;this.users=users;}
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
  String h=req.getHeader("Authorization");
  if(h!=null && h.startsWith("Bearer ")) {
   try {
    var claims=jwt.validate(h.substring(7));
    var user=(UsuarioDetails)users.loadUserByUsername(claims.getSubject());
    if(!user.getId().toString().equals(claims.get("uid").toString())) throw new JwtException("Identidad invalida");
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,user.getAuthorities()));
   } catch(JwtException | IllegalArgumentException | UsernameNotFoundException e) {
    SecurityContextHolder.clearContext(); res.setStatus(401); res.setContentType("application/json");
    res.getWriter().write("{\"error\":\"Token invalido o expirado\"}"); return;
   }
  }
  chain.doFilter(req,res);
 }
}
