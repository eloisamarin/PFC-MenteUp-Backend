const API_URL = "http://localhost:8080";

export async function apiFetch(
    endpoint: string,
    options: RequestInit = {}
) {
    const token = localStorage.getItem("token");

    const headers = new Headers(options.headers);

    headers.set("Content-Type", "application/json");

    if (token) {
        headers.set("Authorization", token);
    }

    const resposta = await fetch(`${API_URL}${endpoint}`, {
        ...options,
        cache: "no-store",
        headers,
    });

    if (resposta.status === 401) {
        console.error("Token inválido ou expirado");

        localStorage.removeItem("token");
        localStorage.removeItem("usuario");

        window.location.href = "/login";

        throw new Error("Não autorizado");
    }

    return resposta;
}