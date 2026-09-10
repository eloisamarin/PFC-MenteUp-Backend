import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
    Plus,
    Search,
    Eye,
    Pencil,
    Trash2,
    Play,
    X,
    BookOpen,
    Users,
    Trophy,
} from "lucide-react";

import { apiFetch } from "../../services/api";
import type {
    Atividade,
    Turma,
    StatusAtividade,
} from "../../types/atividade";

function AtividadesProfessor() {

    const navigate = useNavigate();

    const [atividades, setAtividades] = useState<Atividade[]>([]);
    const [turmas, setTurmas] = useState<Turma[]>([]);

    const [busca, setBusca] = useState("");

    const [carregando, setCarregando] = useState(true);
    const [erro, setErro] = useState("");

    const [modalAberto, setModalAberto] = useState(false);
    const [modalVisualizar, setModalVisualizar] = useState(false);

    const [atividadeSelecionada, setAtividadeSelecionada] =
        useState<Atividade | null>(null);

    const [modoEdicao, setModoEdicao] = useState(false);

    const [titulo, setTitulo] = useState("");
    const [descricao, setDescricao] = useState("");
    const [pontuacao, setPontuacao] = useState("");
    const [status, setStatus] =
        useState<StatusAtividade>("PENDENTE");

    const [turmaId, setTurmaId] = useState("");

    // ==============================
    // CARREGAR ATIVIDADES
    // ==============================

    async function carregarAtividades() {

        try {

            setCarregando(true);
            setErro("");

            const resposta = await apiFetch("/atividades");

            if (!resposta.ok) {
                throw new Error("Erro ao carregar atividades");
            }

            const dados = await resposta.json();

            setAtividades(dados);

        } catch (error) {

            console.error(error);
            setErro("Não foi possível carregar as atividades.");

        } finally {

            setCarregando(false);

        }
    }

    // ==============================
    // CARREGAR TURMAS
    // ==============================

    async function carregarTurmas() {

        try {

            const resposta = await apiFetch("/turmas/all");

            if (!resposta.ok) {
                throw new Error("Erro ao carregar turmas");
            }

            const dados = await resposta.json();

            setTurmas(dados);

        } catch (error) {

            console.error(error);

        }
    }

    useEffect(() => {

        carregarAtividades();
        carregarTurmas();

    }, []);

    // ==============================
    // ABRIR NOVA ATIVIDADE
    // ==============================

    function abrirNovaAtividade() {

        setModoEdicao(false);

        setAtividadeSelecionada(null);

        setTitulo("");
        setDescricao("");
        setPontuacao("");
        setStatus("PENDENTE");
        setTurmaId("");

        setModalAberto(true);
    }

    // ==============================
    // EDITAR
    // ==============================

    function editarAtividade(atividade: Atividade) {

        setModoEdicao(true);

        setAtividadeSelecionada(atividade);

        setTitulo(atividade.titulo);
        setDescricao(atividade.descricao);
        setPontuacao(String(atividade.pontuacao));
        setStatus(atividade.status);

        setTurmaId(
            atividade.turma?.id
                ? String(atividade.turma.id)
                : ""
        );

        setModalAberto(true);
    }

    // ==============================
    // VISUALIZAR
    // ==============================

    function visualizarAtividade(atividade: Atividade) {

        setAtividadeSelecionada(atividade);

        setModalVisualizar(true);
    }

    // ==============================
    // SALVAR
    // ==============================

    async function salvarAtividade(
        event: React.FormEvent
    ) {

        event.preventDefault();

        if (!titulo || !descricao || !pontuacao || !turmaId) {

            alert("Preencha todos os campos.");

            return;
        }

        const atividade: any = {

            titulo,
            descricao,
            pontuacao: Number(pontuacao),
            status,

            turma: {
                id: Number(turmaId)
            }

        };

        try {

            let resposta;

            if (modoEdicao && atividadeSelecionada) {

                atividade.id = atividadeSelecionada.id;

                resposta = await apiFetch("/atividades", {
                    method: "PUT",
                    body: JSON.stringify(atividade),
                });

            } else {

                resposta = await apiFetch("/atividades", {
                    method: "POST",
                    body: JSON.stringify(atividade),
                });

            }

            if (!resposta.ok) {

                const mensagem = await resposta.text();

                console.error(mensagem);

                alert(
                    modoEdicao
                        ? "Não foi possível atualizar a atividade."
                        : "Não foi possível criar a atividade."
                );

                return;
            }

            setModalAberto(false);

            await carregarAtividades();

        } catch (error) {

            console.error(error);

            alert("Erro ao conectar com o servidor.");

        }

    }

    // ==============================
    // EXCLUIR
    // ==============================

    async function excluirAtividade(id: number) {

        const confirmar = window.confirm(
            "Tem certeza que deseja excluir esta atividade?"
        );

        if (!confirmar) {
            return;
        }

        try {

            const resposta = await apiFetch(
                `/atividades/${id}`,
                {
                    method: "DELETE",
                }
            );

            if (!resposta.ok) {

                alert("Não foi possível excluir a atividade.");

                return;
            }

            await carregarAtividades();

        } catch (error) {

            console.error(error);

            alert("Erro ao excluir atividade.");

        }
    }

    // ==============================
    // ATIVAR
    // ==============================

    async function ativarAtividade(atividade: Atividade) {

        try {

            const resposta = await apiFetch("/atividades", {
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
            });

            if (!resposta.ok) {

                alert("Não foi possível ativar a atividade.");

                return;
            }

            await carregarAtividades();

        } catch (error) {

            console.error(error);

            alert("Erro ao ativar atividade.");

        }
    }

    // ==============================
    // FILTRO
    // ==============================

    const atividadesFiltradas = atividades.filter(
        (atividade) =>
            atividade.titulo
                .toLowerCase()
                .includes(busca.toLowerCase())
    );

    // ==============================
    // STATUS
    // ==============================

    function statusLabel(status: StatusAtividade) {

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

    function statusClass(status: StatusAtividade) {

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

    // ==============================
    // TELA
    // ==============================

    return (

        <div className="min-h-screen bg-[#f5f6fc] text-slate-800">

            {/* SIDEBAR */}

            <aside className="fixed left-0 top-0 h-screen w-56 bg-[#121329] text-white">

                <div className="flex items-center gap-3 px-6 py-6">

                    <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-gradient-to-br from-indigo-500 to-purple-600 font-bold">
                        M
                    </div>

                    <span className="text-lg font-bold">
                        Mente<span className="text-purple-400">Up</span>
                    </span>

                </div>

                <nav className="mt-6 space-y-2 px-3">

                    <button
                        onClick={() =>
                            navigate("/professor/dashboard")
                        }
                        className="flex w-full items-center gap-3 rounded-lg px-4 py-3 text-sm text-slate-300 hover:bg-white/10"
                    >
                        Dashboard
                    </button>

                    <button
                        onClick={() =>
                            navigate("/professor/turmas")
                        }
                        className="flex w-full items-center gap-3 rounded-lg px-4 py-3 text-sm text-slate-300 hover:bg-white/10"
                    >
                        <Users size={17} />
                        Turmas
                    </button>

                    <button
                        className="flex w-full items-center gap-3 rounded-lg bg-white/10 px-4 py-3 text-sm font-medium text-white"
                    >
                        <BookOpen size={17} />
                        Atividades
                    </button>

                    <button
                        className="flex w-full items-center gap-3 rounded-lg px-4 py-3 text-sm text-slate-300 hover:bg-white/10"
                    >
                        Desempenho
                    </button>

                    <button
                        className="flex w-full items-center gap-3 rounded-lg px-4 py-3 text-sm text-slate-300 hover:bg-white/10"
                    >
                        <Trophy size={17} />
                        Ranking
                    </button>

                    <button
                        className="flex w-full items-center gap-3 rounded-lg px-4 py-3 text-sm text-slate-300 hover:bg-white/10"
                    >
                        Perfil
                    </button>

                </nav>

            </aside>


            {/* CONTEÚDO */}

            <main className="ml-56 min-h-screen">

                {/* HEADER */}

                <header className="flex h-20 items-center justify-between border-b bg-white px-8">

                    <div className="relative">

                        <Search
                            size={17}
                            className="absolute left-4 top-3 text-slate-400"
                        />

                        <input
                            value={busca}
                            onChange={(e) =>
                                setBusca(e.target.value)
                            }
                            placeholder="Buscar atividade..."
                            className="w-72 rounded-full bg-[#f4f5fb] py-2.5 pl-10 pr-4 text-sm outline-none focus:ring-2 focus:ring-indigo-300"
                        />

                    </div>

                    <div className="flex items-center gap-3">

                        <div className="flex h-10 w-10 items-center justify-center rounded-full bg-indigo-600 text-sm font-bold text-white">
                            {JSON.parse(
                                localStorage.getItem("usuario") || "{}"
                            ).nome?.charAt(0) || "P"}
                        </div>

                        <div className="text-sm">

                            <p className="font-semibold">
                                {JSON.parse(
                                    localStorage.getItem("usuario") || "{}"
                                ).nome || "Professor"}
                            </p>

                            <p className="text-xs text-slate-400">
                                Professor
                            </p>

                        </div>

                    </div>

                </header>


                {/* CONTEÚDO */}

                <section className="p-8">

                    <div className="mb-7 flex items-center justify-between">

                        <div>

                            <h1 className="text-2xl font-semibold">
                                Atividades
                            </h1>

                            <p className="mt-1 text-sm text-slate-400">
                                {atividades.length} atividades criadas
                            </p>

                        </div>

                        <button
                            onClick={abrirNovaAtividade}
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


                    {/* TABELA */}

                    <div className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">

                        <div className="overflow-x-auto">

                            <table className="w-full">

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


                                <tbody>

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

                                        atividadesFiltradas.map(
                                            (atividade) => (

                                                <tr
                                                    key={atividade.id}
                                                    className="border-b last:border-0 hover:bg-slate-50"
                                                >

                                                    <td className="px-5 py-5">

                                                        <p className="font-semibold text-slate-700">
                                                            {atividade.titulo}
                                                        </p>

                                                        <p className="mt-1 max-w-md truncate text-xs text-slate-400">
                                                            {atividade.descricao}
                                                        </p>

                                                    </td>

                                                    <td className="px-5 py-5 text-sm text-slate-600">

                                                        {atividade.turma?.nome ||
                                                            "Sem turma"}

                                                    </td>

                                                    <td className="px-5 py-5 text-sm font-semibold">

                                                        {atividade.pontuacao} XP

                                                    </td>

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

                                                    <td className="px-5 py-5">

                                                        <div className="flex justify-center gap-2">

                                                            {/* VISUALIZAR */}

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


                                                            {/* EDITAR */}

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


                                                            {/* ATIVAR */}

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


                                                            {/* EXCLUIR */}

                                                            <button
                                                                title="Excluir"
                                                                onClick={() =>
                                                                    excluirAtividade(
                                                                        atividade.id
                                                                    )
                                                                }
                                                                className="rounded-lg border border-red-200 p-2 text-red-500 hover:bg-red-50"
                                                            >
                                                                <Trash2 size={16} />
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


            {/* ================================= */}
            {/* MODAL CRIAR / EDITAR */}
            {/* ================================= */}

            {modalAberto && (

                <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4">

                    <div className="w-full max-w-lg rounded-2xl bg-white shadow-2xl">

                        <div className="flex items-center justify-between border-b px-6 py-5">

                            <div>

                                <h2 className="text-lg font-semibold">

                                    {modoEdicao
                                        ? "Editar atividade"
                                        : "Criar atividade"}

                                </h2>

                                <p className="text-sm text-slate-400">
                                    Preencha os dados da atividade.
                                </p>

                            </div>

                            <button
                                onClick={() =>
                                    setModalAberto(false)
                                }
                                className="rounded-lg p-2 hover:bg-slate-100"
                            >
                                <X size={20} />
                            </button>

                        </div>


                        <form
                            onSubmit={salvarAtividade}
                            className="space-y-5 p-6"
                        >

                            {/* TÍTULO */}

                            <div>

                                <label className="mb-2 block text-sm font-medium">
                                    Título
                                </label>

                                <input
                                    value={titulo}
                                    onChange={(e) =>
                                        setTitulo(e.target.value)
                                    }
                                    placeholder="Ex.: Equações do 2º Grau"
                                    className="w-full rounded-lg border border-slate-200 px-4 py-3 text-sm outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100"
                                />

                            </div>


                            {/* DESCRIÇÃO */}

                            <div>

                                <label className="mb-2 block text-sm font-medium">
                                    Descrição
                                </label>

                                <textarea
                                    value={descricao}
                                    onChange={(e) =>
                                        setDescricao(e.target.value)
                                    }
                                    rows={4}
                                    placeholder="Descreva a atividade..."
                                    className="w-full resize-none rounded-lg border border-slate-200 px-4 py-3 text-sm outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100"
                                />

                            </div>


                            <div className="grid grid-cols-2 gap-4">

                                {/* PONTUAÇÃO */}

                                <div>

                                    <label className="mb-2 block text-sm font-medium">
                                        Pontuação
                                    </label>

                                    <input
                                        type="number"
                                        min="1"
                                        value={pontuacao}
                                        onChange={(e) =>
                                            setPontuacao(
                                                e.target.value
                                            )
                                        }
                                        placeholder="50"
                                        className="w-full rounded-lg border border-slate-200 px-4 py-3 text-sm outline-none focus:border-indigo-500"
                                    />

                                </div>


                                {/* TURMA */}

                                <div>

                                    <label className="mb-2 block text-sm font-medium">
                                        Turma
                                    </label>

                                    <select
                                        value={turmaId}
                                        onChange={(e) =>
                                            setTurmaId(
                                                e.target.value
                                            )
                                        }
                                        className="w-full rounded-lg border border-slate-200 bg-white px-4 py-3 text-sm outline-none focus:border-indigo-500"
                                    >

                                        <option value="">
                                            Selecione
                                        </option>

                                        {turmas.map((turma) => (

                                            <option
                                                key={turma.id}
                                                value={turma.id}
                                            >
                                                {turma.nome}
                                            </option>

                                        ))}

                                    </select>

                                </div>

                            </div>


                            {/* STATUS */}

                            <div>

                                <label className="mb-2 block text-sm font-medium">
                                    Status
                                </label>

                                <select
                                    value={status}
                                    onChange={(e) =>
                                        setStatus(
                                            e.target.value as StatusAtividade
                                        )
                                    }
                                    className="w-full rounded-lg border border-slate-200 bg-white px-4 py-3 text-sm outline-none focus:border-indigo-500"
                                >

                                    <option value="PENDENTE">
                                        Pendente
                                    </option>

                                    <option value="EM_ANDAMENTO">
                                        Em andamento
                                    </option>

                                    <option value="CONCLUIDO">
                                        Concluído
                                    </option>

                                </select>

                            </div>


                            {/* BOTÕES */}

                            <div className="flex justify-end gap-3 pt-3">

                                <button
                                    type="button"
                                    onClick={() =>
                                        setModalAberto(false)
                                    }
                                    className="rounded-lg border border-slate-200 px-5 py-3 text-sm font-medium hover:bg-slate-50"
                                >
                                    Cancelar
                                </button>

                                <button
                                    type="submit"
                                    className="rounded-lg bg-gradient-to-r from-indigo-500 to-purple-600 px-5 py-3 text-sm font-semibold text-white shadow-md"
                                >

                                    {modoEdicao
                                        ? "Salvar alterações"
                                        : "Criar atividade"}

                                </button>

                            </div>

                        </form>

                    </div>

                </div>

            )}


            {/* ================================= */}
            {/* MODAL VISUALIZAR */}
            {/* ================================= */}

            {modalVisualizar && atividadeSelecionada && (

                <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4">

                    <div className="w-full max-w-xl rounded-2xl bg-white shadow-2xl">

                        <div className="flex items-center justify-between border-b px-6 py-5">

                            <div>

                                <span className="text-xs font-semibold uppercase text-indigo-500">
                                    Atividade
                                </span>

                                <h2 className="mt-1 text-xl font-bold">
                                    {atividadeSelecionada.titulo}
                                </h2>

                            </div>

                            <button
                                onClick={() =>
                                    setModalVisualizar(false)
                                }
                                className="rounded-lg p-2 hover:bg-slate-100"
                            >
                                <X size={20} />
                            </button>

                        </div>


                        <div className="space-y-5 p-6">

                            <div>

                                <p className="mb-1 text-xs font-semibold uppercase text-slate-400">
                                    Descrição
                                </p>

                                <p className="text-sm leading-6 text-slate-600">
                                    {atividadeSelecionada.descricao}
                                </p>

                            </div>


                            <div className="grid grid-cols-3 gap-3">

                                <div className="rounded-xl bg-indigo-50 p-4">

                                    <p className="text-xs text-indigo-500">
                                        Pontuação
                                    </p>

                                    <p className="mt-1 text-xl font-bold text-indigo-700">
                                        {atividadeSelecionada.pontuacao}
                                    </p>

                                    <p className="text-xs text-indigo-400">
                                        XP
                                    </p>

                                </div>


                                <div className="rounded-xl bg-purple-50 p-4">

                                    <p className="text-xs text-purple-500">
                                        Turma
                                    </p>

                                    <p className="mt-1 font-bold text-purple-700">
                                        {atividadeSelecionada.turma?.nome ||
                                            "Sem turma"}
                                    </p>

                                </div>


                                <div className="rounded-xl bg-emerald-50 p-4">

                                    <p className="text-xs text-emerald-500">
                                        Status
                                    </p>

                                    <p className="mt-1 font-bold text-emerald-700">
                                        {statusLabel(
                                            atividadeSelecionada.status
                                        )}
                                    </p>

                                </div>

                            </div>


                            <div className="flex justify-end">

                                <button
                                    onClick={() =>
                                        setModalVisualizar(false)
                                    }
                                    className="rounded-lg bg-slate-900 px-5 py-3 text-sm font-medium text-white"
                                >
                                    Fechar
                                </button>

                            </div>

                        </div>

                    </div>

                </div>

            )}

        </div>
    );
}

export default AtividadesProfessor;