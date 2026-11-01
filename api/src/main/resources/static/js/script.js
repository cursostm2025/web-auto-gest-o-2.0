const listaInstrutores = document.getElementById('lista-instrutores');
const totalInstrutores = document.getElementById('total-instrutores');
const filtroLista = document.getElementById('filtro-lista');
let instrutoresCarregados = [];

function normalizarTexto(valor) {
    return String(valor ?? '')
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .toLocaleLowerCase('pt-BR');
}

function criarElemento(tag, className, texto) {
    const elemento = document.createElement(tag);
    if (className) elemento.className = className;
    if (texto) elemento.textContent = texto;
    return elemento;
}

function obterIniciais(nome) {
    const palavras = String(nome || 'Instrutor')
        .trim()
        .split(/\s+/)
        .filter(Boolean);

    return palavras
        .slice(0, 2)
        .map((palavra) => palavra.charAt(0))
        .join('')
        .toLocaleUpperCase('pt-BR');
}

function criarContato(instrutor, telefone) {
    const link = document.createElement('a');
    link.className = 'card-button card-button-primary';

    if (telefone) {
        const mensagem = `Olá, ${instrutor.nome}! Encontrei seu perfil no Web Autogestão e gostaria de saber mais sobre suas aulas.`;
        link.href = `https://wa.me/${telefone}?text=${encodeURIComponent(mensagem)}`;
        link.target = '_blank';
        link.rel = 'noopener noreferrer';
        link.append(document.createTextNode('Agendar aula'));
    } else if (instrutor.email) {
        link.href = `mailto:${encodeURIComponent(instrutor.email)}`;
        link.append(document.createTextNode('Falar por e-mail'));
    } else {
        link.removeAttribute('href');
        link.setAttribute('aria-disabled', 'true');
        link.classList.add('card-button-secondary');
        link.append(document.createTextNode('Sem contato'));
    }

    return link;
}

function atualizarVisualAvaliacao(estrelas, instrutor, notaTemporaria) {
    const notaPropria = instrutor.notaDoAluno;
    const notaExibida = notaTemporaria ?? notaPropria ?? Math.round(instrutor.avaliacaoMedia || 0);

    estrelas.forEach((estrela, indice) => {
        const nota = indice + 1;
        estrela.textContent = nota <= notaExibida ? '★' : '☆';
        estrela.setAttribute('aria-pressed', String(nota === notaPropria));
    });
}

function criarAvaliacao(instrutor) {
    const avaliacao = criarElemento('div', 'card-rating');
    const estrelas = [];
    const grupo = criarElemento('div', 'card-rating-stars');
    grupo.setAttribute('role', 'group');
    grupo.setAttribute('aria-label', `Sua avaliação para ${instrutor.nome || 'este instrutor'}`);
    const resumo = criarElemento('span', 'card-rating-summary');
    const feedback = criarElemento('span', 'rating-feedback');
    feedback.setAttribute('role', 'status');
    feedback.setAttribute('aria-live', 'polite');

    for (let nota = 1; nota <= 5; nota += 1) {
        const estrela = criarElemento('button', 'rating-star', '☆');
        estrela.type = 'button';
        estrela.setAttribute('aria-label', `Avaliar com ${nota} ${nota === 1 ? 'estrela' : 'estrelas'}`);
        estrela.setAttribute('aria-pressed', 'false');
        estrela.addEventListener('mouseenter', () => atualizarVisualAvaliacao(estrelas, instrutor, nota));
        estrela.addEventListener('focus', () => atualizarVisualAvaliacao(estrelas, instrutor, nota));
        estrela.addEventListener('click', () => salvarAvaliacao(instrutor, nota, estrelas, resumo, feedback));
        estrelas.push(estrela);
        grupo.append(estrela);
    }

    grupo.addEventListener('mouseleave', () => atualizarVisualAvaliacao(estrelas, instrutor));
    grupo.addEventListener('focusout', (evento) => {
        if (!grupo.contains(evento.relatedTarget)) atualizarVisualAvaliacao(estrelas, instrutor);
    });

    avaliacao.append(grupo, resumo, feedback);
    atualizarVisualAvaliacao(estrelas, instrutor);
    atualizarResumoAvaliacao(instrutor, resumo);
    return avaliacao;
}

