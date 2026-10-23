import { analyzeIncident } from "./api.js";

const elements = {
    section: document.querySelector("#incident-analysis"),
    run: document.querySelector("#analysis-run"),
    feedback: document.querySelector("#analysis-feedback"),
    result: document.querySelector("#analysis-result"),
    category: document.querySelector("#analysis-category"),
    summary: document.querySelector("#analysis-summary")
};

const categories = {
    HARDWARE: "Hardware",
    SOFTWARE: "Software",
    NETWORK: "Red",
    ACCESS: "Acceso",
    OTHER: "Otra"
};

let incidentId;
let activeRequest;

export function resetIncidentAnalysis() {
    activeRequest?.abort();
    activeRequest = undefined;
    incidentId = undefined;

    elements.section.hidden = true;
    elements.section.setAttribute("aria-busy", "false");
    elements.result.hidden = true;
    elements.category.textContent = "";
    elements.summary.textContent = "";
    elements.feedback.textContent = "";
    elements.feedback.classList.remove("error");
    elements.run.disabled = false;
    elements.run.textContent = "Analizar con Gemini";
}

export function setAnalysisIncident(id) {
    incidentId = id;
    elements.section.hidden = false;
}

async function handleAnalysis() {
    if (!incidentId || activeRequest) return;

    const request = new AbortController();
    activeRequest = request;

    elements.run.disabled = true;
    elements.run.textContent = "Analizando…";
    elements.result.hidden = true;
    elements.feedback.classList.remove("error");
    elements.feedback.textContent = "Esperando el análisis de Gemini…";
    elements.section.setAttribute("aria-busy", "true");

    try {
        const result = await analyzeIncident(
            incidentId,
            request.signal
        );

        if (request !== activeRequest) return;

        elements.category.textContent =
            categories[result.suggestedCategory]
            ?? result.suggestedCategory;

        elements.summary.textContent = result.suggestedSummary;
        elements.result.hidden = false;
        elements.feedback.textContent = "Análisis completado.";
    } catch (error) {
        if (request !== activeRequest || request.signal.aborted) return;

        elements.feedback.classList.add("error");
        elements.feedback.textContent = error.message;
    } finally {
        if (request === activeRequest) {
            activeRequest = undefined;
            elements.run.disabled = false;
            elements.run.textContent = "Analizar con Gemini";
            elements.section.setAttribute("aria-busy", "false");
        }
    }
}

elements.run.addEventListener("click", handleAnalysis);

document.querySelector("#incident-detail")
    .addEventListener("close", resetIncidentAnalysis);