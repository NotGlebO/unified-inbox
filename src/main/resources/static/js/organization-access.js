
document.addEventListener("DOMContentLoaded", () => {

    const page = document.querySelector("[data-organization-id]");

    if (!page) {
        return;
    }

    const organizationId = page.dataset.organizationId;
    let checking = false;
    let redirecting = false;

    async function checkOrganizationAccess() {

        if (checking || redirecting) {
            return;
        }

        checking = true;

        try {
            const response = await fetch(
                `/api/organizations/${encodeURIComponent(organizationId)}/conversations`,
                {
                    method: "GET",
                    credentials: "same-origin",
                    headers: {
                        "Accept": "application/json"
                    },
                    cache: "no-store"
                }
            );

            if (response.status === 404) {
                redirecting = true;
                window.location.replace("/organizations");
            }

        } catch (error) {
            // A temporary network failure does not mean access was revoked
            console.error("Access check failed:", error);

        } finally {
            checking = false;
        }
    }

    // Check immediately and then every 10 seconds
    checkOrganizationAccess();

    setInterval(checkOrganizationAccess, 10000);

});
