import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { BookOpen, Eye, Pencil, Plus, Search, Trash2 } from "lucide-react";
import { apiFetch } from "../../services/api";

type Atividade = {
  id: number | string;
  titulo: string;
  descricao?: string;
  pontuacao?: number;
  status?: string;
  turma?: { id: number | string; nome?: string };
};

function AtividadesAdministrador() {
  const navigate = useNavigate();
  const [atividades, setAtividades] = useState<Atividade[]>([]);
  const [busca, setBusca] = useState("");
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");

  async function carregarAtividades() {
    try {
      setCarregando(true);
      setErro("");
      const resposta = await apiFetch("/atividades");
      if (!resposta.ok) throw new Error("Erro ao carregar atividades");
      const dados: Atividade[] = await resposta.json();
      setAtividades(
        dados.sort((a, b) => Number(b.id) - Number(a.id))
      );
    } catch (error) {
      console.error("Erro ao carregar atividades:", error);
      setErro("Não foi possível carregar as atividades.");
    } finally {
      setCarregando(false);
    }
  }

  useEffect(() => {
    carregarAtividades();
  }, []);

  const filtradas = atividades.filter((atividade) =>
    atividade.titulo.toLowerCase().includes(busca.toLowerCase())
  );

  return (
    <div className="min-h-screen bg-[#f5f6fc] text-slate-800">
      <aside className="fixed left-0 top-0 h-screen w-56 bg-[#121329] text-white">
        <div className="flex items-center gap-3 px-6 py-6">
          <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-gradient-to-br from-indigo-500 to-purple-600 font-bold">M</div>
          <span className="text-lg font-bold">Mente<span className="text-purple-400">Up</span></span>
        </div>
        <nav className="mt-6 space-y-2 px-3">
          <button onClick={() => navigate("/administrador/dashboard")} className="w-full rounded-lg px-4 py-3 text-left text-sm text-slate-300 hover:bg-white/10">Dashboard</button>
          <button onClick={() => navigate("/administrador/usuarios")} className="w-full rounded-lg px-4 py-3 text-left text-sm text-slate-300 hover:bg-white/10">Usuários</button>
          <button onClick={() => navigate("/administrador/atividades")} className="flex w-full items-center gap-3 rounded-lg bg-white/10 px-4 py-3 text-left text-sm font-medium text-white">
            <BookOpen size={17} /> Atividades
          </button>
        </nav>
      </aside>

      <main className="ml-56 min-h-screen">
        <header className="flex h-20 items-center justify-between border-b bg-white px-8">
          <div className="relative">
            <Search size={17} className="absolute left-4 top-3 text-slate-400" />
            <input value={busca} onChange={(event) => setBusca(event.target.value)} placeholder="Buscar atividade..." className="w-72 rounded-full bg-[#f4f5fb] py-2.5 pl-10 pr-4 text-sm outline-none focus:ring-2 focus:ring-indigo-300" />
          </div>
          <span className="text-sm font-semibold text-slate-600">Administrador</span>
        </header>

        <section className="p-8">
          <div className="mb-7 flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-semibold">Atividades</h1>
              <p className="mt-1 text-sm text-slate-400">{atividades.length} atividades cadastradas</p>
            </div>
            <div className="flex gap-3">
              <button onClick={carregarAtividades} disabled={carregando} className="rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-50 disabled:opacity-60">
                {carregando ? "Atualizando..." : "Atualizar lista"}
              </button>
              <button onClick={() => navigate("/administrador/atividades/criar")} className="flex items-center gap-2 rounded-lg bg-gradient-to-r from-indigo-500 to-purple-600 px-5 py-3 text-sm font-semibold text-white shadow-lg shadow-indigo-200">
                <Plus size={18} /> Criar atividade
              </button>
            </div>
          </div>

          {erro && <div className="mb-5 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-600">{erro}</div>}

          <div className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="border-b bg-slate-50">
                  <tr className="text-left text-xs uppercase text-slate-400">
                    <th className="px-5 py-4">Atividade</th>
                    <th className="px-5 py-4">Turma</th>
                    <th className="px-5 py-4">Pontuação</th>
                    <th className="px-5 py-4">Status</th>
                    <th className="px-5 py-4 text-center">Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {!carregando && filtradas.length === 0 && (
                    <tr><td colSpan={5} className="px-5 py-12 text-center text-sm text-slate-400">Nenhuma atividade encontrada.</td></tr>
                  )}
                  {filtradas.map((atividade) => (
                    <tr key={atividade.id} className="border-b last:border-b-0 hover:bg-slate-50">
                      <td className="px-5 py-4"><p className="font-semibold">{atividade.titulo}</p><p className="text-xs text-slate-400">{atividade.descricao || "Sem descrição"}</p></td>
                      <td className="px-5 py-4 text-sm">{atividade.turma?.nome || "Sem turma"}</td>
                      <td className="px-5 py-4 text-sm">{atividade.pontuacao ?? 0} XP</td>
                      <td className="px-5 py-4 text-sm">{atividade.status || "PENDENTE"}</td>
                      <td className="px-5 py-4">
                        <div className="flex justify-center gap-2">
                          <button title="Visualizar" onClick={() => navigate(`/administrador/atividades/${atividade.id}`)} className="rounded p-2 text-slate-500 hover:bg-indigo-50 hover:text-indigo-600"><Eye size={17} /></button>
                          <button title="Editar" onClick={() => navigate(`/administrador/atividades/editar/${atividade.id}`)} className="rounded p-2 text-slate-500 hover:bg-indigo-50 hover:text-indigo-600"><Pencil size={17} /></button>
                          <button title="Excluir" onClick={() => navigate(`/administrador/atividades/deletar/${atividade.id}`)} className="rounded p-2 text-slate-500 hover:bg-red-50 hover:text-red-600"><Trash2 size={17} /></button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}

export default AtividadesAdministrador;
