
const telegramModal = document.getElementById("telegramModal");
const telegramTokenInput = document.getElementById("telegramToken");

function openTelegramModal() {
    telegramModal.classList.add("open");
    document.body.style.overflow = "hidden";
    telegramTokenInput.focus();
}

function closeTelegramModal() {
    telegramModal.classList.remove("open");
    document.body.style.overflow = "";
    telegramTokenInput.value = "";
}

// Close when clicking outside the modal
telegramModal.addEventListener("click", (event) => {
    if (event.target === telegramModal) {
        closeTelegramModal();
    }
});

// Close with Escape
document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" &&
        telegramModal.classList.contains("open")) {
        closeTelegramModal();
    }
});