function atualizarResumoAvaliacao(instrutor, resumo) {
    if (!instrutor.totalAvaliacoes) {
        resumo.textContent = 'Sem avaliações';
        return;
    }

    const media = Number(instrutor.avaliacaoMedia).toLocaleString('pt-BR', {
        minimumFractionDigits: 1,
        maximumFractionDigits: 1
    });
    const total = instrutor.totalAvaliacoes;
    resumo.textContent = `${media} · ${total} ${total === 1 ? 'avaliação' : 'avaliações'}`;
}

async function salvarAvaliacao(instrutor, nota, estrelas, resumo, feedback) {
    estrelas.forEach((estrela) => { estrela.disabled = true; });
    feedback.textContent = 'Salvando avaliação...';

    try {
        const resposta = await fetch(`/api/instrutores/${encodeURIComponent(instrutor.id)}/avaliacoes`, {
            method: 'POST',
            headers: {
                Accept: 'application/json',
                'Content-Type': 'application/json'
            },
            credentials: 'same-origin',
            body: JSON.stringify({ nota })
        });

        if (resposta.status === 401) {
            atualizarEstadoAluno(null);
            return;
        }

        const resultado = await resposta.json().catch(() => null);
        if (!resposta.ok) {
            throw new Error(resultado?.message || `A API respondeu com HTTP ${resposta.status}`);
        }

        instrutor.avaliacaoMedia = resultado.media;
        instrutor.totalAvaliacoes = resultado.total;
        instrutor.notaDoAluno = resultado.notaDoAluno;
        atualizarVisualAvaliacao(estrelas, instrutor);
        atualizarResumoAvaliacao(instrutor, resumo);
        feedback.textContent = 'Sua avaliação foi salva.';
    } catch (erro) {
        console.error('Erro ao salvar a avaliação do instrutor:', erro);
        feedback.textContent = 'Não foi possível salvar sua avaliação. Tente novamente.';
    } finally {
        estrelas.forEach((estrela) => { estrela.disabled = false; });
    }
}

function criarInstrutorCard(instrutor, indice) {
    const card = criarElemento('article', 'instructor-card');
    const capa = criarElemento('div', `card-cover${indice % 6 === 0 ? '' : ` card-cover-tone-${indice % 6}`}`);
    const selo = criarElemento('span', 'profile-badge', 'Perfil de instrutor');
    const avatar = criarElemento('span', 'card-avatar', obterIniciais(instrutor.nome));
    avatar.setAttribute('aria-hidden', 'true');
    capa.append(selo, avatar);

    const conteudo = criarElemento('div', 'card-content');
    const categoria = criarElemento('span', 'card-category', instrutor.categoriaCnh || 'Categoria não informada');
    const nome = criarElemento('h3', 'card-name', instrutor.nome || 'Instrutor');
    const localizacao = criarElemento('p', 'card-location');
    const iconeLocal = document.createElement('span');
    iconeLocal.setAttribute('aria-hidden', 'true');
    iconeLocal.textContent = '⌖';
    const endereco = document.createElement('span');
    endereco.textContent = instrutor.endereco || 'Localização a combinar';
    localizacao.append(iconeLocal, endereco);

    const descricao = criarElemento(
        'p',
        'card-description',
        instrutor.descricao || 'Entre em contato para conhecer o perfil e combinar os detalhes das aulas.'
    );

    const metadados = criarElemento('div', 'card-meta');
    const avaliacao = criarElemento('div', 'card-meta-item');
    avaliacao.append(
        criarElemento('span', 'card-meta-label', 'Avaliação'),
        criarAvaliacao(instrutor)
    );

    const valor = criarElemento('div', 'card-meta-item');
    valor.append(
        criarElemento('span', 'card-meta-label', 'Valor por aula'),
        criarElemento('span', 'card-meta-value', 'Consulte o instrutor')
    );
    metadados.append(avaliacao, valor);

    const acoes = criarElemento('div', 'card-actions');
    const telefone = String(instrutor.telefoneWhatsapp || '').replace(/\D/g, '');
    acoes.append(criarContato(instrutor, telefone));

    if (instrutor.email) {
        const email = document.createElement('a');
        email.className = 'card-button card-button-secondary';
        email.href = `mailto:${encodeURIComponent(instrutor.email)}`;
        email.append(document.createTextNode('Enviar e-mail'));
        acoes.append(email);
    }

    conteudo.append(categoria, nome, localizacao, descricao, metadados, acoes);
    card.append(capa, conteudo);
    card.dataset.search = normalizarTexto([
        instrutor.nome,
        instrutor.categoriaCnh,
        instrutor.endereco,
        instrutor.descricao
    ].join(' '));

    return card;
}

