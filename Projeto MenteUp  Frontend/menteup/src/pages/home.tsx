import React, { useState } from "react";
import "../App.css";
import {useNavigate} from "react-router-dom";

/* ============================================================
   ÍCONES (inline SVG, tipados como componentes React)
   ============================================================ */
type IconProps = { size?: number; className?: string };

const IconZap: React.FC<IconProps> = ({ size = 18, className }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor" className={className}>
    <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
  </svg>
);

const IconTarget: React.FC<IconProps> = ({ size = 18, className }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} className={className}>
    <circle cx="12" cy="12" r="10" />
    <circle cx="12" cy="12" r="6" />
    <circle cx="12" cy="12" r="2" />
  </svg>
);

const IconAward: React.FC<IconProps> = ({ size = 18, className }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" className={className}>
    <circle cx="12" cy="8" r="6" />
    <path d="M8.7 13.9L7 23l5-3 5 3-1.7-9.1" />
  </svg>
);

const IconBarChart: React.FC<IconProps> = ({ size = 18, className }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" className={className}>
    <line x1="18" y1="20" x2="18" y2="10" />
    <line x1="12" y1="20" x2="12" y2="4" />
    <line x1="6" y1="20" x2="6" y2="14" />
  </svg>
);

const IconGraduation: React.FC<IconProps> = ({ size = 18, className }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" className={className}>
    <path d="M22 10L12 5 2 10l10 5 10-5z" />
    <path d="M6 12v5c0 1.66 2.69 3 6 3s6-1.34 6-3v-5" />
  </svg>
);

const IconUsers: React.FC<IconProps> = ({ size = 18, className }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" className={className}>
    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
    <circle cx="9" cy="7" r="4" />
    <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
    <path d="M16 3.13a4 4 0 0 1 0 7.75" />
  </svg>
);

const IconFlame: React.FC<IconProps> = ({ size = 18, className }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor" className={className}>
    <path d="M12 2s-6 6-6 12a6 6 0 0 0 12 0c0-2-1-3-1-3s-1 2-2.5 2c1-2-1-4-1-6 0 2-2 3-2 5-1-1-1-3.5.5-6 .5-1.5 0-3 0-4z" />
  </svg>
);

const IconCheck: React.FC<IconProps> = ({ size = 16, className }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={3} strokeLinecap="round" strokeLinejoin="round" className={className}>
    <polyline points="20 6 9 17 4 12" />
  </svg>
);

const IconChevron: React.FC<IconProps> = ({ size = 16, className }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2.5} strokeLinecap="round" strokeLinejoin="round" className={className}>
    <polyline points="9 18 15 12 9 6" />
  </svg>
);

/* ============================================================
   TIPOS
   ============================================================ */
type UserType = "estudante" | "professor";

export interface HomeProps {
  /** Chamado quando o usuário clica em "Entrar" */
  onLogin?: () => void;
  /** Chamado quando o usuário clica em "Criar conta grátis" */
  onSignup?: (userType: UserType) => void;
}

interface Feature {
  icon: React.FC<IconProps>;
  title: string;
  description: string;
  accent: "blue" | "purple" | "gold" | "green";
}

interface Step {
  number: string;
  title: string;
  description: string;
}

/* ============================================================
   DADOS ESTÁTICOS DA PÁGINA
   ============================================================ */
const FEATURES: Feature[] = [
  {
    icon: IconZap,
    title: "Ganhe XP a cada atividade",
    description: "Cada exercício concluído rende pontos de experiência que fazem você subir de nível.",
    accent: "gold",
  },
  {
    icon: IconTarget,
    title: "Desafios",
    description: "Complete metas desafios diários e semanais para ganhar recompensas extras.",
    accent: "blue",
  },
  {
    icon: IconAward,
    title: "Conquistas",
    description: "Desbloqueie selos exclusivos conforme evolui.",
    accent: "purple",
  },
  {
    icon: IconBarChart,
    title: "Ranking da turma",
    description: "Acompanhe sua posição entre os colegas e acompanhe sua evolução em tempo real.",
    accent: "green",
  },
];

const STEPS: Step[] = [
  { number: "01", title: "Crie sua conta", description: "Cadastre-se como estudante ou professor." },
  { number: "02", title: "Complete atividades", description: "Resolva quizzes e exercícios das suas disciplinas .favoritas" },
  { number: "03", title: "Evolua e conquiste", description: "Ganhe XP, suba de nível e desbloqueie conquistas exclusivas." },
];

