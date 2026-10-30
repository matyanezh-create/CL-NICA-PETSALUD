package com.petsalud.backendpetsalud.security;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class JwtUtil {
 private final SecretKey key; private final long ttl;
 public JwtUtil(@Value("${security.jwt.secret}") String secret, @Value("${security.jwt.ttl-seconds:900}") long ttl) {
  key=Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
  if(ttl<1 || ttl>900) throw new IllegalArgumentException("JWT TTL debe estar entre 1 y 900 segundos");
  this.ttl=ttl;
 }
 public String generate(UsuarioDetails user) {
  Instant now=Instant.now();
  return Jwts.builder().subject(user.getUsername()).issuer("petsalud").audience().add("petsalud-api").and()
   .id(UUID.randomUUID().toString()).issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(ttl)))
   .claim("uid",user.getId()).claim("roles",user.getAuthorities().stream().map(a->a.getAuthority()).toList())
   .signWith(key,Jwts.SIG.HS256).compact();
 }
 public Claims validate(String token) {
  var jwt=Jwts.parser().verifyWith(key).requireIssuer("petsalud").requireAudience("petsalud-api").build().parseSignedClaims(token);
  if(!"HS256".equals(jwt.getHeader().getAlgorithm())) throw new JwtException("Algoritmo no permitido");
  Claims c=jwt.getPayload();
  if(c.getSubject()==null || c.getExpiration()==null || c.getIssuedAt()==null || c.getId()==null || c.get("uid")==null)
   throw new JwtException("Claims requeridos ausentes");
  if(c.getIssuedAt().after(new Date())) throw new JwtException("Emision futura");
  return c;
 }
}
