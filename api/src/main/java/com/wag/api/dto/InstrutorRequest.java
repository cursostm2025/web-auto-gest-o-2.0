package com.wag.api.dto;

import java.util.UUID;

/** Dados recebidos pela API ao cadastrar um instrutor e associá-lo a um administrador. */
public record InstrutorRequest(
        String nome,
        String email,
        String telefoneWhatsapp,
        String endereco,
        String categoriaCnh,
        String descricao,
        UUID administradorId) {
}