/* ============================================================
   COMPONENTE PRINCIPAL
   ============================================================ */
const Home: React.FC<HomeProps> = ({ onLogin, onSignup }) => {
  const [activeAudience, setActiveAudience] = useState<UserType>("estudante");
  const navigate = useNavigate();
  const handleLogin = () => {
    navigate("/login");
  }
  const handleSignupClick = (): void => {
    onSignup?.(activeAudience);
  };

  return (
    <div className="mu-page">
      {/* ===================== HEADER ===================== */}
      <header className="mu-header">
        <div className="mu-container mu-header-inner">
          <div className="mu-brand">
            <div className="mu-brand-mark">M</div>
            <span className="mu-brand-name">
              Mente<span>Up</span>
            </span>
          </div>
          <nav className="mu-nav">
            <a href="#recursos">Recursos</a>
            <a href="#como-funciona">Como funciona</a>
            <a href="#professores">Para professores</a>
          </nav>
          <div className="mu-header-actions">
            <button className="mu-btn mu-btn-ghost" onClick={handleLogin}>
              Entrar
            </button>
            <button className="mu-btn mu-btn-primary" onClick={handleSignupClick}>
              Criar conta 
            </button>
          </div>
        </div>
      </header>

      {/* ===================== HERO ===================== */}
      <section className="mu-hero">
        <div className="mu-container mu-hero-inner">
          <div className="mu-hero-copy">
            <span className="mu-eyebrow">Plataforma de gamificação educacional</span>
            <h1 className="mu-hero-title">
              Aprenda. <span className="mu-grad-text">Conquiste.</span> Evolua.
            </h1>
            <p className="mu-hero-sub">
              O MenteUp transforma o estudo em uma jornada com XP, missões e conquistas —
              e dá aos professores uma visão clara do desempenho de cada turma.
            </p>

            <div className="mu-audience-toggle" role="tablist" aria-label="Escolha seu perfil">
              <button
                role="tab"
                aria-selected={activeAudience === "estudante"}
                className={`mu-audience-opt ${activeAudience === "estudante" ? "active" : ""}`}
                onClick={() => setActiveAudience("estudante")}
              >
                <IconGraduation size={17} /> Sou estudante
              </button>
              <button
                role="tab"
                aria-selected={activeAudience === "professor"}
                className={`mu-audience-opt ${activeAudience === "professor" ? "active" : ""}`}
                onClick={() => setActiveAudience("professor")}
              >
                <IconUsers size={17} /> Sou professor
              </button>
            </div>

            <div className="mu-hero-actions">
              <button className="mu-btn mu-btn-primary mu-btn-lg" onClick={handleSignupClick}>
                Começar agora <IconChevron size={16} />
              </button>
              <button className="mu-btn mu-btn-secondary mu-btn-lg" onClick={handleLogin}>
                Já tenho conta
              </button>
            </div>
          </div>

          {/* Painel visual — resumo gamificado (sem dados reais de usuário) */}
          <div className="mu-hero-panel" aria-hidden="true">
            <div className="mu-panel-card mu-panel-level">
              <div className="mu-ring-wrap">
                <svg width="86" height="86" viewBox="0 0 86 86">
                  <circle cx="43" cy="43" r="37" fill="none" stroke="#EDEFFB" strokeWidth="7" />
                  <circle
                    cx="43"
                    cy="43"
                    r="37"
                    fill="none"
                    stroke="url(#muRingGrad)"
                    strokeWidth="7"
                    strokeLinecap="round"
                    strokeDasharray="232"
                    strokeDashoffset="42"
                    transform="rotate(-90 43 43)"
                  />
                  <defs>
                    <linearGradient id="muRingGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                      <stop offset="0%" stopColor="#3652FF" />
                      <stop offset="100%" stopColor="#7C3AED" />
                    </linearGradient>
                  </defs>
                </svg>
                <div className="mu-ring-center">
                  <b>Nv. 8</b>
                  <span>82%</span>
                </div>
              </div>
              <div>
                <div className="mu-panel-title">Nível atual</div>
                <div className="mu-panel-sub">1.240 / 1.500 XP</div>
              </div>
            </div>

            <div className="mu-panel-card mu-panel-streak">
              <div className="mu-panel-icon gold">
                <IconFlame size={20} />
              </div>
              <div>
                <div className="mu-panel-title">12 dias</div>
                <div className="mu-panel-sub">de sequência de estudos</div>
              </div>
            </div>

            <div className="mu-panel-card mu-panel-badge">
              <div className="mu-hex">
                <IconAward size={26} />
              </div>
              <div>
                <div className="mu-panel-title">Mestre do Quiz</div>
                <div className="mu-panel-sub">Nova conquista desbloqueada</div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ===================== FEATURES ===================== */}
      <section id="recursos" className="mu-section">
        <div className="mu-container">
          <span className="mu-eyebrow mu-center">Recursos</span>
          <h2 className="mu-section-title mu-center">Feito para engajar de verdade</h2>
          <p className="mu-section-sub mu-center">
            Cada elemento da plataforma foi pensado para transformar tarefas escolares em progresso visível.
          </p>
          <div className="mu-features-grid">
            {FEATURES.map((feature) => {
              const Icon = feature.icon;
              return (
                <div key={feature.title} className="mu-feature-card">
                  <div className={`mu-feature-icon ${feature.accent}`}>
                    <Icon size={20} />
                  </div>
                  <h3>{feature.title}</h3>
                  <p>{feature.description}</p>
                </div>
              );
            })}
          </div>
        </div>
      </section>

      {/* ===================== COMO FUNCIONA ===================== */}
      <section id="como-funciona" className="mu-section mu-section-alt">
        <div className="mu-container">
          <span className="mu-eyebrow mu-center">Como funciona</span>
          <h2 className="mu-section-title mu-center">Três passos até sua primeira conquista</h2>
          <div className="mu-steps-row">
            {STEPS.map((step) => (
              <div key={step.number} className="mu-step-card">
                <span className="mu-step-number">{step.number}</span>
                <h3>{step.title}</h3>
                <p>{step.description}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ===================== PARA PROFESSORES ===================== */}
      <section id="professores" className="mu-section">
        <div className="mu-container mu-teacher-split">
          <div>
            <span className="mu-eyebrow">Para professores</span>
            <h2 className="mu-section-title">Acompanhe o desempenho sem esforço</h2>
            <p className="mu-section-sub" style={{ textAlign: "left", margin: "12px 0 20px" }}>
              Crie atividades, veja quem está com baixa participação e acompanhe a evolução de cada
              turma em um painel único.
            </p>
            <ul className="mu-teacher-list">
              <li>
                <IconCheck className="mu-check" /> Criação rápida de atividades e quizzes
              </li>
              <li>
                <IconCheck className="mu-check" /> Alertas automáticos de baixa participação
              </li>
              <li>
                <IconCheck className="mu-check" /> Ranking e relatórios por turma
              </li>
            </ul>
            <button className="mu-btn mu-btn-primary" onClick={onLogin}>
              Acessar painel do professor
            </button>
          </div>
          <div className="mu-teacher-visual" aria-hidden="true">
            <div className="mu-mini-stat">
              <div className="mu-panel-icon blue">
                <IconUsers size={18} />
              </div>
              <div>
                <b>118</b>
                <span>estudantes ativos</span>
              </div>
            </div>
            <div className="mu-mini-stat">
              <div className="mu-panel-icon green">
                <IconCheck size={18} />
              </div>
              <div>
                <b>86%</b>
                <span>taxa média de conclusão</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ===================== CTA FINAL ===================== */}
      <section className="mu-cta">
        <div className="mu-container mu-cta-inner">
          <h2>Pronto para começar sua jornada?</h2>
          <p>Crie sua conta gratuita e dê o primeiro passo hoje mesmo.</p>
          <button className="mu-btn mu-btn-cta" onClick={handleSignupClick}>
            Criar minha conta <IconChevron size={16} />
          </button>
        </div>
      </section>

      {/* ===================== FOOTER ===================== */}
      <footer className="mu-footer">
        <div className="mu-container mu-footer-inner">
          <div className="mu-brand">
            <div className="mu-brand-mark small">M</div>
            <span className="mu-brand-name small">
              Mente<span>Up</span>
            </span>
          </div>
          <p>© {new Date().getFullYear()} MenteUp. Todos os direitos reservados.</p>
        </div>
      </footer>
    </div>
  );
};

export default Home;
