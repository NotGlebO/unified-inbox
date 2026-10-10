
const conversationList = document.getElementById("conversationList");
const messageList = document.getElementById("messageList");

let selectedConversationId = null;

async function loadConversations() {
    try {
        const response = await fetch(
            `/api/organizations/${organizationId}/conversations`
        );

        if (!response.ok) {
            throw new Error("Failed to load conversations");
        }

        const conversations = await response.json();

        conversationList.replaceChildren();

        if (conversations.length === 0) {
            conversationList.textContent = "No conversations yet.";
            messageList.textContent = "No messages to display.";
            selectedConversationId = null;
            return;
        }

        if (!conversations.some(c => c.id === selectedConversationId)) {
            selectedConversationId = conversations[0].id;
            await loadMessages();
        }

        for (const conversation of conversations) {
            const item = document.createElement("div");
            item.className = "conversation";

            if (conversation.id === selectedConversationId) {
                item.classList.add("active");
            }

            const name = document.createElement("div");
            name.className = "conversation-name";
            name.textContent = conversation.customerName || "Unknown";

            const integration = document.createElement("small");
            integration.textContent = conversation.integrationName;

            item.append(name, integration);

            item.addEventListener("click", async () => {
                selectedConversationId = conversation.id;
                await loadMessages();
                await loadConversations();
            });

            conversationList.appendChild(item);
        }

    } catch (error) {
        console.error(error);
        conversationList.textContent = "Could not load conversations.";
    }
}

async function loadMessages() {
    if (selectedConversationId === null) return;

    const conversationId = selectedConversationId;

    try {
        const response = await fetch(
            `/api/organizations/${organizationId}/conversations/${conversationId}/messages`
        );

        if (!response.ok) {
            throw new Error("Failed to load messages");
        }

        const messages = await response.json();

        if (conversationId !== selectedConversationId) return;

        messageList.replaceChildren();

        if (messages.length === 0) {
            messageList.textContent = "No messages yet.";
            return;
        }

        for (const message of messages) {
            const item = document.createElement("div");
            item.className = "message";

            if (message.direction === "OUTGOING") {
                item.classList.add("outgoing");
            }

            const sender = document.createElement("strong");
            sender.textContent = message.sender;

            const text = document.createElement("p");
            text.textContent = message.text;

            const date = document.createElement("small");
            date.textContent = new Date(
                message.receivedAt
            ).toLocaleString();

            item.append(sender, text, date);
            messageList.appendChild(item);
        }

    } catch (error) {
        console.error(error);
        messageList.textContent = "Could not load messages.";
    }
}

async function refresh() {
    await loadConversations();

    if (selectedConversationId !== null) {
        await loadMessages();
    }
}

refresh();
setInterval(refresh, 3000);
