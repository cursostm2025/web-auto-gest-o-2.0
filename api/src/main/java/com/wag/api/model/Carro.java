package com.wag.api.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import java.util.UUID;

/** Representa um veículo cadastrado e associado obrigatoriamente a um instrutor. */
@Data
@NoArgsConstructor
@Entity
@Table(name = "carro")
public class Carro {

    // Identificador único do veículo.
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nome_veiculo", nullable = false, length = 100)
    private String nomeVeiculo;

    @Column(nullable = false, unique = true, length = 10)
    private String placa;

    @Column(nullable = false, length = 100)
    private String modelo;

    @Column(nullable = false, length = 50)
    private String cor;

    @Column(nullable = false)
    private Integer ano;

    // Um instrutor pode possuir vários veículos, mas cada veículo pertence a um só instrutor.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrutor_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Instrutor instrutor;
}
