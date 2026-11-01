DO $$
BEGIN
    IF to_regclass('instrutor') IS NOT NULL THEN
        ALTER TABLE instrutor ADD COLUMN IF NOT EXISTS ativo BOOLEAN NOT NULL DEFAULT TRUE;
    END IF;
END
$$;
