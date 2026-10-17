-- Migração V1: Criação da tabela de bolsas de sangue com chave técnica LIFO
CREATE TABLE IF NOT EXISTS bolsas (
    id_seq          BIGSERIAL PRIMARY KEY,
    id              VARCHAR(50) NOT NULL,
    tipo_sanguineo  VARCHAR(10) NOT NULL,
    volume_ml       DOUBLE PRECISION NOT NULL,
    data_coleta     DATE NOT NULL,
    validade        DATE NOT NULL,
    doador          VARCHAR(255) NOT NULL,
    criado_em       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Índice composto cobrindo busca pontual e ordenação LIFO por ocorrência mais recente
CREATE INDEX IF NOT EXISTS idx_bolsas_id_seq ON bolsas(id, id_seq DESC);
