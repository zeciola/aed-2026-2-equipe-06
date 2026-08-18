CREATE TABLE IF NOT EXISTS emprestimo (
    id UUID PRIMARY KEY,
    cpf VARCHAR(11) NOT NULL,
    valor_parcela DECIMAL(10, 2) NOT NULL,
    valor_total DECIMAL(10, 2) NOT NULL,
    codigo_verba INT NOT NULL,
    data_emprestimo TIMESTAMP NOT NULL
);
