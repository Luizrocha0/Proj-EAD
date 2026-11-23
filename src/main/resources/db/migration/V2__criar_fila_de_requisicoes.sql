CREATE TABLE requisicoes (
    ordem              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id                 VARCHAR(50) NOT NULL UNIQUE,
    hospital_id        VARCHAR(50) NOT NULL,
    tipo_sanguineo     VARCHAR(3) NOT NULL,
    quantidade_bolsas  INTEGER NOT NULL CHECK (quantidade_bolsas > 0),
    prioridade         VARCHAR(10) NOT NULL CHECK (prioridade IN ('NORMAL', 'URGENTE', 'EMERGENCIA')),
    data_hora          TIMESTAMP NOT NULL
);
