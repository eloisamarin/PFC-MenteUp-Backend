-- Etapa 4 (Ordenação e Lacunas) - rodar uma vez no PostgreSQL, depois de subir a aplicação com o código novo.
-- O ddl-auto=update cria a coluna alternativas.ordem e a tabela resposta_atividade_sequencia sozinho,
-- mas NÃO remove a restrição NOT NULL de uma coluna que já existe:
ALTER TABLE resposta_atividade ALTER COLUMN alternativa_id DROP NOT NULL;
