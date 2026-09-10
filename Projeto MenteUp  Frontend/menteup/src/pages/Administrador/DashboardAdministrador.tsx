import { useNavigate } from "react-router-dom";

function DashboardAdministrador() {

    const navigate = useNavigate();

    return (
        <div>

            <h1>Atividades</h1>

            <p>Gerenciamento de atividades.</p>

            <button
                onClick={() => navigate("/administrador/atividades")}
            >
                Ver atividades
            </button>

        </div>
    );
}

export default DashboardAdministrador;