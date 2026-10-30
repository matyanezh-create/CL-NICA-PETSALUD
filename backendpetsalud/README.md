# Petsalud autenticación y autorización con JWT

Implementación de la actividad PBY2202 Exp 1 Semana 2 forma B sobre el backend entregado por la asignatura. Conserva entidades, repositorios, servicios y controladores del caso; sustituye HTTP Basic por JWT e incorpora autorización por rol y propiedad.

## Requisitos y ejecución

Java JDK 17, conexión a Maven Central la primera vez y puerto 8080 disponible. Maven 3.9.9 se obtiene mediante el wrapper incluido. Versiones: Spring Boot 3.3.6 (del caso), Spring Security administrado por Boot, JJWT 0.13.0. Se conserva la versión del caso para reproducibilidad académica; antes de un despliegue real se debe revisar el soporte y las actualizaciones de seguridad.

```powershell
.\mvnw.cmd clean verify
.\iniciar-demo.ps1
```

El script solicita una contraseña de demostración y genera un secreto JWT aleatorio de 32 bytes en memoria si no existe JWT_SECRET. H2 guarda los datos en ./data/petsalud. No se utiliza una contraseña fija en el repositorio. El perfil demo crea, si no existen, cliente@petsalud.test, empleado@petsalud.test y gerente@petsalud.test con la contraseña indicada. Si la base ya contiene esas cuentas, no modifica sus claves. El perfil demo es exclusivamente local; no activarlo en un servidor público. Al regenerar JWT_SECRET se invalidan los tokens firmados con la clave anterior.

Linux/macOS: `chmod +x mvnw`, exportar JWT_SECRET y DEMO_PASSWORD, y ejecutar `./mvnw -Dspring-boot.run.profiles=demo spring-boot:run`.

Sin perfil demo, no se crean usuarios privilegiados. Para una instalación real, un administrador de base de datos debe aprovisionar las cuentas internas con hashes BCrypt y roles autorizados mediante un proceso controlado; no hay endpoint público para asignar roles.

## Base MySQL opcional

Crear una base vacía petsalud_db y exportar DB_URL=jdbc:mysql://localhost:3306/petsalud_db, DB_USER y DB_PASSWORD antes de iniciar. El driver se autodetecta. No reutilizar sin migración la base antigua: los roles VETERINARIO y ADMIN fueron reemplazados por EMPLEADO y GERENTE. Para datos existentes, respaldar y migrar esos valores, normalizar emails y reemplazar contraseñas en texto por hashes mediante un procedimiento controlado. H2 fue validada; la conexión MySQL depende de la instancia local y debe verificarse aparte. `ddl-auto=update` facilita la práctica; en producción utilizar migraciones versionadas y `validate`.

## Autenticación

POST /api/auth/registro (alias /api/usuarios/registro):

```json
{"email":"ana@example.com","password":"ClaveSegura123!","nombre":"Ana","apellido":"Perez"}
```

Devuelve 201 y un resumen sin contraseña. Email se normaliza a minúsculas, es único; contraseña de 12 a 72 caracteres ASCII imprimibles (límite de BCrypt en bytes); el registro siempre asigna CLIENTE. Un campo rol es rechazado con 400. Email repetido: 409.

POST /api/auth/login (alias /api/login):

```json
{"email":"cliente@petsalud.test","password":"LA_CLAVE_QUE_INGRESASTE"}
```

Devuelve `access_token` y `token_type: Bearer`. Copiar el token al encabezado `Authorization: Bearer <token>`. GET /api/auth/me verifica la identidad. Contraseña incorrecta o ausencia de credenciales en un recurso protegido: 401. Rol insuficiente: 403. HTTP Basic y formLogin deshabilitados. No hay JSESSIONID ni sesión HTTP.

JWT HS256 de 15 minutos con sub, uid, roles, iss=petsalud, aud=petsalud-api, iat, exp y jti. JWT_SECRET debe ser Base64 de al menos 32 bytes aleatorios y permanece fuera del código. Firma, algoritmo, emisor, audiencia, emisión, expiración y claims obligatorios se verifican antes de ejecutar el controlador. Se consultan identidad y rol vigentes en la base en cada solicitud: la sesión HTTP es stateless, aunque existe esa consulta de autorización. El claim roles no sustituye al dato vigente de la base.

## Matriz de permisos

| Recurso u operación | CLIENTE | EMPLEADO | GERENTE |
|---|---|---|---|
| Registro e inicio de sesión | Público | Público | Público |
| GET /api/auth/me | Sí | Sí | Sí |
| GET productos y categoría | Sí | Sí | Sí |
| GET productos/stock-bajo | No | Sí | Sí |
| POST PUT DELETE productos | No | No | Sí |
| GET PUT usuario por id | Propio | Propio | Todos |
| GET lista usuarios y DELETE usuario | No | No | Sí |
| Mascotas por id o propietario | Propias | Todas | Todas |
| Citas y expedientes | No | Sí | Sí |
| Ventas, recompensas y notificaciones | No | Sí | Sí |
| Rutas no declaradas | No | No | No |

