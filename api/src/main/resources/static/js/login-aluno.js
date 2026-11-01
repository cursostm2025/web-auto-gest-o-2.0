const formLoginAluno = document.getElementById('form-login-aluno');
const mensagemLoginAluno = document.getElementById('mensagem-login-aluno');
const parametrosPagina = new URLSearchParams(window.location.search);
const mensagemCadastroRealizado = document.getElementById('mensagem-cadastro-realizado');

if (parametrosPagina.get('cadastrado') === '1') {
    mensagemCadastroRealizado.hidden = false;
    const emailCadastrado = parametrosPagina.get('email');
    if (emailCadastrado) formLoginAluno.elements.email.value = emailCadastrado;
}

formLoginAluno.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const botao = formLoginAluno.querySelector('button[type="submit"]');
    botao.disabled = true;
    mostrarMensagemLoginAluno('', '');

    try {
        const resposta = await fetch('/api/alunos/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify({
                email: formLoginAluno.elements.email.value.trim(),
                senha: formLoginAluno.elements.senha.value
            })
        });

        if (!resposta.ok) {
            let detalhe = 'Não foi possível entrar. Confira seu e-mail e sua senha.';
            try {
                const erro = await resposta.json();
                detalhe = erro.detail || erro.message || detalhe;
            } catch (erroLeitura) {
                console.error('Não foi possível ler a resposta de erro do login do aluno:', erroLeitura);
            }
            mostrarMensagemLoginAluno(detalhe, 'error');
            return;
        }

        window.location.assign('/#instrutores');
    } catch (erro) {
        console.error('Erro ao entrar como aluno:', erro);
        mostrarMensagemLoginAluno('Não foi possível conectar ao servidor. Tente novamente.', 'error');
    } finally {
        botao.disabled = false;
    }
});

function mostrarMensagemLoginAluno(texto, tipo) {
    mensagemLoginAluno.textContent = texto;
    mensagemLoginAluno.className = `form-message${texto ? ` is-visible is-${tipo}` : ''}`;
}
