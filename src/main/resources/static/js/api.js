export async function fetchIncidentPage(page, size = 3) {
    const params = new URLSearchParams({
        page: String(page),
        size: String(size)
    });

    const response = await fetch(`/api/incidents?${params}`, {
        headers: { Accept: "application/json" }
    });

    if (!response.ok) {
        let problem;

        try {
            problem = await response.json();
        } catch {
            // La respuesta de error podría no contener JSON.
        }

        throw new Error(
            problem?.detail ?? `La consulta falló: HTTP ${response.status}.`
        );
    }

    return response.json();
}