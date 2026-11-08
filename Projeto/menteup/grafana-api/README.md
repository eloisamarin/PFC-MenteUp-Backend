# Dashboard do MenteUp no Grafana

Indicadores de uso e de aprendizado do MenteUp. O Grafana **lê os dados pela API REST
do back-end** (`GET /dashboard/**`), com o plugin **Infinity**. Ele não acessa o banco.

```
Grafana (Infinity) --HTTP + X-Dashboard-Token--> back-end /dashboard/** --> PostgreSQL
```

## Como subir

1. **Token**: gere um valor aleatório (no Git Bash):
   `openssl rand -base64 32`
2. **Back-end**: no IntelliJ, em *Run > Edit Configurations > Environment variables*,
   acrescente `MENTEUP_DASHBOARD_TOKEN=<o token>` e rode o back-end.
3. **Grafana**: copie `.env.exemplo` para `.env`, preencha a senha do admin e **o mesmo token**.
4. Nesta pasta: `docker compose up -d` (com o Docker Desktop aberto).
5. Acesse http://localhost:3001 (usuário `admin`, senha do `.env`).

Para parar: `docker compose down`.

## Endpoints usados

| Endpoint | Painel |
|---|---|
| `GET /dashboard/resumo` | Os 8 Big Numbers |
| `GET /dashboard/atividades-concluidas-por-dia?de=&ate=` | Atividades concluídas por dia |
| `GET /dashboard/acerto-por-turma` | Acerto por turma (%) |
| `GET /dashboard/desafios-por-dia?de=&ate=` | Desafios: acertos x erros por dia |
| `GET /dashboard/alunos-por-turma` | Alunos por turma |
| `GET /dashboard/atividades-por-template` | Atividades por template |
| `GET /dashboard/atividades-por-status` | Status das atividades |
| `GET /dashboard/avatares` | Avatares escolhidos |
| `GET /dashboard/acoes-por-dia?de=&ate=` | Ações no sistema por dia |

`de` e `ate` são opcionais, em milissegundos (o Grafana manda `${__from}` e `${__to}`);
sem eles, a API devolve os últimos 30 dias, um ponto por dia.

## Segurança

- A API `/dashboard/**` só responde ao **administrador logado** (JWT) ou a quem manda o
  **token da dashboard** no cabeçalho `X-Dashboard-Token` (o Grafana).
- O token só vale para `GET /dashboard/**`: não abre nenhuma outra rota do MenteUp.
- As respostas têm só números e categorias: nenhum nome de aluno, e-mail ou senha.
- O Grafana só aceita o login do `admin`, sem cadastro, sem acesso anônimo,
  sem link público, e só abre neste computador (`127.0.0.1`).
- O `.env` (senha e token) está no `.gitignore`.
