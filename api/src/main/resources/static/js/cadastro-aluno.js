const formCadastroAluno = document.getElementById('form-cadastro-aluno');
const mensagemCadastro = document.getElementById('mensagem-cadastro');

formCadastroAluno.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const botao = formCadastroAluno.querySelector('button[type="submit"]');
    botao.disabled = true;
    mostrarMensagemCadastro('', '');

    const dadosAluno = {
        nome: formCadastroAluno.elements.nome.value.trim(),
        email: formCadastroAluno.elements.email.value.trim(),
        telefone: formCadastroAluno.elements.telefone.value.trim(),
        endereco: formCadastroAluno.elements.endereco.value.trim(),
        senha: formCadastroAluno.elements.senha.value
    };

    if (dadosAluno.senha !== formCadastroAluno.elements.confirmarSenha.value) {
        mostrarMensagemCadastro('As senhas informadas não coincidem.', 'error');
        botao.disabled = false;
        formCadastroAluno.elements.confirmarSenha.focus();
        return;
    }

    try {
        const resposta = await fetch('/api/alunos', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
            body: JSON.stringify(dadosAluno)
        });

        if (!resposta.ok) {
            let detalhe = 'Não foi possível concluir o cadastro. Confira os dados e tente novamente.';
            try {
                const erro = await resposta.json();
                detalhe = erro.detail || erro.message || detalhe;
            } catch (erroLeitura) {
                console.error('Não foi possível ler a resposta de erro do cadastro:', erroLeitura);
            }
            mostrarMensagemCadastro(detalhe, 'error');
            return;
        }

        window.location.assign(`/login-aluno.html?cadastrado=1&email=${encodeURIComponent(dadosAluno.email)}`);
    } catch (erro) {
        console.error('Erro ao cadastrar aluno:', erro);
        mostrarMensagemCadastro('Não foi possível conectar ao servidor. Tente novamente.', 'error');
    } finally {
        botao.disabled = false;
    }
});

function mostrarMensagemCadastro(texto, tipo) {
    mensagemCadastro.textContent = texto;
    mensagemCadastro.className = `form-message form-grid-message${texto ? ` is-visible is-${tipo}` : ''}`;
}
