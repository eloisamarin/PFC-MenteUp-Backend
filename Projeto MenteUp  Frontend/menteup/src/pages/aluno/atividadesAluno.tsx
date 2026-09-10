import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
    Plus,
    Search,
    MoreVertical,
    Eye,
    Pencil,
    Trash2,
    Users,
    BookOpen,
    Clock,
    CheckCircle,
} from "lucide-react";

interface Turma {
    id: number;
    nome: string;
}

interface Usuario {
    id: number;
    nomeUsuario: string;
}

interface Atividade {
    id: number;
    titulo: string;
    descricao: string;
    pontuacao: number;
    status: "PENDENTE" | "EM_ANDAMENTO" | "CONCLUIDO";
    dataCriacao: string;
    dataAtualizacao: string;
    turma?: Turma;
    usuario?: Usuario;
}

function AtividadesProfessor() {
    const navigate = useNavigate();

    const [atividades, setAtividades] = useState<Atividade[]>([]);
    const [busca, setBusca] = useState("");
    const [carregando, setCarregando] = useState(true);
    const [erro, setErro] = useState("");

    useEffect(() => {
        buscarAtividades();
    }, []);

    async function buscarAtividades() {
        try {
            setCarregando(true);
            setErro("");

            const token = localStorage.getItem("token");

            if (!token) {
                navigate("/login");
                return;
            }

            const resposta = await fetch(
                "http://localhost:8081/atividades",
                {
                    method: "GET",
                    headers: {
                        "Content-Type": "application/json",
                        Authorization: `Bearer ${token}`,
                    },
                }
            );

            if (resposta.status === 401) {
                localStorage.removeItem("token");
                localStorage.removeItem("usuario");
                navigate("/login");
                return;
            }

            if (!resposta.ok) {
                throw new Error("Não foi possível carregar as atividades.");
            }

            const dados = await resposta.json();

            setAtividades(dados);

        } catch (error) {
            console.error("Erro ao buscar atividades:", error);
            setErro("Não foi possível carregar as atividades.");
        } finally {
            setCarregando(false);
        }
    }

    async function excluirAtividade(id: number) {
        const confirmar = window.confirm(
            "Deseja realmente excluir esta atividade?"
        );

        if (!confirmar) {
            return;
        }

        try {
            const token = localStorage.getItem("token");

            const resposta = await fetch(
                `http://localhost:8081/atividades/${id}`,
                {
                    method: "DELETE",
                    headers: {
                        Authorization: `Bearer ${token}`,
                    },
                }
            );

            if (!resposta.ok) {
                throw new Error("Erro ao excluir atividade.");
            }

            setAtividades((listaAtual) =>
                listaAtual.filter((atividade) => atividade.id !== id)
            );

        } catch (error) {
            console.error(error);
            alert("Não foi possível excluir a atividade.");
        }
    }

    function formatarData(data?: string) {
        if (!data) {
            return "-";
        }

        return new Date(data).toLocaleDateString("pt-BR");
    }

    function obterStatus(status: Atividade["status"]) {
        switch (status) {
            case "CONCLUIDO":
                return {
                    texto: "Concluída",
                    classe:
                        "bg-green-100 text-green-700",
                };

            case "EM_ANDAMENTO":
                return {
                    texto: "Em andamento",
                    classe:
                        "bg-blue-100 text-blue-700",
                };

            case "PENDENTE":
            default:
                return {
                    texto: "Pendente",
                    classe:
                        "bg-yellow-100 text-yellow-700",
                };
        }
    }

    const atividadesFiltradas = atividades.filter((atividade) =>
        atividade.titulo
            .toLowerCase()
            .includes(busca.toLowerCase())
    );

    const totalAtividades = atividades.length;

    const atividadesPendentes = atividades.filter(
        (atividade) => atividade.status === "PENDENTE"
    ).length;

    const atividadesAndamento = atividades.filter(
        (atividade) => atividade.status === "EM_ANDAMENTO"
    ).length;

    const atividadesConcluidas = atividades.filter(
        (atividade) => atividade.status === "CONCLUIDO"
    ).length;

    return (
        <div className="min-h-screen bg-[#f5f6fb] flex">

            {/* SIDEBAR */}
            <aside className="w-64 bg-[#111127] text-white fixed left-0 top-0 bottom-0">

                {/* LOGO */}
                <div className="h-20 flex items-center px-6 border-b border-white/10">

                    <div className="w-8 h-8 rounded-lg bg-[#5b4cff] flex items-center justify-center font-bold mr-3">
                        M
                    </div>

                    <span className="text-lg font-bold">
                        Mente<span className="text-[#7c6cff]">Up</span>
                    </span>

                </div>

                {/* MENU */}
                <nav className="p-4 space-y-2">

                    <button
                        onClick={() =>
                            navigate("/professor/dashboard")
                        }
                        className="w-full flex items-center gap-3 px-4 py-3 rounded-lg text-gray-300 hover:bg-white/10 transition"
                    >
                        <BookOpen size={18} />
                        Dashboard
                    </button>

                    <button
                        onClick={() =>
                            navigate("/professor/turmas")
                        }
                        className="w-full flex items-center gap-3 px-4 py-3 rounded-lg text-gray-300 hover:bg-white/10 transition"
                    >
                        <Users size={18} />
                        Turmas
                    </button>

                    <button
                        className="w-full flex items-center gap-3 px-4 py-3 rounded-lg bg-[#282451] text-white"
                    >
                        <BookOpen size={18} />
                        Atividades
                    </button>

                    <button
                        className="w-full flex items-center gap-3 px-4 py-3 rounded-lg text-gray-300 hover:bg-white/10 transition"
                    >
                        <CheckCircle size={18} />
                        Desempenho
                    </button>

                </nav>

            </aside>

            {/* CONTEÚDO */}
            <main className="ml-64 flex-1">

                {/* HEADER */}
                <header className="h-20 bg-white border-b border-gray-200 flex items-center justify-between px-8">

                    <div className="relative w-80">

                        <Search
                            size={17}
                            className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"
                        />

                        <input
                            type="text"
                            placeholder="Buscar atividade..."
                            value={busca}
                            onChange={(event) =>
                                setBusca(event.target.value)
                            }
                            className="w-full bg-gray-100 border-0 rounded-full py-2.5 pl-10 pr-4 text-sm outline-none focus:ring-2 focus:ring-[#5b4cff]/30"
                        />

                    </div>

                    <div className="flex items-center gap-4">

                        <div className="w-9 h-9 rounded-full bg-[#5b4cff] text-white flex items-center justify-center text-sm font-bold">
                            P
                        </div>

                        <span className="text-sm font-semibold text-gray-700">
                            Professor
                        </span>

                    </div>

                </header>

                {/* CONTEÚDO */}
                <div className="p-8">

                    {/* TÍTULO */}
                    <div className="flex items-center justify-between mb-7">

                        <div>

                            <h1 className="text-2xl font-bold text-gray-800">
                                Atividades
                            </h1>

                            <p className="text-sm text-gray-500 mt-1">
                                {totalAtividades} atividades cadastradas
                            </p>

                        </div>

                        <button
                            onClick={() =>
                                navigate("/professor/atividades/nova")
                            }
                            className="flex items-center gap-2 bg-[#5b4cff] hover:bg-[#4d3fe0] text-white px-5 py-3 rounded-lg font-semibold text-sm shadow-sm transition"
                        >
                            <Plus size={18} />
                            Criar atividade
                        </button>

                    </div>

                    {/* CARDS DE RESUMO */}
                    <div className="grid grid-cols-4 gap-5 mb-7">

                        <div className="bg-white rounded-xl p-5 border border-gray-100 shadow-sm">

                            <div className="flex items-center justify-between">

                                <div>
                                    <p className="text-sm text-gray-500">
                                        Total
                                    </p>

                                    <p className="text-2xl font-bold text-gray-800 mt-1">
                                        {totalAtividades}
                                    </p>
                                </div>

                                <div className="w-10 h-10 rounded-lg bg-purple-100 text-purple-600 flex items-center justify-center">
                                    <BookOpen size={20} />
                                </div>

                            </div>

                        </div>

                        <div className="bg-white rounded-xl p-5 border border-gray-100 shadow-sm">

                            <div className="flex items-center justify-between">

                                <div>
                                    <p className="text-sm text-gray-500">
                                        Pendentes
                                    </p>

                                    <p className="text-2xl font-bold text-gray-800 mt-1">
                                        {atividadesPendentes}
                                    </p>
                                </div>

                                <div className="w-10 h-10 rounded-lg bg-yellow-100 text-yellow-600 flex items-center justify-center">
                                    <Clock size={20} />
                                </div>

                            </div>

                        </div>

                        <div className="bg-white rounded-xl p-5 border border-gray-100 shadow-sm">

                            <div className="flex items-center justify-between">

                                <div>
                                    <p className="text-sm text-gray-500">
                                        Em andamento
                                    </p>

                                    <p className="text-2xl font-bold text-gray-800 mt-1">
                                        {atividadesAndamento}
                                    </p>
                                </div>

                                <div className="w-10 h-10 rounded-lg bg-blue-100 text-blue-600 flex items-center justify-center">
                                    <Clock size={20} />
                                </div>

                            </div>

                        </div>

                        <div className="bg-white rounded-xl p-5 border border-gray-100 shadow-sm">

                            <div className="flex items-center justify-between">

                                <div>
                                    <p className="text-sm text-gray-500">
                                        Concluídas
                                    </p>

                                    <p className="text-2xl font-bold text-gray-800 mt-1">
                                        {atividadesConcluidas}
                                    </p>
                                </div>

                                <div className="w-10 h-10 rounded-lg bg-green-100 text-green-600 flex items-center justify-center">
                                    <CheckCircle size={20} />
                                </div>

                            </div>

                        </div>

                    </div>

                    {/* ERRO */}
                    {erro && (
                        <div className="mb-5 bg-red-50 border border-red-200 text-red-700 rounded-lg px-4 py-3 text-sm">
                            {erro}
                        </div>
                    )}

                    {/* TABELA */}
                    <div className="bg-white rounded-xl border border-gray-100 shadow-sm overflow-hidden">

                        {carregando ? (

                            <div className="py-16 text-center text-gray-500">
                                Carregando atividades...
                            </div>

                        ) : atividadesFiltradas.length === 0 ? (

                            <div className="py-16 text-center">

                                <BookOpen
                                    size={40}
                                    className="mx-auto text-gray-300 mb-3"
                                />

                                <p className="font-semibold text-gray-600">
                                    Nenhuma atividade encontrada
                                </p>

                                <p className="text-sm text-gray-400 mt-1">
                                    Crie uma nova atividade para começar.
                                </p>

                            </div>

                        ) : (

                            <table className="w-full">

                                <thead className="bg-gray-50 border-b border-gray-200">

                                    <tr>

                                        <th className="text-left px-6 py-4 text-xs font-semibold text-gray-500 uppercase">
                                            Atividade
                                        </th>

                                        <th className="text-left px-6 py-4 text-xs font-semibold text-gray-500 uppercase">
                                            Turma
                                        </th>

                                        <th className="text-center px-6 py-4 text-xs font-semibold text-gray-500 uppercase">
                                            Pontuação
                                        </th>

                                        <th className="text-left px-6 py-4 text-xs font-semibold text-gray-500 uppercase">
                                            Data
                                        </th>

                                        <th className="text-center px-6 py-4 text-xs font-semibold text-gray-500 uppercase">
                                            Status
                                        </th>

                                        <th className="text-center px-6 py-4 text-xs font-semibold text-gray-500 uppercase">
                                            Ações
                                        </th>

                                    </tr>

                                </thead>

                                <tbody>

                                    {atividadesFiltradas.map(
                                        (atividade) => {

                                            const status =
                                                obterStatus(
                                                    atividade.status
                                                );

                                            return (

                                                <tr
                                                    key={atividade.id}
                                                    className="border-b border-gray-100 hover:bg-gray-50 transition"
                                                >

                                                    {/* ATIVIDADE */}
                                                    <td className="px-6 py-4">

                                                        <div>

                                                            <p className="font-semibold text-gray-800">
                                                                {atividade.titulo}
                                                            </p>

                                                            <p className="text-xs text-gray-400 mt-1 max-w-xs truncate">
                                                                {atividade.descricao}
                                                            </p>

                                                        </div>

                                                    </td>

                                                    {/* TURMA */}
                                                    <td className="px-6 py-4">

                                                        <div className="flex items-center gap-2">

                                                            <div className="w-8 h-8 rounded-full bg-purple-100 text-purple-600 flex items-center justify-center">
                                                                <Users size={15} />
                                                            </div>

                                                            <span className="text-sm text-gray-700">
                                                                {atividade.turma?.nome ||
                                                                    "Sem turma"}
                                                            </span>

                                                        </div>

                                                    </td>

                                                    {/* PONTUAÇÃO */}
                                                    <td className="px-6 py-4 text-center">

                                                        <span className="text-sm font-semibold text-gray-700">
                                                            {atividade.pontuacao} XP
                                                        </span>

                                                    </td>

                                                    {/* DATA */}
                                                    <td className="px-6 py-4">

                                                        <span className="text-sm text-gray-600">
                                                            {formatarData(
                                                                atividade.dataCriacao
                                                            )}
                                                        </span>

                                                    </td>

                                                    {/* STATUS */}
                                                    <td className="px-6 py-4 text-center">

                                                        <span
                                                            className={`inline-flex px-3 py-1 rounded-full text-xs font-semibold ${status.classe}`}
                                                        >
                                                            {status.texto}
                                                        </span>

                                                    </td>

                                                    {/* AÇÕES */}
                                                    <td className="px-6 py-4">

                                                        <div className="flex justify-center gap-2">

                                                            <button
                                                                title="Visualizar"
                                                                onClick={() =>
                                                                    navigate(
                                                                        `/professor/atividades/${atividade.id}`
                                                                    )
                                                                }
                                                                className="w-8 h-8 rounded-lg border border-gray-200 flex items-center justify-center text-gray-500 hover:bg-gray-100 hover:text-[#5b4cff]"
                                                            >
                                                                <Eye size={16} />
                                                            </button>

                                                            <button
                                                                title="Editar"
                                                                onClick={() =>
                                                                    navigate(
                                                                        `/professor/atividades/${atividade.id}/editar`
                                                                    )
                                                                }
                                                                className="w-8 h-8 rounded-lg border border-gray-200 flex items-center justify-center text-gray-500 hover:bg-gray-100 hover:text-[#5b4cff]"
                                                            >
                                                                <Pencil size={16} />
                                                            </button>

                                                            <button
                                                                title="Excluir"
                                                                onClick={() =>
                                                                    excluirAtividade(
                                                                        atividade.id
                                                                    )
                                                                }
                                                                className="w-8 h-8 rounded-lg border border-gray-200 flex items-center justify-center text-gray-500 hover:bg-red-50 hover:text-red-500"
                                                            >
                                                                <Trash2 size={16} />
                                                            </button>

                                                        </div>

                                                    </td>

                                                </tr>

                                            );
                                        }
                                    )}

                                </tbody>

                            </table>

                        )}

                    </div>

                </div>

            </main>

        </div>
    );
}

export default AtividadesProfessor;