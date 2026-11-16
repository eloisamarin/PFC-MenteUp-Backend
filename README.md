# 🎓 MenteUp — Back-end

## Plataforma Web de Gamificação Educacional

![Versão](https://img.shields.io/badge/vers%C3%A3o-1.0.0-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-336791)
![Status](https://img.shields.io/badge/status-vers%C3%A3o%20final%20de%20lan%C3%A7amento-success)

O **MenteUp** é uma plataforma web de gamificação educacional. Ela incentiva o engajamento e a participação dos estudantes com atividades interativas, desafios ao vivo, pontuação, ranking, conquistas e avatares. Os professores acompanham o desempenho das turmas. O administrador cuida dos usuários, das turmas, dos logs de auditoria e dos indicadores da plataforma.

Este repositório tem a **API REST (back-end)** do MenteUp. O front-end fica em um repositório separado ([PFC-MenteUp-Frontend](https://github.com/eloisamarin/PFC-MenteUp-Frontend)).

| Ambiente | Endereço |
|---|---|
| Aplicação (front-end) | https://menteup.vercel.app |
| API (back-end) | https://pfc-menteup-backend.onrender.com |

> O plano gratuito do Render coloca o serviço para dormir quando ninguém o usa. O primeiro acesso depois de um tempo parado pode levar cerca de 1 minuto.

---

## 📑 Sumário

1. [Objetivo](#-objetivo)
2. [Funcionalidades](#-funcionalidades)
3. [Arquitetura](#️-arquitetura)
4. [Tecnologias](#-tecnologias)
5. [Estrutura do projeto](#-estrutura-do-projeto)
6. [Como executar localmente](#️-como-executar-localmente)
7. [Variáveis de ambiente](#-variáveis-de-ambiente)
8. [Execução com Docker](#-execução-com-docker)
9. [Implantação (deploy)](#️-implantação-deploy)
10. [Documentação da API](#-documentação-da-api)
11. [Segurança](#-segurança)
12. [Conformidade com a LGPD](#️-conformidade-com-a-lgpd)
13. [Dashboard e Grafana](#-dashboard-e-grafana)
14. [Testes e integração contínua](#-testes-e-integração-contínua)
15. [Instruções e evidências de execução](#-instruções-e-evidências-de-execução)
16. [Equipe](#-equipe)

---

## 🎯 Objetivo

Desenvolver uma plataforma educacional baseada em gamificação que incentive o engajamento dos estudantes e ajude os professores a acompanhar o processo de aprendizagem.

### Objetivos específicos

- Promover a resolução de atividades e desafios.
- Permitir que os professores criem e disponibilizem questionários.
- Atribuir pontos aos estudantes conforme o desempenho nas atividades.
- Estabelecer um sistema de ranqueamento.
- Converter os pontos acumulados em conquistas.

---

## 🚀 Funcionalidades

### 👨‍🎓 Aluno

- Cadastro público (acesso imediato) e login com **verificação em duas etapas (2FA)** por e-mail, com opção de reenviar o código.
- Atividades em cinco formatos: **Quiz**, **Verdadeiro ou Falso**, **Associação**, **Ordenação** e **Lacunas**.
- Progresso salvo durante a atividade e resultado com a pontuação no final.
- **Desafios ao vivo**, com perguntas cronometradas e ranking da partida.
- **Ranking** geral e por turma.
- **Conquistas** nas categorias Nível, Atividade, Ranking e Intensivão (dias seguidos de acesso), com raridades Bronze, Prata, Ouro e Especial.
- Escolha de **avatar** (cachorro, gato, raposa, panda, coala, tigre, leão, sapo, macaco ou coelho).
- Pedido de entrada em turmas e acompanhamento das solicitações.
- **Painel de indicadores** com os próprios dados, filtrado por período e turma.
- Meu perfil: troca de senha, download dos próprios dados e exclusão da conta.

### 👨‍🏫 Professor

- Criação, edição e exclusão de atividades e questionários **somente nas turmas dele**.
- Criação e condução de desafios ao vivo nas turmas dele.
- Inclusão de alunos na turma (vê o e-mail do aluno para identificá-lo) e aprovação ou recusa de solicitações.
- Desempenho da turma e de cada aluno, com comentários nos resultados.
- **Painel de indicadores** das turmas dele e dos alunos delas, com filtros por período, turma e aluno.

### 🛡️ Administrador

- Gestão de usuários: criação (o usuário recebe um convite por e-mail para o primeiro acesso), edição, aprovação ou rejeição dos cadastros de professor e exclusão. Um administrador não edita outro administrador.
- Gestão de turmas e escolha do **professor responsável** por cada uma. As permissões das atividades e dos desafios seguem esse vínculo.
- **Arquivamento de turmas.** Uma turma com alunos ou com histórico não pode ser excluída: a API explica o motivo e oferece o arquivamento.
- Logs do sistema (acessos e auditoria das ações), com o IP real do cliente.
- **Painel de indicadores** de toda a plataforma e acesso ao **dashboard no Grafana**.

---

## 🏗️ Arquitetura

O back-end segue a arquitetura em camadas: **Controller → Service → Repository**. As regras de negócio e de permissão ficam nos *services*. Os erros de negócio usam uma exceção única (`ApiException`), tratada no `GlobalExceptionHandler`, que devolve sempre o mesmo formato: `{ "codigo", "mensagem" }`.

```text
┌─────────────────────────────────────────────────────────────────────────────────┐
│                                      GitHub                                     │
│   PFC-MenteUp-Frontend                PFC-MenteUp-Backend                       │
│                                       GitHub Actions (CI):                      │
│                                       testes + Checkstyle no PR                 │
└──────────┬─────────────────────────────────────────┬────────────────────────────┘
           │ push na main                            │ push na main
           │ (deploy automático)                     │ (deploy automático)
           ▼                                         │
┌──────────────────────┐                             │     ┌────────────────────┐
│  Front-end (Vercel)  │                             │     │   Grafana Cloud    │
│  React + TypeScript  │                             │     │  plugin Infinity   │
└──────────┬───────────┘                             │     └──────────┬─────────┘
           │ HTTPS / JSON + JWT                      │                │ HTTPS +
           │                                         │                │ X-Dashboard-Token
           ▼                                         ▼                ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         Back-end (Render · Docker)                              │
│                 Java 21 + Spring Boot 4 + Spring Security                       │
│            Controller → Service → Repository   ·   Filtros JWT                  │
└──────────┬──────────────────────────────────────────────────────────┬───────────┘
           │ JPA / Hibernate                                          │ API HTTPS
           ▼                                                          ▼
┌──────────────────────┐                                   ┌────────────────────┐
│ PostgreSQL 18 (Neon) │                                   │ Brevo (e-mails)    │
└──────────────────────┘                                   └────────────────────┘
```

O código dos dois repositórios fica no **GitHub**. Cada Pull Request para a `main` do back-end passa pela pipeline do **GitHub Actions** (testes unitários, testes de integração e Checkstyle). Depois do merge, a Vercel publica o front-end e o Render gera a imagem Docker e publica o back-end automaticamente.

As atividades usam o padrão **Strategy**: cada formato tem o seu *template* (`QuizTemplate`, `VerdadeiroFalsoTemplate`, `AssociacaoTemplate`, `OrdenacaoTemplate` e `LacunasTemplate`), escolhido pelo `TemplateResolver`. As conquistas são avaliadas por **eventos** do Spring (`AvaliarConquistasEvento` e `ConquistaListener`).

---

## 💻 Tecnologias

### Back-end

| Tecnologia | Uso |
|---|---|
| Java 21 | Linguagem principal |
| Spring Boot 4.1.1 | Base da aplicação e da API REST (Spring Web MVC) |
| Spring Data JPA / Hibernate | Persistência dos dados |
| Spring Security + JJWT | Autenticação por JWT, 2FA e autorização por perfil |
| Bean Validation | Validação dos dados de entrada |
| springdoc-openapi 3 | Documentação da API (Swagger UI) |
| Brevo (API HTTPS) | Envio dos e-mails (2FA, convites e redefinição de senha) |
| Firebase Admin | Notificações no celular (opcional) |
| Lombok | Redução de código repetitivo |
| Maven (wrapper) | Build e dependências |
| JUnit 5, H2 e Checkstyle | Testes e padronização do código |

### Banco de dados

- **PostgreSQL 18** (em produção, no Neon, região São Paulo).
- **H2** em memória, só nos testes.

### Infraestrutura

- **Docker** (*build* multi-stage).
- **Render** (back-end), **Vercel** (front-end), **Neon** (banco), **Brevo** (e-mail) e **Grafana Cloud** (dashboard).
- **GitHub Actions** (integração contínua).

### Ferramentas

IntelliJ IDEA, Visual Studio Code, Insomnia, Figma, Git e GitHub.

---

## 📂 Estrutura do projeto

```text
MenteUp/
├── .github/workflows/ci.yml      # Pipeline de CI (testes e linter)
├── Documentação/                 # Documentos do PFC
├── Projeto/menteup/              # Projeto Spring Boot
│   ├── Dockerfile                # Imagem multi-stage (JDK para o build, JRE para executar)
│   ├── docker.env.exemplo        # Modelo das variáveis de ambiente
│   ├── checkstyle.xml            # Regras do linter
│   ├── grafana-api/              # Dashboard do Grafana (local e Cloud)
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/umc/menteup/
│       │   ├── config/           # Swagger, seeders e migrações de dados
│       │   ├── controller/       # Endpoints REST
│       │   ├── dto/              # Objetos de entrada e saída da API
│       │   ├── exception/        # ApiException e tratamento global de erros
│       │   ├── mapper/           # Conversão entre entidades e DTOs
│       │   ├── model/            # Entidades JPA e enums
│       │   ├── repository/       # Acesso ao banco (Spring Data)
│       │   ├── security/         # JWT, filtros, regras de senha e IP do cliente
│       │   ├── service/          # Regras de negócio
│       │   └── template/         # Correção de cada formato de atividade (Strategy)
│       ├── main/resources/
│       │   ├── application.properties
│       │   └── documentos/       # Textos versionados do Termo e da Política
│       └── test/                 # Testes unitários e de integração
└── README.md
```

---

## ▶️ Como executar localmente

### Pré-requisitos

- **JDK 21**
- **PostgreSQL 18** (ou Docker)
- Uma conta no **Brevo**, com o remetente verificado e uma chave de API (sem ela a aplicação não sobe)
- IntelliJ IDEA (recomendado)

### Passo a passo (IntelliJ)

1. Clone o repositório:

   ```bash
   git clone https://github.com/eloisamarin/PFC-MenteUp-Backend.git
   ```

2. Crie um banco vazio no PostgreSQL (por exemplo, `menteup`). As tabelas são criadas automaticamente pelo Hibernate (`ddl-auto=update`).
3. Abra a pasta `Projeto/menteup` no IntelliJ.
4. Em **Run → Edit Configurations → Environment variables**, preencha as [variáveis de ambiente](#-variáveis-de-ambiente) obrigatórias.
5. Execute a classe `MenteupApplication`. A API sobe em `http://localhost:8080`.

Pelo terminal, a partir de `Projeto/menteup`:

```bash
./mvnw spring-boot:run
```

> **Primeiro administrador:** cadastre um usuário pela tela de cadastro e altere o tipo dele para `ADMINISTRADOR` direto no banco. Depois disso, os demais usuários são criados pelo próprio sistema.

---

## 🔑 Variáveis de ambiente

Nenhuma senha ou chave fica no código: tudo vem de variáveis de ambiente. **Nunca envie valores reais para o GitHub.** O `.env` está no `.gitignore`.

### Obrigatórias

| Variável | Descrição |
|---|---|
| `DATABASE_URL` | URL JDBC do banco (por exemplo, `jdbc:postgresql://localhost:5432/menteup`; no Neon, com `?sslmode=require`) |
| `DATABASE_USERNAME` | Usuário do banco |
| `DATABASE_PASSWORD` | Senha do banco |
| `JWT_SECRET` | Chave dos tokens de login, em Base64, com pelo menos 32 bytes (gere com `openssl rand -base64 32`) |
| `BREVO_API_KEY` | Chave da API do Brevo |
| `EMAIL_PROJETO` | E-mail remetente, verificado no Brevo |

### Opcionais

| Variável | Padrão | Descrição |
|---|---|---|
| `APP_FRONTEND_URL` | `http://localhost:5173` | Endereço do front-end usado nos links enviados por e-mail |
| `CORS_ORIGENS` | `http://localhost:5173` | Origens que podem chamar a API, separadas por vírgula |
| `MENTEUP_DASHBOARD_TOKEN` | *(vazio)* | Token de leitura do Grafana. Vazio desliga esse acesso |
| `FIREBASE_ENABLED` | `true` | `false` desliga as notificações no celular |
| `GOOGLE_APPLICATION_CREDENTIALS` | — | Caminho do JSON do Firebase, quando ele estiver ligado |
| `SWAGGER_ATIVO` | `false` | `true` liga o Swagger UI |
| `MOSTRAR_SQL` | `false` | `true` mostra o SQL no console |
| `IP_CABECALHOS` | `CF-Connecting-IP` | Cabeçalho com o IP real do cliente. Vazio usa o IP da conexão |
| `EXIGIR_ACEITE` | `false` | `true` bloqueia (HTTP 428) quem não aceitou a versão atual do Termo e da Política |
| `RETENCAO_ATIVA` | `true` | Liga o descarte automático de dados vencidos |
| `RETENCAO_LOGS_MESES` | `6` | Meses de guarda dos registros de acesso (mínimo de 6) |
| `ESQUECI_LIMITE_IP` / `ESQUECI_LIMITE_EMAIL` | `10` / `3` | Limites de pedidos de redefinição de senha |
| `PORT` | `8080` | Porta HTTP (o Render define automaticamente) |

---

## 🐳 Execução com Docker

O `Dockerfile` usa *build* multi-stage: compila com o JDK 21 e executa só com o JRE 21, o que deixa a imagem menor. Os comandos abaixo são executados em `Projeto/menteup`.

Copie o modelo de variáveis e preencha os valores reais:

```bash
cp docker.env.exemplo .env
```

Gere a imagem:

```bash
docker build -t menteup .
```

Execute o container:

```bash
docker run --env-file .env -p 8080:8080 menteup
```

> Dentro do container, `localhost` é o próprio container. Para usar o PostgreSQL do seu computador, troque o host da `DATABASE_URL` por `host.docker.internal`. Sem o arquivo do Firebase, use `FIREBASE_ENABLED=false`.

---

## ☁️ Implantação (deploy)

| Serviço | Função | Configuração principal |
|---|---|---|
| **Neon** | PostgreSQL gerenciado | URL JDBC com `sslmode=require` |
| **Render** | Back-end (Docker) | *Root Directory* `Projeto/menteup` e as variáveis de ambiente da tabela acima |
| **Vercel** | Front-end | `VITE_API_URL` com o endereço do Render; `VITE_GRAFANA_URL` com o link do Grafana |
| **Brevo** | E-mails | Remetente verificado e chave de API (o plano gratuito do Render bloqueia SMTP, por isso o envio é pela API HTTPS) |
| **Grafana Cloud** | Dashboard | Plugin Infinity com o cabeçalho `X-Dashboard-Token` |

No Render, configure também:

```text
APP_FRONTEND_URL=https://menteup.vercel.app
CORS_ORIGENS=https://menteup.vercel.app
FIREBASE_ENABLED=false
JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75
```

Cada *push* na `main` dispara um novo deploy automático no Render e na Vercel.

---

## 📘 Documentação da API

Com `SWAGGER_ATIVO=true`, a documentação interativa fica em:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI (JSON): `http://localhost:8080/v3/api-docs`

Os endpoints estão agrupados com `@Tag` e descritos com `@Operation`. As rotas protegidas exigem o cabeçalho `Authorization: Bearer <token>`.

### Principais grupos de rotas

| Grupo | Prefixo | Exemplos |
|---|---|---|
| Usuários e login | `/usuarios` | `POST /cadastrar`, `POST /logar`, `POST /validar-2fa`, `POST /reenviar-2fa`, `POST /senha/esqueci`, `POST /senha/redefinir`, `POST /senha/primeiro-acesso` |
| Meu perfil | `/usuarios/me` | `GET /`, `PUT /senha`, `PUT /avatar`, `GET /exportar`, `POST /exclusao`, `GET` e `POST /aceites` |
| Turmas | `/turmas` | CRUD, `PUT /{id}/arquivar`, `PUT /{id}/desarquivar`, `GET /{id}/exclusao`, inclusão e remoção de alunos |
| Solicitações de turma | `/alunos/me/solicitacoes`, `/turmas/{id}/solicitacoes` | Pedir entrada, aprovar e recusar |
| Atividades | `/atividades` | CRUD, `POST /{id}/questionario`, `POST /{id}/respostas`, `PUT /{id}/progresso` |
| Área do aluno | `/alunos/me` | `GET /atividades`, `GET /turmas`, `GET /conquistas` |
| Desafios ao vivo | `/desafios` | CRUD, `POST /{id}/iniciar`, `POST /{id}/avancar`, `GET /{id}/estado`, `POST /{id}/respostas`, `GET /{id}/ranking` |
| Desempenho | `/turmas` | `GET /minhas`, `GET /{id}/desempenho`, `GET /{turmaId}/alunos/{alunoId}/desempenho` |
| Ranking e conquistas | `/ranking`, `/conquistas` | Classificação e conquistas do aluno |
| Dashboard | `/dashboard` | `GET /painel` (todos os perfis) e as rotas de indicadores do Grafana |
| Termo e Política | `/documentos` | `GET /{tipo}/atual`, `GET /{tipo}/versoes` (rotas públicas) |
| Logs do sistema | `/logs` | Consulta dos logs (somente administrador) |

---

## 🔒 Segurança

- **Autenticação em duas etapas:** login com senha e código enviado por e-mail. O código fica salvo em *hash* e vale por 5 minutos, com no máximo 5 tentativas e até 3 reenvios em 15 minutos (intervalo de 60 segundos entre eles). O link de redefinição de senha vale por 30 minutos, e o convite de primeiro acesso, por 72 horas.
- **JWT** assinado com chave vinda de variável de ambiente. Um token inválido ou expirado devolve **401**, e as sessões de uma conta bloqueada são encerradas.
- **Senhas** guardadas com **BCrypt**. É obrigatório usar maiúscula, minúscula, número e caractere especial, sem espaços nas pontas e com no máximo 72 bytes.
- **Autorização por perfil** (`ALUNO`, `PROFESSOR` e `ADMINISTRADOR`) no `SecurityConfig` e **regras de posse** nos *services*: o professor só altera as atividades e os desafios das turmas dele.
- **Limite de tentativas** no "esqueci minha senha", por IP e por e-mail.
- **CORS** restrito às origens configuradas, **HSTS** atrás do proxy e Swagger desligado em produção.
- **Validação de entrada:** dados inválidos devolvem **400** com código e mensagem, nunca **500**.
- **IP real do cliente** nos logs (cabeçalho `CF-Connecting-IP` no Render).

---

## ⚖️ Conformidade com a LGPD

- **Termo de Aceite e Política de Privacidade versionados** (versão 1.1 em vigor desde 15/11/2026), servidos pela API, com registro de cada aceite (usuário, versão e data).
- **Minimização de dados:** cada perfil recebe só os dados de que precisa. O aluno vê apenas o nome e o tipo dos colegas de turma, e as respostas nunca expõem senha nem token do celular.
- **Direitos do titular:** download dos próprios dados (acesso e portabilidade) e exclusão da conta pelo próprio usuário.
- **Descarte automático** de dados vencidos, todo dia às 3h e também quando a aplicação sobe. Os prazos são os do item 8 da Política: registros de acesso por 6 meses (Marco Civil da Internet, Lei nº 12.965/2014), códigos 2FA por 1 dia, links de senha por 7 dias, e cadastros rejeitados e convites vencidos por 30 dias. Os aceites não são apagados, porque são a prova do consentimento.
- **Logs de auditoria** das ações relevantes, inclusive do próprio descarte.

---

## 📊 Dashboard e Grafana

- **Painel no MenteUp** (`GET /dashboard/painel`): cada perfil vê somente o seu recorte (o aluno, os próprios dados; o professor, as turmas dele; o administrador, tudo). Os filtros são período, turma e aluno.
- **Grafana:** o administrador também acompanha os indicadores no Grafana Cloud. O Grafana lê a API (`GET /dashboard/**`) com o plugin **Infinity** e o cabeçalho `X-Dashboard-Token`. Ele não acessa o banco, e as respostas trazem só números e categorias, sem dados pessoais.

O passo a passo para subir o Grafana localmente e os modelos de dashboard ficam em [`Projeto/menteup/grafana-api`](Projeto/menteup/grafana-api/README.md).

---

## 🧪 Testes e integração contínua

O projeto tem **mais de 500 testes automatizados**, entre unitários (services, templates e regras de senha) e de integração (API, service e banco H2 em memória). Os comandos abaixo são executados em `Projeto/menteup`.

Para rodar todos os testes:

```bash
./mvnw test
```

Para rodar só os testes unitários:

```bash
./mvnw test -DexcludedGroups=integration
```

Para rodar só os testes de integração:

```bash
./mvnw test -Dgroups=integration
```

Para rodar o linter:

```bash
./mvnw checkstyle:check
```

A pipeline do **GitHub Actions** (`.github/workflows/ci.yml`) roda em cada Pull Request para a `main`, com três checagens em paralelo: **testes unitários**, **testes de integração** e **linter (Checkstyle)**.

---

## ✅ Instruções e evidências de execução

Esta seção mostra como verificar, passo a passo, que as funcionalidades e as integrações descritas neste documento estão funcionando, e registra as evidências coletadas na versão 1.0.0.

### 1. Verificação rápida da API em produção

Os comandos abaixo podem ser executados em qualquer terminal (Git Bash, Linux ou macOS) e não exigem login. Na primeira chamada, o Render pode levar até 1 minuto para acordar o serviço.

Termo de Aceite em vigor (rota pública):

```bash
curl -i https://pfc-menteup-backend.onrender.com/documentos/termo/atual
```

Política de Privacidade em vigor (rota pública):

```bash
curl -i https://pfc-menteup-backend.onrender.com/documentos/politica/atual
```

Rota protegida chamada sem token (deve ser recusada):

```bash
curl -i https://pfc-menteup-backend.onrender.com/usuarios/all
```

Login com credenciais inválidas (deve ser recusado):

```bash
curl -i -X POST https://pfc-menteup-backend.onrender.com/usuarios/logar -H "Content-Type: application/json" -d '{"usuario":"naoexiste@exemplo.com","senha":"x"}'
```

CORS liberado somente para o front-end oficial:

```bash
curl -i -X OPTIONS https://pfc-menteup-backend.onrender.com/documentos/termo/atual -H "Origin: https://menteup.vercel.app" -H "Access-Control-Request-Method: GET"
```

**Resultados obtidos em 15/11/2026:**

| Verificação | Resultado esperado | Resultado obtido |
|---|---|---|
| Front-end `https://menteup.vercel.app` | HTTP 200 | ✅ HTTP 200 |
| `GET /documentos/termo/atual` | HTTP 200 com a versão em vigor | ✅ HTTP 200, `"versao":"1.1"`, `"vigenteDesde":"2026-11-15"` |
| `GET /documentos/politica/atual` | HTTP 200 com a versão em vigor | ✅ HTTP 200, `"versao":"1.1"`, `"vigenteDesde":"2026-11-15"` |
| `GET /usuarios/all` sem token | HTTP 401 | ✅ HTTP 401 |
| `GET /dashboard/resumo` sem token | HTTP 401 | ✅ HTTP 401 |
| `POST /usuarios/logar` com credenciais inválidas | HTTP 401 | ✅ HTTP 401 |
| CORS para `https://menteup.vercel.app` | `Access-Control-Allow-Origin` com o endereço do front | ✅ `access-control-allow-origin: https://menteup.vercel.app` |
| Swagger em produção | Desligado (HTTP 404) | ✅ HTTP 404 (`SWAGGER_ATIVO=false`) |
| Conexão com o banco Neon | API responde com dados do banco | ✅ Os documentos legais acima vêm do PostgreSQL no Neon |

### 2. Roteiro de verificação das funcionalidades

Siga o roteiro em https://menteup.vercel.app. Cada passo também confirma uma integração.

| # | Passo | Resultado esperado | Integração verificada |
|---|---|---|---|
| 1 | Abrir **Criar conta**, abrir o Termo e a Política e só então marcar a caixa de aceite | A caixa só é liberada depois de abrir os dois documentos | Vercel → Render → Neon |
| 2 | Cadastrar com uma senha fraca e depois com uma senha forte | A senha fraca é recusada com os requisitos na tela; a forte é aceita e a tela volta para o login | Validação da API |
| 3 | Cadastrar um **professor** pela tela pública e, como **administrador**, aprová-lo em **Usuários** | Antes da aprovação o login do professor é recusado; depois, é liberado | API + banco |
| 4 | Fazer login com e-mail e senha | Chega um **código 2FA** por e-mail | **Brevo** |
| 5 | Digitar o código errado 5 vezes e usar **Reenviar código** | Depois de 5 erros o código é bloqueado; o reenvio respeita 60 segundos | Segurança do 2FA |
| 6 | Como administrador, criar um **professor** | O professor recebe o convite de primeiro acesso por e-mail, com link para `menteup.vercel.app` | **Brevo** + `APP_FRONTEND_URL` |
| 7 | Usar **Esqueci minha senha** | Chega o link de redefinição, válido por 30 minutos | **Brevo** |
| 8 | Como administrador, criar uma **turma** e escolher o professor responsável | A turma aparece para o professor escolhido | Regra de posse |
| 9 | Como professor, adicionar o aluno pelo e-mail e criar uma **atividade** de cada formato | As atividades aparecem na área do aluno | — |
| 10 | Como professor, tentar editar uma atividade de uma turma de outro professor | A API recusa (HTTP 403) | Regra de posse |
| 11 | Como aluno, responder às atividades | Pontos, XP e **conquistas** são atualizados, e o ranking muda | Gamificação |
| 12 | Como professor, iniciar um **desafio ao vivo**; como aluno, responder | As perguntas avançam com cronômetro e o ranking do desafio é exibido | Desafios |
| 13 | Como administrador, tentar excluir a turma com alunos | A API explica o motivo e oferece o **arquivamento** | Regra de exclusão |
| 14 | Abrir o **Painel** com cada perfil e trocar os filtros | Cada perfil vê somente o próprio recorte (aluno, turmas do professor, tudo para o admin) | `/dashboard/painel` |
| 15 | Como administrador, abrir **Logs** | As ações aparecem com data, usuário e IP real | Auditoria |
| 16 | Como administrador, abrir o botão do **Grafana** | O dashboard mostra os Big Numbers e os gráficos | **Grafana Cloud** → API com `X-Dashboard-Token` |
| 17 | Em **Meu perfil**, baixar os dados e excluir a conta | O arquivo traz os dados da pessoa; depois da exclusão o login deixa de funcionar | LGPD |

### 3. Verificação local e testes automatizados

1. Siga a seção [Como executar localmente](#️-como-executar-localmente) ou [Execução com Docker](#-execução-com-docker).
2. Com `SWAGGER_ATIVO=true`, abra `http://localhost:8080/swagger-ui/index.html` e teste as rotas com o botão **Authorize** (cole o token recebido no `/usuarios/validar-2fa`).
3. Rode os testes e o linter em `Projeto/menteup`:

```bash
./mvnw test checkstyle:check
```

**Resultado obtido em 15/11/2026:** ✅ **585 testes em 56 classes, com 0 falhas, 0 erros e 0 ignorados**, e o Checkstyle sem nenhuma violação.

### 4. Evidências em imagem

As capturas de tela da versão 1.0.0 ficam na pasta [`docs/evidencias`](docs/evidencias):

| Arquivo | O que mostra |
|---|---|
| `01-login.png` | Tela de login em `menteup.vercel.app` |
| `02-cadastro-termo.png` | Cadastro com o aceite do Termo e da Política |
| `03-email-2fa.png` | E-mail com o código 2FA enviado pelo Brevo |
| `04-painel-aluno.png` | Painel de indicadores do aluno |
| `05-atividade.png` | Atividade sendo respondida |
| `06-desafio-ao-vivo.png` | Desafio ao vivo com o ranking |
| `07-painel-professor.png` | Painel do professor com os filtros |
| `08-turmas-arquivamento.png` | Gestão de turmas e arquivamento |
| `09-logs.png` | Logs do sistema (administrador) |
| `10-grafana.png` | Dashboard no Grafana Cloud |
| `11-render.png` | Serviço em execução no Render |
| `12-github-actions.png` | Pipeline do GitHub Actions aprovada |

---

## 👥 Equipe

Projeto Final de Curso (PFC), Universidade de Mogi das Cruzes (UMC).

- **Eloisa Marin** — [@eloisamarin](https://github.com/eloisamarin)
- [@Nikolas26](https://github.com/Nikolas26)

---

<p align="center">MenteUp · versão 1.0.0 · 2026</p>
