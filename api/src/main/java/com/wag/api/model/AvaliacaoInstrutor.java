package com.wag.api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

/** Guarda a nota dada por um aluno a um instrutor. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "avaliacao_instrutor",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_avaliacao_instrutor_aluno",
                columnNames = {"instrutor_id", "aluno_id"}))
public class AvaliacaoInstrutor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrutor_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Instrutor instrutor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Aluno aluno;

    @Column(nullable = false)
    private int nota;
}
