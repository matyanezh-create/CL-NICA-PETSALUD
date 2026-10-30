package com.petsalud.backendpetsalud.dto;
import jakarta.validation.constraints.*;
public record RegistroRequest(@NotBlank @Email @Size(max=254) String email,
 @NotBlank @Size(min=12,max=72) @Pattern(regexp="[\\x20-\\x7E]+",message="Use caracteres ASCII imprimibles") String password,
 @NotBlank @Size(max=80) String nombre, @Size(max=80) String apellido) {}
