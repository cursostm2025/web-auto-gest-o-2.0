DO $$
BEGIN
    IF to_regclass('aluno') IS NOT NULL THEN
        ALTER TABLE aluno ALTER COLUMN administrador_id DROP NOT NULL;
        ALTER TABLE aluno ALTER COLUMN instrutor_id DROP NOT NULL;
    END IF;
END
$$;
