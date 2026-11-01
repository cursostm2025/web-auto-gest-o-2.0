package com.wag.api.repository;

import java.util.UUID;

/** Projeção agregada das avaliações agrupadas por instrutor. */
public interface AvaliacaoInstrutorResumo {
    UUID getInstrutorId();

    Double getMedia();

    Long getTotal();

    Integer getNotaDoAluno();
}
