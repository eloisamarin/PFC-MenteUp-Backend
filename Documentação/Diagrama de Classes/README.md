# Diagrama de Classes do MenteUp

Modelo de domínio do back-end (pacote `com.umc.menteup.model`), gerado a partir das entidades JPA
reais do projeto, incluindo as conquistas, o perfil, a recuperação de senha e as solicitações de turma.

## Arquivos

- **`diagrama-de-classes-menteup.mmd`**: código do diagrama (Mermaid). Editável: cole em
  [mermaid.live](https://mermaid.live) ou coloque num README do GitHub entre ```` ```mermaid ```` e ```` ``` ````.
- **`diagrama-de-classes-menteup.png`**: imagem em alta resolução, para o TCC.
- **`diagrama-de-classes-menteup.svg`**: imagem vetorial (amplia sem perder qualidade).

## Pacotes

| Pacote | Classes |
|---|---|
| Usuários e acesso | Usuario, TipoUsuario, Codigo2FA, TokenRedefinicaoSenha, AvatarAnimal |
| Turmas | Turma, SolicitacaoTurma (+ TipoSolicitacao, StatusSolicitacao) |
| Atividades | Atividade, Pergunta, Alternativa, ProgressoAtividade, RespostaAtividade, ResultadoAtividade (+ StatusAtividade, TipoAtividade) |
| Desafios ao vivo | Desafio, PerguntaDesafio, AlternativaDesafio, RespostaDesafio (+ StatusDesafio) |
| Gamificação | Conquista, UsuarioConquista, GamificacaoUsuario (+ CategoriaConquista, TipoRegraConquista, RaridadeConquista) |
| Auditoria | LogSistema (+ AcaoLog, EntidadeLog, ResultadoLog) |

## Notação (UML)

- `-` atributo privado · `+` método público · sublinhado = método estático
- **Losango cheio** (composição): a parte não existe sem o todo (ex.: Pergunta de uma Atividade)
- **Losango vazio** (agregação): Turma agrupa Atividades
- **Seta cheia**: associação (referência por chave estrangeira)
- **Seta tracejada**: dependência sem chave estrangeira (Codigo2FA guarda o e-mail; TipoUsuario consulta o tipo)
- Multiplicidades: `1`, `0..1`, `0..*`, `1..*`, `2..4` (o desafio exige de 2 a 4 alternativas por pergunta)
- `SolicitacaoTurma.Tipo` e `.Status` são enums internos da classe; no diagrama aparecem como
  `TipoSolicitacao` e `StatusSolicitacao`.
