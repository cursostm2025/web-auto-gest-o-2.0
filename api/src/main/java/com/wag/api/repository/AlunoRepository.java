package com.wag.api.repository;

import com.wag.api.model.Aluno;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

/** Fornece as operações CRUD de alunos com identificadores UUID. */
public interface AlunoRepository extends JpaRepository<Aluno, UUID> {
    Optional<Aluno> findFirstByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    boolean existsByInstrutorId(UUID instrutorId);
}