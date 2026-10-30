package com.petsalud.backendpetsalud;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petsalud.backendpetsalud.entity.*;
import com.petsalud.backendpetsalud.repository.*;
import com.petsalud.backendpetsalud.security.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "security.jwt.secret=YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE=", "spring.jpa.hibernate.ddl-auto=create-drop"})
@org.springframework.test.context.TestExecutionListeners(listeners={org.springframework.test.context.support.DependencyInjectionTestExecutionListener.class,org.springframework.test.context.web.ServletTestExecutionListener.class}, inheritListeners=false)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BackendpetsaludApplicationTests {
 @Autowired MockMvc mvc; @Autowired UsuarioRepository users; @Autowired MascotaRepository pets;
 @Autowired PasswordEncoder encoder; @Autowired JwtUtil jwt; @Autowired ObjectMapper json;
 Map<String,Usuario> accounts=new HashMap<>();
 @BeforeAll void seed(){ for(Rol role:Rol.values()){Usuario u=users.save(Usuario.builder().email(role.name().toLowerCase()+"@test.cl").password(encoder.encode("ClaveSegura123!")).rol(role).nombre(role.name()).build());accounts.put(role.name(),u);} }
 String token(String role){return jwt.generate(new UsuarioDetails(accounts.get(role)));}
 @Test void registroSeguro() throws Exception {
  mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"nuevo@test.cl\",\"password\":\"ClaveSegura123!\",\"nombre\":\"Ana\"}"))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.rol").value("CLIENTE")).andExpect(jsonPath("$.password").doesNotExist());
  var u=users.findByEmail("nuevo@test.cl").orElseThrow();assertNotEquals("ClaveSegura123!",u.getPassword());assertTrue(encoder.matches("ClaveSegura123!",u.getPassword()));
 }
 @Test void noAutoasignarGerente() throws Exception {mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"ataque@test.cl\",\"password\":\"ClaveSegura123!\",\"nombre\":\"X\",\"rol\":\"GERENTE\"}")).andExpect(status().isBadRequest());assertTrue(users.findByEmail("ataque@test.cl").isEmpty());}
 @Test void registroInvalido() throws Exception {mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"mal\",\"password\":\"123\",\"nombre\":\"\"}")).andExpect(status().isBadRequest());}
 @Test void duplicado() throws Exception {mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"cliente@test.cl\",\"password\":\"ClaveSegura123!\",\"nombre\":\"Ana\"}")).andExpect(status().isConflict());}
 @Test void loginYToken()throws Exception {
  String body=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"CLIENTE@test.cl\",\"password\":\"ClaveSegura123!\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  String token=json.readTree(body).get("access_token").asText(); var c=jwt.validate(token);assertEquals("cliente@test.cl",c.getSubject());assertNotNull(c.getExpiration());
  var r=mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andReturn();assertNull(r.getRequest().getSession(false));assertNull(r.getResponse().getHeader("Set-Cookie"));
 }
 @Test void claveIncorrecta()throws Exception {mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"cliente@test.cl\",\"password\":\"equivocada\"}")).andExpect(status().isUnauthorized());}
 @Test void sinToken()throws Exception {mvc.perform(get("/api/productos")).andExpect(status().isUnauthorized());}
 @Test void basicDeshabilitado()throws Exception {mvc.perform(get("/api/productos").header("Authorization","Basic "+Base64.getEncoder().encodeToString("CLIENTE@test.cl:ClaveSegura123!".getBytes()))).andExpect(status().isUnauthorized());}
 @Test void tokenMalformado()throws Exception {mvc.perform(get("/api/productos").header("Authorization","Bearer basura")).andExpect(status().isUnauthorized());}
 String signed(Date exp,String issuer,String audience,byte[] key){return Jwts.builder().subject("cliente@test.cl").issuer(issuer).audience().add(audience).and().issuedAt(new Date(System.currentTimeMillis()-60000)).expiration(exp).id("test").claim("uid",accounts.get("CLIENTE").getId()).signWith(Keys.hmacShaKeyFor(key),Jwts.SIG.HS256).compact();}
 @ParameterizedTest @CsvSource({"expired,petsalud,petsalud-api,valid", "future,otro,petsalud-api,valid", "future,petsalud,otra,valid", "future,petsalud,petsalud-api,wrong"})
 void tokensRechazados(String exp,String issuer,String audience,String key)throws Exception {byte[] bytes=new byte[32];Arrays.fill(bytes,(byte)(key.equals("valid")?'a':'b'));String t=signed(new Date(System.currentTimeMillis()+(exp.equals("expired")?-1000:60000)),issuer,audience,bytes);mvc.perform(get("/api/productos").header("Authorization","Bearer "+t)).andExpect(status().isUnauthorized());}
 @ParameterizedTest @CsvSource({"CLIENTE,/api/productos,200","EMPLEADO,/api/productos,200","GERENTE,/api/productos,200","CLIENTE,/api/ventas,403","EMPLEADO,/api/ventas,200","GERENTE,/api/ventas,200","CLIENTE,/api/usuarios,403","EMPLEADO,/api/usuarios,403","GERENTE,/api/usuarios,200","CLIENTE,/api/productos/stock-bajo?cantidad=5,403","EMPLEADO,/api/productos/stock-bajo?cantidad=5,200"})
 void matrizRoles(String role,String path,int code)throws Exception {mvc.perform(get(path).header("Authorization","Bearer "+token(role))).andExpect(status().is(code));}
 @ParameterizedTest @CsvSource({"CLIENTE,403","EMPLEADO,403","GERENTE,200"})
 void escrituraCatalogo(String role,int code)throws Exception {mvc.perform(post("/api/productos").header("Authorization","Bearer "+token(role)).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Vacuna\",\"precio\":1000,\"stock\":20}")).andExpect(status().is(code));}
 @Test void propiedadMascota()throws Exception {
  Usuario owner=accounts.get("CLIENTE");Mascota m=pets.save(Mascota.builder().nombre("Luna").propietario(owner).build());
  mvc.perform(get("/api/mascotas/"+m.getId()).header("Authorization","Bearer "+token("CLIENTE"))).andExpect(status().isOk());
  Usuario other=users.save(Usuario.builder().email("otro@test.cl").password(encoder.encode("ClaveSegura123!")).nombre("Otro").rol(Rol.CLIENTE).build());
  mvc.perform(get("/api/mascotas/"+m.getId()).header("Authorization","Bearer "+jwt.generate(new UsuarioDetails(other)))).andExpect(status().isForbidden());
 }
 @Test void passwordNoExpuesto()throws Exception {mvc.perform(get("/api/usuarios/"+accounts.get("CLIENTE").getId()).header("Authorization","Bearer "+token("CLIENTE"))).andExpect(status().isOk()).andExpect(jsonPath("$.password").doesNotExist());}
 @Test void rolesVigentesDesdeBD()throws Exception {Usuario u=users.save(Usuario.builder().email("cambio@test.cl").password("hash").rol(Rol.GERENTE).nombre("X").build());String t=jwt.generate(new UsuarioDetails(u));u.setRol(Rol.CLIENTE);users.save(u);mvc.perform(get("/api/usuarios").header("Authorization","Bearer "+t)).andExpect(status().isForbidden());}
 @Test void rutaNoDeclarada()throws Exception {mvc.perform(get("/api/desconocido").header("Authorization","Bearer "+token("GERENTE"))).andExpect(status().isForbidden());}
}
