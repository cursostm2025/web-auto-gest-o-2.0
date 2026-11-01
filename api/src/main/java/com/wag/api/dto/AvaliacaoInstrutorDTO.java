package com.wag.api.dto;

/** Resumo das avaliações de um instrutor e da nota do aluno autenticado. */
public record AvaliacaoInstrutorDTO(double media, long total, Integer notaDoAluno) {
}
