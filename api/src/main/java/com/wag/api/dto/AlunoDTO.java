package com.wag.api.dto;

import com.wag.api.model.Aluno;
import java.util.UUID;

/** Dados do aluno retornados pela API, com associações representadas por UUID. */
public record AlunoDTO(
        UUID id,
        String nome,
        String telefone,
        String email,
        String endereco,
        UUID administradorId,
        UUID instrutorId,
        UUID carroId,
        boolean ativo) {
    
    /** Converte a entidade para DTO e mantém nulos os vínculos ainda não definidos. */
    public AlunoDTO(Aluno aluno) {
        this(
                aluno.getId(),
                aluno.getNome(),
                aluno.getTelefone(),
                aluno.getEmail(),
                aluno.getEndereco(),
                aluno.getAdministrador() == null ? null : aluno.getAdministrador().getId(),
                aluno.getInstrutor() == null ? null : aluno.getInstrutor().getId(),
                aluno.getCarro() == null ? null : aluno.getCarro().getId(),
                aluno.isAtivo());
    }
}