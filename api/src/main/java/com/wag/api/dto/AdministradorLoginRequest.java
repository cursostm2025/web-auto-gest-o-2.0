package com.wag.api.dto;

/** Credenciais enviadas pelo formulário de login do administrador. */
public record AdministradorLoginRequest(String email, String senha) {
}
