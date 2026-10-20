
document.addEventListener("DOMContentLoaded", () => {
    const page = document.querySelector("[data-organization-id]");
    const modal = document.getElementById("integrationModal");
    const openButton = document.getElementById("openIntegrationModal");
    const connected = document.getElementById("connectedIntegrations");

    if (!page) return;

    const organizationId = encodeURIComponent(page.dataset.organizationId);
    const base = `/organizations/${organizationId}/integrations`;
    const csrf = document.querySelector('input[name="_csrf"]')?.value;

    const headers = {
        "Accept": "application/json",
        "Content-Type": "application/json",
        "X-CSRF-TOKEN": csrf || ""
    };

    const closeButton = document.getElementById("closeIntegrationModal");
    const selectButton = document.getElementById("integrationSelect");
    const selectedService = document.getElementById("integrationSelectedService");
    const options = document.getElementById("integrationOptions");
    const setupPanel = document.getElementById("integrationSetup");
    const description = document.getElementById("integrationDescription");
    const instructions = document.getElementById("integrationInstructions");
    const fields = document.getElementById("integrationFields");
    const form = document.getElementById("integrationForm");
    const submit = document.getElementById("integrationSubmit");
    const feedback = document.getElementById("integrationFeedback");

    let selected = null;
    let pollTimer = null;
    let qrTimer = null;
    let generation = 0;

    async function request(url, options = {}) {
        const response = await fetch(url, {
            credentials: "same-origin",
            cache: "no-store",
            ...options
        });

        if (!response.ok) {
            let message = `Request failed (${response.status})`;

            try {
                const data = await response.json();
                message = data.detail || data.message || message;
            } catch (_) {}

            throw new Error(message);
        }

        if (response.status === 204) return null;
        return response.json();
    }

    function stopPolling() {
        generation++;

        if (pollTimer) clearTimeout(pollTimer);
        if (qrTimer) clearTimeout(qrTimer);

        pollTimer = null;
        qrTimer = null;
    }

    function setFeedback(message) {
        if (feedback) feedback.textContent = message || "";
    }

    function closeOptions() {
        if (options) options.hidden = true;
        selectButton?.setAttribute("aria-expanded", "false");
    }

    function closeModal() {
        stopPolling();
        modal.hidden = true;
        closeOptions();
        form.reset();
        openButton?.focus();
    }

    async function loadConnected() {
        if (!connected) return;

        try {
            const integrations = await request(`${base}/connected`);
            connected.replaceChildren();

            if (!integrations.length) {
                connected.textContent = "No integrations connected yet.";
                return;
            }

            for (const integration of integrations) {
                const card = document.createElement("div");
                card.className = "connected-integration";

                const icon = document.createElement("img");
                icon.className = "connected-integration-icon";
                icon.src = integration.iconPath || "/icons/telegram.svg";
                icon.alt = "";

                const info = document.createElement("div");
                info.className = "connected-integration-info";

                const name = document.createElement("strong");
                name.textContent = integration.displayName;

                const type = document.createElement("small");
                type.textContent = integration.type;

                info.append(name, type);

                const actions = document.createElement("div");
                actions.className = "integration-card-actions";

                const status = document.createElement("span");
                status.className = integration.enabled
                    ? "integration-status-connected"
                    : "integration-status-disabled";

                status.textContent = integration.enabled
                    ? "Connected"
                    : "Disabled";

                actions.append(status);

                if (integration.disconnectable && openButton) {
                    const button = document.createElement("button");
                    button.type = "button";
                    button.className = "integration-disconnect-button";
                    button.textContent = "Disconnect";

                    button.onclick = async () => {
                        if (!confirm(`Disconnect ${integration.displayName}?`)) {
                            return;
                        }

                        button.disabled = true;

                        try {
                            await request(
                                `${base}/${encodeURIComponent(integration.id)}/disconnect`,
                                { method: "POST", headers }
                            );

                            await loadConnected();
                        } catch (error) {
                            alert(error.message);
                            button.disabled = false;
                        }
                    };

                    actions.append(button);
                }

                card.append(icon, info, actions);
                connected.append(card);
            }
        } catch (error) {
            connected.textContent =
                `Could not load integrations: ${error.message}`;
        }
    }

    function selectProvider(provider) {
        stopPolling();
        selected = provider;
        closeOptions();

        selectedService.replaceChildren();

        const icon = document.createElement("img");
        icon.src = provider.iconPath;
        icon.alt = "";
        icon.width = 22;
        icon.height = 22;

        const label = document.createElement("span");
        label.textContent = provider.displayName;

        selectedService.append(icon, label);
        description.textContent = provider.setup.description || "";

        instructions.replaceChildren();

        for (const step of provider.setup.instructions || []) {
            const li = document.createElement("li");
            li.textContent = step;
            instructions.append(li);
        }

        fields.replaceChildren();
        form.reset();

        for (const field of provider.setup.fields || []) {
            const wrapper = document.createElement("div");
            wrapper.className = "integration-field";

            const label = document.createElement("label");
            label.textContent = field.label;
            label.htmlFor = `integration-${field.name}`;

            const input = document.createElement("input");
            input.id = label.htmlFor;
            input.name = field.name;

            input.type = ["text", "password", "email"].includes(field.type)
                ? field.type
                : "text";

            input.placeholder = field.placeholder || "";
            input.required = !!field.required;
            input.autocomplete = "off";

            wrapper.append(label, input);
            fields.append(wrapper);
        }

        submit.hidden = false;
        submit.disabled = !["TOKEN", "QR", "OAUTH"].includes(
            provider.setup.connectionType
        );

        submit.textContent = `Connect ${provider.displayName}`;

        
        setFeedback("");
        setupPanel.hidden = false;

        if (provider.setup.connectionType === "QR") {
            submit.hidden = true;
            connectSelectedProvider();
        }

    }

    function normalizeQr(payload) {
        if (typeof payload === "string") {
            if (payload.startsWith("data:image/")) {
                return payload;
            }

            if (
                /^[A-Za-z0-9+/=\r\n]+$/.test(payload) &&
                payload.length > 100
            ) {
                return `data:image/png;base64,${payload.replace(/\s/g, "")}`;
            }

            return null;
        }

        if (!payload || typeof payload !== "object") {
            return null;
        }

        for (const key of [
            "base64",
            "qrcode",
            "qrCode",
            "qr",
            "image"
        ]) {
            if (payload[key]) {
                const result = normalizeQr(payload[key]);
                if (result) return result;
            }
        }

        return null;
    }

    async function refreshQr(url, expectedGeneration) {
        const response = await request(url);

        if (generation !== expectedGeneration || modal.hidden) {
            return;
        }

        const source = normalizeQr(response);

        if (!source) {
            throw new Error("Evolution API did not return a QR image");
        }

        let img = fields.querySelector('img[data-connection-qr]');

        if (!img) {
            img = document.createElement("img");
            img.dataset.connectionQr = "true";
            img.alt = "WhatsApp QR code";

            img.style.cssText = [
                "display:block",
                "width:240px",
                "max-width:100%",
                "margin:16px auto",
                "background:white",
                "padding:10px",
                "border-radius:8px"
            ].join(";");

            fields.append(img);
        }

        if (img.src !== source) {
            img.src = source;
        }
    }

    function startQrRefresh(url) {
        const expectedGeneration = generation;

        async function refresh() {
            if (generation !== expectedGeneration || modal.hidden) {
                return;
            }

            try {
                await refreshQr(url, expectedGeneration);
            } catch (error) {
                if (generation === expectedGeneration) {
                    setFeedback(`Could not refresh QR: ${error.message}`);
                }
            }

            if (generation === expectedGeneration && !modal.hidden) {
                qrTimer = setTimeout(refresh, 30000);
            }
        }

        qrTimer = setTimeout(refresh, 30000);
    }

    async function renderResult(result) {
        setFeedback(result.view?.message || result.status || "");
        fields.replaceChildren();


        if (result.view?.type === "QR" && result.view.imageUrl) {
            setFeedback("Loading QR code...");

            try {
                await refreshQr(result.view.imageUrl, generation);

                setFeedback(
                    "Scan the QR code using WhatsApp > Linked devices."
                );
            } catch (error) {
                setFeedback(
                    `QR is not ready yet: ${error.message}`
                );
            }

            startQrRefresh(result.view.imageUrl);
        }

        if (result.status === "CONNECTED") {
            stopPolling();
            submit.hidden = true;
            await loadConnected();
            return;
        }

        submit.hidden = true;

        if (result.nextAction?.type === "POLL") {
            pollStatus(result.nextAction);
        }
    }


    function pollStatus(action) {
        const currentGeneration = generation;
        const interval = Math.max(1000, action.intervalMs || 2000);

        async function tick() {
            if (modal.hidden || generation !== currentGeneration) {
                return;
            }

            try {
                const state = await request(action.url);

                if (generation !== currentGeneration || modal.hidden) {
                    return;
                }

                if (state.status === "CONNECTED") {
                    stopPolling();

                    setFeedback("WhatsApp connected successfully!");
                    fields.replaceChildren();

                    await loadConnected();

                    // Close modal automatically after successful connection.
                    closeModal();
                    return;
                }

                if (["FAILED", "CONFLICT", "DISCONNECTED"].includes(
                    state.status
                )) {
                    stopPolling();

                    setFeedback(
                        `Connection ${state.status.toLowerCase()}. Please try again.`
                    );

                    submit.hidden = false;
                    submit.disabled = false;
                    return;
                }

                setFeedback(
                    "Waiting for WhatsApp QR code to be scanned..."
                );

            } catch (error) {
                setFeedback(
                    `Checking connection: ${error.message}`
                );
            }

            if (generation === currentGeneration) {
                pollTimer = setTimeout(tick, interval);
            }
        }

        pollTimer = setTimeout(tick, interval);
    }


    if (!modal || !openButton) {
        loadConnected();
        return;
    }

    openButton.onclick = async () => {
        stopPolling();

        modal.hidden = false;
        setupPanel.hidden = true;
        selected = null;

        selectedService.textContent = "Choose a service...";
        setFeedback("Loading integrations...");

        try {
            const providers = await request(`${base}/catalog`);
            options.replaceChildren();

            for (const provider of providers) {
                const button = document.createElement("button");
                button.type = "button";
                button.className = "integration-option";

                const icon = document.createElement("img");
                icon.src = provider.iconPath;
                icon.alt = "";
                icon.width = 24;
                icon.height = 24;

                const name = document.createElement("span");
                name.textContent = provider.displayName;

                button.append(icon, name);
                button.onclick = () => selectProvider(provider);

                options.append(button);
            }

            setFeedback(
                providers.length ? "" : "No integrations available"
            );
        } catch (error) {
            setFeedback(error.message);
        }
    };

    closeButton.onclick = closeModal;

    modal.addEventListener("click", event => {
        if (event.target === modal) closeModal();
    });

    document.addEventListener("keydown", event => {
        if (event.key === "Escape" && !modal.hidden) {
            if (!options.hidden) {
                closeOptions();
            } else {
                closeModal();
            }
        }
    });

    selectButton.onclick = () => {
        options.hidden = !options.hidden;

        selectButton.setAttribute(
            "aria-expanded",
            String(!options.hidden)
        );
    };

    
    async function connectSelectedProvider() {
        if (!selected) return;

        const provider = selected;
        const mode = provider.setup.connectionType;

        if (mode === "OAUTH") {
            const url = provider.setup.submitUrl?.replace(
                "{id}",
                organizationId
            );

            if (!url || !url.startsWith("/") || url.startsWith("//")) {
                setFeedback("Invalid OAuth URL");
                return;
            }

            window.location.assign(url);
            return;
        }

        if (!["TOKEN", "QR"].includes(mode)) return;

        stopPolling();

        const currentGeneration = generation;

        submit.disabled = true;

        if (mode === "QR") {
            submit.hidden = true;
            setFeedback("Generating QR code...");
        } else {
            setFeedback("Connecting...");
        }

        const credentials = Object.fromEntries(
            new FormData(form).entries()
        );

        delete credentials._csrf;

        try {
            const result = await request(
                `${base}/connect/${encodeURIComponent(provider.type)}`,
                {
                    method: "POST",
                    headers,
                    body: JSON.stringify(credentials)
                }
            );

            if (
                generation !== currentGeneration ||
                modal.hidden ||
                selected !== provider
            ) {
                return;
            }

            await renderResult(result);

        } catch (error) {
            if (
                generation !== currentGeneration ||
                modal.hidden ||
                selected !== provider
            ) {
                return;
            }

            setFeedback(error.message);
            submit.disabled = false;
            submit.hidden = false;
            submit.textContent = "Retry connection";
        }
    }

    form.addEventListener("submit", async event => {
        event.preventDefault();
        await connectSelectedProvider();
    });


    loadConnected();
});
