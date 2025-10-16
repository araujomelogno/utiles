from playwright.sync_api import sync_playwright, expect

def run_verification():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()

        # Navigate to the login page and log in
        page.goto("http://localhost:8080/login")
        page.get_by_label("Username").fill("user")
        page.get_by_label("Password").fill("user")
        page.get_by_role("button", name="Log in").click()

        # Navigate to the ExtraInputView
        page.goto("http://localhost:8080/IngresarExtras")

        # Wait for the view to load
        expect(page.get_by_text("Ingresar Extras")).to_be_visible()

        # Select a study
        study_combo_box = page.get_by_label("Estudio")
        expect(study_combo_box).to_be_visible()
        study_combo_box.click()
        page.get_by_text("Estudio de prueba").click()

        # Select a month
        month_picker = page.get_by_label("Mes")
        expect(month_picker).to_be_visible()
        month_picker.click()
        page.get_by_text("enero").click()

        # Take a screenshot
        page.screenshot(path="jules-scratch/verification/verification.png")

        browser.close()

if __name__ == "__main__":
    run_verification()