
document.addEventListener("DOMContentLoaded", () => {
    const page = document.querySelector("[data-organization-id]");
    const modal = document.getElementById("integrationModal");
    const openButton = document.getElementById("openIntegrationModal");

    if (!page || !modal || !openButton) return;

    const organizationId = page.dataset.organizationId;
    const closeButton = document.getElementById("closeIntegrationModal");
    const selectButton = document.getElementById("integrationSelect");
    const selectedService = document.getElementById("integrationSelectedService");
    const options = document.getElementById("integrationOptions");
    const setupPanel = document.getElementById("integrationSetup");
    const description = document.getElementById("integrationDescription");
    const instructions = document.getElementById("integrationInstructions");
    const fields = document.getElementById("integrationFields");
    const form = document.getElementById("integrationForm");
    const submitButton = document.getElementById("integrationSubmit");
    const feedback = document.getElementById("integrationFeedback");

    let providers = [];
    let selectedProvider = null;


    
    const connectedContainer =
        document.getElementById("connectedIntegrations");

    async function loadConnectedIntegrations() {
        if (!connectedContainer) return;

        try {
            const response = await fetch(
                `/organizations/${encodeURIComponent(organizationId)}/integrations/connected`,
                {
                    credentials: "same-origin",
                    headers: {
                        "Accept": "application/json"
                    },
                    cache: "no-store"
                }
            );

            if (!response.ok) {
                throw new Error(
                    `Failed to load integrations: ${response.status}`
                );
            }

            const integrations = await response.json();

            connectedContainer.replaceChildren();

            if (integrations.length === 0) {
                const empty = document.createElement("p");
                empty.className = "integration-empty";
                empty.textContent = "No integrations connected yet.";
                connectedContainer.appendChild(empty);
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

                const status = document.createElement("span");
                status.className = integration.enabled
                    ? "integration-status-connected"
                    : "integration-status-disabled";

                status.textContent = integration.enabled
                    ? "Connected"
                    : "Disabled";

                
                const actions = document.createElement("div");
                actions.className = "integration-card-actions";

                actions.appendChild(status);

                if (integration.disconnectable && openButton) {
                    const disconnectButton = document.createElement("button");
                    disconnectButton.type = "button";
                    disconnectButton.className = "integration-disconnect-button";
                    disconnectButton.textContent = "Disconnect";

                    disconnectButton.addEventListener("click", async () => {
                        const confirmed = confirm(
                            `Disconnect ${integration.displayName}?`
                        );

                        if (!confirmed) return;

                        const csrfInput = document.querySelector(
                            "#integrationForm input[type='hidden']"
                        );

                        if (!csrfInput) {
                            alert("CSRF token is missing.");
                            return;
                        }

                        disconnectButton.disabled = true;
                        disconnectButton.textContent = "Disconnecting...";

                        try {
                            const response = await fetch(
                                `/organizations/${encodeURIComponent(organizationId)}` +
                                `/integrations/${encodeURIComponent(integration.id)}/disconnect`,
                                {
                                    method: "POST",
                                    credentials: "same-origin",
                                    headers: {
                                        "Accept": "application/json",
                                        [csrfInput.name]: csrfInput.value
                                    }
                                }
                            );

                            if (!response.ok) {
                                throw new Error(
                                    `Disconnect failed: ${response.status}`
                                );
                            }

                            await loadConnectedIntegrations();

                        } catch (error) {
                            console.error(error);
                            alert("Could not disconnect integration.");
                            disconnectButton.disabled = false;
                            disconnectButton.textContent = "Disconnect";
                        }
                    });

                    actions.appendChild(disconnectButton);
                }

                card.append(icon, info, actions);
                connectedContainer.appendChild(card);

            }

        } catch (error) {
            console.error(error);

            connectedContainer.textContent =
                "Could not load connected integrations.";
        }
    }

    loadConnectedIntegrations();

    function setFeedback(message) {
        feedback.textContent = message;
    }

    function closeOptions() {
        options.hidden = true;
        selectButton.setAttribute("aria-expanded", "false");
    }

    function closeModal() {
        modal.hidden = true;
        closeOptions();
        form.reset();
        openButton.focus();
    }

    async function openModal() {
        modal.hidden = false;
        setupPanel.hidden = true;
        selectedProvider = null;
        selectedService.textContent = "Choose a service...";
        setFeedback("Loading integrations...");
        openButton.disabled = true;

        try {
            const response = await fetch(
                `/organizations/${encodeURIComponent(organizationId)}/integrations/catalog`,
                {
                    credentials: "same-origin",
                    headers: { "Accept": "application/json" }
                }
            );

            if (!response.ok) {
                throw new Error("Could not load integrations.");
            }

            providers = await response.json();
            renderOptions();

            setFeedback(
                providers.length === 0
                    ? "No integrations are available."
                    : ""
            );

            selectButton.focus();
        } catch (error) {
            setFeedback("Failed to load integrations. Please try again.");
        } finally {
            openButton.disabled = false;
        }
    }

    function renderOptions() {
        options.replaceChildren();

        for (const provider of providers) {
            const button = document.createElement("button");
            button.type = "button";
            button.className = "integration-option";
            button.setAttribute("role", "option");
            button.setAttribute("aria-selected", "false");

            const icon = document.createElement("img");
            icon.src = provider.iconPath;
            icon.alt = "";
            icon.width = 24;
            icon.height = 24;

            const name = document.createElement("span");
            name.textContent = provider.displayName;

            button.append(icon, name);

            button.addEventListener("click", () => {
                selectProvider(provider);
            });

            options.appendChild(button);
        }
    }

    function selectProvider(provider) {
        selectedProvider = provider;
        selectedService.replaceChildren();

        const icon = document.createElement("img");
        icon.src = provider.iconPath;
        icon.alt = "";
        icon.width = 22;
        icon.height = 22;

        const name = document.createElement("span");
        name.textContent = provider.displayName;

        selectedService.append(icon, name);
        closeOptions();

        description.textContent = provider.setup.description;

        instructions.replaceChildren();

        for (const instruction of provider.setup.instructions) {
            const item = document.createElement("li");
            item.textContent = instruction;
            instructions.appendChild(item);
        }

        fields.replaceChildren();
        form.reset();

        const connectionType = provider.setup.connectionType;

        if (connectionType === "TOKEN") {
            for (const field of provider.setup.fields) {
                const wrapper = document.createElement("div");
                wrapper.className = "integration-field";

                const label = document.createElement("label");
                label.textContent = field.label;
                label.htmlFor = `integration-${field.name}`;

                const input = document.createElement("input");
                input.id = `integration-${field.name}`;
                input.name = field.name;
                input.type = ["text", "password", "email"].includes(field.type)
                    ? field.type
                    : "text";
                input.placeholder = field.placeholder || "";
                input.required = field.required;
                input.autocomplete = "off";

                wrapper.append(label, input);
                fields.appendChild(wrapper);
            }

            submitButton.textContent = `Connect ${provider.displayName}`;
            submitButton.disabled = false;

        } else {
            submitButton.textContent = "Connection not available yet";
            submitButton.disabled = true;
        }

        setFeedback("");
        setupPanel.hidden = false;

        const firstInput = fields.querySelector("input");
        if (firstInput) firstInput.focus();
    }

    selectButton.addEventListener("click", () => {
        if (options.hidden) {
            options.hidden = false;
            selectButton.setAttribute("aria-expanded", "true");
            options.querySelector("button")?.focus();
        } else {
            closeOptions();
        }
    });

    openButton.addEventListener("click", openModal);
    closeButton.addEventListener("click", closeModal);

    modal.addEventListener("click", (event) => {
        if (event.target === modal) closeModal();
    });

    document.addEventListener("keydown", (event) => {
        if (modal.hidden) return;

        if (event.key === "Escape") {
            if (!options.hidden) {
                closeOptions();
                selectButton.focus();
            } else {
                closeModal();
            }
        }
    });

    form.addEventListener("submit", (event) => {
        if (!selectedProvider ||
            selectedProvider.setup.connectionType !== "TOKEN") {
            event.preventDefault();
            return;
        }

        const template = selectedProvider.setup.submitUrl;

        // Only allow application-relative integration endpoints.
        const expectedPrefix =
            `/organizations/{id}/integrations/`;

        if (!template.startsWith(expectedPrefix)) {
            event.preventDefault();
            setFeedback("Invalid integration endpoint.");
            return;
        }

        const action = template.replace(
            "{id}",
            encodeURIComponent(organizationId)
        );

        if (!/^\/organizations\/[^/]+\/integrations\/[a-zA-Z0-9_-]+$/.test(action)) {
            event.preventDefault();
            setFeedback("Invalid integration endpoint.");
            return;
        }

        form.action = action;
        submitButton.disabled = true;
        submitButton.textContent = "Connecting...";
    });
});
