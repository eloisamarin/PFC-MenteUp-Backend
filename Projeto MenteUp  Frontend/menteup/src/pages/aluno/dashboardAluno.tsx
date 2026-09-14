import { useNavigate } from "react-router-dom";

function DashboardAluno() {

    const navigate = useNavigate();

    return (
        <div>

            <h1>Atividades</h1>

            <p>Gerenciamento de atividades.</p>

            <button
                onClick={() => navigate("/aluno/atividades")}
            >
                Ver atividades
            </button>

        </div>
    );
}

export default DashboardAluno;