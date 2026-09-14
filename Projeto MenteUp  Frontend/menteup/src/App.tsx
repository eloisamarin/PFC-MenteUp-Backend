import { Routes, Route } from "react-router-dom";

import Home from "./pages/home";
import Login from "./pages/login";
import Cadastro from "./pages/cadastro";

import DashboardAluno from "./pages/aluno/dashboardAluno";
import AtividadesAluno from "./pages/aluno/atividadesAluno";
import ResponderAtividadesAluno from "./pages/aluno/ResponderAtividadeAluno";

import DashboardProfessor from "./pages/professor/dashboardProfessor.tsx";
import AtividadesProfessor from "./pages/professor/atividadesProfessor";
import Turmas from "./pages/professor/turma";
import VisualizarAtividadeProfessor from "./pages/professor/VisualizarAtividadeProfessor";
import EditarAtividadeProfessor from "./pages/professor/EditarAtividadeProfessor";
import CriarAtividadeProfessor from "./pages/professor/CriarAtividadeProfessor";
import DeletarAtividadeProfessor from "./pages/professor/DeletarAtividadeProfessor";

import DashboardAdmin from "./pages/Administrador/DashboardAdministrador.tsx";
import Usuarios from "./pages/Administrador/usuario";
import VisualizarAtividadeAdministrador from "./pages/Administrador/VisualizarAtividadeAdministrador";
import EditarAtividadeAdministrador from "./pages/Administrador/EditarAtividadeAdministrador";
import DeletarAtividadeAdministrador from "./pages/Administrador/DeletarAtividadeAdministrador";
import CriarAtividadeAdministrador from "./pages/Administrador/CriarAtividadeAdministrador";
import AtividadesAdministrador from "./pages/Administrador/AtividadesAdministrador";

import PrivateRoute from "./routes/privateRoute";

function App() {

    return (
        <Routes>

            {/* PÚBLICO */}
            <Route path="/" element={<Home />} />
            <Route path="/login" element={<Login />} />
            <Route path="/cadastro" element={<Cadastro />} />


            {/* ALUNO */}
            <Route
                path="/aluno/dashboard"
                element={
                    <PrivateRoute permitido={["ALUNO"]}>
                        <DashboardAluno />
                    </PrivateRoute>
                }
            />

            <Route
                path="/aluno/atividades"
                element={
                    <PrivateRoute permitido={["ALUNO"]}>
                        <AtividadesAluno />
                    </PrivateRoute>
                }
            />
            <Route
                path="/aluno/atividades/responder/:id"
                element={
                    <PrivateRoute permitido={["ALUNO"]}>
                        <ResponderAtividadesAluno />
                    </PrivateRoute>
                }
            />


            {/* PROFESSOR */}
            <Route
                path="/professor/dashboard"
                element={
                    <PrivateRoute permitido={["PROFESSOR"]}>
                        <DashboardProfessor />
                    </PrivateRoute>
                }
            />

            <Route
                path="/professor/atividades"
                element={
                    <PrivateRoute permitido={["PROFESSOR"]}>
                        <AtividadesProfessor />
                    </PrivateRoute>
                }
            />

            <Route
                path="/professor/turmas"
                element={
                    <PrivateRoute permitido={["PROFESSOR"]}>
                        <Turmas />
                    </PrivateRoute>
                }
            />
            <Route
                path="/professor/atividades/criar"
                element={
                    <PrivateRoute permitido={["PROFESSOR"]}>
                        <CriarAtividadeProfessor />
                    </PrivateRoute>
                }
            />
            <Route
                path="/professor/atividades/:id"
                element={
                    <PrivateRoute permitido={["PROFESSOR"]}>
                        <VisualizarAtividadeProfessor />
                    </PrivateRoute>
                }
            />
            <Route
                path="/professor/atividades/editar/:id"
                element={
                    <PrivateRoute permitido={["PROFESSOR"]}>
                        <EditarAtividadeProfessor />
                    </PrivateRoute>
                }
            />
            <Route
                path="/professor/atividades/deletar/:id"
                element={<DeletarAtividadeProfessor />}

               /> 

                {/* ADMINISTRADOR */}
            <Route
                path="/administrador/dashboard"
                element={
                    <PrivateRoute permitido={["ADMINISTRADOR"]}>
                        <DashboardAdmin />
                    </PrivateRoute>
                }
            />

            <Route
                path="/administrador/usuarios"
                element={
                    <PrivateRoute permitido={["ADMINISTRADOR"]}>
                        <Usuarios />
                    </PrivateRoute>
                }
            />
            <Route
                path="/administrador/atividades"
                element={
                    <PrivateRoute permitido={["ADMINISTRADOR"]}>
                        <AtividadesAdministrador />
                    </PrivateRoute>
                }
            />
            <Route
                path="/administrador/atividades/criar"
                element={
                    <PrivateRoute permitido={["ADMINISTRADOR"]}>
                        <CriarAtividadeAdministrador />
                    </PrivateRoute>
                }
            />
            <Route
                path="/administrador/atividades/:id"
                element={
                    <PrivateRoute permitido={["ADMINISTRADOR"]}>
                        <VisualizarAtividadeAdministrador />
                    </PrivateRoute>
                }
            />
            <Route
                path="/administrador/atividades/editar/:id"
                element={
                    <PrivateRoute permitido={["ADMINISTRADOR"]}>
                        <EditarAtividadeAdministrador />
                    </PrivateRoute>
                }
            />
            <Route
                path="/administrador/atividades/deletar/:id"
                element={<DeletarAtividadeAdministrador />}

               /> 

        </Routes>
    );
}

export default App;