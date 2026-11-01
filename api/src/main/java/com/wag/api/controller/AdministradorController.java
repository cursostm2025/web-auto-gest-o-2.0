package com.wag.api.controller;

import com.wag.api.dto.AdministradorDTO;
import com.wag.api.dto.AdministradorLoginRequest;
import com.wag.api.model.Administrador;
import com.wag.api.service.AdministradorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Expõe as operações HTTP de consulta, cadastro e autenticação de administradores.
 */
@RestController
@RequestMapping("/api/administradores")
@CrossOrigin(origins = "*")
public class AdministradorController {

    public static final String SESSION_ADMIN_ID = "administradorId";

    // Serviço responsável pelas regras de negócio dos administradores.
    private final AdministradorService service;

    public AdministradorController(AdministradorService service) {
        this.service = service;
    }

    /** Lista os administradores sem incluir suas senhas na resposta. */
    @GetMapping
    public ResponseEntity<List<AdministradorDTO>> buscarAdministradores(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute(SESSION_ADMIN_ID) instanceof UUID)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(service.listarTodos());
    }

    /** Cadastra um administrador e retorna os dados públicos do registro criado. */
    @PostMapping
    public ResponseEntity<AdministradorDTO> criarAdministrador(@RequestBody Administrador administrador) {
        Administrador novoAdmin = service.salvar(administrador);
        return ResponseEntity.ok(new AdministradorDTO(novoAdmin));
    }

    /** Valida as credenciais e retorna 401 quando o e-mail ou a senha forem inválidos. */
    @PostMapping("/login")
    public ResponseEntity<AdministradorDTO> login(
            @RequestBody AdministradorLoginRequest request,
            HttpServletRequest httpRequest) {
        // Um corpo ausente não pode ser usado para validar as credenciais.
        if (request == null) {
            return ResponseEntity.badRequest().build();
        }
        AdministradorDTO administrador = service.autenticar(request.email(), request.senha())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos."));
        HttpSession session = httpRequest.getSession(true);
        httpRequest.changeSessionId();
        session.setAttribute(SESSION_ADMIN_ID, administrador.id());
        return ResponseEntity.ok(administrador);
    }

    /** Retorna o administrador autenticado na sessão atual. */
    @GetMapping("/sessao")
    public ResponseEntity<AdministradorDTO> sessaoAtual(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute(SESSION_ADMIN_ID) instanceof UUID adminId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return service.buscarPorId(adminId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /** Encerra somente a sessão administrativa atual. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(SESSION_ADMIN_ID);
            if (!session.getAttributeNames().asIterator().hasNext()) {
                session.invalidate();
            }
        }
        return ResponseEntity.noContent().build();
    }

    /** Converte falhas de login e validação em mensagens úteis para o cliente. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> tratarErro(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(Map.of("message", exception.getReason() == null
                        ? "Não foi possível concluir a operação."
                        : exception.getReason()));
    }
}