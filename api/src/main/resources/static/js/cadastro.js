const formInstrutor = document.getElementById('form-instrutor');
const divMensagem = document.getElementById('mensagem-retorno');
let administradorSessao = null;

async function verificarSessaoAdministrador() {
    try {
        const resposta = await fetch('/api/administradores/sessao', {
            headers: { Accept: 'application/json' },
            credentials: 'same-origin'
        });
        if (resposta.status === 401) {
            window.location.replace('/login-admin.html');
            return;
        }
        if (!resposta.ok) throw new Error(`A API respondeu com HTTP ${resposta.status}`);
        administradorSessao = await resposta.json();
        sessionStorage.setItem('administradorId', administradorSessao.id);
    } catch (erro) {
        console.error('Não foi possível validar a sessão administrativa:', erro);
        mostrarMensagem('Não foi possível validar seu acesso administrativo. Atualize a página e tente novamente.', 'mensagem-erro');
    }
}

verificarSessaoAdministrador();

formInstrutor.addEventListener('submit', async (evento) => {
    evento.preventDefault();

    if (!administradorSessao) {
        mostrarMensagem('Faça login como administrador antes de cadastrar um instrutor.', 'mensagem-erro');
        const linkLogin = document.createElement('a');
        linkLogin.href = '/login-admin.html';
        linkLogin.textContent = ' Ir para o login';
        divMensagem.querySelector('.mensagem-erro')?.append(linkLogin);
        return;
    }

    const dadosInstrutor = {
        nome: document.getElementById('nome').value.trim(),
        email: document.getElementById('email').value.trim(),
        telefoneWhatsapp: document.getElementById('telefoneWhatsapp').value.trim(),
        endereco: document.getElementById('endereco').value.trim(),
        categoriaCnh: document.getElementById('categoriaCnh').value.trim(),
        descricao: document.getElementById('descricao').value.trim(),
        administradorId: administradorSessao.id
    };

    const botao = formInstrutor.querySelector('button[type="submit"]');
    botao.disabled = true;

    try {
        const resposta = await fetch('/api/instrutores', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Accept: 'application/json'
            },
            credentials: 'same-origin',
            body: JSON.stringify(dadosInstrutor)
        });

        if (resposta.ok) {
            mostrarMensagem('Instrutor registado com sucesso!', 'mensagem-sucesso');
            formInstrutor.reset();
            return;
        }

        let mensagemErro = `Não foi possível cadastrar o instrutor (HTTP ${resposta.status}).`;
        try {
            const erro = await resposta.json();
            mensagemErro = erro.detail || erro.message || mensagemErro;
        } catch (erroLeitura) {
            console.error('Não foi possível ler a resposta de erro do cadastro:', erroLeitura);
        }
        mostrarMensagem(mensagemErro, 'mensagem-erro');
    } catch (erro) {
        console.error('Erro ao cadastrar instrutor:', erro);
        mostrarMensagem('Erro de comunicação com o servidor. Verifique se a API está em execução.', 'mensagem-erro');
    } finally {
        botao.disabled = false;
    }
});

function mostrarMensagem(texto, classe) {
    const mensagem = document.createElement('div');
    mensagem.className = classe;
    mensagem.textContent = texto;
    divMensagem.replaceChildren(mensagem);
}
