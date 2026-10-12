
const match = window.location.pathname.match(
    /^\/organizations\/(\d+)\/conversations\/?$/
);

const organizationId = match ? match[1] : null;

const conversationList = document.getElementById("conversationList");
const messageList = document.getElementById("messageList");
const header = document.getElementById("conversationHeader");
const search = document.getElementById("search");
const messageInput = document.getElementById("messageInput");
const sendButton = document.getElementById("sendButton");

let sending = false;

let conversations = [];
let selectedConversationId = null;
let lastRenderedMessages = "";
let refreshing = false;

function element(tag, className, text) {
    const node = document.createElement(tag);

    if (className) {
        node.className = className;
    }

    if (text !== undefined && text !== null) {
        node.textContent = String(text);
    }

    return node;
}

function sourceInfo(conversation) {
    const name = conversation.integrationName || "Unknown";

    const icon = /gmail/i.test(name) || /@gmail\.com/i.test(name)
        ? "/icons/gmail.svg"
        : /whatsapp/i.test(name)
            ? "/icons/whatsapp.svg"
            : "/icons/telegram.svg";

    return { name, icon };
}

function appendSource(parent, conversation, className) {
    const source = element("div", className);
    const info = sourceInfo(conversation);

    const icon = document.createElement("img");
    icon.src = info.icon;
    icon.alt = "";

    source.append(
        icon,
        document.createTextNode(info.name)
    );

    parent.append(source);
}

function showHeader(conversation) {
    header.replaceChildren();
    header.hidden = false;

    const name = conversation.customerName || "Unknown";

    const avatar = element(
        "div",
        "avatar",
        name.charAt(0).toUpperCase()
    );

    const details = element("div");

    details.append(
        element("div", "header-name", name)
    );

    appendSource(
        details,
        conversation,
        "header-source"
    );

    header.append(avatar, details);
}

function renderConversations() {
    const query = search.value.trim().toLowerCase();

    const filtered = conversations.filter(conversation =>
        (conversation.customerName || "")
            .toLowerCase()
            .includes(query) ||

        (conversation.integrationName || "")
            .toLowerCase()
            .includes(query)
    );

    conversationList.replaceChildren();

    if (filtered.length === 0) {
        const text = conversations.length
            ? "No matching conversations."
            : "No conversations yet.";

        conversationList.append(
            element("div", "empty", text)
        );

        return;
    }

    for (const conversation of filtered) {
        const name = conversation.customerName || "Unknown";

        const item = element("div", "chat-item");

        if (conversation.id === selectedConversationId) {
            item.classList.add("active");
        }

        const avatar = element(
            "div",
            "avatar",
            name.charAt(0).toUpperCase()
        );

        const details = element("div", "chat-details");
        const top = element("div", "chat-top");

        top.append(
            element("span", "chat-name", name)
        );

        details.append(top);

        appendSource(
            details,
            conversation,
            "source"
        );

        details.append(
            element("div", "preview", "Open conversation")
        );

        item.append(avatar, details);

        item.addEventListener("click", () => {
            selectConversation(conversation.id);
        });

        conversationList.append(item);
    }
}

async function selectConversation(id) {
    selectedConversationId = id;
    lastRenderedMessages = "";

    messageInput.disabled = false;
    sendButton.disabled = false;
    messageInput.placeholder = "Type your message...";

    const selected = conversations.find(
        conversation => conversation.id === id
    );

    if (selected) {
        showHeader(selected);
    }

    renderConversations();

    messageList.replaceChildren(
        element("div", "empty", "Loading messages...")
    );

    await loadMessages();
}

