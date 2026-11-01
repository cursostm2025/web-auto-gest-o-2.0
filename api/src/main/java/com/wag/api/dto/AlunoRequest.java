package com.wag.api.dto;

import java.util.UUID;

/** Dados aceitos pela API ao cadastrar ou atualizar um aluno. */
public record AlunoRequest(
        String nome,
        String telefone,
        String email,
        String endereco,
        UUID administradorId,
        UUID instrutorId,
        UUID carroId,
        String senha) {
}
