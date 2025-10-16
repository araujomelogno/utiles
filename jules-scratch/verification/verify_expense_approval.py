from playwright.sync_api import sync_playwright

def run(playwright):
    browser = playwright.chromium.launch(headless=True)
    context = browser.new_context()
    page = context.new_page()

    # Log in
    page.goto("http://localhost:8080/login")
    page.get_by_label("Username").fill("user")
    page.get_by_label("Password").fill("user")
    page.get_by_role("button", name="Log in").click()

    # Go to the expenses approval view
    page.goto("http://localhost:8080/expenses-approval")

    # Click on the first expense request to open the editor
    page.locator("vaadin-grid-cell-content").first.click()

    # Select a study
    study_combobox = page.locator("vaadin-combo-box[label='Estudio']")
    study_combobox.click()
    page.locator("vaadin-combo-box-item >> text=Estudio de Movilidad").click()

    # Take a screenshot to verify the budget entry combobox is populated
    page.screenshot(path="jules-scratch/verification/verification.png")

    browser.close()

with sync_playwright() as playwright:
    run(playwright)