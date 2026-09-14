import { useNavigate, useParams } from "react-router-dom";
import { useState } from "react";

function DeletarAtividadeProfessor() {

  const { id } = useParams();
  const navigate = useNavigate();

  const [deletando, setDeletando] = useState(false);
  const [erro, setErro] = useState("");

  const deletarAtividade = async () => {

    try {

      setDeletando(true);
      setErro("");

      // Recupera o usuário logado
      const usuario = localStorage.getItem("usuario");

      if (!usuario) {
        navigate("/login");
        return;
      }

      const dadosUsuario = JSON.parse(usuario);

      const token = dadosUsuario.token;

      if (!token) {
        navigate("/login");
        return;
      }

      // Requisição DELETE
      const response = await fetch(
        `http://localhost:8080/atividades/${id}`,
        {
          method: "DELETE",
          headers: {
            Authorization: token,
            "Content-Type": "application/json",
          },
        }
      );

      if (!response.ok) {

        if (response.status === 404) {
          throw new Error("Atividade não encontrada.");
        }

        if (response.status === 403) {
          throw new Error(
            "Você não tem permissão para excluir esta atividade."
          );
        }

        throw new Error(
          "Não foi possível excluir a atividade."
        );
      }

      // Exclusão realizada
      alert("Atividade excluída com sucesso!");

      navigate("/professor/atividades");

    } catch (error) {

      console.error(error);

      if (error instanceof Error) {
        setErro(error.message);
      } else {
        setErro("Erro ao excluir atividade.");
      }

    } finally {

      setDeletando(false);

    }
  };


  const cancelar = () => {
    navigate("/professor/atividades");
  };


  return (
    <div className="mu-dashboard">

      <main className="mu-main">

        <header className="mu-header">

          <div />

          <div className="mu-header-user">

            <div className="mu-avatar">
              PE
            </div>

            <span className="mu-user-name">
              Professora
            </span>

          </div>

        </header>


        <div className="mu-content">

          <div
            className="mu-card"
            style={{
              maxWidth: "550px",
              margin: "60px auto",
              padding: "35px",
              textAlign: "center"
            }}
          >

            <div
              style={{
                fontSize: "50px",
                marginBottom: "15px"
              }}
            >
              🗑️
            </div>


            <h1
              style={{
                fontSize: "24px",
                fontWeight: 700,
                color: "#202038",
                marginBottom: "10px"
              }}
            >
              Excluir atividade?
            </h1>


            <p
              style={{
                color: "#77778e",
                fontSize: "14px",
                lineHeight: 1.6,
                marginBottom: "25px"
              }}
            >
              Tem certeza que deseja excluir esta atividade?
              <br />
              Essa ação não poderá ser desfeita.
            </p>


            {erro && (

              <div
                style={{
                  background: "#fff0f0",
                  border: "1px solid #ffcaca",
                  color: "#d64545",
                  padding: "12px",
                  borderRadius: "8px",
                  marginBottom: "20px",
                  fontSize: "13px"
                }}
              >
                ⚠️ {erro}
              </div>

            )}


            <div
              style={{
                display: "flex",
                justifyContent: "center",
                gap: "12px"
              }}
            >

              <button
                className="mu-btn mu-btn-secondary"
                onClick={cancelar}
                disabled={deletando}
              >
                Cancelar
              </button>


              <button
                className="mu-btn"
                onClick={deletarAtividade}
                disabled={deletando}
                style={{
                  background: "#e34d59",
                  color: "#fff",
                  padding: "11px 20px",
                  borderRadius: "8px",
                  border: "none",
                  fontWeight: 600
                }}
              >
                {deletando
                  ? "Excluindo..."
                  : "Excluir atividade"
                }
              </button>

            </div>

          </div>

        </div>

      </main>

    </div>
  );
}

export default DeletarAtividadeProfessor;