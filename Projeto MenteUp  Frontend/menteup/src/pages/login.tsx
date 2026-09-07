import { useState } from "react";
import { useNavigate } from "react-router-dom";

function Login() {
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");
  const [erro, setErro] = useState("");

  function handleLogin(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    setErro("");

    if (!email || !senha) {
      setErro("Preencha todos os campos.");
      return;
    }

    // Temporário
    navigate("/atividade");
  }

  function handleCadastro() {
    navigate("/cadastro");
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
            <h1>Bem-vindo!</h1>

            <p>
              Entre para sua jornada de aprendizado
            </p>
          </div>

          {/* FORMULÁRIO */}
          <form
            className="mu-login-form"
            onSubmit={handleLogin}
          >

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

              <div className="mu-login-password-header">

                <label htmlFor="senha">
                  Senha
                </label>

                <button
                  type="button"
                  className="mu-login-forgot"
                >
                  Esqueceu a senha?
                </button>

              </div>

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

            {/* ERRO */}
            {erro && (
              <div className="mu-login-error">
                {erro}
              </div>
            )}

            {/* ENTRAR */}
            <button
              type="submit"
              className="mu-btn mu-btn-primary mu-btn-lg mu-login-submit"
            >
              Entrar
            </button>

          </form>

          {/* CADASTRO */}
          <div className="mu-login-register">

            Ainda não possui uma conta?{" "}

            <button
              type="button"
              className="mu-btn mu-btn-ghost"
              onClick={handleCadastro}
            >
              Criar conta
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

export default Login;