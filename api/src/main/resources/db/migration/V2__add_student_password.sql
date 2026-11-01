DO $$
BEGIN
    IF to_regclass('aluno') IS NOT NULL THEN
        ALTER TABLE aluno ADD COLUMN IF NOT EXISTS senha VARCHAR(255);
    END IF;
END
$$;
