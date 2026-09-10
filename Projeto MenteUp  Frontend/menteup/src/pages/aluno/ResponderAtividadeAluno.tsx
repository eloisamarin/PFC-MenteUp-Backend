import { useNavigate, useParams } from "react-router-dom";
import { useEffect, useState } from "react";
import { apiFetch } from "../../services/api";

interface Alternativa {
  id: number;
  texto: string;
  correta?: boolean;
}

interface Pergunta {
  id: number;
  enunciado: string;
  alternativas: Alternativa[];
}

interface Atividade {
  id: number;
  titulo: string;
  descricao: string;
  pontuacao: number;
  status: string;
  turma?: {
    id: number;
    nome: string;
  };
  perguntas: Pergunta[];
}

function chaveAtividadesConcluidas() {
  const usuario = JSON.parse(
    localStorage.getItem("usuario") || "{}"
  );
  return `atividades-concluidas-${usuario.id || usuario.usuario || "aluno"}`;
}

function VisualizarAtividadeAluno() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [atividade, setAtividade] = useState<Atividade | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");
  const [respostas, setRespostas] = useState<Record<number, number>>({});
  const [enviada, setEnviada] = useState(false);
  const [resultado, setResultado] = useState<number | null>(null);

  useEffect(() => {
    const buscarAtividade = async () => {
      try {
        const usuarioSalvo = localStorage.getItem("usuario");

        if (!usuarioSalvo) {
          navigate("/login");
          return;
        }

        if (!id) {
          setErro("ID da atividade não informado.");
          return;
        }

        const response = await apiFetch(`/atividades/${id}`);

        if (!response.ok) {
          throw new Error("Atividade não encontrada.");
        }

        const dados = await response.json();

        setAtividade(dados);

        const concluidas: Record<string, number> = JSON.parse(
          localStorage.getItem(chaveAtividadesConcluidas()) || "{}"
        );
        const resultadoSalvo = concluidas[String(dados.id)];

        if (resultadoSalvo !== undefined) {
          setResultado(resultadoSalvo);
          setEnviada(true);
        }
      } catch (error) {
        console.error("Erro ao buscar atividade:", error);
        setErro("Não foi possível carregar a atividade.");
      } finally {
        setCarregando(false);
      }
    };

    buscarAtividade();
  }, [id, navigate]);

  function selecionarResposta(perguntaId: number, alternativaId: number) {
    if (enviada) return;

    setRespostas((atuais) => ({
      ...atuais,
      [perguntaId]: alternativaId,
    }));
  }

  function finalizarAtividade() {
    if (!atividade) return;

    const faltando = atividade.perguntas.some(
      (pergunta) => respostas[pergunta.id] === undefined
    );

    if (faltando) {
      setErro("Responda todas as perguntas antes de finalizar.");
      return;
    }

    const acertos = atividade.perguntas.filter((pergunta) => {
      const alternativa = pergunta.alternativas.find(
        (item) => item.id === respostas[pergunta.id]
      );
      return alternativa?.correta === true;
    }).length;

    setErro("");
    setResultado(acertos);
    setEnviada(true);

    const chave = chaveAtividadesConcluidas();
    const concluidas: Record<string, number> = JSON.parse(
      localStorage.getItem(chave) || "{}"
    );
    concluidas[String(atividade.id)] = acertos;
    localStorage.setItem(chave, JSON.stringify(concluidas));
  }

  /* ==============================
     CARREGANDO
  ============================== */

  if (carregando) {
    return (
      <div className="mu-dashboard">
        <main className="mu-main">
          <div className="mu-content">
            <div className="mu-card mu-empty">
              <div className="mu-empty-icon">⏳</div>

              <h3>Carregando atividade...</h3>

              <p>Aguarde enquanto buscamos os dados.</p>
            </div>
          </div>
        </main>
      </div>
    );
  }

  /* ==============================
     ERRO
  ============================== */

  if (!atividade) {
    return (
      <div className="mu-dashboard">
        <main className="mu-main">
          <div className="mu-content">
            <div className="mu-card mu-empty">
              <div className="mu-empty-icon">⚠️</div>

              <h3>Atividade não encontrada</h3>

              <p>
                {erro || "Não foi possível encontrar esta atividade."}
              </p>

              <button
                className="mu-btn mu-btn-primary"
                onClick={() => navigate("/aluno/atividades")}
              >
                ← Voltar para atividades
              </button>
            </div>
          </div>
        </main>
      </div>
    );
  }

  /* ==============================
     TELA DE VISUALIZAÇÃO
  ============================== */

  return (
    <div className="mu-dashboard">
      <main className="mu-main">

        {/* HEADER */}

        <header className="mu-header">
          <input
            className="mu-search"
            type="text"
            placeholder="Pesquisar..."
          />

          <div className="mu-header-user">
            <div className="mu-avatar">
              PE
            </div>

            <span className="mu-user-name">
              Aluno
            </span>
          </div>
        </header>

        {/* CONTEÚDO */}

        <div className="mu-content">

          {/* VOLTAR */}

          <button
            className="mu-btn mu-btn-secondary"
            onClick={() =>
              navigate("/aluno/atividades")
            }
          >
            ← Voltar
          </button>

          {/* CABEÇALHO */}

          <div className="mu-view-header">

            <div>
              <span className="mu-view-label">
                ATIVIDADE
              </span>

              <h1>
                {atividade.titulo}
              </h1>

              <p>
                {atividade.descricao}
              </p>
            </div>

            <div className="mu-view-status">

              <span
                className={`mu-badge ${
                  atividade.status === "CONCLUIDO"
                    ? "mu-status-concluida"
                    : atividade.status === "EM_ANDAMENTO"
                    ? "mu-status-andamento"
                    : "mu-status-rascunho"
                }`}
              >
                {atividade.status}
              </span>

            </div>

          </div>

          {/* INFORMAÇÕES */}

          <div className="mu-info-grid">

            <div className="mu-info-card">
              <span>🏆</span>

              <div>
                <small>Pontuação</small>

                <strong>
                  {atividade.pontuacao} pontos
                </strong>
              </div>
            </div>

            <div className="mu-info-card">
              <span>🏫</span>

              <div>
                <small>Turma</small>

                <strong>
                  {atividade.turma?.nome ||
                    "Não informada"}
                </strong>
              </div>
            </div>

            <div className="mu-info-card">
              <span>📝</span>

              <div>
                <small>Perguntas</small>

                <strong>
                  {atividade.perguntas?.length || 0}
                </strong>
              </div>
            </div>

          </div>

          {/* PERGUNTAS */}

          <div className="mu-card mu-questions-card">

            <div className="mu-section-header">

              <div>
                <h2>
                  Perguntas da atividade
                </h2>

                <p>
                  Responda todas as perguntas e finalize para
                  visualizar seu resultado.
                </p>
              </div>

            </div>

            {atividade.perguntas?.length === 0 ? (

              <div className="mu-empty">

                <div className="mu-empty-icon">
                  📝
                </div>

                <h3>
                  Nenhuma pergunta cadastrada
                </h3>

                <p>
                  Esta atividade ainda não possui
                  perguntas.
                </p>

              </div>

            ) : (

              <div className="mu-question-list">

                {atividade.perguntas.map(
                  (pergunta, index) => (

                    <div
                      className="mu-question"
                      key={pergunta.id}
                    >

                      <div className="mu-question-number">
                        {index + 1}
                      </div>

                      <div className="mu-question-content">

                        <h3>
                          {pergunta.enunciado}
                        </h3>

                        <div className="mu-alternatives">

                          {pergunta.alternativas?.map(
                            (alternativa) => (

                              <button
                                type="button"
                                className={`mu-alternative ${
                                  respostas[pergunta.id] === alternativa.id
                                    ? "selected"
                                    : ""
                                }`}
                                key={alternativa.id}
                                onClick={() =>
                                  selecionarResposta(
                                    pergunta.id,
                                    alternativa.id
                                  )
                                }
                                disabled={enviada}
                              >

                                <span className="mu-alternative-letter">
                                  •
                                </span>

                                <span>
                                  {alternativa.texto}
                                </span>

                              </button>

                            )
                          )}

                        </div>

                      </div>

                    </div>

                  )
                )}

              </div>

            )}

            {!enviada && atividade.perguntas?.length > 0 && (
              <button
                type="button"
                className="mu-finish-button"
                onClick={finalizarAtividade}
              >
                Finalizar atividade
              </button>
            )}

            {enviada && resultado !== null && (
              <div className="mu-success">
                Atividade concluída. Você acertou {resultado} de{" "}
                {atividade.perguntas.length} pergunta(s). Não é possível
                responder novamente.
              </div>
            )}

          </div>

        </div>

      </main>
    </div>
  );
}

export default VisualizarAtividadeAluno;