function aplicarFiltro(termo) {
    const busca = normalizarTexto(termo.trim());
    const cards = listaInstrutores.querySelectorAll('.instructor-card');
    let visiveis = 0;

    cards.forEach((card) => {
        const corresponde = card.dataset.search.includes(busca);
        card.hidden = !corresponde;
        if (corresponde) visiveis += 1;
    });

    totalInstrutores.textContent = busca
        ? `${visiveis} de ${instrutoresCarregados.length} instrutor${instrutoresCarregados.length === 1 ? '' : 'es'}`
        : `${instrutoresCarregados.length} instrutor${instrutoresCarregados.length === 1 ? '' : 'es'} cadastrado${instrutoresCarregados.length === 1 ? '' : 's'}`;

    let mensagemVazia = listaInstrutores.querySelector('.filter-empty');
    if (visiveis === 0 && cards.length > 0) {
        if (!mensagemVazia) {
            mensagemVazia = criarElemento('p', 'listing-message filter-empty', 'Nenhum instrutor corresponde à busca. Tente outro nome, categoria ou cidade.');
            listaInstrutores.append(mensagemVazia);
        }
        mensagemVazia.hidden = false;
    } else if (mensagemVazia) {
        mensagemVazia.hidden = true;
    }
}

function mostrarErro() {
    listaInstrutores.replaceChildren();
    const mensagem = criarElemento(
        'div',
        'listing-message listing-message-error',
        'Não foi possível carregar os instrutores. Verifique a conexão e tente novamente.'
    );
    const botaoTentarNovamente = criarElemento('button', 'retry-button', 'Tentar novamente');
    botaoTentarNovamente.type = 'button';
    botaoTentarNovamente.addEventListener('click', carregarInstrutores);
    mensagem.append(botaoTentarNovamente);
    listaInstrutores.append(mensagem);
    listaInstrutores.setAttribute('aria-busy', 'false');
    totalInstrutores.textContent = '';
}

async function carregarInstrutores() {
    if (!alunoAutenticado) return;
    listaInstrutores.setAttribute('aria-busy', 'true');
    listaInstrutores.replaceChildren(
        criarElemento('p', 'listing-message', 'Buscando instrutores...')
    );

    try {
        const resposta = await fetch('/api/instrutores', {
            headers: { Accept: 'application/json' },
            credentials: 'same-origin'
        });

        if (resposta.status === 401) {
            atualizarEstadoAluno(null);
            return;
        }
        if (!resposta.ok) {
            throw new Error(`A API respondeu com HTTP ${resposta.status}`);
        }

        const instrutores = await resposta.json();
        if (!Array.isArray(instrutores)) {
            throw new Error('A resposta da API não contém uma lista de instrutores.');
        }

        instrutoresCarregados = instrutores.filter((instrutor) => instrutor && typeof instrutor === 'object');
        listaInstrutores.replaceChildren();

        if (instrutoresCarregados.length === 0) {
            listaInstrutores.append(
                criarElemento('p', 'listing-message', 'Ainda não há instrutores cadastrados. Volte em breve para conferir novos perfis.')
            );
            totalInstrutores.textContent = '0 instrutores cadastrados';
            return;
        }

        const cards = instrutoresCarregados.map(criarInstrutorCard);
        listaInstrutores.append(...cards);
        aplicarFiltro(filtroLista.value);
    } catch (erro) {
        console.error('Erro ao carregar instrutores:', erro);
        mostrarErro();
    } finally {
        listaInstrutores.setAttribute('aria-busy', 'false');
    }
}

