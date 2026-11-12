# Diagramas BPMN do MenteUp

Processos do sistema em BPMN 2.0, atualizados com as funcionalidades implementadas até a entrega de novembro.

**Arquivo único:** `menteup-processos.bpmn` (e `.png` / `.svg`) traz os 6 processos num só diagrama,
um pool embaixo do outro. Os arquivos numerados abaixo são os mesmos processos separados.

| Diagrama | O que mostra |
|---|---|
| `00-cadastro-de-conta` | Visitante escolhe Aluno ou Professor e preenche os dados; sistema valida, confere se o e-mail já existe, criptografa a senha e salva; administrador é quem muda o tipo de usuário |
| `01-acesso-e-recuperacao-de-senha` | Login com código 2FA por e-mail, bloqueio após 3 senhas erradas e recuperação de senha por link (30 min, uso único) |
| `02-entrada-e-saida-de-turma` | Aluno pede para entrar ou sair da turma; professor da turma ou administrador aprova ou recusa; professor/admin também adiciona e remove direto |
| `03-atividade-desempenho-e-conquistas` | Professor cria e publica a atividade (5 templates), aluno responde, sistema corrige, avalia conquistas, atualiza ranking; professor analisa o desempenho e comenta |
| `04-desafio-ao-vivo` | Desafio em tempo real: pergunta com cronômetro, pontos por acerto e rapidez, ranking final |
| `05-monitoramento-dashboard-grafana` | Administrador acompanha os indicadores no Grafana, que lê a API `/dashboard` com token |

## Arquivos de cada diagrama

- **`.bpmn`**: o diagrama editável (BPMN 2.0 XML). Abre no [bpmn.io](https://demo.bpmn.io), no Camunda Modeler ou no draw.io.
- **`.png`**: imagem em alta resolução, para colar no Word do TCC.
- **`.svg`**: imagem vetorial (não perde qualidade ao ampliar).

## Legenda

- **Raias** (faixas horizontais): quem executa cada tarefa (Aluno, Professor, Administrador, Sistema, Grafana).
- **Tarefa com bonequinho**: feita por uma pessoa na tela.
- **Tarefa com engrenagem**: feita automaticamente pelo sistema.
- **Tarefa com envelope**: envio de mensagem (e-mail, notificação ou chamada à API).
- **Losango com X**: decisão (cada saída tem "Sim" ou "Não").
- **Círculo fino**: início do processo. **Círculo grosso**: fim.
