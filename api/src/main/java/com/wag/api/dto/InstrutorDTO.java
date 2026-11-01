package com.wag.api.dto;

import com.wag.api.model.Instrutor;
import java.util.UUID;

/** Dados do perfil de instrutor retornados pela API, sem serializar entidades relacionadas. */
public record InstrutorDTO(
        UUID id, 
        String nome, 
        String email, 
        String telefoneWhatsapp, 
        String endereco, 
        String categoriaCnh, 
        String descricao,
        UUID administradorId,
        boolean ativo,
        double avaliacaoMedia,
        long totalAvaliacoes,
        Integer notaDoAluno) {
    
    /** Converte a entidade de instrutor para o formato público da API. */
    public InstrutorDTO(Instrutor instrutor) {
        this(instrutor, 0, 0, null);
    }

    public InstrutorDTO(
            Instrutor instrutor,
            double avaliacaoMedia,
            long totalAvaliacoes,
            Integer notaDoAluno) {
        this(
            instrutor.getId(), 
            instrutor.getNome(), 
            instrutor.getEmail(), 
            instrutor.getTelefoneWhatsapp(), 
            instrutor.getEndereco(), 
            instrutor.getCategoriaCnh(),
            instrutor.getDescricao(),
            instrutor.getAdministrador().getId(),
            instrutor.isAtivo(),
            avaliacaoMedia,
            totalAvaliacoes,
            notaDoAluno
        );
    }
}