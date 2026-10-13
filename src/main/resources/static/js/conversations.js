
document.addEventListener("DOMContentLoaded", () => {

    // Page elements
    const app = document.getElementById("inboxApp");
    const layout = document.querySelector(".inbox-layout");

    const conversationList = document.getElementById("conversationList");
    const conversationCount = document.getElementById("conversationCount");
    const conversationSearch = document.getElementById("conversationSearch");

    const connectionStatus = document.getElementById("connectionStatus");
    const statusDot = document.querySelector(".status-dot");

    const chatPlaceholder = document.getElementById("chatPlaceholder");
    const chatHeader = document.getElementById("chatHeader");
    const chatContent = document.getElementById("chatContent");

    const contactAvatar = document.getElementById("contactAvatar");
    const contactName = document.getElementById("contactName");
    const contactPlatform = document.getElementById("contactPlatform");

    const messageList = document.getElementById("messageList");
    const mobileBackButton = document.getElementById("mobileBackButton");

    // Current application state
    const organizationId = app.dataset.organizationId;

    let conversations = [];
    let selectedConversationId = null;
    let activeFilter = "ALL";
    let messageRequestVersion = 0;

    // -------------------------
    // API
    // -------------------------

    async function fetchJson(url) {
        const response = await fetch(url, {
            method: "GET",
            credentials: "same-origin",
            headers: {
                "Accept": "application/json"
            }
        });

        if (!response.ok) {
            throw new Error(`Request failed: ${response.status}`);
        }

        const contentType = response.headers.get("content-type") || "";

        if (!contentType.includes("application/json")) {
            throw new Error("Expected JSON response. Please sign in again.");
        }

        return response.json();
    }

    async function loadConversations() {
        setConnectionStatus("Loading conversations...", false);

        try {
            const data = await fetchJson(
                `/api/organizations/${encodeURIComponent(organizationId)}/conversations`
            );

            if (!Array.isArray(data)) {
                throw new Error("Invalid conversations response");
            }

            conversations = data;

            renderConversations();

            setConnectionStatus("Inbox connected", false);

        } catch (error) {
            console.error("Failed to load conversations:", error);

            conversations = [];
            renderConversations();

            showListEmpty(
                "Unable to load conversations",
                "Please refresh the page and try again.",
                "!"
            );

            setConnectionStatus("Connection error", true);
        }
    }

    async function loadMessages(conversationId) {
        const requestVersion = ++messageRequestVersion;

        showMessagesNotice("Loading messages...");

        try {
            const messages = await fetchJson(
                `/api/conversations/${encodeURIComponent(conversationId)}/messages`
            );

            // Ignore an outdated response if another chat was selected
            if (requestVersion !== messageRequestVersion ||
                selectedConversationId !== conversationId) {
                return;
            }

            if (!Array.isArray(messages)) {
                throw new Error("Invalid messages response");
            }

            renderMessages(messages);

        } catch (error) {
            if (requestVersion !== messageRequestVersion ||
                selectedConversationId !== conversationId) {
                return;
            }

            console.error("Failed to load messages:", error);

            showMessagesNotice(
                "Unable to load messages. Please try again."
            );
        }
    }

    // -------------------------
    // Conversations
    // -------------------------

    function renderConversations() {
        conversationList.replaceChildren();
        conversationCount.textContent = conversations.length;

        const searchText = conversationSearch.value
            .trim()
            .toLowerCase();

        const filtered = conversations.filter(conversation => {

            const name = (conversation.customerName || "")
                .toLowerCase();

            const integrationName = (conversation.integrationName || "")
                .toLowerCase();

            const type = (conversation.integrationType || "")
                .toUpperCase();

            const matchesSearch =
                name.includes(searchText) ||
                integrationName.includes(searchText) ||
                type.toLowerCase().includes(searchText);

            const matchesFilter =
                activeFilter === "ALL" ||
                type === activeFilter;

            return matchesSearch && matchesFilter;
        });

        if (filtered.length === 0) {

            if (conversations.length === 0) {
                showListEmpty(
                    "No conversations yet",
                    "Messages from your connected channels will appear here.",
                    "✉"
                );
            } else {
                showListEmpty(
                    "No matching conversations",
                    "Try changing your search or channel filter.",
                    "⌕"
                );
            }

            return;
        }

        filtered.forEach(conversation => {
            const button = createConversationItem(conversation);
            conversationList.appendChild(button);
        });
    }

    function createConversationItem(conversation) {

        const button = document.createElement("button");
        button.type = "button";
        button.className = "conversation-item";

        if (selectedConversationId === conversation.id) {
            button.classList.add("selected");
        }

        const avatar = document.createElement("div");
        avatar.className = "conversation-avatar";
        avatar.textContent = getInitial(conversation.customerName);

        const info = document.createElement("div");
        info.className = "conversation-info";

        const nameRow = document.createElement("div");
        nameRow.className = "conversation-name-row";

        const name = document.createElement("span");
        name.className = "conversation-name";
        name.textContent = conversation.customerName || "Unknown customer";

        const time = document.createElement("span");
        time.className = "conversation-time";
        time.textContent = formatTime(conversation.updatedAt);

        nameRow.append(name, time);

        const platform = document.createElement("div");
        platform.className = "conversation-platform";
        platform.textContent = getPlatformLabel(conversation);

        info.append(nameRow, platform);
        button.append(avatar, info);

        button.addEventListener("click", () => {
            selectConversation(conversation);
        });

        return button;
    }

    function showListEmpty(title, description, icon) {

        conversationList.replaceChildren();

        const container = document.createElement("div");
        container.className = "list-empty";

        const iconElement = document.createElement("div");
        iconElement.className = "list-empty-icon";
        iconElement.textContent = icon;

        const heading = document.createElement("h3");
        heading.textContent = title;

        const paragraph = document.createElement("p");
        paragraph.textContent = description;

        container.append(iconElement, heading, paragraph);
        conversationList.appendChild(container);
    }

    // -------------------------
    // Selected conversation
    // -------------------------

    function selectConversation(conversation) {

        selectedConversationId = conversation.id;

        contactName.textContent =
            conversation.customerName || "Unknown customer";

        contactPlatform.textContent = getPlatformLabel(conversation);

        contactAvatar.textContent =
            getInitial(conversation.customerName);

        chatPlaceholder.hidden = true;
        chatHeader.hidden = false;
        chatContent.hidden = false;

        layout.classList.add("chat-open");

        renderConversations();

        loadMessages(conversation.id);
    }

    function renderMessages(messages) {

        messageList.replaceChildren();

        if (messages.length === 0) {
            showMessagesNotice("No messages in this conversation yet.");
            return;
        }

        messages.forEach(message => {

            const bubble = document.createElement("div");
            bubble.className = "message-bubble";

            if (message.direction === "OUTGOING") {
                bubble.classList.add("outgoing");
            }

            const sender = document.createElement("div");
            sender.className = "message-sender";
            sender.textContent = message.senderName || "Unknown sender";

            const text = document.createElement("div");
            text.className = "message-text";
            text.textContent = message.text || "";

            const time = document.createElement("div");
            time.className = "message-time";
            time.textContent = formatTime(message.sentAt);

            bubble.append(sender, text, time);
            messageList.appendChild(bubble);
        });

        messageList.scrollTop = messageList.scrollHeight;
    }

    function showMessagesNotice(text) {

        messageList.replaceChildren();

        const paragraph = document.createElement("p");
        paragraph.className = "messages-empty";
        paragraph.textContent = text;

        messageList.appendChild(paragraph);
    }

    function closeChat() {
        ++messageRequestVersion;
        selectedConversationId = null;

        layout.classList.remove("chat-open");

        chatHeader.hidden = true;
        chatContent.hidden = true;
        chatPlaceholder.hidden = false;

        messageList.replaceChildren();

        renderConversations();
    }

    // -------------------------
    // Helpers
    // -------------------------

    function getInitial(name) {
        return (name || "U").trim().charAt(0).toUpperCase() || "U";
    }

    function getPlatformLabel(conversation) {

        const type = conversation.integrationType || "UNKNOWN";
        const name = conversation.integrationName || type;

        return `${type} · ${name}`;
    }

    function formatTime(value) {

        if (!value) {
            return "";
        }

        const date = new Date(value);

        if (Number.isNaN(date.getTime())) {
            return "";
        }

        return date.toLocaleString(undefined, {
            day: "2-digit",
            month: "short",
            hour: "2-digit",
            minute: "2-digit"
        });
    }

    function setConnectionStatus(text, isError) {
        connectionStatus.textContent = text;
        statusDot.classList.toggle("error", isError);
    }

    // -------------------------
    // Events
    // -------------------------

    conversationSearch.addEventListener("input", () => {
        renderConversations();
    });

    document.querySelectorAll(".filter-button").forEach(button => {

        button.addEventListener("click", () => {

            activeFilter = button.dataset.filter;

            document.querySelectorAll(".filter-button")
                .forEach(filterButton => {
                    filterButton.classList.remove("active");
                });

            button.classList.add("active");

            renderConversations();
        });
    });

    mobileBackButton.addEventListener("click", closeChat);

    // Initial API request
    loadConversations();

});
