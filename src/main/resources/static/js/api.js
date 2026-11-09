async function readJsonResponse(
    response,
    fallbackMessage,
    { message, includeValidation = false } = {}
) {
    if (response.ok) {
        return response.json();
    }

    let problem;

    try {
        problem = await response.json();
    } catch {
        // El servidor podría responder sin un cuerpo JSON válido.
    }

    let validationMessages;

    if (includeValidation && Array.isArray(problem?.errors)) {
        validationMessages = problem.errors
            .filter(error =>
                typeof error?.field === "string"
                && typeof error?.message === "string"
            )
            .map(error => `${error.field}: ${error.message}`)
            .join(" ");
    }

    const detail = typeof problem?.detail === "string"
        ? problem.detail
        : undefined;

    const errorMessage =
        message
        || validationMessages
        || detail
        || fallbackMessage;

    const header = response.headers.get("X-Request-ID");

    const requestId = header
        && /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(header)
        ? header
        : undefined;

    const error = new Error(
        requestId
            ? `${errorMessage} Identificador de solicitud: ${requestId}.`
            : errorMessage
    );

    error.status = response.status;
    error.requestId = requestId;

    throw error;
}

export async function fetchIncidentPage(page, status = "", size = 3) {
    const params = new URLSearchParams({
        page: String(page),
        size: String(size)
    });

    if (status) {
        params.set("status", status);
    }

    const response = await fetch(`/api/incidents?${params}`, {
        headers: { Accept: "application/json" }
    });

    return readJsonResponse(
        response,
        `La consulta falló: HTTP ${response.status}.`
    );
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

    return readJsonResponse(
        response,
        `No se pudo crear la incidencia: HTTP ${response.status}.`,
        { includeValidation: true }
    );
}

export async function fetchIncident(id, signal) {
    const response = await fetch(
        `/api/incidents/${encodeURIComponent(id)}`,
        {
            headers: { Accept: "application/json" },
            signal
        }
    );

    return readJsonResponse(
        response,
        `No se pudo consultar la incidencia: HTTP ${response.status}.`,
        {
            message: response.status === 404
                ? "La incidencia ya no está disponible."
                : undefined
        }
    );
}

export async function changeIncidentStatus(id, status) {
    const response = await fetch(
        `/api/incidents/${encodeURIComponent(id)}/status`,
        {
            method: "PATCH",
            headers: {
                Accept: "application/json",
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ status })
        }
    );

    return readJsonResponse(
        response,
        `No se pudo cambiar el estado: HTTP ${response.status}.`
    );
}

export async function analyzeIncident(id, signal) {
    const response = await fetch(
        `/api/incidents/${encodeURIComponent(id)}/analysis`,
        {
            method: "POST",
            headers: { Accept: "application/json" },
            signal
        }
    );

    return readJsonResponse(
        response,
        `No se pudo analizar la incidencia: HTTP ${response.status}.`,
        {
            message: response.status === 503
                ? "El análisis está desactivado o temporalmente no disponible."
                : `No se pudo analizar la incidencia: HTTP ${response.status}.`
        }
    );
}