Política conservadora: las operaciones clínicas y comerciales se realizan a través del personal. No se ofrece autoservicio de citas o ventas al cliente en esta práctica. El rol EMPLEADO agrupa al personal interno; el dominio aún denomina veterinario al profesional asignado. Un gerente tiene administración comercial y de usuarios; no se declara una jerarquía de roles implícita. Se emplea `hasRole` con authorities `ROLE_...`.

`Acceso` y `@PreAuthorize` comprueban propietario en mascotas y cuenta propia en usuarios, evitando acceso directo por adivinar IDs. Los identificadores enviados al registrar usuarios o mascotas no se reutilizan. Las relaciones JPA no se aceptan ni serializan como grafos JSON; los IDs de asociación se proporcionan mediante las rutas/parámetros existentes.

## Pruebas y evidencia

`src/test/java/.../BackendpetsaludApplicationTests.java` ejecuta 31 casos con MockMvc, filtros reales, JPA y H2 en memoria, sin mocks de JWT ni de roles. Cubre registro, BCrypt, validación, duplicados, autoasignación de roles, login, sesiones, Basic, tokens inválidos, expiración, emisor, audiencia, firma, matriz de roles, catálogo, propiedad, datos sensibles y cambios de rol vigentes.

Resultado verificado el 29-10-2026: 31 ejecutadas, 0 fallos, 0 errores y 0 omitidas. Evidencia en `evidencia/resultado-pruebas.txt`. El resumen debe regenerarse al modificar el código; los reportes completos se producen en target/surefire-reports. La clave estática de pruebas solo funciona contra la base en memoria del contexto de pruebas.

La colección Postman permite registro, login de tres roles, consulta de identidad, catálogo y denegaciones. Configurar `demoPassword` localmente y no sincronizar ni publicar valores reales. Las pruebas automáticas de la colección esperan que la aplicación se ejecute con el perfil demo y la base inicializada.

## Protección y límites de despliegue

CSRF se deshabilita porque la API solo acepta Bearer explícito en Authorization, sin cookies de autenticación ni Basic. Si se cambia a cookies, se debe restaurar protección CSRF. CORS no se abre globalmente; para un frontend separado, definir orígenes y métodos concretos. Usar HTTPS en un proxy o servidor TLS: la demo HTTP es local. No poner tokens en URL ni logs; almacenarlos preferentemente en memoria del cliente y aplicar controles XSS. Un JWT firmado se puede leer: no contiene contraseña ni historia clínica.

No se implementan refresh tokens, cierre de sesión con lista de revocación, MFA ni limitación de intentos. Un token robado es reutilizable hasta expirar (máximo 15 minutos); jti por sí solo no bloquea replay. El logout del cliente consiste en borrar su copia. Para revocación inmediata por token se requiere un mecanismo adicional; eliminación de usuario o cambio de rol sí se refleja por la consulta a BD. Un cambio de contraseña no invalida por sí solo los JWT existentes. HS256 comparte secreto; para microservicios independientes conviene evaluar firma asimétrica y rotación con identificador de clave. La autorización no elimina por sí sola inyecciones ni todos los riesgos del CRUD base. Antes de producción, añadir controles de intentos en el gateway, auditoría sin secretos, validación de dominio y manejo uniforme de errores de negocio.

## Relación con la pauta

1. Entorno y dependencias: pom.xml y wrapper Java 17.
2. Configuración personalizada: SecurityConfig, STATELESS y mecanismos por defecto deshabilitados.
3. Usuario y hashing: Usuario, RegistroRequest, UsuarioServiceImpl y BCrypt coste 12.
4. JWT: JwtUtil, firma HS256 y claims verificados.
5. Filtro: JwtRequestFilter antes de UsernamePasswordAuthenticationFilter.
6. Roles y permisos: Rol, SecurityConfig, Acceso, @PreAuthorize y pruebas de matriz.
7. Informe: documento Word de respuesta que acompaña el código.

## Publicar el código en GitHub

Crear un repositorio en tu cuenta y subir el contenido de esta carpeta. Puede usarse la interfaz web o Git:

```text
git init
git add .
git commit -m "Implementa seguridad JWT y roles en Petsalud"
git branch -M main
git remote add origin https://github.com/TU_USUARIO/TU_REPOSITORIO.git
git push -u origin main
```

Reemplazar usuario y repositorio con los reales. No subir data/, secretos ni material institucional adjunto. Abrir el enlace del repositorio para comprobar acceso del docente, pegarlo en el informe y entregar el Word en AVA. La preparación local no publica ni entrega por ti.

## Referencias

- Spring Security: https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html
- JJWT documentación oficial: https://github.com/jwtk/jjwt
- JWT estándar RFC 7519: https://www.rfc-editor.org/rfc/rfc7519
