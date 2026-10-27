import { fetchIncident, changeIncidentStatus } from "./api.js";
import { resetIncidentAnalysis, setAnalysisIncident } from "./analysis.js";

const detail = {
    dialog: document.querySelector("#incident-detail"),
    heading: document.querySelector("#detail-heading"),
    close: document.querySelector("#detail-close"),
    feedback: document.querySelector("#detail-feedback"),
    data: document.querySelector("#detail-data"),
    title: document.querySelector("#detail-title"),
    id: document.querySelector("#detail-id"),
    description: document.querySelector("#detail-description"),
    status: document.querySelector("#detail-status"),
    created: document.querySelector("#detail-created"),
    updated: document.querySelector("#detail-updated"),
    actions: document.querySelector("#detail-actions"),
    changeStatus: document.querySelector("#detail-change-status")
};

const labels = {
    OPEN: "Abierta",
    IN_PROGRESS: "En atención",
    RESOLVED: "Resuelta"
};

const transitions = {
    OPEN: {
        target: "IN_PROGRESS",
        label: "Iniciar atención"
    },
    IN_PROGRESS: {
        target: "RESOLVED",
        label: "Marcar como resuelta"
    },
    RESOLVED: {
        target: "IN_PROGRESS",
        label: "Reabrir atención"
    }
};

const dateFormatter = new Intl.DateTimeFormat("es-CL", {
    dateStyle: "medium",
    timeStyle: "short"
});

let activeRequest;
let openingButton;
let currentIncident;
let saving = false;

export async function openIncidentDetail(id, trigger) {
    if (saving) return;

    resetIncidentAnalysis();

    currentIncident = undefined;
    detail.actions.hidden = true;
    activeRequest?.abort();

    const request = new AbortController();
    activeRequest = request;
    openingButton = trigger;

    detail.data.hidden = true;
    detail.dialog.setAttribute("aria-busy", "true");
    detail.feedback.classList.remove("error");
    detail.feedback.textContent = "Cargando detalle…";

    if (!detail.dialog.open) {
        detail.dialog.showModal();
        document.body.classList.add("modal-open");
    }

    detail.dialog.scrollTop = 0;
    detail.heading.focus({ preventScroll: true });

    try {
        const incident = await fetchIncident(id, request.signal);

        if (request !== activeRequest) return;

        renderIncident(incident);
        detail.data.hidden = false;
        detail.feedback.textContent = "";
    } catch (error) {
        if (request !== activeRequest || request.signal.aborted) return;

        detail.feedback.classList.add("error");
        detail.feedback.textContent = error.message;
    } finally {
        if (request === activeRequest) {
            detail.dialog.setAttribute("aria-busy", "false");
        }
    }
}

function renderIncident(incident) {
    detail.title.textContent = incident.title;
    detail.id.textContent = incident.id;
    detail.description.textContent = incident.description;
    detail.status.textContent =
        labels[incident.status] ?? incident.status;

    detail.created.textContent =
        dateFormatter.format(new Date(incident.createdAt));

    detail.updated.textContent =
        dateFormatter.format(new Date(incident.updatedAt));

    currentIncident = incident;

    setAnalysisIncident(incident.id);

    const transition = transitions[incident.status];

    detail.actions.hidden = !transition;
    detail.changeStatus.disabled = saving;
    detail.changeStatus.textContent = transition?.label ?? "";
}

function closeIncidentDetail() {
    if (saving) return;

    detail.dialog.close();
}

function handleDetailClosed() {
    activeRequest?.abort();
    activeRequest = undefined;

    document.body.classList.remove("modal-open");
    detail.dialog.setAttribute("aria-busy", "false");

    if (openingButton?.isConnected) {
        openingButton.focus({ preventScroll: true });
    } else {
        document.querySelector("#refresh").focus({
            preventScroll: true
        });
    }

    openingButton = undefined;
}

async function handleStatusChange() {
    if (saving || !currentIncident) return;

    const transition = transitions[currentIncident.status];
    if (!transition) return;

    const incidentId = currentIncident.id;

    saving = true;
    detail.close.disabled = true;
    detail.changeStatus.disabled = true;
    detail.changeStatus.textContent = "Guardando…";
    detail.dialog.setAttribute("aria-busy", "true");
    detail.feedback.classList.remove("error");
    detail.feedback.textContent = "Actualizando estado…";

    try {
        const updated = await changeIncidentStatus(
            incidentId,
            transition.target
        );

        renderIncident(updated);
        detail.feedback.textContent = "Estado actualizado correctamente.";

        document.dispatchEvent(new CustomEvent("incident-updated"));
    } catch (error) {
        detail.feedback.classList.add("error");
        detail.feedback.textContent =
            `${error.message} Cierra y vuelve a abrir el detalle para consultar el estado actual antes de reintentar.`;

        // Tras un fallo, no asumimos que el estado local siga vigente.
        detail.actions.hidden = true;
    } finally {
        saving = false;
        detail.close.disabled = false;
        detail.changeStatus.disabled = false;
        detail.dialog.setAttribute("aria-busy", "false");

        const nextTransition = transitions[currentIncident?.status];
        detail.changeStatus.textContent = nextTransition?.label ?? "";
    }
}

function handleDetailCancel(event) {
    if (saving) {
        event.preventDefault();
    }
}

let pointerStartedOutside = false;

function isOutsideDialog(event) {
    const bounds = detail.dialog.getBoundingClientRect();

    return event.clientX < bounds.left
        || event.clientX > bounds.right
        || event.clientY < bounds.top
        || event.clientY > bounds.bottom;
}

function handleDialogPointerDown(event) {
    pointerStartedOutside =
        event.isPrimary
        && event.button === 0
        && event.target === detail.dialog
        && isOutsideDialog(event);
}

function handleDialogBackdropClick(event) {
    const shouldClose =
        pointerStartedOutside
        && event.target === detail.dialog
        && isOutsideDialog(event);

    pointerStartedOutside = false;

    if (shouldClose) {
        closeIncidentDetail();
    }
}

detail.close.addEventListener("click", closeIncidentDetail);
detail.dialog.addEventListener("close", handleDetailClosed);
detail.changeStatus.addEventListener("click", handleStatusChange);
detail.dialog.addEventListener("cancel", handleDetailCancel);
detail.dialog.addEventListener("pointerdown", handleDialogPointerDown);
detail.dialog.addEventListener("click", handleDialogBackdropClick);