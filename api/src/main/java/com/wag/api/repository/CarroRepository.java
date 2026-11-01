package com.wag.api.repository;

import com.wag.api.model.Carro;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

/** Consultas de persistência para veículos e seus instrutores associados. */
public interface CarroRepository extends JpaRepository<Carro, UUID> {
    /** Busca os veículos vinculados ao instrutor indicado. */
    List<Carro> findAllByInstrutorId(UUID instrutorId);

    boolean existsByInstrutorId(UUID instrutorId);
}
