package com.wag.api.service;

import com.wag.api.dto.CarroDTO;
import com.wag.api.dto.CarroRequest;
import com.wag.api.model.Carro;
import com.wag.api.repository.CarroRepository;
import com.wag.api.repository.InstrutorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/** Implementa as regras de consulta e cadastro dos veículos dos instrutores. */
@Service
public class CarroService {

    // Repositórios usados para consultar veículos e validar seu instrutor.
    private final CarroRepository repository;
    private final InstrutorRepository instrutorRepository;

    public CarroService(CarroRepository repository, InstrutorRepository instrutorRepository) {
        this.repository = repository;
        this.instrutorRepository = instrutorRepository;
    }

    /** Lista todos os carros ou filtra a lista pelo UUID do instrutor. */
    public List<CarroDTO> listarTodos(UUID instrutorId) {
        List<Carro> carros = instrutorId == null
                ? repository.findAll()
                : repository.findAllByInstrutorId(instrutorId);
        return carros.stream().map(CarroDTO::new).toList();
    }

    /** Valida o instrutor associado, persiste o carro e retorna seu DTO. */
    public CarroDTO salvar(CarroRequest request) {
        if (request.instrutorId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "instrutorId é obrigatório");
        }

        var instrutor = instrutorRepository.findById(request.instrutorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrutor não encontrado"));

        Carro carro = new Carro();
        carro.setNomeVeiculo(request.nomeVeiculo());
        carro.setPlaca(request.placa());
        carro.setModelo(request.modelo());
        carro.setCor(request.cor());
        carro.setAno(request.ano());
        carro.setInstrutor(instrutor);
        return new CarroDTO(repository.save(carro));
    }
}
