package com.wag.api.controller;

import com.wag.api.dto.AlunoDTO;
import com.wag.api.dto.AlunoLoginRequest;
import com.wag.api.dto.AlunoRequest;
import com.wag.api.service.AlunoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Expõe as operações HTTP disponíveis para o cadastro e a manutenção de alunos. */
@RestController
@RequestMapping("/api/alunos")
@CrossOrigin(origins = "*") 
public class AlunoController {

    public static final String SESSION_ALUNO_ID = "alunoId";

    // Serviço que aplica as regras de cadastro e atualização de alunos.
    private final AlunoService service;

    public AlunoController(AlunoService service) {
        this.service = service;
    }

    /** Lista os alunos cadastrados e converte os resultados para DTOs. */
    @GetMapping
    public ResponseEntity<List<AlunoDTO>> buscarAlunos(HttpServletRequest request) {
        if (!isAdministrador(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(service.listarTodos());
    }

    /** Cadastra um aluno a partir dos dados recebidos em JSON. */
    @PostMapping
    public ResponseEntity<AlunoDTO> criarAluno(@RequestBody AlunoRequest aluno) {
        var novoAluno = service.salvar(aluno);
        return ResponseEntity.ok(new AlunoDTO(novoAluno));
    }

    /** Valida as credenciais e inicia uma sessão autenticada para o aluno. */
    @PostMapping("/login")
    public ResponseEntity<AlunoDTO> login(@RequestBody AlunoLoginRequest request, HttpServletRequest httpRequest) {
        if (request == null) {
            return ResponseEntity.badRequest().build();
        }

        AlunoDTO aluno = service.autenticar(request.email(), request.senha());
        HttpSession session = httpRequest.getSession(true);
        httpRequest.changeSessionId();
        session.setAttribute(SESSION_ALUNO_ID, aluno.id());
        return ResponseEntity.ok(aluno);
    }

    /** Retorna os dados do aluno autenticado na sessão atual. */
    @GetMapping("/sessao")
    public ResponseEntity<AlunoDTO> sessaoAtual(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute(SESSION_ALUNO_ID) instanceof UUID alunoId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(service.buscarPorId(alunoId));
    }

    /** Encerra somente a sessão do aluno autenticado. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(SESSION_ALUNO_ID);
            if (session.getAttributeNames().asIterator().hasNext()) {
                return ResponseEntity.noContent().build();
            }
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    /** Atualiza os dados de um aluno identificado pelo UUID informado na rota. */
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizarAluno(
            @PathVariable UUID id,
            @RequestBody AlunoRequest aluno,
            HttpServletRequest request) {
        if (!isAdministrador(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        var alunoAtualizado = service.atualizar(id, aluno);
        return ResponseEntity.ok(new AlunoDTO(alunoAtualizado));
    }

    /** Altera somente o estado ativo do aluno e não retorna um corpo de resposta. */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> alterarStatusAluno(
            @PathVariable UUID id,
            @RequestParam boolean ativo,
            HttpServletRequest request) {
        if (!isAdministrador(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        service.alterarStatus(id, ativo);
        return ResponseEntity.noContent().build();
    }

    /** Exclui um aluno somente por uma sessão administrativa. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirAluno(@PathVariable UUID id, HttpServletRequest request) {
        if (!isAdministrador(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }

    /** Confirma a presença de uma sessão administrativa antes de liberar funções de gestão. */
    private boolean isAdministrador(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute(AdministradorController.SESSION_ADMIN_ID) instanceof UUID;
    }

    /** Retorna erros de validação de forma consistente para os formulários do aluno. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> tratarErro(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(Map.of("message", exception.getReason() == null
                        ? "Não foi possível concluir a operação."
                        : exception.getReason()));
    }
}