from playwright.sync_api import sync_playwright, expect

def run():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()

        # Navigate to the login page
        page.goto("http://localhost:8080/login")

        # Wait for the login overlay to be visible
        expect(page.locator("vaadin-login-overlay-wrapper")).to_be_visible()

        # Fill in the login form using more specific locators
        page.locator('input[name="username"]').fill("admin")
        page.locator('input[name="password"]').fill("admin")
        page.get_by_role("button", name="Log in").click()

        # Wait for navigation to the main page
        expect(page).to_have_url("http://localhost:8080/")

        # Navigate to the new view
        page.get_by_role("button", name="Extras").click()
        page.get_by_role("link", name="Conceptos de extra").click()

        # Wait for the view to load and take a screenshot
        expect(page.get_by_role("heading", name="Conceptos de extra")).to_be_visible()
        page.screenshot(path="jules-scratch/verification/extra_concept_view.png")

        browser.close()

if __name__ == "__main__":
    run()