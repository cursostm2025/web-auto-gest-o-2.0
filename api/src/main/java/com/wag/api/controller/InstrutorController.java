package com.wag.api.controller;

import com.wag.api.dto.InstrutorDTO;
import com.wag.api.dto.InstrutorRequest;
import com.wag.api.dto.AvaliacaoInstrutorDTO;
import com.wag.api.dto.AvaliacaoInstrutorRequest;
import com.wag.api.service.AlunoService;
import com.wag.api.service.InstrutorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Expõe as operações HTTP para consulta e cadastro de instrutores. */
@RestController
@RequestMapping("/api/instrutores")
@CrossOrigin(origins = "*") 
public class InstrutorController {

    // Serviço que aplica as regras de cadastro dos instrutores.
    private final InstrutorService service;
    private final AlunoService alunoService;

    public InstrutorController(InstrutorService service, AlunoService alunoService) {
        this.service = service;
        this.alunoService = alunoService;
    }

    /** Lista os instrutores cadastrados em formato apropriado para a API. */
    @GetMapping
    public ResponseEntity<List<InstrutorDTO>> buscarInstrutores(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (session.getAttribute(AdministradorController.SESSION_ADMIN_ID) instanceof java.util.UUID) {
            return ResponseEntity.ok(service.listarTodos());
        }
        if (session.getAttribute(AlunoController.SESSION_ALUNO_ID) instanceof java.util.UUID alunoId) {
            alunoService.buscarPorId(alunoId);
            return ResponseEntity.ok(service.listarAtivos(alunoId));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    /** Registra ou atualiza a avaliação do instrutor pelo aluno autenticado. */
    @PostMapping("/{id}/avaliacoes")
    public ResponseEntity<AvaliacaoInstrutorDTO> avaliarInstrutor(
            @PathVariable UUID id,
            @RequestBody AvaliacaoInstrutorRequest avaliacao,
            HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!(session.getAttribute(AlunoController.SESSION_ALUNO_ID) instanceof UUID alunoId)) {
            return ResponseEntity.status(session.getAttribute(AdministradorController.SESSION_ADMIN_ID) instanceof UUID
                    ? HttpStatus.FORBIDDEN
                    : HttpStatus.UNAUTHORIZED).build();
        }
        alunoService.buscarPorId(alunoId);
        return ResponseEntity.ok(service.avaliar(id, alunoId, avaliacao == null ? null : avaliacao.nota()));
    }

    /** Cadastra um instrutor, exigindo o UUID do administrador responsável. */
    @PostMapping
    public ResponseEntity<?> criarInstrutor(
            @RequestBody InstrutorRequest instrutor,
            HttpServletRequest request) {
        UUID administradorId = administradorId(request);
        if (administradorId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Faça login como administrador antes de cadastrar um instrutor."));
        }
        InstrutorRequest dados = new InstrutorRequest(
                instrutor.nome(),
                instrutor.email(),
                instrutor.telefoneWhatsapp(),
                instrutor.endereco(),
                instrutor.categoriaCnh(),
                instrutor.descricao(),
                administradorId);
        var novoInstrutor = service.salvar(dados);
        return ResponseEntity.ok(new InstrutorDTO(novoInstrutor));
    }

    /** Atualiza dados de um instrutor somente por uma sessão administrativa. */
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizarInstrutor(
            @PathVariable UUID id,
            @RequestBody InstrutorRequest instrutor,
            HttpServletRequest request) {
        if (administradorId(request) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(new InstrutorDTO(service.atualizar(id, instrutor)));
    }

    /** Ativa ou bloqueia um instrutor somente por uma sessão administrativa. */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> alterarStatusInstrutor(
            @PathVariable UUID id,
            @RequestParam boolean ativo,
            HttpServletRequest request) {
        if (administradorId(request) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        service.alterarStatus(id, ativo);
        return ResponseEntity.noContent().build();
    }

    /** Exclui um instrutor somente por uma sessão administrativa. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirInstrutor(@PathVariable UUID id, HttpServletRequest request) {
        if (administradorId(request) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }

    /** Lê o UUID administrativo associado à sessão atual. */
    private UUID administradorId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute(AdministradorController.SESSION_ADMIN_ID) instanceof UUID id)) {
            return null;
        }
        return id;
    }

    /** Converte erros de validação do serviço em respostas JSON com o status correspondente. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> tratarErroDeCadastro(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(Map.of("message", exception.getReason() == null
                        ? "Não foi possível cadastrar o instrutor."
                        : exception.getReason()));
    }
}