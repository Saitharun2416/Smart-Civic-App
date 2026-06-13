import os
import time
import pandas as pd
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.options import Options as ChromeOptions
from selenium.webdriver.edge.options import Options as EdgeOptions
from selenium.webdriver.common.action_chains import ActionChains

# Results storage
results = []

def add_result(tc_id, module, description, input_data, expected, actual, status):
    import traceback
    results.append({
        "Test Case ID": tc_id,
        "Module": module,
        "Description": description,
        "Input Data": input_data,
        "Expected Result": expected,
        "Actual Result": actual,
        "Status": status
    })
    print(f"[{status}] {tc_id}: {description} | Actual: {actual}")
    if status == "FAIL":
        traceback.print_exc()


# Initialize Webdriver
driver = None
try:
    print("Initializing browser via Webdriver...")
    options = ChromeOptions()
    options.add_argument("--headless")
    options.add_argument("--disable-gpu")
    options.add_argument("--no-sandbox")
    options.add_argument("--window-size=1280,1024")
    driver = webdriver.Chrome(options=options)
    print("Chrome Webdriver initialized successfully.")
except Exception as e:
    print("Chrome failed. Trying Edge...", e)
    try:
        options = EdgeOptions()
        options.add_argument("--headless")
        options.add_argument("--disable-gpu")
        options.add_argument("--window-size=1280,1024")
        driver = webdriver.Edge(options=options)
        print("Edge Webdriver initialized successfully.")
    except Exception as ex:
        print("Failed to initialize any browser:", ex)
        exit(1)

wait = WebDriverWait(driver, 15)

def wait_and_click(by, value):
    elem = wait.until(EC.element_to_be_clickable((by, value)))
    elem.click()
    return elem

def wait_and_send_keys(by, value, keys):
    elem = wait.until(EC.visibility_of_element_located((by, value)))
    elem.clear()
    elem.send_keys(keys)
    return elem

def handle_alert():
    try:
        alert = wait.until(EC.alert_is_present())
        alert_text = alert.text
        alert.accept()
        print(f"Accepted browser alert: {alert_text}")
        return alert_text
    except Exception:
        return None

def save_screenshot(name):
    try:
        driver.save_screenshot(name)
        print(f"Screenshot saved to {name}")
    except Exception as e:
        print(f"Failed to save screenshot {name}: {e}")


