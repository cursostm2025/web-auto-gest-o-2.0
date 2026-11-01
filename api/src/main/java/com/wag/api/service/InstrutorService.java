package com.wag.api.service;

import com.wag.api.dto.InstrutorDTO;
import com.wag.api.dto.InstrutorRequest;
import com.wag.api.dto.AvaliacaoInstrutorDTO;
import com.wag.api.model.Administrador;
import com.wag.api.model.Aluno;
import com.wag.api.model.AvaliacaoInstrutor;
import com.wag.api.model.Instrutor;
import com.wag.api.repository.AdministradorRepository;
import com.wag.api.repository.AlunoRepository;
import com.wag.api.repository.AvaliacaoInstrutorRepository;
import com.wag.api.repository.AvaliacaoInstrutorResumo;
import com.wag.api.repository.CarroRepository;
import com.wag.api.repository.InstrutorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Aplica as regras de negócio para consulta e cadastro de instrutores. */
@Service
public class InstrutorService {

    // Repositórios usados para persistir o instrutor e localizar seu administrador.
    private final InstrutorRepository repository;
    private final AdministradorRepository administradorRepository;
    private final AlunoRepository alunoRepository;
    private final AvaliacaoInstrutorRepository avaliacaoRepository;
    private final CarroRepository carroRepository;

    public InstrutorService(
            InstrutorRepository repository,
            AdministradorRepository administradorRepository,
            AlunoRepository alunoRepository,
            AvaliacaoInstrutorRepository avaliacaoRepository,
            CarroRepository carroRepository) {
        this.repository = repository;
        this.administradorRepository = administradorRepository;
        this.alunoRepository = alunoRepository;
        this.avaliacaoRepository = avaliacaoRepository;
        this.carroRepository = carroRepository;
    }

    /** Lista instrutores convertidos para o formato público da API. */
    public List<InstrutorDTO> listarTodos() {
        return repository.findAll().stream()
                .map(InstrutorDTO::new)
                .toList(); 
    }

    /** Lista somente instrutores liberados para consulta pelos alunos. */
    public List<InstrutorDTO> listarAtivos(UUID alunoId) {
        List<Instrutor> instrutores = repository.findAllByAtivoTrue();
        if (instrutores.isEmpty()) {
            return List.of();
        }

        List<UUID> instrutorIds = instrutores.stream().map(instrutor -> instrutor.getId()).toList();
        Map<UUID, AvaliacaoInstrutorResumo> resumos =
                avaliacaoRepository.resumirPorInstrutores(instrutorIds, alunoId).stream()
                        .collect(Collectors.toMap(
                                resumo -> resumo.getInstrutorId(),
                                Function.identity()));

        return instrutores.stream()
                .map(instrutor -> {
                    var resumo = resumos.get(instrutor.getId());
                    return resumo == null
                            ? new InstrutorDTO(instrutor)
                            : new InstrutorDTO(
                                    instrutor,
                                    resumo.getMedia(),
                                    resumo.getTotal(),
                                    resumo.getNotaDoAluno());
                })
                .toList();
    }

    /** Salva ou atualiza a nota do aluno e retorna o resumo atualizado. */
    @Transactional
    public AvaliacaoInstrutorDTO avaliar(UUID instrutorId, UUID alunoId, Integer nota) {
        if (nota == null || nota < 1 || nota > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A avaliação deve ser uma nota entre 1 e 5.");
        }

        Instrutor instrutor = repository.findById(instrutorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrutor não encontrado."));
        Aluno aluno = alunoRepository.findById(alunoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Faça login novamente."));
        AvaliacaoInstrutor avaliacao = avaliacaoRepository.findByAlunoIdAndInstrutorId(alunoId, instrutorId)
                .orElseGet(AvaliacaoInstrutor::new);
        avaliacao.setInstrutor(instrutor);
        avaliacao.setAluno(aluno);
        avaliacao.setNota(nota);
        avaliacaoRepository.save(avaliacao);

        List<AvaliacaoInstrutorResumo> resumos =
                avaliacaoRepository.resumirPorInstrutores(List.of(instrutorId), alunoId);
        var resumo = resumos.getFirst();
        return new AvaliacaoInstrutorDTO(resumo.getMedia(), resumo.getTotal(), resumo.getNotaDoAluno());
    }

    /** Valida e-mail e administrador antes de persistir um novo instrutor. */
    public Instrutor salvar(InstrutorRequest request) {

        // Rejeita e-mail ausente e normaliza espaços antes de verificar duplicidade.

        if (request.email() == null || request.email().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail é obrigatório");
        }
        String email = request.email().trim();

        // A busca sem diferenciar maiúsculas evita duplicatas por variação de caixa.

        if (repository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um instrutor cadastrado com este e-mail");
        }
        // Todo instrutor precisa estar associado a um administrador existente.
        if (request.administradorId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "administradorId é obrigatório");
        }
        Administrador administrador = administradorRepository.findById(request.administradorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Administrador não encontrado"));

        Instrutor instrutor = new Instrutor();
        instrutor.setNome(request.nome());
        instrutor.setEmail(email);
        instrutor.setTelefoneWhatsapp(request.telefoneWhatsapp());
        instrutor.setEndereco(request.endereco());
        instrutor.setCategoriaCnh(request.categoriaCnh());
        instrutor.setDescricao(request.descricao());
        instrutor.setAdministrador(administrador);
        return repository.save(instrutor);
    }

    /** Atualiza dados editáveis sem alterar o administrador responsável nem o status. */

    public Instrutor atualizar(UUID id, InstrutorRequest request) {
        if (request.nome() == null || request.nome().isBlank()
                || request.email() == null || request.email().isBlank()
                || request.telefoneWhatsapp() == null || request.telefoneWhatsapp().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Nome, e-mail e WhatsApp são obrigatórios.");
        }
        Instrutor instrutor = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrutor não encontrado"));
        String email = request.email() == null ? instrutor.getEmail() : request.email().trim();
        if (repository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um instrutor cadastrado com este e-mail");
        }
        instrutor.setNome(request.nome());
        instrutor.setEmail(email);
        instrutor.setTelefoneWhatsapp(request.telefoneWhatsapp());
        instrutor.setEndereco(request.endereco());
        instrutor.setCategoriaCnh(request.categoriaCnh());
        instrutor.setDescricao(request.descricao());
        return repository.save(instrutor);
    }

    /** Altera a disponibilidade do instrutor para os alunos. */
    public void alterarStatus(UUID id, boolean ativo) {
        Instrutor instrutor = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrutor não encontrado"));
        instrutor.setAtivo(ativo);
        repository.save(instrutor);
    }

    /** Exclui um instrutor sem remover alunos ou veículos associados. */
    @Transactional
    public void excluir(UUID id) {
        Instrutor instrutor = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrutor não encontrado"));
        if (alunoRepository.existsByInstrutorId(id) || carroRepository.existsByInstrutorId(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Não é possível excluir este instrutor enquanto houver alunos ou veículos vinculados.");
        }
        avaliacaoRepository.excluirPorInstrutorId(id);
        repository.delete(instrutor);
    }
}