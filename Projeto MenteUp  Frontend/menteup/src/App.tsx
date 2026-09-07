import { Routes, Route } from "react-router-dom";

import Home from "./pages/home";
import Login from "./pages/login";
import Cadastro from "./pages/cadastro";

import DashboardAluno from "./pages/aluno/dashboardAluno";
import AtividadesAluno from "./pages/aluno/atividadesAluno";

import DashboardProfessor from "./pages/professor/dashboardProfessor.tsx";
import AtividadesProfessor from "./pages/professor/atividadesProfessor";
import Turmas from "./pages/professor/turma";

import DashboardAdmin from "./pages/Administrador/dashboardAdmin";
import Usuarios from "./pages/Administrador/usuario";

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

        </Routes>
    );
}

export default App;