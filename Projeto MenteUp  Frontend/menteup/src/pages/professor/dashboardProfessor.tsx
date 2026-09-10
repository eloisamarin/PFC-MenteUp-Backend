import { useNavigate } from "react-router-dom";

function DashboardProfessor() {

    const navigate = useNavigate();

    return (
        <div>

            <h1>Atividades</h1>

            <p>Gerenciamento de atividades.</p>

            <button
                onClick={() => navigate("/professor/atividades")}
            >
                Ver atividades
            </button>

        </div>
    );
}

export default DashboardProfessor;