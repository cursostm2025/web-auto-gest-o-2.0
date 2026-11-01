const tabelaAlunos = document.getElementById('tabela-alunos');
const tabelaInstrutores = document.getElementById('tabela-instrutores');
const mensagemAdmin = document.getElementById('admin-message');
const dialogEdicao = document.getElementById('dialog-edicao');
const formularioEdicao = document.getElementById('form-edicao');
const camposEdicao = document.getElementById('campos-edicao');
const mensagemDialogo = document.getElementById('dialog-message');
let alunos = [];
let instrutores = [];
let cadastroEditado = null;

const camposAluno = [
    { key: 'nome', label: 'Nome completo', required: true },
    { key: 'email', label: 'E-mail', type: 'email', required: true },
    { key: 'telefone', label: 'Telefone', required: true },
    { key: 'endereco', label: 'Endereço' },
    { key: 'senha', label: 'Nova senha (opcional)', type: 'password', minLength: 8 }
];

const camposInstrutor = [
    { key: 'nome', label: 'Nome completo', required: true },
    { key: 'email', label: 'E-mail', type: 'email', required: true },
    { key: 'telefoneWhatsapp', label: 'WhatsApp', required: true },
    { key: 'endereco', label: 'Endereço' },
    { key: 'categoriaCnh', label: 'Categoria CNH' },
    { key: 'descricao', label: 'Descrição', multiline: true }
];

async function requisicao(url, opcoes = {}) {
    const resposta = await fetch(url, {
        ...opcoes,
        headers: {
            Accept: 'application/json',
            ...(opcoes.body ? { 'Content-Type': 'application/json' } : {}),
            ...opcoes.headers
        },
        credentials: 'same-origin'
    });

    if (resposta.status === 401) {
        window.location.replace('/login-admin.html');
        throw new Error('Sua sessão administrativa expirou. Entre novamente.');
    }
    if (!resposta.ok) {
        let mensagem = `A API respondeu com HTTP ${resposta.status}.`;
        try {
            const erro = await resposta.json();
            mensagem = erro.message || erro.detail || mensagem;
        } catch (erroLeitura) {
            console.error('Não foi possível ler os detalhes do erro da API:', erroLeitura);
        }
        throw new Error(mensagem);
    }
    if (resposta.status === 204) return null;
    return resposta.json();
}

function mostrarMensagem(texto, sucesso = false) {
    mensagemAdmin.textContent = texto;
    mensagemAdmin.className = `admin-message${sucesso ? ' is-success' : ''}`;
    mensagemAdmin.hidden = !texto;
}

function adicionarCelula(linha, texto, classe) {
    const celula = document.createElement('td');
    if (classe) celula.className = classe;
    celula.textContent = texto || '—';
    linha.append(celula);
    return celula;
}

function criarSeloStatus(ativo) {
    const selo = document.createElement('span');
    selo.className = `status ${ativo ? 'status-active' : 'status-blocked'}`;
    selo.textContent = ativo ? 'Ativo' : 'Bloqueado';
    return selo;
}

function criarBotao(texto, classe, acao) {
    const botao = document.createElement('button');
    botao.type = 'button';
    botao.className = `button ${classe}`;
    botao.textContent = texto;
    botao.addEventListener('click', acao);
    return botao;
}

function renderizarTabela(tipo, registros) {
    const tabela = tipo === 'aluno' ? tabelaAlunos : tabelaInstrutores;
    tabela.replaceChildren();

    if (registros.length === 0) {
        const linha = document.createElement('tr');
        const celula = adicionarCelula(linha, 'Nenhum cadastro encontrado.', 'table-empty');
        celula.colSpan = 5;
        tabela.append(linha);
        return;
    }

    registros.forEach((registro) => {
        const linha = document.createElement('tr');
        adicionarCelula(linha, registro.nome);
        adicionarCelula(linha, registro.email);
        adicionarCelula(linha, tipo === 'aluno' ? registro.telefone : registro.telefoneWhatsapp);
        const status = document.createElement('td');
        status.append(criarSeloStatus(registro.ativo));
        linha.append(status);

        const acoes = document.createElement('td');
        acoes.className = 'row-actions';
        acoes.append(criarBotao('Editar', 'button-secondary', () => abrirEdicao(tipo, registro)));
        acoes.append(criarBotao(
            registro.ativo ? 'Bloquear' : 'Desbloquear',
            registro.ativo ? 'button-secondary' : 'button-primary',
            () => alterarStatus(tipo, registro)
        ));
        acoes.append(criarBotao('Excluir', 'button-danger', () => excluirCadastro(tipo, registro)));
        linha.append(acoes);
        tabela.append(linha);
    });
}

function atualizarResumo() {
    document.getElementById('total-alunos').textContent = String(alunos.length);
    document.getElementById('ativos-alunos').textContent =
        `${alunos.filter((aluno) => aluno.ativo).length} ativos`;
    document.getElementById('total-instrutores').textContent = String(instrutores.length);
    document.getElementById('ativos-instrutores').textContent =
        `${instrutores.filter((instrutor) => instrutor.ativo).length} ativos`;
}

async function carregarCadastros() {
    mostrarMensagem('');
    try {
        [alunos, instrutores] = await Promise.all([
            requisicao('/api/alunos'),
            requisicao('/api/instrutores')
        ]);
        renderizarTabela('aluno', alunos);
        renderizarTabela('instrutor', instrutores);
        atualizarResumo();
    } catch (erro) {
        console.error('Erro ao carregar cadastros para o painel:', erro);
        mostrarMensagem(erro.message || 'Não foi possível carregar os cadastros.');
    }
}

