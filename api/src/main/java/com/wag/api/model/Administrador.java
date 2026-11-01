package com.wag.api.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import java.util.UUID;

/** Representa a conta administrativa persistida na tabela administrador. */
@Data
@NoArgsConstructor
@Entity
@Table(name = "administrador")
public class Administrador {

    // Chave primária gerada como UUID ao inserir o registro.
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 20)
    private String telefone;

    @Column(length = 255)
    // Evita que a senha seja incluída acidentalmente no toString gerado pelo Lombok.
    @ToString.Exclude
    private String senha;
}