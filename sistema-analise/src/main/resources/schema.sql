CREATE TABLE IF NOT EXISTS evento_processado_analise (
    evento_id     VARCHAR(64) PRIMARY KEY,
    processado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS analise (
    id UUID PRIMARY KEY,
    cpf VARCHAR(11) NOT NULL,
    solicitacao_id VARCHAR(64) NOT NULL,
    aprovada BOOLEAN NOT NULL,
    criado_em TIMESTAMP NOT NULL
);
