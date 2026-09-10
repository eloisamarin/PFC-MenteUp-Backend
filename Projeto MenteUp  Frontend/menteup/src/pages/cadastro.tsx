import { useState } from "react";
import { useNavigate } from "react-router-dom";

function Cadastro() {
    const navigate = useNavigate();

    const [nome, setNome] = useState("");
    const [email, setEmail] = useState("");
    const [senha, setSenha] = useState("");
    const [confirmarSenha, setConfirmarSenha] = useState("");
    const [tipoUsuario, setTipoUsuario] = useState("aluno");

    const [erro, setErro] = useState("");

    async function handleCadastro(
        event: React.FormEvent<HTMLFormElement>
    ) {
        event.preventDefault();

        setErro("");

        if (!nome || !email || !senha || !confirmarSenha) {
            setErro("Preencha todos os campos.");
            return;
        }

        if (senha.length < 8) {
            setErro("A senha deve ter no mínimo 8 caracteres.");
            return;
        }

        if (senha !== confirmarSenha) {
            setErro("As senhas não coincidem.");
            return;
        }

        try {
            const resposta = await fetch(
                "http://localhost:8080/usuarios/cadastrar",
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                    },
                    body: JSON.stringify({
                        nomeUsuario: nome,
                        usuario: email,
                        senha: senha,
                        tipoUsuario: tipoUsuario.toUpperCase(),
                    }),
                }
            );

            if (resposta.status === 400) {
                setErro("Este e-mail já está cadastrado.");
                return;
            }

            if (!resposta.ok) {
                setErro("Não foi possível criar a conta.");
                return;
            }

            alert("Conta criada com sucesso!");

            navigate("/login");

        } catch (error) {
            console.error("Erro ao cadastrar:", error);
            setErro("Não foi possível conectar ao servidor.");
        }
    }

    return (
        <div className="mu-page">
            <div className="mu-login-page">
                <div className="mu-login-card">

                    {/* LOGO */}
                    <div className="mu-login-brand">
                        <div className="mu-brand">
                            <div className="mu-brand-mark small">
                                M
                            </div>

                            <div className="mu-brand-name small">
                                Mente<span>Up</span>
                            </div>
                        </div>
                    </div>

                    {/* TÍTULO */}
                    <div className="mu-login-title">
                        <h1>Crie sua conta</h1>

                        <p>
                            Comece sua jornada de aprendizado
                        </p>
                    </div>

                    {/* FORMULÁRIO */}
                    <form
                        className="mu-login-form"
                        onSubmit={handleCadastro}
                    >

                        {/* TIPO DE USUÁRIO */}
                        <div className="radio-group">

                            {/* ESTUDANTE */}
                            <label
                                className={`card-option ${
                                    tipoUsuario === "aluno"
                                        ? "active"
                                        : ""
                                }`}
                            >
                                <input
                                    type="radio"
                                    name="tipoUsuario"
                                    value="aluno"
                                    checked={tipoUsuario === "aluno"}
                                    onChange={(e) =>
                                        setTipoUsuario(e.target.value)
                                    }
                                />

                                <span style={{ fontSize: "24px" }}>
                                    🎓
                                </span>

                                <span>
                                    Estudante
                                </span>
                            </label>

                            {/* PROFESSOR */}
                            <label
                                className={`card-option ${
                                    tipoUsuario === "professor"
                                        ? "active"
                                        : ""
                                }`}
                            >
                                <input
                                    type="radio"
                                    name="tipoUsuario"
                                    value="professor"
                                    checked={tipoUsuario === "professor"}
                                    onChange={(e) =>
                                        setTipoUsuario(e.target.value)
                                    }
                                />

                                <span style={{ fontSize: "24px" }}>
                                    🧑‍🏫
                                </span>

                                <span>
                                    Professor
                                </span>
                            </label>

                        </div>

                        {/* NOME */}
                        <div className="mu-login-field">
                            <label htmlFor="nome">
                                Nome completo
                            </label>

                            <input
                                type="text"
                                id="nome"
                                placeholder="Digite seu nome completo"
                                value={nome}
                                onChange={(event) =>
                                    setNome(event.target.value)
                                }
                            />
                        </div>

                        {/* E-MAIL */}
                        <div className="mu-login-field">
                            <label htmlFor="email">
                                E-mail
                            </label>

                            <input
                                type="email"
                                id="email"
                                placeholder="seuemail@escola.com"
                                value={email}
                                onChange={(event) =>
                                    setEmail(event.target.value)
                                }
                            />
                        </div>

                        {/* SENHA */}
                        <div className="mu-login-field">
                            <label htmlFor="senha">
                                Senha
                            </label>

                            <input
                                type="password"
                                id="senha"
                                placeholder="Digite sua senha"
                                value={senha}
                                onChange={(event) =>
                                    setSenha(event.target.value)
                                }
                            />
                        </div>

                        {/* CONFIRMAR SENHA */}
                        <div className="mu-login-field">
                            <label htmlFor="confirmarSenha">
                                Confirmar senha
                            </label>

                            <input
                                type="password"
                                id="confirmarSenha"
                                placeholder="Digite sua senha novamente"
                                value={confirmarSenha}
                                onChange={(event) =>
                                    setConfirmarSenha(event.target.value)
                                }
                            />
                        </div>

                        {/* ERRO */}
                        {erro && (
                            <div className="mu-login-error">
                                {erro}
                            </div>
                        )}

                        {/* CADASTRAR */}
                        <button
                            type="submit"
                            className="mu-btn mu-btn-primary mu-btn-lg mu-login-submit"
                        >
                            Criar conta
                        </button>

                    </form>

                    {/* LOGIN */}
                    <div className="mu-login-register">
                        Já possui uma conta?{" "}

                        <button
                            type="button"
                            className="mu-btn mu-btn-ghost"
                            onClick={() => navigate("/login")}
                        >
                            Entrar
                        </button>
                    </div>

                    {/* VOLTAR */}
                    <button
                        type="button"
                        className="mu-login-back"
                        onClick={() => navigate("/")}
                    >
                        ← Voltar para página inicial
                    </button>

                </div>
            </div>
        </div>
    );
}

export default Cadastro;