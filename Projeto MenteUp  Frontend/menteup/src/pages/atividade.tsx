import { useNavigate } from "react-router-dom";

function Atividade() {
  const navigate = useNavigate();

  return (
    <div className="mu-dashboard">

      {/* ================= SIDEBAR ================= */}
      <aside className="mu-sidebar">

        {/* LOGO */}
        <div className="mu-sidebar-logo">

          <div className="mu-brand-mark">
            M
          </div>

          <div className="mu-sidebar-brand">
            Mente<span>Up</span>
          </div>

        </div>


        {/* MENU */}
        <nav className="mu-sidebar-menu">

          <button
            className="mu-sidebar-item"
            onClick={() => navigate("/")}
          >
            <span>⌂</span>
            Dashboard
          </button>

          <button className="mu-sidebar-item">
            <span>♧</span>
            Turmas
          </button>

          <button className="mu-sidebar-item active">
            <span>▣</span>
            Atividades
          </button>

          <button className="mu-sidebar-item">
            <span>↗</span>
            Desempenho
          </button>

          <button className="mu-sidebar-item">
            <span>▥</span>
            Ranking
          </button>

          <button className="mu-sidebar-item">
            <span>♙</span>
            Perfil
          </button>

        </nav>

      </aside>


      {/* ================= ÁREA PRINCIPAL ================= */}
      <div className="mu-dashboard-main">

        {/* ================= TOPBAR ================= */}
        <header className="mu-dashboard-topbar">

          {/* BUSCA */}
          <div className="mu-search">

            <span>⌕</span>

            <input
              type="text"
              placeholder="Buscar atividade..."
            />

          </div>


          {/* AÇÕES DO USUÁRIO */}
          <div className="mu-topbar-actions">

            <button className="mu-notification">
              ♧
            </button>

            <div className="mu-topbar-divider"></div>

            <div className="mu-profile">

              <div className="mu-profile-avatar">
                RC
              </div>

              <span>
                Prof. Ricardo Costa
              </span>

            </div>

          </div>

        </header>


        {/* ================= CONTEÚDO ================= */}
        <main className="mu-dashboard-content">

          {/* CABEÇALHO */}
          <div className="mu-activity-header">

            <div>

              <h1>
                Atividades
              </h1>

              <p>
                27 atividades criadas
              </p>

            </div>

            <button
              className="mu-btn mu-btn-primary mu-create-button"
            >
              <span>+</span>
              Criar atividade
            </button>

          </div>


          {/* ================= TABELA ================= */}
          <section className="mu-table-card">

            <table className="mu-activity-table">

              <thead>

                <tr>

                  <th>ATIVIDADE</th>
                  <th>DISCIPLINA</th>
                  <th>DIFICULDADE</th>
                  <th>XP</th>
                  <th>PRAZO</th>
                  <th>RESPOSTAS</th>
                  <th>STATUS</th>
                  <th>AÇÕES</th>

                </tr>

              </thead>

              <tbody>

                {/* ATIVIDADE 1 */}
                <tr>

                  <td>
                    <strong>
                      Equações do 2º Grau
                    </strong>
                  </td>

                  <td>
                    Matemática
                  </td>

                  <td>
                    <span className="mu-difficulty medium">
                      Médio
                    </span>
                  </td>

                  <td>
                    50
                  </td>

                  <td>
                    20/08
                  </td>

                  <td>
                    24/32
                  </td>

                  <td>
                    <span className="mu-status published">
                      Publicada
                    </span>
                  </td>

                  <td>

                    <div className="mu-action-buttons">

                      <button title="Visualizar">
                        ◉
                      </button>

                      <button title="Editar">
                        ✎
                      </button>

                      <button title="Excluir">
                        ▣
                      </button>

                    </div>

                  </td>

                </tr>


                {/* ATIVIDADE 2 */}
                <tr>

                  <td>
                    <strong>
                      Ciclo da Água
                    </strong>
                  </td>

                  <td>
                    Ciências
                  </td>

                  <td>
                    <span className="mu-difficulty easy">
                      Fácil
                    </span>
                  </td>

                  <td>
                    40
                  </td>

                  <td>
                    15/08
                  </td>

                  <td>
                    29/29
                  </td>

                  <td>
                    <span className="mu-status published">
                      Publicada
                    </span>
                  </td>

                  <td>

                    <div className="mu-action-buttons">

                      <button title="Visualizar">
                        ◉
                      </button>

                      <button title="Editar">
                        ✎
                      </button>

                      <button title="Excluir">
                        ▣
                      </button>

                    </div>

                  </td>

                </tr>


                {/* ATIVIDADE 3 */}
                <tr>

                  <td>
                    <strong>
                      Genética Básica
                    </strong>
                  </td>

                  <td>
                    Ciências
                  </td>

                  <td>
                    <span className="mu-difficulty hard">
                      Difícil
                    </span>
                  </td>

                  <td>
                    80
                  </td>

                  <td>
                    28/08
                  </td>

                  <td>
                    —
                  </td>

                  <td>
                    <span className="mu-status draft">
                      Rascunho
                    </span>
                  </td>

                  <td>

                    <div className="mu-action-buttons">

                      <button title="Visualizar">
                        ◉
                      </button>

                      <button title="Editar">
                        ✎
                      </button>

                      <button title="Excluir">
                        ▣
                      </button>

                    </div>

                  </td>

                </tr>

              </tbody>

            </table>

          </section>


          {/* ================= LOG DE VISUALIZAÇÕES ================= */}
          <section className="mu-log-card">

            <h2>
              Log de visualizações — Equações do 2º Grau
            </h2>

            <table className="mu-log-table">

              <thead>

                <tr>

                  <th>ALUNO</th>
                  <th>TURMA</th>
                  <th>VISUALIZOU</th>
                  <th>RESPONDEU</th>

                </tr>

              </thead>

              <tbody>

                <tr>

                  <td>
                    <div className="mu-student">

                      <span className="mu-student-avatar">
                        CR
                      </span>

                      Camila R.
                    </div>
                  </td>

                  <td>
                    8ºA
                  </td>

                  <td>
                    12/08 09:14
                  </td>

                  <td>
                    <span className="mu-response yes">
                      Sim — 09:41
                    </span>
                  </td>

                </tr>


                <tr>

                  <td>
                    <div className="mu-student">

                      <span className="mu-student-avatar">
                        LM
                      </span>

                      Lucas M.
                    </div>
                  </td>

                  <td>
                    8ºA
                  </td>

                  <td>
                    12/08 10:02
                  </td>

                  <td>
                    <span className="mu-response yes">
                      Sim — 10:20
                    </span>
                  </td>

                </tr>


                <tr>

                  <td>
                    <div className="mu-student">

                      <span className="mu-student-avatar">
                        JV
                      </span>

                      João V.
                    </div>
                  </td>

                  <td>
                    8ºA
                  </td>

                  <td>
                    13/08 08:55
                  </td>

                  <td>
                    <span className="mu-response no">
                      Não respondeu
                    </span>
                  </td>

                </tr>

              </tbody>

            </table>

          </section>

        </main>

      </div>

    </div>
  );
}

export default Atividade;