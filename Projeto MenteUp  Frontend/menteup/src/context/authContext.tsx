import { createContext, useContext, useState } from "react";

type TipoUsuario = "ALUNO" | "PROFESSOR" | "ADMINISTRADOR";

interface Usuario {
    id: number;
    nome: string;
    usuario: string;
    tipoUsuario: TipoUsuario;
}

interface AuthContextType {
    usuario: Usuario | null;
    login: (usuario: Usuario) => void;
    logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {

    const [usuario, setUsuario] = useState<Usuario | null>(() => {
        const usuarioSalvo = localStorage.getItem("usuario");

        return usuarioSalvo
            ? JSON.parse(usuarioSalvo)
            : null;
    });

    function login(usuario: Usuario) {
        setUsuario(usuario);
        localStorage.setItem("usuario", JSON.stringify(usuario));
    }

    function logout() {
        setUsuario(null);
        localStorage.removeItem("usuario");
    }

    return (
        <AuthContext.Provider value={{ usuario, login, logout }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {

    const context = useContext(AuthContext);

    if (!context) {
        throw new Error("useAuth deve ser usado dentro de AuthProvider");
    }

    return context;
}