import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import {
    Plus,
    Search,
    Eye,
    Pencil,
    Trash2,
    Play,
    BookOpen,
    Users,
    Trophy,
} from "lucide-react";

import { apiFetch } from "../../services/api";

type StatusAtividade =
    | "PENDENTE"
    | "EM_ANDAMENTO"
    | "CONCLUIDO";

type Atividade = {
    id: number | string;
    titulo: string;
    descricao?: string;
    pontuacao: number;
    status: StatusAtividade;
    turma?: {
        id: number | string;
        nome?: string;
    };
};


function AtividadesProfessor() {

    const navigate = useNavigate();

    const [atividades, setAtividades] = useState<Atividade[]>([]);

    const [busca, setBusca] = useState("");

    const [carregando, setCarregando] = useState(true);

    const [erro, setErro] = useState("");


    // ==========================================
    // CARREGAR ATIVIDADES
    // ==========================================

    async function carregarAtividades() {

        try {

            setCarregando(true);
            setErro("");

            const resposta = await apiFetch("/atividades");

            if (!resposta.ok) {
                throw new Error("Erro ao carregar atividades");
            }

            const dados = await resposta.json();

            setAtividades(
                [...dados].sort(
                    (a: Atividade, b: Atividade) =>
                        Number(b.id) - Number(a.id)
                )
            );

        } catch (error) {

            console.error(error);

            setErro(
                "Não foi possível carregar as atividades."
            );

        } finally {

            setCarregando(false);

        }
    }


    // ==========================================
    // CARREGAR AO ABRIR A PÁGINA
    // ==========================================

    useEffect(() => {

        carregarAtividades();

    }, []);


    // ==========================================
    // CRIAR ATIVIDADE
    // ==========================================

    function criarAtividade() {

        navigate("/professor/atividades/criar");

    }


    // ==========================================
    // VISUALIZAR ATIVIDADE
    // ==========================================

    function visualizarAtividade(
        atividade: Atividade
    ) {

        navigate(
            `/professor/atividades/${atividade.id}`
        );

    }


    // ==========================================
    // EDITAR ATIVIDADE
    // ==========================================

    function editarAtividade(
        atividade: Atividade
    ) {

        navigate(
            `/professor/atividades/editar/${atividade.id}`
        );

    }


    // ==========================================
    // DELETAR ATIVIDADE
    // ==========================================

    function deletarAtividade(
        atividade: Atividade
    ) {

        navigate(
            `/professor/atividades/deletar/${atividade.id}`
        );

    }


    // ==========================================
    // ATIVAR ATIVIDADE
    // ==========================================

    async function ativarAtividade(
        atividade: Atividade
    ) {

        try {

            const resposta = await apiFetch(
                "/atividades",
                {
                    method: "PUT",

                    body: JSON.stringify({

                        id: atividade.id,

                        titulo: atividade.titulo,

                        descricao: atividade.descricao,

                        pontuacao: atividade.pontuacao,

                        status: "EM_ANDAMENTO",

                        turma: {
                            id: atividade.turma?.id
                        }

                    }),
                }
            );


            if (!resposta.ok) {

                alert(
                    "Não foi possível ativar a atividade."
                );

                return;

            }


            await carregarAtividades();


        } catch (error) {

            console.error(error);

            alert(
                "Erro ao ativar atividade."
            );

        }

    }


    // ==========================================
    // FILTRO DE PESQUISA
    // ==========================================

    const atividadesFiltradas =
        atividades.filter(
            (atividade) =>
                atividade.titulo
                    .toLowerCase()
                    .includes(
                        busca.toLowerCase()
                    )
        );


    // ==========================================
    // LABEL DO STATUS
    // ==========================================

    function statusLabel(
        status: StatusAtividade
    ) {

        switch (status) {

            case "PENDENTE":
                return "Pendente";

            case "EM_ANDAMENTO":
                return "Em andamento";

            case "CONCLUIDO":
                return "Concluído";

            default:
                return status;

        }

    }


    // ==========================================
    // CLASSE DO STATUS
    // ==========================================

    function statusClass(
        status: StatusAtividade
    ) {

        switch (status) {

            case "PENDENTE":

                return "bg-slate-100 text-slate-600";


            case "EM_ANDAMENTO":

                return "bg-emerald-100 text-emerald-700";


            case "CONCLUIDO":

                return "bg-blue-100 text-blue-700";


            default:

                return "bg-gray-100 text-gray-600";

        }

    }


    // ==========================================
    // TELA
    // ==========================================

    return (

        <div className="min-h-screen bg-[#f5f6fc] text-slate-800">


            {/* ================================= */}
            {/* SIDEBAR */}
            {/* ================================= */}

            <aside className="fixed left-0 top-0 h-screen w-56 bg-[#121329] text-white">


                {/* LOGO */}

                <div className="flex items-center gap-3 px-6 py-6">

                    <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-gradient-to-br from-indigo-500 to-purple-600 font-bold">

                        M

                    </div>


                    <span className="text-lg font-bold">

                        Mente
                        <span className="text-purple-400">
                            Up
                        </span>

                    </span>

                </div>


                {/* MENU */}

                <nav className="mt-6 space-y-2 px-3">


                    {/* DASHBOARD */}

                    <button
                        onClick={() =>
                            navigate(
                                "/professor/dashboard"
                            )
                        }
                        className="flex w-full items-center gap-3 rounded-lg px-4 py-3 text-sm text-slate-300 hover:bg-white/10"
                    >

                        Dashboard

                    </button>


                    {/* TURMAS */}

                    <button
                        onClick={() =>
                            navigate(
                                "/professor/turmas"
                            )
                        }
                        className="flex w-full items-center gap-3 rounded-lg px-4 py-3 text-sm text-slate-300 hover:bg-white/10"
                    >

                        <Users size={17} />

                        Turmas

                    </button>


                    {/* ATIVIDADES */}

                    <button
                        onClick={() =>
                            navigate(
                                "/professor/atividades"
                            )
                        }
                        className="flex w-full items-center gap-3 rounded-lg bg-white/10 px-4 py-3 text-sm font-medium text-white"
                    >

                        <BookOpen size={17} />

                        Atividades

                    </button>


                    {/* DESEMPENHO */}

                    <button
                        className="flex w-full items-center gap-3 rounded-lg px-4 py-3 text-sm text-slate-300 hover:bg-white/10"
                    >

                        Desempenho

                    </button>


                    {/* RANKING */}

                    <button
                        className="flex w-full items-center gap-3 rounded-lg px-4 py-3 text-sm text-slate-300 hover:bg-white/10"
                    >

                        <Trophy size={17} />

                        Ranking

                    </button>


                    {/* PERFIL */}

                    <button
                        className="flex w-full items-center gap-3 rounded-lg px-4 py-3 text-sm text-slate-300 hover:bg-white/10"
                    >

                        Perfil

                    </button>

                </nav>

            </aside>


            {/* ================================= */}
            {/* CONTEÚDO PRINCIPAL */}
            {/* ================================= */}

            <main className="ml-56 min-h-screen">


                {/* HEADER */}

                <header className="flex h-20 items-center justify-between border-b bg-white px-8">


                    {/* PESQUISA */}

                    <div className="relative">

                        <Search
                            size={17}
                            className="absolute left-4 top-3 text-slate-400"
                        />


                        <input
                            value={busca}
                            onChange={(e) =>
                                setBusca(
                                    e.target.value
                                )
                            }
                            placeholder="Buscar atividade..."
                            className="w-72 rounded-full bg-[#f4f5fb] py-2.5 pl-10 pr-4 text-sm outline-none focus:ring-2 focus:ring-indigo-300"
                        />

                    </div>


                    {/* USUÁRIO */}

                    <div className="flex items-center gap-3">


                        <div className="flex h-10 w-10 items-center justify-center rounded-full bg-indigo-600 text-sm font-bold text-white">

                            {JSON.parse(
                                localStorage.getItem(
                                    "usuario"
                                ) || "{}"
                            ).nome?.charAt(0) || "P"}

                        </div>


                        <div className="text-sm">

                            <p className="font-semibold">

                                {JSON.parse(
                                    localStorage.getItem(
                                        "usuario"
                                    ) || "{}"
                                ).nome || "Professor"}

                            </p>


                            <p className="text-xs text-slate-400">

                                Professor

                            </p>

                        </div>

                    </div>

                </header>


                {/* ================================= */}
                {/* SEÇÃO */}
                {/* ================================= */}

                <section className="p-8">


                    {/* TÍTULO */}

                    <div className="mb-7 flex items-center justify-between">


                        <div>

                            <div>
                                <h1 className="text-2xl font-semibold">
                                    Atividades
                                </h1>

                                <p className="mt-1 text-sm text-slate-400">
                                    {atividades.length} atividades criadas
                                </p>
                            </div>

                            <button
                                type="button"
                                onClick={carregarAtividades}
                                disabled={carregando}
                                className="rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-50 disabled:opacity-60"
                            >
                                {carregando ? "Atualizando..." : "Atualizar lista"}
                            </button>

                        </div>


                        {/* CRIAR */}

                        <button
                            onClick={criarAtividade}
                            className="flex items-center gap-2 rounded-lg bg-gradient-to-r from-indigo-500 to-purple-600 px-5 py-3 text-sm font-semibold text-white shadow-lg shadow-indigo-200 transition hover:-translate-y-0.5"
                        >

                            <Plus size={18} />

                            Criar atividade

                        </button>

                    </div>


                    {/* ERRO */}

                    {erro && (

                        <div className="mb-5 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-600">

                            {erro}

                        </div>

                    )}


                    {/* ================================= */}
                    {/* TABELA */}
                    {/* ================================= */}

                    <div className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">

                        <div className="overflow-x-auto">

                            <table className="w-full">


                                {/* CABEÇALHO */}

                                <thead className="border-b bg-slate-50">

                                    <tr className="text-left text-xs uppercase text-slate-400">

                                        <th className="px-5 py-4">
                                            Atividade
                                        </th>

                                        <th className="px-5 py-4">
                                            Turma
                                        </th>

                                        <th className="px-5 py-4">
                                            XP
                                        </th>

                                        <th className="px-5 py-4">
                                            Status
                                        </th>

                                        <th className="px-5 py-4 text-center">
                                            Ações
                                        </th>

                                    </tr>

                                </thead>


                                {/* CORPO */}

                                <tbody>


                                    {/* CARREGANDO */}

                                    {carregando ? (

                                        <tr>

                                            <td
                                                colSpan={5}
                                                className="py-12 text-center text-sm text-slate-400"
                                            >

                                                Carregando atividades...

                                            </td>

                                        </tr>


                                    ) : atividadesFiltradas.length === 0 ? (


                                        /* NENHUMA ATIVIDADE */

                                        <tr>

                                            <td
                                                colSpan={5}
                                                className="py-12 text-center"
                                            >

                                                <BookOpen
                                                    size={40}
                                                    className="mx-auto mb-3 text-slate-300"
                                                />


                                                <p className="font-medium text-slate-600">

                                                    Nenhuma atividade encontrada

                                                </p>


                                                <p className="mt-1 text-sm text-slate-400">

                                                    Crie sua primeira atividade.

                                                </p>

                                            </td>

                                        </tr>


                                    ) : (


                                        /* ATIVIDADES */

                                        atividadesFiltradas.map(
                                            (atividade) => (

                                                <tr
                                                    key={atividade.id}
                                                    className="border-b last:border-0 hover:bg-slate-50"
                                                >


                                                    {/* ATIVIDADE */}

                                                    <td className="px-5 py-5">

                                                        <p className="font-semibold text-slate-700">

                                                            {atividade.titulo}

                                                        </p>


                                                        <p className="mt-1 max-w-md truncate text-xs text-slate-400">

                                                            {atividade.descricao}

                                                        </p>

                                                    </td>


                                                    {/* TURMA */}

                                                    <td className="px-5 py-5 text-sm text-slate-600">

                                                        {atividade.turma?.nome ||
                                                            "Sem turma"}

                                                    </td>


                                                    {/* XP */}

                                                    <td className="px-5 py-5 text-sm font-semibold">

                                                        {atividade.pontuacao} XP

                                                    </td>


                                                    {/* STATUS */}

                                                    <td className="px-5 py-5">

                                                        <span
                                                            className={`rounded-full px-3 py-1 text-xs font-semibold ${statusClass(
                                                                atividade.status
                                                            )}`}
                                                        >

                                                            {statusLabel(
                                                                atividade.status
                                                            )}

                                                        </span>

                                                    </td>


                                                    {/* AÇÕES */}

                                                    <td className="px-5 py-5">

                                                        <div className="flex justify-center gap-2">


                                                            {/* ================= */}
                                                            {/* VISUALIZAR */}
                                                            {/* ================= */}

                                                            <button
                                                                title="Visualizar"
                                                                onClick={() =>
                                                                    visualizarAtividade(
                                                                        atividade
                                                                    )
                                                                }
                                                                className="rounded-lg border border-slate-200 p-2 text-slate-500 hover:bg-slate-100"
                                                            >

                                                                <Eye size={16} />

                                                            </button>


                                                            {/* ================= */}
                                                            {/* EDITAR */}
                                                            {/* ================= */}

                                                            <button
                                                                title="Editar"
                                                                onClick={() =>
                                                                    editarAtividade(
                                                                        atividade
                                                                    )
                                                                }
                                                                className="rounded-lg border border-slate-200 p-2 text-indigo-500 hover:bg-indigo-50"
                                                            >

                                                                <Pencil size={16} />

                                                            </button>


                                                            {/* ================= */}
                                                            {/* ATIVAR */}
                                                            {/* ================= */}

                                                            {atividade.status ===
                                                                "PENDENTE" && (

                                                                    <button
                                                                        title="Ativar"
                                                                        onClick={() =>
                                                                            ativarAtividade(
                                                                                atividade
                                                                            )
                                                                        }
                                                                        className="rounded-lg border border-emerald-200 p-2 text-emerald-600 hover:bg-emerald-50"
                                                                    >

                                                                        <Play
                                                                            size={16}
                                                                        />

                                                                    </button>

                                                                )}


                                                            {/* ================= */}
                                                            {/* DELETAR */}
                                                            {/* ================= */}

                                                            <button
                                                                title="Excluir"
                                                                onClick={() =>
                                                                    deletarAtividade(
                                                                        atividade
                                                                    )
                                                                }
                                                                className="rounded-lg border border-red-200 p-2 text-red-500 hover:bg-red-50"
                                                            >

                                                                <Trash2
                                                                    size={16}
                                                                />

                                                            </button>

                                                        </div>

                                                    </td>

                                                </tr>

                                            )
                                        )

                                    )}

                                </tbody>

                            </table>

                        </div>

                    </div>

                </section>

            </main>

        </div>
    );
}


export default AtividadesProfessor;