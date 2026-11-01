package com.wag.api.dto;

import com.wag.api.model.Carro;
import java.util.UUID;

/** Dados do veículo retornados pela API, incluindo o UUID do instrutor responsável. */
public record CarroDTO(
        UUID id,
        String nomeVeiculo,
        String placa,
        String modelo,
        String cor,
        Integer ano,
        UUID instrutorId) {

    /** Converte a entidade de veículo para o formato de resposta da API. */
    public CarroDTO(Carro carro) {
        this(
                carro.getId(),
                carro.getNomeVeiculo(),
                carro.getPlaca(),
                carro.getModelo(),
                carro.getCor(),
                carro.getAno(),
                carro.getInstrutor().getId());
    }
}