async function loadConversations() {
    const response = await fetch(
        `/api/organizations/${organizationId}/conversations`
    );

    if (!response.ok) {
        throw new Error(
            `Could not load conversations (${response.status}).`
        );
    }

    conversations = await response.json();

    const selectedExists = conversations.some(
        conversation => conversation.id === selectedConversationId
    );

    if (!selectedExists) {
        selectedConversationId = null;
        messageInput.disabled = true;
        sendButton.disabled = true;
        messageInput.placeholder = "Select a conversation to reply...";
        header.hidden = true;
        lastRenderedMessages = "";

        messageList.replaceChildren(
            element(
                "div",
                "empty",
                "Select a conversation to read messages."
            )
        );
    }

    renderConversations();

    if (selectedConversationId !== null) {
        const selected = conversations.find(
            conversation => conversation.id === selectedConversationId
        );

        if (selected) {
            showHeader(selected);
        }
    }
}

async function loadMessages() {
    if (selectedConversationId === null) {
        return;
    }

    const id = selectedConversationId;

    try {
        const response = await fetch(
            `/api/organizations/${organizationId}/conversations/${id}/messages`
        );

        if (!response.ok) {
            throw new Error(
                `Could not load messages (${response.status}).`
            );
        }

        const messages = await response.json();

        if (id !== selectedConversationId) {
            return;
        }

        const signature = JSON.stringify(messages);

        if (signature === lastRenderedMessages) {
            return;
        }

        const nearBottom =
            messageList.scrollHeight -
            messageList.scrollTop -
            messageList.clientHeight < 100;

        messageList.replaceChildren();

        if (messages.length === 0) {
            messageList.append(
                element("div", "empty", "No messages yet.")
            );
        }

        for (const message of messages) {
            const bubble = element(
                "div",
                "bubble",
                message.text || ""
            );

            if (message.direction === "OUTGOING") {
                bubble.classList.add("outgoing");
            }

            const date = message.receivedAt
                ? new Date(message.receivedAt).toLocaleTimeString(
                    "en-GB",
                    {
                        hour: "2-digit",
                        minute: "2-digit"
                    }
                )
                : "";

            bubble.append(
                element("div", "bubble-time", date)
            );

            messageList.append(bubble);
        }

        lastRenderedMessages = signature;

        if (nearBottom) {
            messageList.scrollTop = messageList.scrollHeight;
        }

    } catch (error) {
        if (id === selectedConversationId) {
            messageList.replaceChildren(
                element("div", "error", error.message)
            );
        }
    }
}

async function refresh() {
    if (refreshing || !organizationId) {
        return;
    }

    refreshing = true;

    try {
        await loadConversations();
        await loadMessages();

    } catch (error) {
        conversationList.replaceChildren(
            element("div", "error", error.message)
        );

    } finally {
        refreshing = false;
    }
}

search.addEventListener(
    "input",
    renderConversations
);


async function sendMessage() {
    const text = messageInput.value.trim();

    if (!text || selectedConversationId === null || sending) {
        return;
    }

    const conversationId = selectedConversationId;

    sending = true;
    sendButton.disabled = true;
    messageInput.disabled = true;

    try {
        // Get CSRF token from Spring Security
        const csrfResponse = await fetch("/api/csrf");

        if (!csrfResponse.ok) {
            throw new Error("Could not get CSRF token.");
        }

        const csrf = await csrfResponse.json();

        // Send message to our backend
        const response = await fetch(
            `/api/organizations/${organizationId}/conversations/${conversationId}/messages`,
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    [csrf.headerName]: csrf.token
                },
                body: JSON.stringify({ text })
            }
        );

        if (!response.ok) {
            throw new Error(
                `Message could not be sent (${response.status}).`
            );
        }

        // Clear input only after successful response
        if (selectedConversationId === conversationId) {
            messageInput.value = "";
            await loadMessages();
            messageList.scrollTop = messageList.scrollHeight;
        }

    } catch (error) {
        console.error(error);
        alert(error.message);

    } finally {
        sending = false;
        messageInput.disabled = selectedConversationId === null;
        sendButton.disabled = selectedConversationId === null;
    }
}

sendButton.addEventListener("click", sendMessage);

messageInput.addEventListener("keydown", event => {
    if (event.key === "Enter") {
        event.preventDefault();
        sendMessage();
    }
});


refresh();

setInterval(refresh, 3000);
