import { Navigate } from "react-router-dom";
import { useAuth } from "../context/authContext";

interface PrivateRouteProps {
    children: React.ReactNode;
    permitido?: string[];
}

function PrivateRoute({
    children,
    permitido
}: PrivateRouteProps) {

    const { usuario } = useAuth();

    if (!usuario) {
        return <Navigate to="/login" replace />;
    }

    if (
        permitido &&
        !permitido.includes(usuario.tipoUsuario)
    ) {
        return <Navigate to="/login" replace />;
    }

    return children;
}

export default PrivateRoute;