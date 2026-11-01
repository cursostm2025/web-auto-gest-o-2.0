package com.wag.api.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import java.util.UUID;

/** Representa os dados pessoais e os vínculos opcionais de um aluno cadastrado. */
@Data
@NoArgsConstructor
@Entity
@Table(name = "aluno")
public class Aluno {

    // Identificador único gerado pelo banco de dados como UUID.
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String nome;

    // Os vínculos podem ficar vazios durante o autocadastro público.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "administrador_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Administrador administrador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrutor_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Instrutor instrutor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carro_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Carro carro;

    @Column(nullable = false, length = 20)
    private String telefone;

    @Column(length = 100)
    private String email;

    @Column(length = 255)
    @ToString.Exclude
    private String senha;

    @Column(length = 255)
    private String endereco;

    // Novos alunos ficam ativos por padrão.
    @Column(nullable = false)
    private boolean ativo = true;
}