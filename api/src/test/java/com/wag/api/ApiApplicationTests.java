package com.wag.api;

import com.wag.api.model.Administrador;
import com.wag.api.model.Aluno;
import com.wag.api.model.Carro;
import com.wag.api.model.Instrutor;
import com.wag.api.repository.AdministradorRepository;
import com.wag.api.repository.AlunoRepository;
import com.wag.api.repository.CarroRepository;
import com.wag.api.repository.InstrutorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
		"spring.flyway.enabled=false",
		"spring.datasource.url=jdbc:h2:mem:autowag;DB_CLOSE_DELAY=-1",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.datasource.driver-class-name=org.h2.Driver"
})
class ApiApplicationTests {

	@Autowired
	private AdministradorRepository administradorRepository;

	@Autowired
	private AlunoRepository alunoRepository;

	@Autowired
	private InstrutorRepository instrutorRepository;

	@Autowired
	private CarroRepository carroRepository;

	@Test
	void contextLoads() {
	}

	@Test
	@Transactional
	void savesAdministratorInstructorAndStudentRelationships() {
		Administrador administrador = new Administrador();
		administrador.setNome("Administrador de teste");
		administrador.setEmail("admin-teste@exemplo.com");
		administrador.setTelefone("11999999999");
		administrador = administradorRepository.save(administrador);

		Instrutor instrutor = new Instrutor();
		instrutor.setNome("Instrutor de teste");
		instrutor.setEmail("instrutor-teste@exemplo.com");
		instrutor.setTelefoneWhatsapp("11988888888");
		instrutor.setAdministrador(administrador);
		instrutor = instrutorRepository.save(instrutor);

		Carro carro = new Carro();
		carro.setNomeVeiculo("Carro de teste");
		carro.setPlaca("ABC1234");
		carro.setModelo("Hatch");
		carro.setCor("Branco");
		carro.setAno(2024);
		carro.setInstrutor(instrutor);
		carro = carroRepository.save(carro);

		Carro segundoCarro = new Carro();
		segundoCarro.setNomeVeiculo("Segundo carro");
		segundoCarro.setPlaca("DEF5678");
		segundoCarro.setModelo("Sedan");
		segundoCarro.setCor("Prata");
		segundoCarro.setAno(2023);
		segundoCarro.setInstrutor(instrutor);
		carroRepository.save(segundoCarro);

		Aluno aluno = new Aluno();
		aluno.setNome("Aluno de teste");
		aluno.setTelefone("11977777777");
		aluno.setAdministrador(administrador);
		aluno.setInstrutor(instrutor);
		aluno.setCarro(carro);
		aluno = alunoRepository.save(aluno);

		Aluno outroAluno = new Aluno();
		outroAluno.setNome("Outro aluno de teste");
		outroAluno.setTelefone("11966666666");
		outroAluno.setAdministrador(administrador);
		outroAluno.setInstrutor(instrutor);
		outroAluno.setCarro(carro);
		alunoRepository.save(outroAluno);

		assertEquals(administrador.getId(), aluno.getAdministrador().getId());
		assertEquals(administrador.getId(), instrutor.getAdministrador().getId());
		assertEquals(instrutor.getId(), aluno.getInstrutor().getId());
		assertEquals(carro.getId(), aluno.getCarro().getId());
		assertEquals(2, carroRepository.findAllByInstrutorId(instrutor.getId()).size());
		var carroId = carro.getId();
		assertEquals(2, alunoRepository.findAll().stream()
				.filter(alunoSalvo -> carroId.equals(alunoSalvo.getCarro().getId()))
				.count());
	}
}
