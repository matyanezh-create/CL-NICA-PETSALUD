package com.petsalud.backendpetsalud.controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import java.util.Map;
@RestControllerAdvice
public class ApiExceptionHandler {
 @ExceptionHandler(AuthenticationException.class) ResponseEntity<?> auth(){return ResponseEntity.status(401).body(Map.of("error","Credenciales invalidas"));}
 @ExceptionHandler({IllegalArgumentException.class,DataIntegrityViolationException.class}) ResponseEntity<?> conflict(){return ResponseEntity.status(409).body(Map.of("error","Operacion incompatible con los datos existentes"));}
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> invalid(){return ResponseEntity.badRequest().body(Map.of("error","Datos de entrada invalidos"));}
}