function criarCampo(campo, valor) {
    const grupo = document.createElement('div');
    grupo.className = `edit-field${campo.multiline ? ' edit-field-full' : ''}`;
    const label = document.createElement('label');
    const id = `edit-${campo.key}`;
    label.htmlFor = id;
    label.textContent = campo.label;
    const entrada = document.createElement(campo.multiline ? 'textarea' : 'input');
    entrada.id = id;
    entrada.name = campo.key;
    entrada.dataset.field = campo.key;
    if (!campo.multiline) entrada.type = campo.type || 'text';
    entrada.value = valor ?? '';
    entrada.required = Boolean(campo.required);
    if (campo.minLength) {
        entrada.minLength = campo.minLength;
        entrada.autocomplete = 'new-password';
        entrada.placeholder = 'Deixe em branco para manter a senha atual';
    }
    grupo.append(label, entrada);
    return grupo;
}

function abrirEdicao(tipo, registro) {
    cadastroEditado = { tipo, id: registro.id };
    document.getElementById('dialog-titulo').textContent =
        tipo === 'aluno' ? 'Editar aluno' : 'Editar instrutor';
    camposEdicao.replaceChildren();
    (tipo === 'aluno' ? camposAluno : camposInstrutor)
        .forEach((campo) => camposEdicao.append(criarCampo(campo, registro[campo.key])));
    mensagemDialogo.hidden = true;
    dialogEdicao.showModal();
}

async function alterarStatus(tipo, registro) {
    const recurso = tipo === 'aluno' ? 'alunos' : 'instrutores';
    const proximoStatus = !registro.ativo;
    try {
        await requisicao(`/api/${recurso}/${registro.id}/status?ativo=${proximoStatus}`, {
            method: 'PATCH'
        });
        await carregarCadastros();
        mostrarMensagem(`${tipo === 'aluno' ? 'Aluno' : 'Instrutor'} ${proximoStatus ? 'desbloqueado' : 'bloqueado'} com sucesso.`, true);
    } catch (erro) {
        console.error('Erro ao alterar status do cadastro:', erro);
        mostrarMensagem(erro.message || 'Não foi possível alterar o status.');
    }
}

async function excluirCadastro(tipo, registro) {
    const nomeTipo = tipo === 'aluno' ? 'aluno' : 'instrutor';
    if (!window.confirm(`Tem certeza que deseja excluir o cadastro de ${nomeTipo} "${registro.nome}"?`)) {
        return;
    }

    const recurso = tipo === 'aluno' ? 'alunos' : 'instrutores';
    try {
        await requisicao(`/api/${recurso}/${registro.id}`, { method: 'DELETE' });
        await carregarCadastros();
        mostrarMensagem(`${nomeTipo === 'aluno' ? 'Aluno' : 'Instrutor'} excluído com sucesso.`, true);
    } catch (erro) {
        console.error('Erro ao excluir cadastro:', erro);
        mostrarMensagem(erro.message || 'Não foi possível excluir o cadastro.');
    }
}

formularioEdicao.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    if (!cadastroEditado) return;
    const botaoSalvar = formularioEdicao.querySelector('button[type="submit"]');
    botaoSalvar.disabled = true;
    const payload = Object.fromEntries(
        Array.from(camposEdicao.querySelectorAll('[data-field]'))
            .map((campo) => [campo.dataset.field, campo.value.trim()])
    );
    const recurso = cadastroEditado.tipo === 'aluno' ? 'alunos' : 'instrutores';

    try {
        await requisicao(`/api/${recurso}/${cadastroEditado.id}`, {
            method: 'PUT',
            body: JSON.stringify(payload)
        });
        dialogEdicao.close();
        await carregarCadastros();
        mostrarMensagem('Cadastro atualizado com sucesso.', true);
    } catch (erro) {
        console.error('Erro ao atualizar cadastro:', erro);
        mensagemDialogo.textContent = erro.message || 'Não foi possível salvar as alterações.';
        mensagemDialogo.hidden = false;
    } finally {
        botaoSalvar.disabled = false;
    }
});

document.querySelectorAll('[data-close]').forEach((botao) => {
    botao.addEventListener('click', () => dialogEdicao.close());
});

document.querySelectorAll('[data-reload]').forEach((botao) => {
    botao.addEventListener('click', carregarCadastros);
});

document.getElementById('admin-logout').addEventListener('click', async (evento) => {
    const botao = evento.currentTarget;
    botao.disabled = true;
    try {
        await requisicao('/api/administradores/logout', { method: 'POST' });
        sessionStorage.removeItem('administradorId');
        window.location.replace('/login-admin.html');
    } catch (erro) {
        console.error('Erro ao sair do painel:', erro);
        mostrarMensagem(erro.message || 'Não foi possível encerrar a sessão.');
    } finally {
        botao.disabled = false;
    }
});

async function inicializarPainel() {
    try {
        const administrador = await requisicao('/api/administradores/sessao');
        document.getElementById('admin-name').textContent = administrador.nome || administrador.email;
        await carregarCadastros();
    } catch (erro) {
        console.error('Não foi possível abrir o painel administrativo:', erro);
    }
}

inicializarPainel();
