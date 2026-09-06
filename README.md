# 🎓 MenteUp

## Plataforma Web de Gamificação Educacional

O **MenteUp** é uma plataforma web de gamificação educacional que tem como objetivo incentivar o engajamento e a participação dos estudantes no processo de aprendizagem por meio de recursos de gamificação, como atividades, questionários, pontuação e ranking.

Além disso, o sistema permite que professores acompanhem o desempenho e a participação dos estudantes e disponibiliza um perfil administrativo para gerenciamento de usuários, logs de acesso e auditoria das ações realizadas na plataforma.

---

# 🎯 Objetivo

Desenvolver uma plataforma educacional baseada em gamificação para incentivar o engajamento dos estudantes e auxiliar os professores no acompanhamento do processo de aprendizagem.

## Objetivos específicos

- Promover a resolução de atividades e desafios; 

- Permitir a criação e disponibilização de questionários pelos professores; 

- Atribuir pontuações aos estudantes de acordo com seu desempenho nas atividades; 

- Estabelecer um sistema de ranqueamento; 

- Converter pontos acumulados em conquistas. 

---

# 🚀 Funcionalidades

## 👨‍🎓 Estudante

- Cadastro e login.
- Visualização de atividades.
- Participação em questionários.
- Participação em desafios.
- Acúmulo de pontos.
- Visualização do ranking.
- Acompanhamento do progresso.
- Visualização do histórico de atividades.

## 👨‍🏫 Professor

- Cadastro e login.
- Criação de atividades.
- Criação de questionários.
- Edição e exclusão de atividades.
- Gerenciamento de turmas.
- Visualização dos estudantes.
- Acompanhamento do desempenho.
- Visualização do ranking.
- Consulta ao histórico de alterações das atividades.

## 🛡️ Administrador

- Gerenciamento de usuários.
- Gerenciamento de alunos e professores.
- Controle de status dos usuários.
- Gerenciamento de turmas.
- Visualização de logs de acesso.
- Visualização de logs de auditoria.
- Acompanhamento das ações realizadas no sistema.
- Controle de permissões de acesso.

---

# 🏗️ Arquitetura

O MenteUp utiliza uma arquitetura baseada na separação de responsabilidades entre as camadas da aplicação.

```text
┌──────────────────────────────────┐
│            FRONT-END             │
│         React + JavaScript       │
└────────────────┬─────────────────┘
                 │
                 │ HTTP / REST / JSON
                 ▼
┌──────────────────────────────────┐
│             BACK-END             │
│       Java 21 + Spring Boot      │
│                                  │
│  Controller                      │
│  Service                         │
│  Repository                      │
│  Security                        │
└────────────────┬─────────────────┘
                 │
                 │ JPA / Hibernate
                 ▼
┌──────────────────────────────────┐
│            DATABASE              │
│          PostgreSQL 18           │
└──────────────────────────────────┘
```

# 💻 Tecnologias

### Front-end
- **React** — desenvolvimento da interface da aplicação.
- **Typescriptt** — implementação da lógica da aplicação.
- **HTML5** — estrutura das páginas.
- **CSS** — estilização e responsividade.
- **Vite** — ferramenta utilizada para desenvolvimento e construção do projeto.

### Back-end
- **Java 21** — linguagem principal do back-end.
- **Spring Boot** — desenvolvimento da aplicação e da API.
- **Spring Web** — criação dos endpoints REST.
- **Spring Data JPA** — persistência e acesso aos dados.
- **Spring Security** — autenticação e autorização.
- **Bean Validation** — validação dos dados.
- **Maven** — gerenciamento do projeto e das dependências.

### Banco de dados
- **PostgreSQL 18** — Sistema Gerenciador de Banco de Dados Relacional.

### Versionamento
- **Git** — controle de versão.
- **GitHub** — hospedagem do código-fonte e colaboração.

### Ferramentas
- **IntelliJ IDEA** — desenvolvimento do back-end.
- **Visual Studio Code** — desenvolvimento do front-end.
- **Insomnia** — testes da API REST.
- **Figma** — prototipação das interfaces.

---

# 📦 Dependências do Back-end

O projeto Spring Boot utiliza as seguintes dependências principais:

- Spring Web
- Spring Data JPA
- PostgreSQL Driver
- Spring Security
- Validation
- Lombok
- Spring Boot DevTools

Essas dependências são utilizadas para desenvolvimento da API REST, persistência de dados, conexão com o banco de dados, segurança, validação e apoio ao desenvolvimento.

---

# 🗄️ Banco de Dados

O MenteUp utiliza o PostgreSQL 18 como Sistema Gerenciador de Banco de Dados Relacional.

O banco de dados é responsável pelo armazenamento das principais informações da plataforma:

- Usuários.
- Turmas.
- Atividades.
- Questionários.
- Questões.
- Respostas.
- Pontuação.
- Conquistas.
- Ranking.
- Progresso.
- Logs de acesso.
- Logs de auditoria.

### Configuração do banco

O arquivo de configuração está localizado em:  
`src/main/resources/application.properties`

Exemplo de configuração:

```properties
spring.application.name=MenteUp
spring.datasource.url=jdbc:postgresql://localhost:5432/menteup
spring.datasource.username=postgres
spring.datasource.password=SUA_SENHA

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
