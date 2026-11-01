package com.wag.api.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Representa o perfil profissional de um instrutor e seus veículos cadastrados. */
// @Data: Anotação do Lombok que cria automaticamente os métodos Getters, Setters, toString e equals/hashCode.
@Data
// @NoArgsConstructor: O Lombok cria um construtor vazio, exigido pelo JPA/Hibernate.
@NoArgsConstructor
// @Entity: Diz ao Spring que esta classe representa uma entidade/tabela no banco de dados.
@Entity
// @Table: Permite especificar o nome exato da tabela no banco (PostgreSQL).
@Table(name = "instrutor")
public class Instrutor {

    // @Id: Define que este campo é a chave primária (Primary Key) da tabela.
    @Id
    // @GeneratedValue: Gera o UUID automaticamente ao salvar um novo registo, garantindo segurança.
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // @Column: Define as regras da coluna (não pode ser nula, tamanho máximo de 100 caracteres).
    @Column(nullable = false, length = 100)
    private String nome;

    // unique = true impede que dois instrutores se cadastrem com o mesmo email.
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // Cada instrutor é criado sob responsabilidade de um administrador.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "administrador_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Administrador administrador;

    @Column(name = "telefone_whatsapp", nullable = false, length = 20)
    private String telefoneWhatsapp;

    @Column(length = 255)
    private String endereco;

    // A coleção é carregada sob demanda para evitar consultas desnecessárias.
    @OneToMany(mappedBy = "instrutor")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Carro> carros = new ArrayList<>();

    @Column(name = "categoria_cnh", length = 5)
    private String categoriaCnh; 

    // columnDefinition = "TEXT" permite armazenar textos longos, sem o limite padrão de 255 caracteres.
    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false)
    private boolean ativo = true;
}