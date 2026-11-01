package com.wag.api.controller;

import com.wag.api.dto.CarroDTO;
import com.wag.api.dto.CarroRequest;
import com.wag.api.service.CarroService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Expõe as operações HTTP de consulta e cadastro de veículos. */
@RestController
@RequestMapping("/api/carros")
@CrossOrigin(origins = "*")
public class CarroController {

    // Serviço que consulta veículos e valida seu vínculo com um instrutor.
    private final CarroService service;

    public CarroController(CarroService service) {
        this.service = service;
    }

    /** Lista todos os carros ou filtra pelos veículos de um instrutor. */
    @GetMapping
    public ResponseEntity<List<CarroDTO>> buscarCarros(@RequestParam(required = false) UUID instrutorId) {
        return ResponseEntity.ok(service.listarTodos(instrutorId));
    }

    /** Cadastra um veículo associado ao instrutor informado na requisição. */
    @PostMapping
    public ResponseEntity<CarroDTO> criarCarro(@RequestBody CarroRequest carro) {
        return ResponseEntity.ok(service.salvar(carro));
    }
}
