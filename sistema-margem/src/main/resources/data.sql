DELETE FROM margem where tipo = 'CREDITO' and codigo_verba = 1 and cpf in ('11111111111', '22222222222', '33333333333', '44444444444');


INSERT INTO margem (id, cpf, valor, codigo_verba, criado_em, tipo) VALUES
    (gen_random_uuid(), '11111111111', 3000.00, 1, CURRENT_TIMESTAMP, 'CREDITO'),
    (gen_random_uuid(), '22222222222', 5000.00, 1, CURRENT_TIMESTAMP, 'CREDITO'),
    (gen_random_uuid(), '33333333333', 500.00, 1, CURRENT_TIMESTAMP, 'CREDITO'),
    (gen_random_uuid(), '44444444444', 2000.00, 1, CURRENT_TIMESTAMP, 'CREDITO')
;
