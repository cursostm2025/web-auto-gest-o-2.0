package com.wag.api.repository;

import com.wag.api.model.Administrador;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

/** Consultas de persistência para a entidade {@link Administrador}. */
public interface AdministradorRepository extends JpaRepository<Administrador, UUID> {
    /** Localiza um administrador pelo e-mail informado para autenticação. */
    Optional<Administrador> findByEmail(String email);
}