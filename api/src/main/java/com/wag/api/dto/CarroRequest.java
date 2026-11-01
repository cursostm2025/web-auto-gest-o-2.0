package com.wag.api.dto;

import java.util.UUID;

/** Dados recebidos pela API para cadastrar um veículo associado a um instrutor. */
public record CarroRequest(
        String nomeVeiculo,
        String placa,
        String modelo,
        String cor,
        Integer ano,
        UUID instrutorId) {
}
