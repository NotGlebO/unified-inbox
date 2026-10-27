
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
    const messageInput = document.getElementById("messageInput");
    const sendButton = document.getElementById("sendButton");
    const composerNote = document.getElementById("composerNote");

    const csrfToken = document.querySelector('meta[name="_csrf"]').content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

    
    


let isSending = false;

    // Current application state
    const organizationId = app.dataset.organizationId;

    let conversations = [];
    let selectedConversationId = null;
    let activeFilter = "ALL";
    let messageRequestVersion = 0;
    let lastMessagesSignature = null;

    
    const toggleSelectionButton =
        document.getElementById("toggleSelectionButton");
    const selectionActions =
        document.getElementById("selectionActions");
    const selectAllButton =
        document.getElementById("selectAllButton");
    const deleteSelectedButton =
        document.getElementById("deleteSelectedButton");
    const contextMenu =
        document.getElementById("conversationContextMenu");
    const contextDeleteButton =
        document.getElementById("contextDeleteButton");

    const selectedConversationIds = new Set();

    let selectionMode = false;
    let contextConversationId = null;
    let deletingConversations = false;

    function getFilteredConversations() {
        const searchText = conversationSearch.value.trim().toLowerCase();

        return conversations.filter(conversation => {
            const name = (conversation.customerName || "").toLowerCase();
            const integrationName =
                (conversation.integrationName || "").toLowerCase();
            const type = (conversation.integrationType || "").toUpperCase();

            return (
                (name.includes(searchText)
                    || integrationName.includes(searchText)
                    || type.toLowerCase().includes(searchText))
                && (activeFilter === "ALL" || type === activeFilter)
            );
        });
    }

    function updateSelectionToolbar() {
        selectionActions.hidden = !selectionMode;
        toggleSelectionButton.textContent =
            selectionMode ? "Cancel" : "Select";

        deleteSelectedButton.textContent =
            `Delete selected (${selectedConversationIds.size})`;

        deleteSelectedButton.disabled =
            selectedConversationIds.size === 0 || deletingConversations;

        selectAllButton.disabled = deletingConversations;
        toggleSelectionButton.disabled = deletingConversations;
    }

    function setSelectionMode(enabled) {
        selectionMode = enabled;
        selectedConversationIds.clear();
        contextMenu.hidden = true;
        updateSelectionToolbar();
        renderConversations();
    }

    toggleSelectionButton.addEventListener("click", () => {
        setSelectionMode(!selectionMode);
    });

    selectAllButton.addEventListener("click", () => {
        const filtered = getFilteredConversations();

        const allSelected = filtered.length > 0 &&
            filtered.every(conversation =>
                selectedConversationIds.has(conversation.id)
            );

        for (const conversation of filtered) {
            if (allSelected) {
                selectedConversationIds.delete(conversation.id);
            } else {
                selectedConversationIds.add(conversation.id);
            }
        }

        updateSelectionToolbar();
        renderConversations();
    });

    async function deleteConversations(ids) {
        if (deletingConversations || ids.length === 0) return;

        if (!confirm(
            `Delete ${ids.length} conversation(s) locally? ` +
            "They will reappear when new messages arrive."
        )) return;

        deletingConversations = true;
        updateSelectionToolbar();
        contextMenu.hidden = true;

        try {
            const response = await fetch(
                `/api/organizations/${encodeURIComponent(organizationId)}/conversations/delete-local`,
                {
                    method: "POST",
                    credentials: "same-origin",
                    headers: {
                        "Content-Type": "application/json",
                        [csrfHeader]: csrfToken
                    },
                    body: JSON.stringify({ conversationIds: ids })
                }
            );

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            if (ids.includes(selectedConversationId)) {
                closeChat();
            }

            selectionMode = false;
            selectedConversationIds.clear();
            lastConversationsSignature = null;

            await loadConversations(true);
        } catch (error) {
            console.error("Failed to delete conversations:", error);
            alert("Unable to delete selected conversations.");
        } finally {
            deletingConversations = false;
            updateSelectionToolbar();
            renderConversations();
        }
    }

    deleteSelectedButton.addEventListener("click", () => {
        deleteConversations([...selectedConversationIds]);
    });

    contextDeleteButton.addEventListener("click", () => {
        if (contextConversationId != null) {
            deleteConversations([contextConversationId]);
        }
    });

    document.addEventListener("click", event => {
        if (!contextMenu.contains(event.target)) {
            contextMenu.hidden = true;
        }
    });

    document.addEventListener("keydown", event => {
        if (event.key === "Escape") {
            contextMenu.hidden = true;
        }
    });

    window.addEventListener("resize", () => {
        contextMenu.hidden = true;
    });


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
        

        
        if (response.status === 404) {
            window.location.replace("/organizations");
            throw new Error("Organization access was revoked.");
        }

        if (!response.ok) {
            throw new Error(`Request failed: ${response.status}`);
        }


        const contentType = response.headers.get("content-type") || "";

        if (!contentType.includes("application/json")) {
            throw new Error("Expected JSON response. Please sign in again.");
        }

        return response.json();
    }

  
  
    let conversationsLoading = false;
    let lastConversationsSignature = null;

    async function loadConversations(silent = false) {
        if (conversationsLoading) return;

        conversationsLoading = true;

        if (!silent) {
            setConnectionStatus("Loading conversations...", false);
        }

        try {
            const data = await fetchJson(
                `/api/organizations/${encodeURIComponent(organizationId)}/conversations`,
                true
            );

            if (!Array.isArray(data)) {
                throw new Error("Invalid conversations response");
            }

            const signature = JSON.stringify(data);

            if (signature !== lastConversationsSignature) {
                lastConversationsSignature = signature;
                conversations = data;
                const availableIds = new Set(data.map(item => item.id));

                for (const id of selectedConversationIds) {
                    if (!availableIds.has(id)) {
                        selectedConversationIds.delete(id);
                    }
                }

                updateSelectionToolbar();
                renderConversations();
            }

            setConnectionStatus("Inbox connected", false);

        } catch (error) {
            console.error("Failed to load conversations:", error);

            // Keep the existing list if a background request fails.
            if (!silent) {
                showListEmpty(
                    "Unable to load conversations",
                    "Please refresh the page and try again.",
                    "!"
                );
            }

            setConnectionStatus("Connection error", true);

        } finally {
            conversationsLoading = false;
        }
    }


    
    async function fetchJson(url, redirectOnAccessLost = false) {

        const response = await fetch(url, {
            method: "GET",
            credentials: "same-origin",
            headers: {
                "Accept": "application/json"
            }
        });

        if (response.status === 404 && redirectOnAccessLost) {
            window.location.replace("/organizations");
            throw new Error("Organization access was revoked.");
        }

        if (!response.ok) {
            throw new Error(`Request failed: ${response.status}`);
        }

        const contentType = response.headers.get("content-type") || "";

        if (!contentType.includes("application/json")) {
            throw new Error("Expected JSON response. Please sign in again.");
        }

        return response.json();
    }


    async function loadMessages(conversationId, silent = false) {
        const requestVersion = ++messageRequestVersion;

        if (!silent) {
            showMessagesNotice("Loading messages...");
        }

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

            const messagesSignature = JSON.stringify(messages);

            if (messagesSignature !== lastMessagesSignature) {
                lastMessagesSignature = messagesSignature;
                renderMessages(messages);
            }

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

    
    async function sendMessage() {

        const conversationId = selectedConversationId;
        const text = messageInput.value.trim();

        if (!conversationId || !text || isSending) {
            return;
        }

        isSending = true;
        sendButton.disabled = true;
        messageInput.disabled = true;
        composerNote.textContent = "Sending...";

        try {
            const response = await fetch(
                `/api/conversations/${encodeURIComponent(conversationId)}/messages`,
                {
                    method: "POST",
                    credentials: "same-origin",
                    headers: {
                        "Content-Type": "application/json",
                        "Accept": "application/json",
                        [csrfHeader]: csrfToken
                    },
                    body: JSON.stringify({ text })
                }
            );

            if (!response.ok) {
                throw new Error(
                    `Failed to send message: HTTP ${response.status}`
                );
            }

            const contentType = response.headers.get("content-type") || "";

            if (!contentType.includes("application/json")) {
                throw new Error("Unexpected server response.");
            }

            await response.json();

            messageInput.value = "";
            composerNote.textContent = "Message queued for delivery.";

            if (selectedConversationId === conversationId) {
                await loadMessages(conversationId);
            }

        } catch (error) {
            console.error("Message sending failed:", error);

            composerNote.textContent =
                "Unable to queue message. Check the connection and try again.";

        } finally {
            isSending = false;
            sendButton.disabled = false;
            messageInput.disabled = false;
            messageInput.focus();
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
            }).sort((a, b) =>
                new Date(b.updatedAt).getTime() -
                new Date(a.updatedAt).getTime()
            );


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

        // Иконка, путь к которой передал адаптер
        const icon = document.createElement("img");
        icon.className = "platform-icon";
        icon.src = conversation.iconPath;
        icon.alt = "";

        // Название, которое передал адаптер
        const label = document.createElement("span");
        label.textContent = conversation.integrationName;

        // Цвет, который передал адаптер
        if (/^#[0-9a-fA-F]{6}$/.test(conversation.integrationColor || "")) {
            label.style.color = conversation.integrationColor;
        }

        platform.append(icon, label);

        
        const unreadCount = Number(conversation.unreadCount) || 0;

        if (unreadCount > 0) {
            const badge = document.createElement("span");
            badge.className = "conversation-unread-badge";
            badge.textContent = unreadCount > 99 ? "99+" : String(unreadCount);
            badge.title = `${unreadCount} unread messages`;

            platform.appendChild(badge);
        }


        info.append(nameRow, platform);
        button.append(avatar, info);

        
        if (selectionMode) {
            const checkbox = document.createElement("input");
            checkbox.type = "checkbox";
            checkbox.className = "conversation-checkbox";
            checkbox.checked = selectedConversationIds.has(conversation.id);
            checkbox.tabIndex = -1;

            button.prepend(checkbox);
            button.classList.toggle(
                "multi-selected",
                selectedConversationIds.has(conversation.id)
            );
        }

        button.addEventListener("click", () => {
            if (selectionMode) {
                if (selectedConversationIds.has(conversation.id)) {
                    selectedConversationIds.delete(conversation.id);
                } else {
                    selectedConversationIds.add(conversation.id);
                }

                updateSelectionToolbar();
                renderConversations();
                return;
            }

            selectConversation(conversation);
        });

    button.addEventListener("contextmenu", event => {
        event.preventDefault();

        if (selectionMode || deletingConversations) return;

        contextConversationId = conversation.id;
        contextMenu.hidden = false;

        const menuWidth = contextMenu.offsetWidth;
        const menuHeight = contextMenu.offsetHeight;

        contextMenu.style.left =
            `${Math.max(8, Math.min(event.clientX, window.innerWidth - menuWidth - 8))}px`;

        contextMenu.style.top =
            `${Math.max(8, Math.min(event.clientY, window.innerHeight - menuHeight - 8))}px`;
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

        if (isSending) {
            return;
        }


        messageInput.value = "";
        composerNote.textContent =
        "Press Enter to send, Shift + Enter for a new line.";

        selectedConversationId = conversation.id;
        lastMessagesSignature = null;

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

        
        loadMessages(conversation.id).then(async () => {
            if (selectedConversationId === conversation.id) {
                try {
                    await markConversationAsRead(conversation.id);
                    await loadConversations(true);
                } catch (error) {
                    console.error("Unable to mark conversation as read:", error);
                }
            }
        });


    }

    function createAttachmentElement(attachment) {
        const container = document.createElement("div");
        container.className = "message-attachment";

        const type = attachment.contentType || "";
        const url = attachment.contentUrl;

        if (type.startsWith("image/") &&
            type !== "image/svg+xml") {

            const image = document.createElement("img");
            image.src = url;
            image.alt = attachment.fileName || "Image";
            image.loading = "lazy";
            image.className = "attachment-image";

            container.appendChild(image);

        } else if (type.startsWith("video/")) {

            const video = document.createElement("video");
            video.src = url;
            video.controls = true;
            video.preload = "metadata";
            video.className = "attachment-video";

            container.appendChild(video);

        } else if (type.startsWith("audio/")) {

            const audio = document.createElement("audio");
            audio.src = url;
            audio.controls = true;
            audio.preload = "none";

            container.appendChild(audio);

        } else {

            const link = document.createElement("a");
            link.href = url;
            link.textContent = "📎 " +
                    (attachment.fileName || "Attachment");
            link.target = "_blank";
            link.rel = "noopener noreferrer";
            link.className = "attachment-file";

            container.appendChild(link);
        }

        return container;
    }

    function renderMessages(messages) {
        const wasNearBottom =
            messageList.scrollHeight -
            messageList.scrollTop -
            messageList.clientHeight < 100;

        const previousScrollTop = messageList.scrollTop;

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

            
            if (
                message.direction === "OUTGOING" &&
                message.source === "EXTERNAL"
            ) {
                const sourceIcon = document.createElement("span");
                sourceIcon.className = "message-source-icon";
                sourceIcon.textContent = "📱";
                sourceIcon.title = "Sent outside Unified Inbox";
                sourceIcon.setAttribute(
                    "aria-label",
                    "Sent outside Unified Inbox"
                );

                time.prepend(sourceIcon);
            }


            
            bubble.appendChild(sender);

            if (message.text) {
                bubble.appendChild(text);
            }

            if (Array.isArray(message.attachments)) {
                message.attachments.forEach(attachment => {
                    bubble.appendChild(createAttachmentElement(attachment));
                });
            }

            bubble.appendChild(time);

            if (message.direction === "OUTGOING") {

                const statusLabels = {
                    PENDING: "◷ Queued",
                    SENDING: "◷ Sending...",
                    SENT: "✓ Sent",
                    RETRY: "↻ Retrying",
                    FAILED: "✕ Failed",
                    UNKNOWN: "? Delivery uncertain"
                };

                const status = document.createElement("div");
                status.className = "message-status";

                const statusCode = message.status || "UNKNOWN";

                status.textContent =
                    statusLabels[statusCode] || "Unknown status";

                status.dataset.status = statusCode;

                bubble.appendChild(status);
            }
            
            const deleteButton = document.createElement("button");
            deleteButton.type = "button";
            deleteButton.className = "message-delete-button";
            deleteButton.innerHTML = `
            <svg xmlns="http://www.w3.org/2000/svg"
                width="16" height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.8"
                stroke-linecap="round"
                stroke-linejoin="round">
                <path d="M3 6h18"/>
                <path d="M8 6V4h8v2"/>
                <path d="M19 6l-1 14H6L5 6"/>
                <path d="M10 10v6"/>
                <path d="M14 10v6"/>
            </svg>
        `;
            deleteButton.title = "Delete message locally";
            deleteButton.setAttribute("aria-label", "Delete message locally");
            deleteButton.title = "Hide this message in Unified Inbox";

            deleteButton.addEventListener("click", async () => {
                if (!confirm("Hide this message in Unified Inbox?")) {
                    return;
                }

                deleteButton.disabled = true;

                try {
                    const response = await fetch(
                        `/api/messages/${encodeURIComponent(message.id)}/local`,
                        {
                            method: "DELETE",
                            credentials: "same-origin",
                            headers: {
                                [csrfHeader]: csrfToken
                            }
                        }
                    );

                    if (!response.ok) {
                        throw new Error(`HTTP ${response.status}`);
                    }

                    lastMessagesSignature = null;
                    await loadMessages(selectedConversationId);
                    await loadConversations(true);

                } catch (error) {
                    console.error("Failed to delete message:", error);
                    alert("Unable to delete message.");
                    deleteButton.disabled = false;
                }
            });

            bubble.appendChild(deleteButton);


            messageList.appendChild(bubble);
        });

        if (wasNearBottom) {
            messageList.scrollTop = messageList.scrollHeight;
        } else {
            messageList.scrollTop = previousScrollTop;
        }


    }

    function showMessagesNotice(text) {

        messageList.replaceChildren();

        const paragraph = document.createElement("p");
        paragraph.className = "messages-empty";
        paragraph.textContent = text;

        messageList.appendChild(paragraph);
    }

    function closeChat() {
        if (isSending) {
            return;
        }
        ++messageRequestVersion;
        selectedConversationId = null;
        lastMessagesSignature = null;

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
        if (!value) return "";

        const date = new Date(value);

        if (Number.isNaN(date.getTime())) return "";

        return date.toLocaleString("en-GB", {
            day: "2-digit",
            month: "2-digit",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit",
            hour12: false
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

    sendButton.addEventListener("click", sendMessage);

    messageInput.addEventListener("keydown", event => {
        if (event.key === "Enter" && !event.shiftKey && !event.isComposing) {
            event.preventDefault();
            sendMessage();
        }
    });
    setInterval(() => {
        if (
            selectedConversationId !== null &&
            !isSending &&
            !document.hidden
        ) {
            loadMessages(selectedConversationId, true);
        }
    }, 3000);

    
    setInterval(() => {
        if (!document.hidden) {
            loadConversations(true);
        }
    }, 3000);

    
    setInterval(() => {
        if (!document.hidden) {
            loadConversations(true);
        }
    }, 3000);


    
    async function markConversationAsRead(conversationId) {
        const response = await fetch(
            `/api/conversations/${encodeURIComponent(conversationId)}/read`,
            {
                method: "POST",
                credentials: "same-origin",
                headers: {
                    [csrfHeader]: csrfToken
                }
            }
        );

        if (!response.ok) {
            throw new Error(`Mark as read failed: ${response.status}`);
        }

        const conversation = conversations.find(
            item => item.id === conversationId
        );

        if (conversation) {
            conversation.unreadCount = 0;
            renderConversations();
        }
    }


}); 
