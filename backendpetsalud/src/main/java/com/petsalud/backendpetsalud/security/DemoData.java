package com.petsalud.backendpetsalud.security;
import com.petsalud.backendpetsalud.entity.*;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import org.springframework.context.annotation.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
@Configuration @Profile("demo")
public class DemoData {
 @Bean CommandLineRunner usuariosDemo(UsuarioRepository repo,PasswordEncoder encoder,@Value("${DEMO_PASSWORD}") String password) {
  if(password.length()<12 || password.length()>72 || !password.matches("[\\x20-\\x7E]+")) throw new IllegalArgumentException("DEMO_PASSWORD debe tener 12 a 72 caracteres ASCII imprimibles");
  return args->{for(Rol rol:Rol.values()) {String email=rol.name().toLowerCase()+"@petsalud.test";
   if(repo.findByEmail(email).isEmpty())repo.save(Usuario.builder().email(email).password(encoder.encode(password)).nombre(rol.name()).rol(rol).build());
  }};
 }
}
