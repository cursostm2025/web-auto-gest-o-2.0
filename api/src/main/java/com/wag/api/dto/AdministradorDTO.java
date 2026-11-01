package com.wag.api.dto;

import com.wag.api.model.Administrador;
import java.util.UUID;

/** Dados públicos do administrador, sem incluir a senha armazenada. */
public record AdministradorDTO(UUID id, String nome, String email, String telefone) {
    
    /** Converte a entidade para uma resposta segura da API. */
    public AdministradorDTO(Administrador admin) {
        this(admin.getId(), admin.getNome(), admin.getEmail(), admin.getTelefone());
    }
}