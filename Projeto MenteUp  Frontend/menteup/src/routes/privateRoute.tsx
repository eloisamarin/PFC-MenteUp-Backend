import { Navigate } from "react-router-dom";

interface PrivateRouteProps {
    children: React.ReactNode;
    permitido: string[];
}

function PrivateRoute({
    children,
    permitido
}: PrivateRouteProps) {

    const token = localStorage.getItem("token");
    const usuarioSalvo = localStorage.getItem("usuario");

    console.log("=== PRIVATE ROUTE ===");
    console.log("Token:", token);
    console.log("Usuário:", usuarioSalvo);
    console.log("Permitido:", permitido);

    // Não está logado
    if (!token || !usuarioSalvo) {
        console.log("❌ Sem token ou usuário");
        return <Navigate to="/login" replace />;
    }

    let usuario;

    try {
        usuario = JSON.parse(usuarioSalvo);
    } catch (error) {
        console.error("❌ Erro ao ler usuário:", error);

        localStorage.removeItem("token");
        localStorage.removeItem("usuario");

        return <Navigate to="/login" replace />;
    }

    console.log("Tipo do usuário:", usuario.tipoUsuario);

    // Verifica permissão
    if (!permitido.includes(usuario.tipoUsuario)) {
        console.log("❌ Usuário sem permissão");
        return <Navigate to="/login" replace />;
    }

    console.log("✅ Acesso permitido");

    return <>{children}</>;
}

export default PrivateRoute;