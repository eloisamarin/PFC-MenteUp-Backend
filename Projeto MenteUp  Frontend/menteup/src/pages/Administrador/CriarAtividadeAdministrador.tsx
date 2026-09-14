import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { apiFetch } from "../../services/api";

interface Turma {
  id: number;
  nome: string;
}

interface Alternativa {
  texto: string;
  correta: boolean;
}

interface Pergunta {
  enunciado: string;
  alternativas: Alternativa[];
}

function CriarAtividadeAdministrador() {
  const navigate = useNavigate();

  const [titulo, setTitulo] = useState("");
  const [descricao, setDescricao] = useState("");
  const [pontuacao, setPontuacao] = useState(10);

  const [turmas, setTurmas] = useState<Turma[]>([]);
  const [turmaId, setTurmaId] = useState("");

  const [perguntas, setPerguntas] = useState<Pergunta[]>([
    {
      enunciado: "",
      alternativas: [
        { texto: "", correta: false },
        { texto: "", correta: false },
        { texto: "", correta: false },
        { texto: "", correta: false },
      ],
    },
  ]);

  const [carregandoTurmas, setCarregandoTurmas] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState("");

  // =========================================================
  // CARREGAR TURMAS
  // =========================================================

  useEffect(() => {
    const carregarTurmas = async () => {
      try {
        setCarregandoTurmas(true);

        const response = await apiFetch("/turmas/all");

        if (!response.ok) {
          throw new Error("Erro ao buscar turmas");
        }

        const data = await response.json();

        console.log("Turmas recebidas:", data);

        setTurmas(data);
      } catch (error) {
        console.error("Erro ao carregar turmas:", error);
        setErro("Erro ao carregar turmas. Tente novamente.");
      } finally {
        setCarregandoTurmas(false);
      }
    };

    carregarTurmas();
  }, []);

  // =========================================================
  // PERGUNTAS
  // =========================================================

  function adicionarPergunta() {
    setPerguntas([
      ...perguntas,
      {
        enunciado: "",
        alternativas: [
          { texto: "", correta: false },
          { texto: "", correta: false },
        ],
      },
    ]);
  }

  function removerPergunta(index: number) {
    if (perguntas.length === 1) {
      alert("A atividade precisa ter pelo menos uma pergunta.");
      return;
    }

    setPerguntas(perguntas.filter((_, i) => i !== index));
  }

  function atualizarEnunciado(
    perguntaIndex: number,
    valor: string
  ) {
    const novasPerguntas = [...perguntas];

    novasPerguntas[perguntaIndex].enunciado = valor;

    setPerguntas(novasPerguntas);
  }

  // =========================================================
  // ALTERNATIVAS
  // =========================================================

  function adicionarAlternativa(perguntaIndex: number) {
    const novasPerguntas = [...perguntas];

    novasPerguntas[perguntaIndex].alternativas.push({
      texto: "",
      correta: false,
    });

    setPerguntas(novasPerguntas);
  }

  function removerAlternativa(
    perguntaIndex: number,
    alternativaIndex: number
  ) {
    const novasPerguntas = [...perguntas];

    if (
      novasPerguntas[perguntaIndex].alternativas.length <= 2
    ) {
      alert("A pergunta precisa ter pelo menos 2 alternativas.");
      return;
    }

    novasPerguntas[perguntaIndex].alternativas.splice(
      alternativaIndex,
      1
    );

    setPerguntas(novasPerguntas);
  }

  function atualizarAlternativa(
    perguntaIndex: number,
    alternativaIndex: number,
    valor: string
  ) {
    const novasPerguntas = [...perguntas];

    novasPerguntas[perguntaIndex].alternativas[
      alternativaIndex
    ].texto = valor;

    setPerguntas(novasPerguntas);
  }

  function marcarRespostaCorreta(
    perguntaIndex: number,
    alternativaIndex: number
  ) {
    const novasPerguntas = [...perguntas];

    novasPerguntas[perguntaIndex].alternativas =
      novasPerguntas[perguntaIndex].alternativas.map(
        (alternativa, index) => ({
          ...alternativa,
          correta: index === alternativaIndex,
        })
      );

    setPerguntas(novasPerguntas);
  }

  // =========================================================
  // SALVAR
  // =========================================================

  async function salvarAtividade() {
    try {
      setErro("");

      if (!titulo.trim()) {
        alert("Informe o título da atividade.");
        return;
      }

      if (!descricao.trim()) {
        alert("Informe a descrição da atividade.");
        return;
      }

      if (!turmaId) {
        alert("Selecione uma turma.");
        return;
      }

      for (let i = 0; i < perguntas.length; i++) {
        if (!perguntas[i].enunciado.trim()) {
          alert(`Informe o enunciado da pergunta ${i + 1}.`);
          return;
        }

        if (perguntas[i].alternativas.length < 2) {
          alert(
            `A pergunta ${i + 1} precisa ter pelo menos 2 alternativas.`
          );
          return;
        }

        const temRespostaCorreta =
          perguntas[i].alternativas.some(
            (alternativa) => alternativa.correta
          );

        if (!temRespostaCorreta) {
          alert(
            `Marque a resposta correta da pergunta ${i + 1}.`
          );
          return;
        }

        for (
          let j = 0;
          j < perguntas[i].alternativas.length;
          j++
        ) {
          if (
            !perguntas[i].alternativas[j].texto.trim()
          ) {
            alert(
              `Preencha a alternativa ${j + 1} da pergunta ${i + 1}.`
            );
            return;
          }
        }
      }

      setSalvando(true);

      // =================================================
      // 1 - CRIA A ATIVIDADE
      // =================================================

      const atividadeResponse = await apiFetch("/atividades", {
        method: "POST",
        body: JSON.stringify({
          titulo: titulo.trim(),
          descricao: descricao.trim(),
          pontuacao: Number(pontuacao),
          status: "PENDENTE",
          turma: {
            id: Number(turmaId),
          },
        }),
      });

      if (!atividadeResponse.ok) {
        const erroBackend =
          await atividadeResponse.text();

        console.error(erroBackend);

        throw new Error(
          "Não foi possível criar a atividade."
        );
      }

      const atividade =
        await atividadeResponse.json();

      console.log(
        "Atividade criada:",
        atividade
      );

      // =================================================
      // 2 - CRIA PERGUNTAS E ALTERNATIVAS
      // =================================================

      const questionarioResponse = await apiFetch(
        `/atividades/${atividade.id}/questionario`,
        {
          method: "POST",
          body: JSON.stringify({
            perguntas: perguntas.map((pergunta) => ({
              enunciado: pergunta.enunciado.trim(),
              alternativas: pergunta.alternativas.map((alternativa) => ({
                texto: alternativa.texto.trim(),
                correta: alternativa.correta,
              })),
            })),
          }),
        }
      );

      if (!questionarioResponse.ok) {
        const erroQuestionario =
          await questionarioResponse.text();

        console.error(
          erroQuestionario
        );

        throw new Error(
          "A atividade foi criada, mas houve um erro ao salvar as perguntas."
        );
      }

      alert("Atividade criada com sucesso!");

      navigate("/administrador/atividades");

    } catch (error) {
      console.error(
        "Erro ao salvar atividade:",
        error
      );

      if (error instanceof Error) {
        alert(error.message);
      } else {
        alert(
          "Erro ao salvar a atividade."
        );
      }
    } finally {
      setSalvando(false);
    }
  }

  // =========================================================
  // TELA
  // =========================================================

  return (
    <div className="criar-atividade-container">

      <button
        type="button"
        onClick={() =>
          navigate("/administrador/atividades")
        }
        className="botao-voltar"
      >
        ← Voltar
      </button>

      <h1>Criar atividade</h1>

      <p>
        Cadastre uma nova atividade para uma turma.
      </p>

      {erro && (
        <div className="erro-turmas">
          ⚠️ {erro}
        </div>
      )}

      {/* ================================================= */}
      {/* DADOS DA ATIVIDADE */}
      {/* ================================================= */}

      <section className="card-atividade">

        <h2>Dados da atividade</h2>

        <label>
          Título da atividade
        </label>

        <input
          type="text"
          value={titulo}
          onChange={(e) =>
            setTitulo(e.target.value)
          }
          placeholder="Digite o título da atividade"
        />

        <label>
          Descrição da atividade
        </label>

        <textarea
          value={descricao}
          onChange={(e) =>
            setDescricao(e.target.value)
          }
          placeholder="Digite a descrição da atividade"
        />

        <label>
          Pontuação
        </label>

        <input
          type="number"
          min="1"
          value={pontuacao}
          onChange={(e) =>
            setPontuacao(
              Number(e.target.value)
            )
          }
        />

        <label>
          Turma
        </label>

        {carregandoTurmas ? (
          <p>Carregando turmas...</p>
        ) : turmas.length === 0 ? (
          <p>
            ⚠️ Nenhuma turma encontrada.
          </p>
        ) : (
          <select
            value={turmaId}
            onChange={(e) =>
              setTurmaId(e.target.value)
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
        )}

      </section>

      {/* ================================================= */}
      {/* PERGUNTAS */}
      {/* ================================================= */}

      <section className="card-atividade">

        <h2>Perguntas</h2>

        <p>
          Adicione as perguntas e escolha a
          resposta correta.
        </p>

        {perguntas.map(
          (pergunta, perguntaIndex) => (

            <div
              className="pergunta-card"
              key={perguntaIndex}
            >

              <div className="pergunta-header">

                <h3>
                  Pergunta{" "}
                  {perguntaIndex + 1}
                </h3>

                {perguntas.length > 1 && (
                  <button
                    type="button"
                    onClick={() =>
                      removerPergunta(
                        perguntaIndex
                      )
                    }
                  >
                    ✕
                  </button>
                )}

              </div>

              <label>
                Enunciado da pergunta
              </label>

              <textarea
                value={
                  pergunta.enunciado
                }
                onChange={(e) =>
                  atualizarEnunciado(
                    perguntaIndex,
                    e.target.value
                  )
                }
                placeholder="Digite a pergunta"
              />

              <h4>
                Alternativas
              </h4>

              <p>
                Marque a opção que
                representa a resposta
                correta.
              </p>

              {pergunta.alternativas.map(
                (
                  alternativa,
                  alternativaIndex
                ) => (

                  <div
                    className="alternativa"
                    key={
                      alternativaIndex
                    }
                  >

                    <input
                      type="radio"
                      name={`correta-${perguntaIndex}`}
                      checked={
                        alternativa.correta
                      }
                      onChange={() =>
                        marcarRespostaCorreta(
                          perguntaIndex,
                          alternativaIndex
                        )
                      }
                    />

                    <input
                      type="text"
                      value={
                        alternativa.texto
                      }
                      onChange={(e) =>
                        atualizarAlternativa(
                          perguntaIndex,
                          alternativaIndex,
                          e.target.value
                        )
                      }
                      placeholder={`Alternativa ${alternativaIndex +
                        1
                        }`}
                    />

                    <button
                      type="button"
                      onClick={() =>
                        removerAlternativa(
                          perguntaIndex,
                          alternativaIndex
                        )
                      }
                    >
                      ✕
                    </button>

                  </div>
                )
              )}

              <button
                type="button"
                onClick={() =>
                  adicionarAlternativa(
                    perguntaIndex
                  )
                }
                className="botao-secundario"
              >
                + Adicionar alternativa
              </button>

            </div>
          )
        )}

        <button
          type="button"
          onClick={adicionarPergunta}
          className="botao-secundario"
        >
          + Adicionar pergunta
        </button>

      </section>

      {/* ================================================= */}
      {/* BOTÕES */}
      {/* ================================================= */}

      <div className="botoes-acoes">

        <button
          type="button"
          onClick={() =>
            navigate(
              "/administrador/atividades"
            )
          }
        >
          Cancelar
        </button>

        <button
          type="button"
          onClick={salvarAtividade}
          disabled={salvando}
        >
          {salvando
            ? "Salvando..."
            : "Salvar atividade"}
        </button>

      </div>

    </div>
  );
}

export default CriarAtividadeAdministrador;