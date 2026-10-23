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

export async function createIncident(title, description) {
    const response = await fetch("/api/incidents", {
        method: "POST",
        headers: {
            Accept: "application/json",
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ title, description })
    });

    if (!response.ok) {
        let problem;

        try {
            problem = await response.json();
        } catch {
            // El servidor podría responder sin un cuerpo JSON.
        }

        const validationMessages = problem?.errors
            ?.map(error => `${error.field}: ${error.message}`)
            .join(" ");

        throw new Error(
            validationMessages
            || problem?.detail
            || `No se pudo crear la incidencia: HTTP ${response.status}.`
        );
    }

    return response.json();
}