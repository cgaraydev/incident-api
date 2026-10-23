import { fetchIncidentPage, createIncident } from "./api.js";
import { openIncidentDetail } from "./detail.js";

const elements = {
    refresh: document.querySelector("#refresh"),
    previous: document.querySelector("#previous"),
    next: document.querySelector("#next"),
    feedback: document.querySelector("#feedback"),
    pageLabel: document.querySelector("#page-label"),
    tableContainer: document.querySelector("#table-container"),
    rows: document.querySelector("#incidents"),
    statusFilter: document.querySelector("#status-filter")
};

const creation = {
    form: document.querySelector("#create-form"),
    fields: document.querySelector("#create-fields"),
    title: document.querySelector("#incident-title"),
    description: document.querySelector("#incident-description"),
    submit: document.querySelector("#create-submit"),
    feedback: document.querySelector("#create-feedback")
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
let submitting = false;

function updateControls() {
    const busy = loading || submitting;

    elements.refresh.disabled = busy;
    elements.previous.disabled = busy || currentPage === 0;
    elements.next.disabled = busy || !hasNext;
    elements.statusFilter.disabled = busy;
    creation.submit.disabled = busy;
}

function renderIncidents(incidents) {
    elements.rows.replaceChildren();

    for (const incident of incidents) {
        const row = document.createElement("tr");
        const titleCell = document.createElement("td");
        const statusCell = document.createElement("td");
        const dateCell = document.createElement("td");

        const title = document.createElement("button");
        title.type = "button";
        title.className = "incident-title";
        title.textContent = incident.title;
        title.addEventListener("click", () => {
            openIncidentDetail(incident.id, title);
        });

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
    elements.tableContainer.hidden = true;

    try {
        const result = await fetchIncidentPage(page, elements.statusFilter.value);

        renderIncidents(result.content);
        currentPage = result.page;
        hasNext = result.hasNext;

        elements.pageLabel.textContent = `Página ${currentPage + 1}`;
        elements.tableContainer.hidden = result.content.length === 0;
        elements.feedback.textContent = result.content.length === 0 ? "No hay incidencias para esta página y filtro." : "";
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

async function handleCreate(event) {
    event.preventDefault();

    if (submitting || loading) return;

    creation.feedback.classList.remove("error");
    creation.feedback.textContent = "";

    if (!creation.title.value.trim()) {
        creation.feedback.classList.add("error");
        creation.feedback.textContent = "El título no puede estar vacío.";
        creation.title.focus();
        return;
    }

    if (!creation.description.value.trim()) {
        creation.feedback.classList.add("error");
        creation.feedback.textContent = "La descripción no puede estar vacía.";
        creation.description.focus();
        return;
    }

    submitting = true;
    creation.fields.disabled = true;
    creation.form.setAttribute("aria-busy", "true");
    creation.submit.textContent = "Guardando…";
    creation.feedback.textContent = "Creando incidencia…";
    updateControls();

    try {
        const incident = await createIncident(
            creation.title.value,
            creation.description.value
        );

        creation.form.reset();
        elements.statusFilter.value = "";
        creation.feedback.textContent = `Incidencia creada: ${incident.title}.`;

        await loadPage(0);
    } catch (error) {
        creation.feedback.classList.add("error");
        creation.feedback.textContent = `${error.message} Si hubo un problema de conexión, actualiza el listado antes de repetir el envío para comprobar si se guardó.`;
    } finally {
        submitting = false;
        creation.fields.disabled = false;
        creation.form.setAttribute("aria-busy", "false");
        creation.submit.textContent = "Crear incidencia";
        updateControls();
    }
}

creation.form.addEventListener("submit", handleCreate);

document.addEventListener("incident-updated", () => loadPage(0));

elements.statusFilter.addEventListener("change", () => {
    currentPage = 0;
    hasNext = false;
    elements.pageLabel.textContent = "";
    loadPage(0);
});

loadPage(0);