package com.wag.api.service;

import com.wag.api.dto.AdministradorDTO;
import com.wag.api.model.Administrador;
import com.wag.api.repository.AdministradorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Reúne as regras de negócio para consulta, cadastro e autenticação de administradores. */
@Service
public class AdministradorService {

    // Repositório usado para acessar os registros de administrador no banco.
    private final AdministradorRepository repository;

    public AdministradorService(AdministradorRepository repository) {
        this.repository = repository;
    }

    // Retorna dados públicos para que a senha nunca faça parte das respostas da API.
    public List<AdministradorDTO> listarTodos() {
        return repository.findAll().stream()
                .map(AdministradorDTO::new)
                .toList();
    }

    // Protege a senha antes de persistir o administrador.
    public Administrador salvar(Administrador administrador) {
        if (administrador.getSenha() != null && !administrador.getSenha().isBlank()) {
            administrador.setSenha(SenhaHasher.hash(administrador.getSenha()));
        }
        return repository.save(administrador);
    }

    /** Retorna os dados públicos do administrador quando as credenciais são válidas. */
    public Optional<AdministradorDTO> autenticar(String email, String senha) {
        // Campos vazios são tratados como requisição inválida, não como credenciais incorretas.
        if (email == null || email.isBlank() || senha == null || senha.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail e senha são obrigatórios.");
        }

        // Compara a senha informada com o hash armazenado sem expor a entidade.
        return repository.findByEmail(email.trim())
                .filter(administrador -> SenhaHasher.corresponde(senha, administrador.getSenha()))
                .map(AdministradorDTO::new);
    }

    /** Retorna os dados públicos do administrador associado à sessão autenticada. */
    public Optional<AdministradorDTO> buscarPorId(UUID id) {
        return repository.findById(id).map(AdministradorDTO::new);
    }
}