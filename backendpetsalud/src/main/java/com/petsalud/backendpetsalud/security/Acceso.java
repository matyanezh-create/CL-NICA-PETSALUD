package com.petsalud.backendpetsalud.security;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import com.petsalud.backendpetsalud.repository.MascotaRepository;
@Component("acceso")
public class Acceso {
 private final MascotaRepository mascotas;
 public Acceso(MascotaRepository mascotas){this.mascotas=mascotas;}
 private boolean personal(Authentication a){return a.getAuthorities().stream().anyMatch(x->x.getAuthority().equals("ROLE_EMPLEADO") || x.getAuthority().equals("ROLE_GERENTE"));}
 public boolean usuario(Authentication a,Long id){return a.getAuthorities().stream().anyMatch(x->x.getAuthority().equals("ROLE_GERENTE")) || ((UsuarioDetails)a.getPrincipal()).getId().equals(id);}
 public boolean propietario(Authentication a,Long id){return personal(a) || ((UsuarioDetails)a.getPrincipal()).getId().equals(id);}
 public boolean mascota(Authentication a,Long id){return personal(a) || mascotas.findById(id).map(m->m.getPropietario().getId().equals(((UsuarioDetails)a.getPrincipal()).getId())).orElse(false);}
}
