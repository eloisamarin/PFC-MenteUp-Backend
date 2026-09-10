import { useNavigate, useParams } from "react-router-dom";
import { useEffect, useState } from "react";

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

function VisualizarAtividadeProfessor() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [atividade, setAtividade] = useState<Atividade | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");

  useEffect(() => {
    const buscarAtividade = async () => {
      try {
        const usuarioSalvo = localStorage.getItem("usuario");

        if (!usuarioSalvo) {
          navigate("/login");
          return;
        }

        const usuario = JSON.parse(usuarioSalvo);
        const token = usuario.token;

        if (!id) {
          setErro("ID da atividade não informado.");
          return;
        }

        const response = await fetch(
          `http://localhost:8080/atividades/${id}`,
          {
            method: "GET",
            headers: {
              Authorization: token,
              "Content-Type": "application/json",
            },
          }
        );

        if (!response.ok) {
          throw new Error("Atividade não encontrada.");
        }

        const dados = await response.json();

        setAtividade(dados);
      } catch (error) {
        console.error("Erro ao buscar atividade:", error);
        setErro("Não foi possível carregar a atividade.");
      } finally {
        setCarregando(false);
      }
    };

    buscarAtividade();
  }, [id, navigate]);

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
                onClick={() => navigate("/professor/atividades")}
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
              Professora
            </span>
          </div>
        </header>

        {/* CONTEÚDO */}

        <div className="mu-content">

          {/* VOLTAR */}

          <button
            className="mu-btn mu-btn-secondary"
            onClick={() =>
              navigate("/professor/atividades")
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
                  Confira as perguntas e alternativas
                  cadastradas.
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

                              <div
                                className={`mu-alternative ${
                                  alternativa.correta
                                    ? "correct"
                                    : ""
                                }`}
                                key={alternativa.id}
                              >

                                <span className="mu-alternative-letter">
                                  •
                                </span>

                                <span>
                                  {alternativa.texto}
                                </span>

                                {alternativa.correta && (
                                  <span className="mu-correct">
                                    ✓ Correta
                                  </span>
                                )}

                              </div>

                            )
                          )}

                        </div>

                      </div>

                    </div>

                  )
                )}

              </div>

            )}

          </div>

        </div>

      </main>
    </div>
  );
}

export default VisualizarAtividadeProfessor;