function configurarCarrossel() {
    const slides = Array.from(document.querySelectorAll('[data-slide]'));
    const pontos = Array.from(document.querySelectorAll('.carousel-dot'));
    const botaoAnterior = document.querySelector('.carousel-previous');
    const botaoProximo = document.querySelector('.carousel-next');
    const carrossel = document.querySelector('.hero');
    const movimentoReduzido = window.matchMedia('(prefers-reduced-motion: reduce)');
    let slideAtual = 0;
    let temporizador;

    function exibirSlide(indice) {
        slideAtual = (indice + slides.length) % slides.length;
        slides.forEach((slide, posicao) => {
            const ativo = posicao === slideAtual;
            slide.classList.toggle('is-active', ativo);
            slide.setAttribute('aria-hidden', String(!ativo));
        });
        pontos.forEach((ponto, posicao) => {
            const ativo = posicao === slideAtual;
            ponto.classList.toggle('is-active', ativo);
            if (ativo) {
                ponto.setAttribute('aria-current', 'true');
            } else {
                ponto.removeAttribute('aria-current');
            }
        });
    }

    function iniciarRotacao() {
        window.clearInterval(temporizador);
        if (!movimentoReduzido.matches && !document.hidden) {
            temporizador = window.setInterval(() => exibirSlide(slideAtual + 1), 6000);
        }
    }

    botaoAnterior.addEventListener('click', () => {
        exibirSlide(slideAtual - 1);
        iniciarRotacao();
    });

    botaoProximo.addEventListener('click', () => {
        exibirSlide(slideAtual + 1);
        iniciarRotacao();
    });

    pontos.forEach((ponto, indice) => {
        ponto.addEventListener('click', () => {
            exibirSlide(indice);
            iniciarRotacao();
        });
    });

    carrossel.addEventListener('mouseenter', () => window.clearInterval(temporizador));
    carrossel.addEventListener('mouseleave', iniciarRotacao);
    carrossel.addEventListener('focusin', () => window.clearInterval(temporizador));
    carrossel.addEventListener('focusout', iniciarRotacao);
    document.addEventListener('visibilitychange', iniciarRotacao);
    movimentoReduzido.addEventListener('change', iniciarRotacao);
    iniciarRotacao();
}

function configurarMenuConta() {
    const botao = document.getElementById('account-toggle');
    const menu = document.getElementById('account-menu');
    const botaoSair = document.getElementById('account-logout');

    function fecharMenu() {
        botao.setAttribute('aria-expanded', 'false');
        menu.hidden = true;
    }

    botao.addEventListener('click', () => {
        const aberto = botao.getAttribute('aria-expanded') === 'true';
        botao.setAttribute('aria-expanded', String(!aberto));
        menu.hidden = aberto;
    });

    document.addEventListener('click', (evento) => {
        if (!evento.target.closest('.account')) fecharMenu();
    });

    document.addEventListener('keydown', (evento) => {
        if (evento.key === 'Escape') {
            fecharMenu();
            botao.focus();
        }
    });

    menu.addEventListener('click', (evento) => {
        if (evento.target.closest('a')) fecharMenu();
    });

    botaoSair.addEventListener('click', async () => {
        fecharMenu();
        botaoSair.disabled = true;
        try {
            const resposta = await fetch('/api/alunos/logout', {
                method: 'POST',
                credentials: 'same-origin'
            });
            if (!resposta.ok) throw new Error(`A API respondeu com HTTP ${resposta.status}`);
            atualizarEstadoAluno(null);
            window.location.hash = 'inicio';
        } catch (erro) {
            console.error('Erro ao encerrar a sessão do aluno:', erro);
            document.getElementById('mensagem-acesso-instrutores').textContent =
                'Não foi possível sair da conta. Tente novamente.';
            document.getElementById('mensagem-acesso-instrutores').hidden = false;
        } finally {
            botaoSair.disabled = false;
        }
    });
}

