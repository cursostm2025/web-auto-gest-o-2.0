package com.wag.api.repository;

import com.wag.api.model.Instrutor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Consultas de persistência de instrutores identificados por UUID. */
public interface InstrutorRepository extends JpaRepository<Instrutor, UUID> {
    /** Verifica duplicidade de e-mail sem diferenciar letras maiúsculas de minúsculas. */
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    List<Instrutor> findAllByAtivoTrue();
}