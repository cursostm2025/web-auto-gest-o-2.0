package com.wag.api.service;

import com.wag.api.dto.AlunoDTO;
import com.wag.api.dto.AlunoRequest;
import com.wag.api.model.Administrador;
import com.wag.api.model.Aluno;
import com.wag.api.model.Carro;
import com.wag.api.model.Instrutor;
import com.wag.api.repository.AdministradorRepository;
import com.wag.api.repository.AlunoRepository;
import com.wag.api.repository.AvaliacaoInstrutorRepository;
import com.wag.api.repository.CarroRepository;
import com.wag.api.repository.InstrutorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/** Aplica as regras de negócio e coordena as associações de alunos com outros registros. */
@Service
public class AlunoService {

    // Repositórios necessários para salvar alunos e validar seus relacionamentos.
    private final AlunoRepository repository;
    private final AdministradorRepository administradorRepository;
    private final InstrutorRepository instrutorRepository;
    private final CarroRepository carroRepository;
    private final AvaliacaoInstrutorRepository avaliacaoRepository;

    public AlunoService(
            AlunoRepository repository,
            AdministradorRepository administradorRepository,
            InstrutorRepository instrutorRepository,
            CarroRepository carroRepository,
            AvaliacaoInstrutorRepository avaliacaoRepository) {
        this.repository = repository;
        this.administradorRepository = administradorRepository;
        this.instrutorRepository = instrutorRepository;
        this.carroRepository = carroRepository;
        this.avaliacaoRepository = avaliacaoRepository;
    }

    /** Lista os alunos cadastrados convertendo cada entidade para DTO. */
    public List<AlunoDTO> listarTodos() {
        return repository.findAll().stream()
                .map(AlunoDTO::new)
                .toList(); 
    }

    /**
     * Cadastra um aluno; vínculos são opcionais no autocadastro público, mas
     * quando algum UUID é informado, todos os vínculos administrativos são validados.
     */
    public Aluno salvar(AlunoRequest request) {
        boolean cadastroPublico = request.administradorId() == null
                && request.instrutorId() == null
                && request.carroId() == null;
        if (cadastroPublico) {
            if (request.email() == null || request.email().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail é obrigatório");
            }
            if (request.senha() == null || request.senha().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha é obrigatória para o cadastro do aluno");
            }
            if (request.senha().length() < 8) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha deve ter pelo menos 8 caracteres");
            }
            if (repository.existsByEmailIgnoreCase(request.email().trim())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um aluno cadastrado com este e-mail");
            }
        }

        Aluno aluno = new Aluno();
        atualizarDadosBasicos(aluno, request);
        if (request.senha() != null && !request.senha().isBlank()) {
            aluno.setSenha(SenhaHasher.hash(request.senha()));
        }

        if (request.administradorId() != null || request.instrutorId() != null || request.carroId() != null) {
            atualizarRelacionamentos(aluno, request);
        }

        return repository.save(aluno);
    }

    /** Atualiza um aluno existente, retornando 404 quando o UUID não for encontrado. */
    public Aluno atualizar(UUID id, AlunoRequest dadosAtualizados) {
        if (dadosAtualizados == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe os dados para atualizar o aluno.");
        }
        if (dadosAtualizados.nome() == null || dadosAtualizados.nome().isBlank()
                || dadosAtualizados.email() == null || dadosAtualizados.email().isBlank()
                || dadosAtualizados.telefone() == null || dadosAtualizados.telefone().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome, e-mail e telefone são obrigatórios.");
        }
        Aluno alunoExistente = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aluno não encontrado"));
        if (dadosAtualizados.email() != null
                && repository.existsByEmailIgnoreCaseAndIdNot(dadosAtualizados.email().trim(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um aluno cadastrado com este e-mail");
        }
        atualizarDados(alunoExistente, dadosAtualizados);
        return repository.save(alunoExistente);
    }

    /** Atualiza os campos comuns entre criação e edição do aluno. */
    private void atualizarDados(Aluno aluno, AlunoRequest request) {
        atualizarDadosBasicos(aluno, request);
        if (request.administradorId() != null || request.instrutorId() != null || request.carroId() != null) {
            atualizarRelacionamentos(aluno, request);
        }
        if (request.senha() != null && !request.senha().isBlank()) {
            aluno.setSenha(SenhaHasher.hash(request.senha()));
        }
    }

    /** Copia para a entidade os dados pessoais enviados na requisição. */
    private void atualizarDadosBasicos(Aluno aluno, AlunoRequest request) {
        aluno.setNome(request.nome());
        aluno.setTelefone(request.telefone());
        aluno.setEmail(request.email() == null ? null : request.email().trim());
        aluno.setEndereco(request.endereco());
    }

    /** Autentica o aluno por e-mail e senha sem retornar a senha na resposta. */
    public AlunoDTO autenticar(String email, String senha) {
        if (email == null || email.isBlank() || senha == null || senha.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail e senha são obrigatórios.");
        }

        return repository.findFirstByEmailIgnoreCase(email.trim())
                .filter(aluno -> aluno.isAtivo() && SenhaHasher.corresponde(senha, aluno.getSenha()))
                .map(AlunoDTO::new)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "E-mail ou senha inválidos. Confira os dados usados no cadastro."));
    }

    /** Busca o aluno autenticado na sessão atual. */
    public AlunoDTO buscarPorId(UUID id) {
        return repository.findById(id)
                .filter(aluno -> aluno.isAtivo())
                .map(AlunoDTO::new)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão do aluno inválida."));
    }

    /** Resolve os UUIDs relacionados e valida se o carro pertence ao instrutor. */
    private void atualizarRelacionamentos(Aluno aluno, AlunoRequest request) {
        aluno.setAdministrador(buscarAdministrador(request.administradorId()));
        Instrutor instrutor = buscarInstrutor(request.instrutorId());
        aluno.setInstrutor(instrutor);
        aluno.setCarro(buscarCarro(request.carroId(), instrutor));
    }

    /** Busca o administrador associado ou informa quando o vínculo não existe. */
    private Administrador buscarAdministrador(UUID id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "administradorId é obrigatório");
        }
        return administradorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Administrador não encontrado"));
    }

    /** Busca o instrutor associado ou informa quando o vínculo não existe. */
    private Instrutor buscarInstrutor(UUID id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "instrutorId é obrigatório");
        }
        return instrutorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrutor não encontrado"));
    }

    /** Confirma que o carro pertence ao instrutor informado antes de associá-lo. */
    private Carro buscarCarro(UUID id, Instrutor instrutor) {
        if (id == null) {
            return null;
        }
        Carro carro = carroRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carro não encontrado"));
        if (!carro.getInstrutor().getId().equals(instrutor.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O carro deve pertencer ao instrutor do aluno");
        }
        return carro;
    }

    /** Atualiza o indicador de atividade do aluno identificado pelo UUID. */
    public void alterarStatus(UUID id, boolean status) {
        Aluno aluno = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aluno não encontrado"));
        aluno.setAtivo(status);
        repository.save(aluno);
    }

    /** Exclui um aluno e suas avaliações de forma atômica. */
    @Transactional
    public void excluir(UUID id) {
        Aluno aluno = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aluno não encontrado"));
        avaliacaoRepository.excluirPorAlunoId(id);
        repository.delete(aluno);
    }
}