let alunoAutenticado = false;

function atualizarEstadoAluno(aluno) {
    alunoAutenticado = Boolean(aluno);
    const visitante = !alunoAutenticado;
    const acessoInstrutores = document.getElementById('acesso-instrutores');
    const secaoInstrutores = document.getElementById('instrutores');
    const mensagemAcesso = document.getElementById('mensagem-acesso-instrutores');

    acessoInstrutores.hidden = alunoAutenticado;
    secaoInstrutores.hidden = visitante;
    document.getElementById('form-busca').hidden = visitante;
    document.getElementById('account-login-link').hidden = alunoAutenticado;
    document.getElementById('account-signup-link').hidden = alunoAutenticado;
    document.getElementById('account-instructors-link').hidden = visitante;
    document.getElementById('account-logout').hidden = visitante;
    document.getElementById('about-signup-link').hidden = alunoAutenticado;
    document.getElementById('account-greeting').textContent =
        alunoAutenticado ? 'Bem-vindo(a),' : 'Acesse sua conta';
    document.getElementById('account-name').textContent = alunoAutenticado ? aluno.nome : 'Visitante';
    document.getElementById('account-avatar').textContent =
        alunoAutenticado ? obterIniciais(aluno.nome) : 'A';
    document.getElementById('account-menu-heading').textContent =
        alunoAutenticado ? 'Conta do aluno' : 'Sua jornada começa aqui';

    document.querySelectorAll('[data-protected-link]').forEach((link) => {
        link.href = alunoAutenticado ? '#instrutores' : '/login-aluno.html';
    });
    mensagemAcesso.hidden = true;

    if (alunoAutenticado) {
        carregarInstrutores();
    } else {
        instrutoresCarregados = [];
        listaInstrutores.replaceChildren();
        totalInstrutores.textContent = '';
    }
}

async function inicializarPagina() {
    configurarCarrossel();
    configurarMenuConta();

    try {
        const resposta = await fetch('/api/alunos/sessao', {
            headers: { Accept: 'application/json' },
            credentials: 'same-origin'
        });
        if (resposta.status === 401) {
            atualizarEstadoAluno(null);
            return;
        }
        if (!resposta.ok) throw new Error(`A API respondeu com HTTP ${resposta.status}`);
        atualizarEstadoAluno(await resposta.json());
    } catch (erro) {
        console.error('Não foi possível verificar a sessão do aluno:', erro);
        atualizarEstadoAluno(null);
        const mensagem = document.getElementById('mensagem-acesso-instrutores');
        mensagem.textContent = 'Não foi possível verificar sua sessão. Atualize a página ou entre novamente.';
        mensagem.hidden = false;
    }
}

document.getElementById('form-busca').addEventListener('submit', (evento) => {
    evento.preventDefault();
    if (!alunoAutenticado) {
        window.location.assign('/login-aluno.html');
        return;
    }
    const busca = document.getElementById('busca-instrutor').value.trim();
    filtroLista.value = busca;
    aplicarFiltro(busca);
    document.getElementById('instrutores').scrollIntoView({ behavior: 'smooth' });
});

filtroLista.addEventListener('input', () => {
    if (alunoAutenticado) aplicarFiltro(filtroLista.value);
});

inicializarPagina();
