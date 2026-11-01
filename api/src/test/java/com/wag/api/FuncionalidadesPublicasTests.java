package com.wag.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:funcionalidades;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa"
})
class FuncionalidadesPublicasTests {

    @Value("${local.server.port}")
    private int port;

    @Test
    void disponibilizaAsPaginasDeAcessoDoAluno() throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> login = send(client, "GET", "/login-aluno.html", null);
        assertEquals(200, login.statusCode());
        assertTrue(login.body().contains("id=\"form-login-aluno\""));
        assertTrue(login.body().contains("/js/login-aluno.js"));

        HttpResponse<String> cadastro = send(client, "GET", "/cadastro-aluno.html", null);
        assertEquals(200, cadastro.statusCode());
        assertTrue(cadastro.body().contains("name=\"senha\""));
        assertTrue(cadastro.body().contains("/js/cadastro-aluno.js"));

        HttpResponse<String> painel = send(client, "GET", "/admin-painel.html", null);
        assertEquals(200, painel.statusCode());
        assertTrue(painel.body().contains("id=\"tabela-alunos\""));
        assertTrue(painel.body().contains("id=\"tabela-instrutores\""));

        HttpResponse<String> paginaInicial = send(client, "GET", "/", null);
        assertEquals(200, paginaInicial.statusCode());
        HttpResponse<String> script = send(client, "GET", "/js/script.js", null);
        assertEquals(200, script.statusCode());
        assertTrue(script.body().contains("rating-star"));
        assertTrue(script.body().contains("salvarAvaliacao"));
    }

    @Test
    void exigeSessaoDeAlunoParaConsultarInstrutores() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .build();

        assertEquals(401, send(client, "GET", "/api/instrutores", null).statusCode());

        String email = "aluno-" + UUID.randomUUID() + "@exemplo.com";
        HttpResponse<String> cadastro = post("/api/alunos", """
                {
                  "nome": "Aluno de teste",
                  "email": "%s",
                  "telefone": "11999999999",
                  "endereco": "Rua de teste",
                  "senha": "senha-segura-123"
                }
                """.formatted(email));
        assertEquals(200, cadastro.statusCode());
        assertFalse(cadastro.body().contains("\"senha\""));

        HttpResponse<String> loginInvalido = send(client, "POST", "/api/alunos/login", """
                {"email": "%s", "senha": "senha-incorreta"}
                """.formatted(email));
        assertEquals(401, loginInvalido.statusCode());
        assertTrue(loginInvalido.body().contains("Confira os dados usados no cadastro"));

        HttpResponse<String> login = send(client, "POST", "/api/alunos/login", """
                {"email": "%s", "senha": "senha-segura-123"}
                """.formatted(email));
        assertEquals(200, login.statusCode());
        assertFalse(login.body().contains("\"senha\""));
        assertEquals(200, send(client, "GET", "/api/alunos/sessao", null).statusCode());
        assertEquals(200, send(client, "GET", "/api/instrutores", null).statusCode());

        assertEquals(204, send(client, "POST", "/api/alunos/logout", "").statusCode());
        assertEquals(401, send(client, "GET", "/api/alunos/sessao", null).statusCode());
        assertEquals(401, send(client, "GET", "/api/instrutores", null).statusCode());
    }

    @Test
    void cadastraAlunoSemVinculosAdministrativos() throws Exception {
        String email = "aluno-" + UUID.randomUUID() + "@exemplo.com";
        HttpResponse<String> resposta = post("/api/alunos", """
                {
                  "nome": "Aluno de teste",
                  "email": "%s",
                  "telefone": "11999999999",
                  "endereco": "Rua de teste",
                  "senha": "senha-segura-123"
                }
                """.formatted(email));

        assertEquals(200, resposta.statusCode());
        assertTrue(resposta.body().contains("\"nome\":\"Aluno de teste\""));
        assertTrue(resposta.body().contains("\"email\":\"" + email + "\""));
        assertTrue(resposta.body().contains("\"administradorId\":null"));
        assertTrue(resposta.body().contains("\"instrutorId\":null"));
        assertFalse(resposta.body().contains("\"senha\""));
    }

    @Test
    void autenticaAdministradorSemExporSenha() throws Exception {
        HttpClient client = criarClienteComSessao();
        String email = "admin-" + UUID.randomUUID() + "@exemplo.com";
        HttpResponse<String> cadastro = post("/api/administradores", """
                {
                  "nome": "Admin de teste",
                  "email": "%s",
                  "telefone": "11988888888",
                  "senha": "senha-forte"
                }
                """.formatted(email));

        assertEquals(200, cadastro.statusCode());
        assertFalse(cadastro.body().contains("\"senha\""));

        HttpResponse<String> loginValido = send(client, "POST", "/api/administradores/login", """
                {"email": "%s", "senha": "senha-forte"}
                """.formatted(email));
        assertEquals(200, loginValido.statusCode());
        assertTrue(loginValido.body().contains("\"email\":\"" + email + "\""));
        assertFalse(loginValido.body().contains("\"senha\""));

        assertEquals(200, send(client, "GET", "/api/administradores/sessao", null).statusCode());
        assertEquals(200, send(client, "GET", "/api/alunos", null).statusCode());

        HttpResponse<String> loginInvalido = send(client, "POST", "/api/administradores/login", """
                {"email": "%s", "senha": "senha-incorreta"}
                """.formatted(email));
        assertEquals(401, loginInvalido.statusCode());

        assertEquals(204, send(client, "POST", "/api/administradores/logout", "").statusCode());
        assertEquals(401, send(client, "GET", "/api/administradores/sessao", null).statusCode());
        assertEquals(403, send(client, "GET", "/api/alunos", null).statusCode());
    }

    @Test
    void cadastraInstrutorVinculadoAoAdminEInformaEmailDuplicado() throws Exception {
        HttpClient client = criarClienteComSessao();
        String sufixo = UUID.randomUUID().toString();
        String emailAdmin = "admin-" + sufixo + "@exemplo.com";
        HttpResponse<String> cadastroAdmin = post("/api/administradores", """
                {
                  "nome": "Admin de teste",
                  "email": "%s",
                  "telefone": "11988888888",
                  "senha": "senha-forte"
                }
                """.formatted(emailAdmin));
        assertEquals(200, cadastroAdmin.statusCode());

        HttpResponse<String> login = send(client, "POST", "/api/administradores/login", """
                {"email": "%s", "senha": "senha-forte"}
                """.formatted(emailAdmin));
        assertEquals(200, login.statusCode());

        Matcher matcher = Pattern.compile("\"id\":\"([^\"]+)\"").matcher(cadastroAdmin.body());
        assertTrue(matcher.find(), "A resposta de cadastro deve conter o ID do administrador.");
        String administradorId = matcher.group(1);
        String emailInstrutor = "instrutor-" + sufixo + "@exemplo.com";
        String requestInstrutor = """
                {
                  "nome": "Instrutor de teste",
                  "email": "%s",
                  "telefoneWhatsapp": "11977777777",
                  "administradorId": "%s"
                }
                """.formatted(emailInstrutor, administradorId);

        HttpResponse<String> cadastroInstrutor = send(client, "POST", "/api/instrutores", requestInstrutor);
        assertEquals(200, cadastroInstrutor.statusCode());

        HttpResponse<String> emailDuplicado = send(client, "POST", "/api/instrutores", requestInstrutor);
        assertEquals(409, emailDuplicado.statusCode());
        assertTrue(emailDuplicado.body().contains("Já existe um instrutor cadastrado"), emailDuplicado.body());
    }

    @Test
    void informaQuandoCadastroDeInstrutorNaoRecebeAdministrador() throws Exception {
        HttpResponse<String> resposta = post("/api/instrutores", """
                {
                  "nome": "Instrutor sem vínculo",
                  "email": "instrutor-%s@exemplo.com",
                  "telefoneWhatsapp": "11977777777"
                }
                """.formatted(UUID.randomUUID()));

        assertEquals(403, resposta.statusCode());
        assertTrue(resposta.body().contains("Faça login como administrador"), resposta.body());
    }

    @Test
    void alunoAvaliaInstrutorEAtualizaSuaNotaSemDuplicarVoto() throws Exception {
        String sufixo = UUID.randomUUID().toString();
        HttpClient adminClient = criarClienteComSessao();
        String emailAdmin = "admin-avaliacao-" + sufixo + "@exemplo.com";
        HttpResponse<String> cadastroAdmin = post("/api/administradores", """
                {
                  "nome": "Admin de avaliações",
                  "email": "%s",
                  "telefone": "11988888888",
                  "senha": "senha-forte"
                }
                """.formatted(emailAdmin));
        assertEquals(200, cadastroAdmin.statusCode());
        assertEquals(200, send(adminClient, "POST", "/api/administradores/login", """
                {"email": "%s", "senha": "senha-forte"}
                """.formatted(emailAdmin)).statusCode());
        Matcher adminMatcher = Pattern.compile("\"id\":\"([^\"]+)\"").matcher(cadastroAdmin.body());
        assertTrue(adminMatcher.find());

        HttpResponse<String> cadastroInstrutor = send(adminClient, "POST", "/api/instrutores", """
                {
                  "nome": "Instrutor avaliável",
                  "email": "instrutor-avaliacao-%s@exemplo.com",
                  "telefoneWhatsapp": "11977777777",
                  "administradorId": "%s"
                }
                """.formatted(sufixo, adminMatcher.group(1)));
        assertEquals(200, cadastroInstrutor.statusCode());
        Matcher instrutorMatcher = Pattern.compile("\"id\":\"([^\"]+)\"").matcher(cadastroInstrutor.body());
        assertTrue(instrutorMatcher.find());
        String instrutorId = instrutorMatcher.group(1);

        String emailAluno = "aluno-avaliacao-" + sufixo + "@exemplo.com";
        assertEquals(200, post("/api/alunos", """
                {
                  "nome": "Aluno avaliador",
                  "email": "%s",
                  "telefone": "11999999999",
                  "senha": "senha-segura-123"
                }
                """.formatted(emailAluno)).statusCode());
        HttpClient alunoClient = criarClienteComSessao();
        assertEquals(200, send(alunoClient, "POST", "/api/alunos/login", """
                {"email": "%s", "senha": "senha-segura-123"}
                """.formatted(emailAluno)).statusCode());

        String caminhoAvaliacao = "/api/instrutores/" + instrutorId + "/avaliacoes";
        assertEquals(401, send(criarClienteComSessao(), "POST", caminhoAvaliacao, "{\"nota\":5}").statusCode());
        assertEquals(403, send(adminClient, "POST", caminhoAvaliacao, "{\"nota\":5}").statusCode());
        assertEquals(400, send(alunoClient, "POST", caminhoAvaliacao, "{\"nota\":6}").statusCode());

        HttpResponse<String> primeiraAvaliacao =
                send(alunoClient, "POST", caminhoAvaliacao, "{\"nota\":4}");
        assertEquals(200, primeiraAvaliacao.statusCode());
        assertTrue(primeiraAvaliacao.body().contains("\"media\":4.0"), primeiraAvaliacao.body());
        assertTrue(primeiraAvaliacao.body().contains("\"total\":1"), primeiraAvaliacao.body());
        assertTrue(primeiraAvaliacao.body().contains("\"notaDoAluno\":4"), primeiraAvaliacao.body());

        HttpResponse<String> notaAtualizada =
                send(alunoClient, "POST", caminhoAvaliacao, "{\"nota\":2}");
        assertEquals(200, notaAtualizada.statusCode());
        assertTrue(notaAtualizada.body().contains("\"media\":2.0"), notaAtualizada.body());
        assertTrue(notaAtualizada.body().contains("\"total\":1"), notaAtualizada.body());
        assertTrue(notaAtualizada.body().contains("\"notaDoAluno\":2"), notaAtualizada.body());

        String segundoEmailAluno = "segundo-aluno-avaliacao-" + sufixo + "@exemplo.com";
        assertEquals(200, post("/api/alunos", """
                {
                  "nome": "Segundo aluno avaliador",
                  "email": "%s",
                  "telefone": "11999999998",
                  "senha": "senha-segura-123"
                }
                """.formatted(segundoEmailAluno)).statusCode());
        HttpClient segundoAlunoClient = criarClienteComSessao();
        assertEquals(200, send(segundoAlunoClient, "POST", "/api/alunos/login", """
                {"email": "%s", "senha": "senha-segura-123"}
                """.formatted(segundoEmailAluno)).statusCode());
        HttpResponse<String> segundaAvaliacao =
                send(segundoAlunoClient, "POST", caminhoAvaliacao, "{\"nota\":5}");
        assertEquals(200, segundaAvaliacao.statusCode());
        assertTrue(segundaAvaliacao.body().contains("\"media\":3.5"), segundaAvaliacao.body());
        assertTrue(segundaAvaliacao.body().contains("\"total\":2"), segundaAvaliacao.body());
        assertTrue(segundaAvaliacao.body().contains("\"notaDoAluno\":5"), segundaAvaliacao.body());

        HttpResponse<String> instrutores = send(alunoClient, "GET", "/api/instrutores", null);
        assertEquals(200, instrutores.statusCode());
        assertTrue(instrutores.body().contains("\"avaliacaoMedia\":3.5"), instrutores.body());
        assertTrue(instrutores.body().contains("\"totalAvaliacoes\":2"), instrutores.body());
        assertTrue(instrutores.body().contains("\"notaDoAluno\":2"), instrutores.body());
    }

    @Test
    void administradorEditaBloqueiaEDesbloqueiaAlunosEInstrutores() throws Exception {
        HttpClient adminClient = criarClienteComSessao();
        String emailAdmin = "admin-" + UUID.randomUUID() + "@exemplo.com";
        assertEquals(200, post("/api/administradores", """
                {
                  "nome": "Admin de gestão",
                  "email": "%s",
                  "telefone": "11988888888",
                  "senha": "senha-forte"
                }
                """.formatted(emailAdmin)).statusCode());
        assertEquals(200, send(adminClient, "POST", "/api/administradores/login", """
                {"email": "%s", "senha": "senha-forte"}
                """.formatted(emailAdmin)).statusCode());

        String emailAluno = "aluno-" + UUID.randomUUID() + "@exemplo.com";
        HttpResponse<String> cadastroAluno = post("/api/alunos", """
                {
                  "nome": "Aluno original",
                  "email": "%s",
                  "telefone": "11999999999",
                  "endereco": "Endereço original",
                  "senha": "senha-segura-123"
                }
                """.formatted(emailAluno));
        Matcher alunoMatcher = Pattern.compile("\"id\":\"([^\"]+)\"").matcher(cadastroAluno.body());
        assertTrue(alunoMatcher.find());
        String alunoId = alunoMatcher.group(1);

        HttpResponse<String> alunoEditado = send(adminClient, "PUT", "/api/alunos/" + alunoId, """
                {
                  "nome": "Aluno editado",
                  "email": "%s",
                  "telefone": "11999999998",
                  "endereco": "Endereço atualizado"
                }
                """.formatted(emailAluno));
        assertEquals(200, alunoEditado.statusCode());
        assertTrue(alunoEditado.body().contains("Aluno editado"));
        assertEquals(204, send(adminClient, "PATCH", "/api/alunos/" + alunoId + "/status?ativo=false", "").statusCode());

        HttpClient alunoClient = criarClienteComSessao();
        assertEquals(401, send(alunoClient, "POST", "/api/alunos/login", """
                {"email": "%s", "senha": "senha-segura-123"}
                """.formatted(emailAluno)).statusCode());
        assertEquals(204, send(adminClient, "PATCH", "/api/alunos/" + alunoId + "/status?ativo=true", "").statusCode());
        assertEquals(200, send(alunoClient, "POST", "/api/alunos/login", """
                {"email": "%s", "senha": "senha-segura-123"}
                """.formatted(emailAluno)).statusCode());

        Matcher adminMatcher = Pattern.compile("\"id\":\"([^\"]+)\"")
                .matcher(send(adminClient, "GET", "/api/administradores/sessao", null).body());
        assertTrue(adminMatcher.find());
        String administradorId = adminMatcher.group(1);
        HttpResponse<String> cadastroInstrutor = send(adminClient, "POST", "/api/instrutores", """
                {
                  "nome": "Instrutor original",
                  "email": "instrutor-%s@exemplo.com",
                  "telefoneWhatsapp": "11977777777",
                  "administradorId": "%s"
                }
                """.formatted(UUID.randomUUID(), administradorId));
        assertEquals(200, cadastroInstrutor.statusCode());
        Matcher instrutorMatcher = Pattern.compile("\"id\":\"([^\"]+)\"").matcher(cadastroInstrutor.body());
        assertTrue(instrutorMatcher.find());
        String instrutorId = instrutorMatcher.group(1);

        HttpResponse<String> instrutorEditado = send(adminClient, "PUT", "/api/instrutores/" + instrutorId, """
                {
                  "nome": "Instrutor editado",
                  "email": "%s",
                  "telefoneWhatsapp": "11977777776",
                  "descricao": "Perfil atualizado"
                }
                """.formatted("instrutor-editado-" + UUID.randomUUID() + "@exemplo.com"));
        assertEquals(200, instrutorEditado.statusCode());
        assertTrue(instrutorEditado.body().contains("Instrutor editado"));
        assertEquals(204, send(adminClient, "PATCH", "/api/instrutores/" + instrutorId + "/status?ativo=false", "").statusCode());

        HttpResponse<String> instrutoresBloqueados = send(adminClient, "GET", "/api/instrutores", null);
        assertTrue(instrutoresBloqueados.body().contains("\"ativo\":false"));
        HttpResponse<String> instrutoresParaAluno = send(alunoClient, "GET", "/api/instrutores", null);
        assertFalse(instrutoresParaAluno.body().contains(instrutorId));

        assertEquals(204, send(adminClient, "PATCH", "/api/instrutores/" + instrutorId + "/status?ativo=true", "").statusCode());
    }

    @Test
    void administradorExcluiAlunosEInstrutores() throws Exception {
        HttpClient adminClient = criarClienteComSessao();
        String sufixo = UUID.randomUUID().toString();
        String emailAdmin = "admin-exclusao-" + sufixo + "@exemplo.com";
        assertEquals(200, post("/api/administradores", """
                {
                  "nome": "Admin de exclusão",
                  "email": "%s",
                  "telefone": "11988888888",
                  "senha": "senha-forte"
                }
                """.formatted(emailAdmin)).statusCode());
        assertEquals(200, send(adminClient, "POST", "/api/administradores/login", """
                {"email": "%s", "senha": "senha-forte"}
                """.formatted(emailAdmin)).statusCode());

        String emailAluno = "aluno-exclusao-" + sufixo + "@exemplo.com";
        HttpResponse<String> cadastroAluno = post("/api/alunos", """
                {
                  "nome": "Aluno para excluir",
                  "email": "%s",
                  "telefone": "11999999999",
                  "senha": "senha-segura-123"
                }
                """.formatted(emailAluno));
        Matcher alunoMatcher = Pattern.compile("\"id\":\"([^\"]+)\"").matcher(cadastroAluno.body());
        assertTrue(alunoMatcher.find());
        String alunoId = alunoMatcher.group(1);
        assertEquals(403, send(HttpClient.newHttpClient(), "DELETE", "/api/alunos/" + alunoId, "").statusCode());
        assertEquals(204, send(adminClient, "DELETE", "/api/alunos/" + alunoId, "").statusCode());
        assertFalse(send(adminClient, "GET", "/api/alunos", null).body().contains(emailAluno));

        Matcher adminMatcher = Pattern.compile("\"id\":\"([^\"]+)\"")
                .matcher(send(adminClient, "GET", "/api/administradores/sessao", null).body());
        assertTrue(adminMatcher.find());
        String emailInstrutor = "instrutor-exclusao-" + sufixo + "@exemplo.com";
        HttpResponse<String> cadastroInstrutor = send(adminClient, "POST", "/api/instrutores", """
                {
                  "nome": "Instrutor para excluir",
                  "email": "%s",
                  "telefoneWhatsapp": "11977777777",
                  "administradorId": "%s"
                }
                """.formatted(emailInstrutor, adminMatcher.group(1)));
        Matcher instrutorMatcher = Pattern.compile("\"id\":\"([^\"]+)\"").matcher(cadastroInstrutor.body());
        assertTrue(instrutorMatcher.find());
        String instrutorId = instrutorMatcher.group(1);

        assertEquals(403, send(HttpClient.newHttpClient(), "DELETE", "/api/instrutores/" + instrutorId, "").statusCode());
        HttpResponse<String> alunoVinculado = post("/api/alunos", """
                {
                  "nome": "Aluno vinculado",
                  "email": "aluno-vinculado-%s@exemplo.com",
                  "telefone": "11999999998",
                  "administradorId": "%s",
                  "instrutorId": "%s"
                }
                """.formatted(sufixo, adminMatcher.group(1), instrutorId));
        Matcher alunoVinculadoMatcher = Pattern.compile("\"id\":\"([^\"]+)\"").matcher(alunoVinculado.body());
        assertTrue(alunoVinculadoMatcher.find());
        assertEquals(409, send(adminClient, "DELETE", "/api/instrutores/" + instrutorId, "").statusCode());
        assertEquals(204, send(adminClient, "DELETE", "/api/alunos/" + alunoVinculadoMatcher.group(1), "").statusCode());
        assertEquals(204, send(adminClient, "DELETE", "/api/instrutores/" + instrutorId, "").statusCode());
        assertFalse(send(adminClient, "GET", "/api/instrutores", null).body().contains(emailInstrutor));
    }

    private HttpResponse<String> post(String caminho, String json) throws Exception {
        return send(HttpClient.newHttpClient(), "POST", caminho, json);
    }

    private HttpClient criarClienteComSessao() {
        return HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .build();
    }

    private HttpResponse<String> send(HttpClient client, String metodo, String caminho, String json) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + caminho));
        if (json == null) {
            request.GET();
        } else {
            request.header("Content-Type", "application/json")
                    .method(metodo, HttpRequest.BodyPublishers.ofString(json));
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
