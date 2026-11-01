package com.wag.api.dto;

/** Credenciais enviadas pelo aluno para iniciar uma sessão. */
public record AlunoLoginRequest(String email, String senha) {
}