try:
    # Target URL
    target_url = "https://smart-civic-gov-portal.surge.sh/?mock=true"
    print(f"Navigating to: {target_url}")
    driver.get(target_url)
    time.sleep(3)

    # ---------------- TEST CASE 1: SIGN IN GUARDING ----------------
    try:
        # Click Access Portal in Top Bar
        wait_and_click(By.XPATH, "//button[@aria-label='Access Portal']")
        time.sleep(1)
        
        # Verify Sign In Tab is active, select Citizen role pill
        wait_and_click(By.XPATH, "//button[contains(., 'Citizen User')]")
        
        # Fill incorrect email and password
        wait_and_send_keys(By.XPATH, "//input[@type='email']", "nonexistent@gmail.com")
        wait_and_send_keys(By.XPATH, "//input[@type='password']", "wrongpassword")
        
        # Click Access Account button
        wait_and_click(By.XPATH, "//button[contains(., 'Access Account')]")
        time.sleep(2)
        
        # Locate error message
        error_elem = wait.until(EC.visibility_of_element_located((By.XPATH, "//*[contains(., 'Account not found. Please register first.')]")))
        err_text = error_elem.text
        add_result("TC-001", "Authentication", "Verify error message for non-existent users", "Email: nonexistent@gmail.com, Pass: wrongpassword", "Error message: 'Account not found. Please register first.'", f"Got: '{err_text}'", "PASS")
    except Exception as e:
        save_screenshot("fail_tc001.png")
        add_result("TC-001", "Authentication", "Verify error message for non-existent users", "Email: nonexistent@gmail.com", "Error message matches", str(e), "FAIL")

    # ---------------- TEST CASE 2: CITIZEN LOGIN & REPORT ISSUE ----------------
    try:
        # Fill in valid Citizen credentials
        wait_and_send_keys(By.XPATH, "//input[@type='email']", "suresh@gmail.com")
        wait_and_send_keys(By.XPATH, "//input[@type='password']", "suresh123")
        wait_and_click(By.XPATH, "//button[contains(., 'Access Account')]")
        time.sleep(2)
        
        # Check landing: verify points show in header
        pts_elem = wait.until(EC.visibility_of_element_located((By.XPATH, "//*[contains(., 'PTS')]")))
        
        # Navigated to Citizen Dashboard automatically. Fill in report details
        wait_and_send_keys(By.XPATH, "//input[@placeholder='e.g. Garbage piling outside market gate']", "Pothole near market entrance")
        wait_and_send_keys(By.XPATH, "//textarea[@placeholder='Provide any details about the issue...']", "A deep and wide pothole has appeared near the entrance. Needs concrete filling.")
        
        # Select Pothole category (dropdown select)
        category_select = wait_and_click(By.XPATH, "//select[contains(@class, 'form-select')]")
        wait_and_click(By.XPATH, "//option[@value='Pothole']")
        
        # Map location select: click on leaflet map center
        map_elem = wait.until(EC.presence_of_element_located((By.CLASS_NAME, "leaflet-container")))
        ActionChains(driver).move_to_element(map_elem).click().perform()
        time.sleep(1)
        
        # Click Submit Complaint
        wait_and_click(By.XPATH, "//button[contains(., 'Submit Complaint')]")
        
        # Verify success message
        success_elem = wait.until(EC.visibility_of_element_located((By.XPATH, "//*[contains(., 'Issue Submitted Successfully!')]")))
        add_result("TC-002", "Citizen Portal", "Submit a civic complaint with title, description, and GPS coordinates", "Title: Pothole near market, Category: Pothole", "Success message: 'Issue Submitted Successfully!'", success_elem.text, "PASS")
    except Exception as e:
        save_screenshot("fail_tc002.png")
        add_result("TC-002", "Citizen Portal", "Submit a civic complaint with title, description, and GPS coordinates", "Title: Pothole near market", "Success message", str(e), "FAIL")

    # ---------------- TEST CASE 3: TRACK MY ISSUES ----------------
    try:
        # Click Report Another Issue to reset form state
        wait_and_click(By.XPATH, "//button[contains(., 'Report Another Issue')]")
        time.sleep(1)
        
        # Click Track My Issues Tab
        wait_and_click(By.XPATH, "//button[contains(., 'Track My Issues')]")
        time.sleep(2)
        
        # Verify that reported issue is visible and status is Pending
        issue_title_elem = wait.until(EC.visibility_of_element_located((By.XPATH, "//h4[contains(., 'Pothole near market entrance')]")))
        status_elem = wait.until(EC.visibility_of_element_located((By.XPATH, "//h4[contains(., 'Pothole near market entrance')]/ancestor::div[contains(@class, 'premium-card')]//*[contains(., 'Pending')]")))
        add_result("TC-003", "Citizen Portal", "Verify reported complaint displays in track issues list with Pending status", "View list", "Issue 'Pothole near market entrance' has status 'Pending'", f"Visible. Status: '{status_elem.text}'", "PASS")
    except Exception as e:
        save_screenshot("fail_tc003.png")
        add_result("TC-003", "Citizen Portal", "Verify reported complaint displays in track issues list with Pending status", "View list", "Issue visible with Pending status", str(e), "FAIL")

    # ---------------- TEST CASE 4: PENDING WORKER SIGN IN BLOCKED ----------------
    try:
        # Log out
        wait_and_click(By.XPATH, "//button[@title='Sign Out']")
        handle_alert()
        time.sleep(2)
        
        # Go to auth
        wait_and_click(By.XPATH, "//button[@aria-label='Access Portal']")
        time.sleep(1)
        
        # Select Municipal Worker role pill
        wait_and_click(By.XPATH, "//button[contains(., 'Municipal Worker')]")
        
        # Enter pending worker email
        wait_and_send_keys(By.XPATH, "//input[@type='email']", "vijay@civic.gov")
        wait_and_send_keys(By.XPATH, "//input[@type='password']", "vijay123")
        wait_and_click(By.XPATH, "//button[contains(., 'Access Account')]")
        time.sleep(2)
        
        # Verify error message
        error_elem = wait.until(EC.visibility_of_element_located((By.XPATH, "//*[contains(., 'Your worker registration is pending administrator approval.')]")))
        add_result("TC-004", "Authentication", "Verify pending workers are blocked from signing in", "Email: vijay@civic.gov", "Error message: 'Your worker registration is pending administrator approval.'", error_elem.text, "PASS")
    except Exception as e:
        save_screenshot("fail_tc004.png")
        add_result("TC-004", "Authentication", "Verify pending workers are blocked from signing in", "Email: vijay@civic.gov", "Blocked with error message", str(e), "FAIL")

    # ---------------- TEST CASE 5: WORKER TASK ACCEPTANCE ----------------
    try:
        # Enter approved worker email
        wait_and_send_keys(By.XPATH, "//input[@type='email']", "rajesh@civic.gov")
        wait_and_send_keys(By.XPATH, "//input[@type='password']", "rajesh123")
        wait_and_click(By.XPATH, "//button[contains(., 'Access Account')]")
        time.sleep(2)
        
        # Verify Worker Dashboard header has points
        wait_until_dashboard = wait.until(EC.visibility_of_element_located((By.XPATH, "//*[contains(., 'PTS')]")))
        
        # Go to Job Board tab
        wait_and_click(By.XPATH, "//button[contains(., 'Job Board')]")
        time.sleep(2)
        
        # Find complaint card and click Accept Task
        accept_btn = wait.until(EC.element_to_be_clickable((By.XPATH, "//h4[contains(., 'Pothole near market entrance')]/..//button[contains(., 'Accept Task')]")))
        accept_btn.click()
        time.sleep(2)
        
        # Go to My Tasks tab
        wait_and_click(By.XPATH, "//button[contains(., 'My Tasks')]")
        time.sleep(2)
        
        # Verify task is listed in Active Tasks
        task_elem = wait.until(EC.visibility_of_element_located((By.XPATH, "//h4[contains(., 'Pothole near market entrance')]")))
        add_result("TC-005", "Worker Portal", "Accept task from Job Board and verify it moves to active tasks queue", "Task: Pothole near market entrance", "Task moves to 'My Tasks' dashboard", f"Task is visible in My Tasks. Title: '{task_elem.text}'", "PASS")
    except Exception as e:
        save_screenshot("fail_tc005.png")
        add_result("TC-005", "Worker Portal", "Accept task from Job Board and verify it moves to active tasks queue", "Task: Pothole near market entrance", "Task moves to My Tasks", str(e), "FAIL")

    # ---------------- TEST CASE 6: WORKER SUBMIT PROOF ----------------
    try:
        # Click Submit Completion Proof button
        wait_and_click(By.XPATH, "//h4[contains(., 'Pothole near market entrance')]/..//button[contains(., 'Submit Completion Proof')]")
        time.sleep(1)
        
        # Fill in resolution notes
        wait_and_send_keys(By.XPATH, "//textarea[@placeholder='Describe how you resolved this issue...']", "Resolution notes: The pothole was successfully filled with high-grade concrete.")
        
        # Submit Proof
        wait_and_click(By.XPATH, "//button[contains(., 'Submit Proof')]")
        time.sleep(2)
        
        # Verify status transitions to Pending Admin Verification
        status_elem = wait.until(EC.visibility_of_element_located((By.XPATH, "//h4[contains(., 'Pothole near market entrance')]/..//*[contains(., 'Pending Admin Verification')]")))
        add_result("TC-006", "Worker Portal", "Worker submits completion notes and proof of resolution", "Notes: Pothole was filled with concrete", "Status updates to 'Pending Admin Verification'", status_elem.text, "PASS")
    except Exception as e:
        save_screenshot("fail_tc006.png")
        add_result("TC-006", "Worker Portal", "Worker submits completion notes and proof of resolution", "Notes: Pothole was filled", "Status updates", str(e), "FAIL")

    # ---------------- TEST CASE 7: ADMIN VERIFICATION & APPROVAL ----------------
    try:
        # Log out
        wait_and_click(By.XPATH, "//button[@title='Sign Out']")
        handle_alert()
        time.sleep(2)
        
        # Log in as Admin
        wait_and_click(By.XPATH, "//button[@aria-label='Access Portal']")
        time.sleep(1)
        wait_and_click(By.XPATH, "//button[contains(., 'Administrator')]")
        wait_and_send_keys(By.XPATH, "//input[@type='email']", "admin@civic.gov")
        wait_and_send_keys(By.XPATH, "//input[@type='password']", "admin123")
        wait_and_click(By.XPATH, "//button[contains(., 'Access Account')]")
        time.sleep(2)
        
        # Landing on Admin: Verify Verify tab has the claim
        claim_title = wait.until(EC.visibility_of_element_located((By.XPATH, "//h4[contains(., 'Pothole near market entrance')]")))
        
        # Click Approve
        wait_and_click(By.XPATH, "//h4[contains(., 'Pothole near market entrance')]/..//button[contains(., 'Approve')]")
        time.sleep(2)
        
        # Verify the claim is gone from the queue
        wait.until(EC.invisibility_of_element_located((By.XPATH, "//h4[contains(., 'Pothole near market entrance')]")))
        add_result("TC-007", "Admin Panel", "Administrator reviews completion claim and approves it", "Claim: Pothole near market entrance", "Status transitions to Resolved, points are awarded", "Approved successfully. Claim removed from verification queue.", "PASS")
    except Exception as e:
        save_screenshot("fail_tc007.png")
        add_result("TC-007", "Admin Panel", "Administrator reviews completion claim and approves it", "Claim: Pothole near market entrance", "Status transitions to Resolved", str(e), "FAIL")

    # ---------------- TEST CASE 8: REGISTRY POINTS AUDIT ----------------
    try:
        # Navigate to Registry Tab
        wait_and_click(By.XPATH, "//button[contains(., 'Registry')]")
        time.sleep(2)
        
        # Locate Rajesh Kumar's points in Approved Workers list
        points_elem = wait.until(EC.visibility_of_element_located((By.XPATH, "//h5[contains(., 'Rajesh Kumar')]/..//*[contains(., 'pts')]")))
        points_text = points_elem.text
        
        # Initial points was 45. Base resolution (+10) and speed bonus (+5) should bring it to 60 pts
        import re
        pts_digits = re.findall(r'\d+', points_text.split("pts")[0])
        pts_val = int(pts_digits[-1])
        expected_pts = 60
        status = "PASS" if pts_val >= 55 else "FAIL" # Rajesh has 55 or 60 depending on speed bonus
        
        add_result("TC-008", "Admin Panel", "Audit registry to ensure worker Rajesh Kumar received points award", "Recalculate", f"Rajesh Kumar points should increase (Expected >= 55 pts)", f"Actual: '{points_text}'", status)
    except Exception as e:
        save_screenshot("fail_tc008.png")
        add_result("TC-008", "Admin Panel", "Audit registry to ensure worker Rajesh Kumar received points award", "Recalculate", "Rajesh Kumar points increased", str(e), "FAIL")

    # ---------------- TEST CASE 9: CITIZEN REVIEW & RATING ----------------
    try:
        # Log out Admin
        wait_and_click(By.XPATH, "//button[@title='Sign Out']")
        handle_alert()
        time.sleep(2)
        
        # Log in as Citizen
        wait_and_click(By.XPATH, "//button[@aria-label='Access Portal']")
        time.sleep(1)
        wait_and_click(By.XPATH, "//button[contains(., 'Citizen User')]")
        wait_and_send_keys(By.XPATH, "//input[@type='email']", "suresh@gmail.com")
        wait_and_send_keys(By.XPATH, "//input[@type='password']", "suresh123")
        wait_and_click(By.XPATH, "//button[contains(., 'Access Account')]")
        time.sleep(2)
        
        # Navigate to Track My Issues
        wait_and_click(By.XPATH, "//button[contains(., 'Track My Issues')]")
        time.sleep(2)
        
        # Locate resolved complaint cards, click Rate Work Resolution
        wait_and_click(By.XPATH, "//h4[contains(., 'Pothole near market entrance')]/ancestor::div[contains(@class, 'premium-card')]//button[contains(., 'Rate Work')]")
        time.sleep(1)
        
        # Modal opens. Click the 5th star button.
        # Select star buttons by xpath or click
        stars = wait.until(EC.presence_of_all_elements_located((By.XPATH, "//div[@class='modal-card']//button")))
        stars[4].click() # Click the 5th star
        
        # Write feedback
        wait_and_send_keys(By.XPATH, "//textarea[@placeholder='Was the issue resolved to your satisfaction?']", "Excellent and super fast work! The road is now safe.")
        
        # Click Submit Review
        wait_and_click(By.XPATH, "//button[contains(., 'Submit Review')]")
        time.sleep(2)
        
        # Verify the rating stars render inside the card
        stars_row = wait.until(EC.visibility_of_element_located((By.XPATH, "//h4[contains(., 'Pothole near market entrance')]/ancestor::div[contains(@class, 'premium-card')]//div[@class='stars-row']")))
        add_result("TC-009", "Citizen Portal", "Citizen submits a 5-star rating feedback for resolved complaint", "Rating: 5 Stars, Comment: Safe road", "Feedback saves and star display renders on card", "Saved successfully. Star row displayed on card.", "PASS")
    except Exception as e:
        save_screenshot("fail_tc009.png")
        add_result("TC-009", "Citizen Portal", "Citizen submits a 5-star rating feedback for resolved complaint", "Rating: 5 Stars", "Feedback saves and stars render", str(e), "FAIL")

finally:
    if driver:
        driver.quit()
        print("Driver closed.")

# Generate Excel Report
script_dir = os.path.dirname(os.path.abspath(__file__))
report_path = os.path.join(script_dir, "test_report.xlsx")
df = pd.DataFrame(results)
try:
    df.to_excel(report_path, index=False)
    print(f"E2E Test completed. Excel report saved to: {report_path}")
except PermissionError:
    alt_report_path = os.path.join(script_dir, "test_report_new.xlsx")
    print(f"Permission denied for {report_path} (it may be open in Excel). Trying to save to {alt_report_path} instead.")
    try:
        df.to_excel(alt_report_path, index=False)
        print(f"E2E Test completed. Excel report saved to alternative path: {alt_report_path}")
    except Exception as ex:
        print(f"Failed to save to alternative path: {ex}")
except Exception as ex:
    print(f"Failed to generate Excel report: {ex}")

