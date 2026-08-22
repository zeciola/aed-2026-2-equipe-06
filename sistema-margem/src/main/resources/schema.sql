CREATE TABLE IF NOT EXISTS evento_processado (
    evento_id     VARCHAR(64) PRIMARY KEY,
    processado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS margem (
    id UUID PRIMARY KEY,
    cpf VARCHAR(11) NOT NULL,
    valor DECIMAL(10, 2) NOT NULL,
    codigo_verba INT NOT NULL,
    criado_em TIMESTAMP NOT NULL,
    tipo VARCHAR(20) NOT NULL
);


DELETE FROM margem where tipo = 'CREDITO' and codigo_verba = 1 and cpf in ('11111111111', '22222222222', '33333333333', '44444444444');


INSERT INTO margem (id, cpf, valor, codigo_verba, criado_em, tipo) VALUES
    (gen_random_uuid(), '11111111111', 3000.00, 1, CURRENT_TIMESTAMP, 'CREDITO'),
    (gen_random_uuid(), '22222222222', 5000.00, 1, CURRENT_TIMESTAMP, 'CREDITO'),
    (gen_random_uuid(), '33333333333', 500.00, 1, CURRENT_TIMESTAMP, 'CREDITO'),
    (gen_random_uuid(), '44444444444', 2000.00, 1, CURRENT_TIMESTAMP, 'CREDITO')
;
