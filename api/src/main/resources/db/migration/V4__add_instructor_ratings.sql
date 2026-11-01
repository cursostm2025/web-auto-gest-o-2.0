CREATE TABLE IF NOT EXISTS avaliacao_instrutor (
    id UUID PRIMARY KEY,
    instrutor_id UUID NOT NULL REFERENCES instrutor(id) ON DELETE CASCADE,
    aluno_id UUID NOT NULL REFERENCES aluno(id) ON DELETE CASCADE,
    nota INTEGER NOT NULL CHECK (nota BETWEEN 1 AND 5),
    CONSTRAINT uk_avaliacao_instrutor_aluno UNIQUE (instrutor_id, aluno_id)
);
