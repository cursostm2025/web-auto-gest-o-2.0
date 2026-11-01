const formLoginAdmin = document.getElementById('form-login-admin');
const mensagemLogin = document.getElementById('mensagem-login');

formLoginAdmin.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const botao = formLoginAdmin.querySelector('button[type="submit"]');
    botao.disabled = true;
    mostrarMensagem(mensagemLogin, '', '');

    try {
        const resposta = await fetch('/api/administradores/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify({
                email: formLoginAdmin.elements.email.value.trim(),
                senha: formLoginAdmin.elements.senha.value
            })
        });

        if (resposta.ok) {
            const administrador = await resposta.json();
            if (!administrador.id) {
                throw new Error('A resposta do login não contém o ID do administrador.');
            }
            sessionStorage.setItem('administradorId', administrador.id);
            window.location.assign('/admin-painel.html');
            return;
        }

        let mensagemErro = resposta.status === 401
            ? 'E-mail ou senha inválidos.'
            : 'Não foi possível entrar. Verifique os dados e tente novamente.';
        try {
            const erro = await resposta.json();
            mensagemErro = erro.message || erro.detail || mensagemErro;
        } catch (erroLeitura) {
            console.error('Não foi possível ler a resposta de erro do login administrativo:', erroLeitura);
        }
        mostrarMensagem(
            mensagemLogin,
            mensagemErro,
            'error'
        );
    } catch (erro) {
        console.error('Erro ao realizar login:', erro);
        mostrarMensagem(mensagemLogin, 'Não foi possível conectar ao servidor. Tente novamente.', 'error');
    } finally {
        botao.disabled = false;
    }
});

function mostrarMensagem(elemento, texto, tipo) {
    elemento.textContent = texto;
    elemento.className = `form-message${texto ? ` is-visible is-${tipo}` : ''}`;
}
