import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { apiFetch } from "../../services/api";

interface Turma {
  id: number;
  nome: string;
}

interface Alternativa {
  id?: number;
  texto: string;
  correta?: boolean;
}

interface Pergunta {
  id?: number;
  enunciado: string;
  alternativas: Alternativa[];
}

interface Atividade {
  id: number;
  titulo: string;
  descricao: string;
  pontuacao: number;
  status: string;
  turma?: Turma;
  perguntas: Pergunta[];
}

function EditarAtividadeAdministrador() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [atividade, setAtividade] = useState<Atividade | null>(null);
  const [turmas, setTurmas] = useState<Turma[]>([]);

  const [titulo, setTitulo] = useState("");
  const [descricao, setDescricao] = useState("");
  const [pontuacao, setPontuacao] = useState<number>(1);
  const [status, setStatus] = useState("PENDENTE");
  const [turmaId, setTurmaId] = useState<number | "">("");

  const [loading, setLoading] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState("");
  const [sucesso, setSucesso] = useState("");

  /*
   * =====================================================
   * CARREGAR ATIVIDADE
   * =====================================================
   */

  useEffect(() => {
    const carregarAtividade = async () => {
      try {
        const response = await apiFetch(`/atividades/${id}`);

        if (!response.ok) {
          throw new Error("Não foi possível carregar a atividade.");
        }

        const data: Atividade = await response.json();

        setAtividade(data);

        setTitulo(data.titulo || "");
        setDescricao(data.descricao || "");
        setPontuacao(data.pontuacao || 1);
        setStatus(data.status || "PENDENTE");
        setTurmaId(data.turma?.id ?? "");
      } catch (error) {
        console.error(error);
        setErro("Não foi possível carregar a atividade.");
      } finally {
        setLoading(false);
      }
    };

    carregarAtividade();
  }, [id, navigate]);

  /*
   * =====================================================
   * CARREGAR TURMAS
   * =====================================================
   */

  useEffect(() => {
    const carregarTurmas = async () => {
      try {
        const response = await apiFetch("/turmas/all");

        if (!response.ok) {
          throw new Error("Erro ao carregar turmas.");
        }

        const data = await response.json();

        setTurmas(data);
      } catch (error) {
        console.error("Erro ao carregar turmas:", error);
      }
    };

    carregarTurmas();
  }, []);

  /*
   * =====================================================
   * ATUALIZAR ATIVIDADE
   * =====================================================
   */

  const atualizarAtividade = async (
    event: React.FormEvent<HTMLFormElement>
  ) => {
    event.preventDefault();

    setErro("");
    setSucesso("");

    if (!titulo.trim()) {
      setErro("Informe o título da atividade.");
      return;
    }

    if (titulo.trim().length < 5) {
      setErro("O título deve possuir pelo menos 5 caracteres.");
      return;
    }

    if (!descricao.trim()) {
      setErro("Informe a descrição da atividade.");
      return;
    }

    if (descricao.trim().length < 10) {
      setErro("A descrição deve possuir pelo menos 10 caracteres.");
      return;
    }

    if (pontuacao < 1) {
      setErro("A pontuação deve ser no mínimo 1.");
      return;
    }

    if (turmaId === "") {
      setErro("Selecione uma turma.");
      return;
    }

    try {
      setSalvando(true);

      /*
       * Mantemos as perguntas existentes.
       * Assim, ao editar os dados principais da atividade,
       * elas não são removidas.
       */

      const atividadeAtualizada = {
        id: Number(id),
        titulo: titulo.trim(),
        descricao: descricao.trim(),
        pontuacao: Number(pontuacao),
        status: status,
        turma: {
          id: Number(turmaId),
        },
      };

      const response = await apiFetch("/atividades", {
        method: "PUT",
        body: JSON.stringify(atividadeAtualizada),
      });

      if (!response.ok) {
        const textoErro = await response.text();

        console.error("Erro do backend:", textoErro);

        throw new Error(
          "Não foi possível atualizar a atividade."
        );
      }

      const data = await response.json();

      setAtividade(data);

      setSucesso("Atividade atualizada com sucesso! 🎉");

      setTimeout(() => {
        navigate(`/administrador/atividades/${id}`);
      }, 1000);
    } catch (error) {
      console.error(error);

      setErro(
        error instanceof Error
          ? error.message
          : "Erro ao atualizar atividade."
      );
    } finally {
      setSalvando(false);
    }
  };

  /*
   * =====================================================
   * LOADING
   * =====================================================
   */

  if (loading) {
    return (
      <div className="mu-dashboard">
        <main className="mu-main">
          <div className="mu-content">
            <div className="mu-card mu-empty">
              <div className="mu-empty-icon">⏳</div>

              <h3>Carregando atividade...</h3>

              <p>
                Aguarde enquanto buscamos os dados da atividade.
              </p>
            </div>
          </div>
        </main>
      </div>
    );
  }

  /*
   * =====================================================
   * ERRO
   * =====================================================
   */

  if (!atividade) {
    return (
      <div className="mu-dashboard">
        <main className="mu-main">
          <div className="mu-content">
            <div className="mu-card mu-empty">
              <div className="mu-empty-icon">⚠️</div>

              <h3>Atividade não encontrada</h3>

              <p>
                Não foi possível carregar os dados da atividade.
              </p>

              <button
                className="mu-btn mu-btn-primary"
                onClick={() =>
                  navigate("/administrador/atividades")
                }
              >
                Voltar para atividades
              </button>
            </div>
          </div>
        </main>
      </div>
    );
  }

  /*
   * =====================================================
   * TELA
   * =====================================================
   */

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
              Administrador
            </span>

          </div>

        </header>

        {/* CONTEÚDO */}

        <div className="mu-content">

          {/* VOLTAR */}

          <button
            type="button"
            className="mu-btn mu-btn-secondary"
            onClick={() =>
              navigate(`/administrador/atividades/${id}`)
            }
            style={{ marginBottom: "20px" }}
          >
            ← Voltar
          </button>

          {/* TÍTULO */}

          <div className="mu-content-header">

            <div className="mu-title">

              <h1>
                Editar atividade
              </h1>

              <p>
                Atualize as informações da atividade.
              </p>

            </div>

          </div>

          {/* MENSAGEM DE ERRO */}

          {erro && (
            <div
              style={{
                background: "#fff0f0",
                color: "#d64545",
                border: "1px solid #ffd0d0",
                borderRadius: "10px",
                padding: "12px 15px",
                marginBottom: "18px",
                fontSize: "13px",
              }}
            >
              ⚠️ {erro}
            </div>
          )}

          {/* MENSAGEM DE SUCESSO */}

          {sucesso && (
            <div
              style={{
                background: "#e1f8ec",
                color: "#209b62",
                border: "1px solid #bdebd2",
                borderRadius: "10px",
                padding: "12px 15px",
                marginBottom: "18px",
                fontSize: "13px",
              }}
            >
              ✅ {sucesso}
            </div>
          )}

          {/* FORMULÁRIO */}

          <form
            className="mu-card"
            onSubmit={atualizarAtividade}
            style={{
              padding: "25px",
            }}
          >

            {/* TÍTULO */}

            <div className="mu-form-group">

              <label htmlFor="titulo">
                Título da atividade
              </label>

              <input
                id="titulo"
                type="text"
                value={titulo}
                onChange={(event) =>
                  setTitulo(event.target.value)
                }
                placeholder="Digite o título da atividade"
                maxLength={100}
              />

            </div>

            {/* DESCRIÇÃO */}

            <div className="mu-form-group">

              <label htmlFor="descricao">
                Descrição
              </label>

              <textarea
                id="descricao"
                value={descricao}
                onChange={(event) =>
                  setDescricao(event.target.value)
                }
                placeholder="Digite a descrição da atividade"
                maxLength={1000}
              />

            </div>

            {/* PONTUAÇÃO + STATUS */}

            <div className="mu-form-row">

              <div className="mu-form-group">

                <label htmlFor="pontuacao">
                  Pontuação
                </label>

                <input
                  id="pontuacao"
                  type="number"
                  min="1"
                  value={pontuacao}
                  onChange={(event) =>
                    setPontuacao(
                      Number(event.target.value)
                    )
                  }
                />

              </div>

              <div className="mu-form-group">

                <label htmlFor="status">
                  Status
                </label>

                <select
                  id="status"
                  value={status}
                  onChange={(event) =>
                    setStatus(event.target.value)
                  }
                >

                  <option value="PENDENTE">
                    Pendente
                  </option>

                  <option value="EM_ANDAMENTO">
                    Em andamento
                  </option>

                  <option value="CONCLUIDO">
                    Concluído
                  </option>

                </select>

              </div>

            </div>

            {/* TURMA */}

            <div className="mu-form-group">

              <label htmlFor="turma">
                Turma
              </label>

              <select
                id="turma"
                value={turmaId}
                onChange={(event) =>
                  setTurmaId(
                    event.target.value === ""
                      ? ""
                      : Number(event.target.value)
                  )
                }
              >

                <option value="">
                  Selecione uma turma
                </option>

                {turmas.map((turma) => (
                  <option
                    key={turma.id}
                    value={turma.id}
                  >
                    {turma.nome}
                  </option>
                ))}

              </select>

            </div>

            {/* PERGUNTAS */}

            <div
              style={{
                marginTop: "25px",
                paddingTop: "20px",
                borderTop: "1px solid #eeeeF5",
              }}
            >

              <h2
                style={{
                  fontSize: "16px",
                  color: "#29293f",
                  marginBottom: "6px",
                }}
              >
                Perguntas
              </h2>

              <p
                style={{
                  color: "#9292a8",
                  fontSize: "12px",
                  marginBottom: "15px",
                }}
              >
                Esta tela mantém as perguntas já cadastradas.
              </p>

              {atividade.perguntas &&
              atividade.perguntas.length > 0 ? (

                <div>

                  {atividade.perguntas.map(
                    (pergunta, index) => (

                      <div
                        key={
                          pergunta.id ?? index
                        }
                        style={{
                          background: "#f7f7fc",
                          border: "1px solid #e7e7f0",
                          borderRadius: "10px",
                          padding: "15px",
                          marginBottom: "10px",
                        }}
                      >

                        <strong
                          style={{
                            color: "#6246ff",
                            fontSize: "12px",
                          }}
                        >
                          Pergunta {index + 1}
                        </strong>

                        <p
                          style={{
                            marginTop: "6px",
                            color: "#38384e",
                            fontSize: "13px",
                          }}
                        >
                          {pergunta.enunciado}
                        </p>

                        {pergunta.alternativas &&
                          pergunta.alternativas.length >
                            0 && (

                            <div
                              style={{
                                marginTop: "10px",
                              }}
                            >

                              {pergunta.alternativas.map(
                                (
                                  alternativa,
                                  alternativaIndex
                                ) => (

                                  <div
                                    key={
                                      alternativa.id ??
                                      alternativaIndex
                                    }
                                    style={{
                                      display: "flex",
                                      alignItems:
                                        "center",
                                      gap: "8px",
                                      fontSize: "12px",
                                      color:
                                        alternativa.correta
                                          ? "#209b62"
                                          : "#55556b",
                                      marginBottom:
                                        "5px",
                                    }}
                                  >

                                    <span>
                                      {alternativa.correta
                                        ? "✓"
                                        : "•"}
                                    </span>

                                    <span>
                                      {
                                        alternativa.texto
                                      }
                                    </span>

                                  </div>

                                )
                              )}

                            </div>

                          )}

                      </div>

                    )
                  )}

                </div>

              ) : (

                <p
                  style={{
                    color: "#9999ad",
                    fontSize: "12px",
                  }}
                >
                  Nenhuma pergunta cadastrada.
                </p>

              )}

            </div>

            {/* BOTÕES */}

            <div className="mu-modal-footer">

              <button
                type="button"
                className="mu-btn mu-btn-secondary"
                onClick={() =>
                  navigate(
                    `/administrador/atividades/${id}`
                  )
                }
              >
                Cancelar
              </button>

              <button
                type="submit"
                className="mu-btn mu-btn-primary"
                disabled={salvando}
              >

                {salvando
                  ? "Salvando..."
                  : "💾 Salvar alterações"}

              </button>

            </div>

          </form>

        </div>

      </main>
    </div>
  );
}

export default EditarAtividadeAdministrador;