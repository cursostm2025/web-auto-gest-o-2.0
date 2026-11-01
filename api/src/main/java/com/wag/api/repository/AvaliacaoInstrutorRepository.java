package com.wag.api.repository;

import com.wag.api.model.AvaliacaoInstrutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Consultas de persistência e consolidação das avaliações dos instrutores. */
public interface AvaliacaoInstrutorRepository extends JpaRepository<AvaliacaoInstrutor, UUID> {
    Optional<AvaliacaoInstrutor> findByAlunoIdAndInstrutorId(UUID alunoId, UUID instrutorId);

    @Modifying
    @Transactional
    @Query("delete from AvaliacaoInstrutor avaliacao where avaliacao.aluno.id = :alunoId")
    void excluirPorAlunoId(@Param("alunoId") UUID alunoId);

    @Modifying
    @Transactional
    @Query("delete from AvaliacaoInstrutor avaliacao where avaliacao.instrutor.id = :instrutorId")
    void excluirPorInstrutorId(@Param("instrutorId") UUID instrutorId);

    @Query("""
            select avaliacao.instrutor.id as instrutorId,
                   avg(avaliacao.nota) as media,
                   count(avaliacao.id) as total,
                   max(case when avaliacao.aluno.id = :alunoId
                       then avaliacao.nota else null end) as notaDoAluno
            from AvaliacaoInstrutor avaliacao
            where avaliacao.instrutor.id in :instrutorIds
            group by avaliacao.instrutor.id
            """)
    List<AvaliacaoInstrutorResumo> resumirPorInstrutores(
            @Param("instrutorIds") List<UUID> instrutorIds,
            @Param("alunoId") UUID alunoId);
}
