import { fetchIncidentPage } from "./api.js";

const elements = {
    refresh: document.querySelector("#refresh"),
    previous: document.querySelector("#previous"),
    next: document.querySelector("#next"),
    feedback: document.querySelector("#feedback"),
    pageLabel: document.querySelector("#page-label"),
    tableContainer: document.querySelector("#table-container"),
    rows: document.querySelector("#incidents")
};

const statuses = {
    OPEN: { label: "Abierta", className: "badge-open" },
    IN_PROGRESS: { label: "En atención", className: "badge-progress" },
    RESOLVED: { label: "Resuelta", className: "badge-resolved" }
};

const dateFormatter = new Intl.DateTimeFormat("es-CL", {
    dateStyle: "medium",
    timeStyle: "short"
});

let currentPage = 0;
let hasNext = false;
let loading = false;

function updateControls() {
    elements.refresh.disabled = loading;
    elements.previous.disabled = loading || currentPage === 0;
    elements.next.disabled = loading || !hasNext;
}

function renderIncidents(incidents) {
    elements.rows.replaceChildren();

    for (const incident of incidents) {
        const row = document.createElement("tr");
        const titleCell = document.createElement("td");
        const statusCell = document.createElement("td");
        const dateCell = document.createElement("td");

        const title = document.createElement("span");
        title.className = "incident-title";
        title.textContent = incident.title;

        const id = document.createElement("span");
        id.className = "incident-id";
        id.textContent = incident.id;

        const status = statuses[incident.status];
        const badge = document.createElement("span");
        badge.className = `badge ${status?.className ?? ""}`;
        badge.textContent = status?.label ?? incident.status;

        const time = document.createElement("time");
        time.dateTime = incident.createdAt;
        time.textContent = dateFormatter.format(new Date(incident.createdAt));

        titleCell.append(title, id);
        statusCell.append(badge);
        dateCell.append(time);
        row.append(titleCell, statusCell, dateCell);
        elements.rows.append(row);
    }
}

async function loadPage(page) {
    if (loading) return;

    loading = true;
    updateControls();
    elements.tableContainer.setAttribute("aria-busy", "true");
    elements.feedback.classList.remove("error");
    elements.feedback.textContent = "Cargando incidencias…";

    try {
        const result = await fetchIncidentPage(page);

        renderIncidents(result.content);
        currentPage = result.page;
        hasNext = result.hasNext;

        elements.pageLabel.textContent = `Página ${currentPage + 1}`;
        elements.tableContainer.hidden = result.content.length === 0;
        elements.feedback.textContent = result.content.length === 0
            ? "No hay incidencias en esta página."
            : "";
    } catch (error) {
        elements.feedback.classList.add("error");
        elements.feedback.textContent =
            `No se pudo cargar el listado. ${error.message} Puedes reintentar.`;
    } finally {
        loading = false;
        elements.tableContainer.setAttribute("aria-busy", "false");
        updateControls();
    }
}

elements.refresh.addEventListener("click", () => loadPage(currentPage));
elements.previous.addEventListener("click", () => loadPage(currentPage - 1));
elements.next.addEventListener("click", () => loadPage(currentPage + 1));

loadPage(0);