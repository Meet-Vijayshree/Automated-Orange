package seleniumTestNGProject;

import java.awt.Desktop;
import java.io.File;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.*;
import org.testng.Assert;
import org.testng.annotations.*;
import io.github.bonigarcia.wdm.WebDriverManager;

public class OrangeHRMFunctionalTest {

    private static WebDriver driver;
    private static WebDriverWait wait;
    private static Actions actions;
    private static JavascriptExecutor js;
    private static ExcelLogger excelLogger;

    private final String BASE_URL = "https://opensource-demo.orangehrmlive.com/web/index.php/auth/login";
    private final String USERNAME = "Admin";
    private final String PASSWORD = "admin123";
    private final Random random = new Random();

    private final String[] VALID_NAMES = {
            "Paul Collings", "Linda Anderson", "John Smith", "Sarah Johnson", "Kevin Lee", "Emma Watson"
    };

    private static class Bug {
        String module;
        String field;
        String message;
        String time;

        Bug(String module, String field, String message) {
            this.module = module;
            this.field = field;
            this.message = message;
            this.time = java.time.LocalTime.now().toString();
        }

        @Override
        public String toString() {
            return time + " | " + module + " | " + field + " | " + message;
        }
    }

    private List<Bug> bugList = new ArrayList<>();

    private void logBug(Bug bug) {
        bugList.add(bug);
        System.out.println("❌ " + bug);
        try {
            excelLogger.log(bug.module, bug.field, bug.message);
        } catch (Exception e) {
            System.out.println("⚠ Failed to log bug to ExcelLogger: " + e.getMessage());
        }
    }

    @BeforeClass
    public void setup() {
        if (driver == null) {   // Initialize only once
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--start-maximized", "--disable-popup-blocking");
            driver = new ChromeDriver(options);
            wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            actions = new Actions(driver);
            js = (JavascriptExecutor) driver;
            driver.get(BASE_URL);
            excelLogger = new ExcelLogger("HRMS_Bugs");
        }
    }

    @Test
    public void testAllModules() {
        login();
        Assert.assertTrue(driver.getCurrentUrl().contains("/dashboard"), "❌ Login failed!");
        System.out.println("✅ Login successful");

        String[] modules = {
                "Admin", "PIM", "Leave", "Time", "Recruitment",
                "My Info", "Performance", "Directory", "Maintenance", "Claim", "Buzz"
        };

        for (String module : modules) {
            try {
                handleModuleSafely(module);
            } catch (Exception e) {
                logBug(new Bug(module, "Critical Error", e.getMessage()));
                safeReturnToDashboard();
            }
        }

        logout();
    }

    // -------------------- Login / Logout --------------------
    private void login() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("username"))).sendKeys(USERNAME);
        driver.findElement(By.name("password")).sendKeys(PASSWORD);
        driver.findElement(By.xpath("//button[@type='submit']")).click();
        wait.until(ExpectedConditions.urlContains("/dashboard"));
    }

    private void logout() {
        try {
            WebElement profile = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//p[@class='oxd-userdropdown-name']")));
            profile.click();
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[text()='Logout']"))).click();
            wait.until(ExpectedConditions.urlContains("/login"));
        } catch (Exception e) {
            logBug(new Bug("Logout", "Click Error", e.getMessage()));
        }
    }

    // -------------------- Safe Module Handling --------------------
    private void handleModuleSafely(String module) {
        System.out.println("🧩 Testing module: " + module);
        long moduleStart = System.currentTimeMillis();
        final long MODULE_TIMEOUT_MS = 30_000L;

        try {
            clickMenu(module);
            slowDown();

            switch (module) {
                case "Directory":
                    handleDirectoryModule();
                    break;
                case "Leave":
                    fillLeaveModule();
                    break;
                case "Time":
                    fillTimesheetModule();
                    break;
                default:
                    tryClickButton("Add");
                    tryClickButton("Edit");
                    autofillForm(module);
                    clickSave();
            }
        } catch (Exception e) {
            logBug(new Bug(module, "Module Failure", e.getMessage()));
        } finally {
            long elapsed = System.currentTimeMillis() - moduleStart;
            if (elapsed > MODULE_TIMEOUT_MS) {
                logBug(new Bug(module, "Timeout", "Module exceeded timeout (" + elapsed + "ms)"));
            }
            try {
                closeOpenForms(module);
                goToDashboard();
            } catch (Exception ex) {
                logBug(new Bug(module, "Recovery", "Failed to exit module: " + ex.getMessage()));
                safeReturnToDashboard();
            }
        }
    }

    private void safeReturnToDashboard() {
        try {
            driver.navigate().to(BASE_URL.replace("/auth/login", "/dashboard"));
            wait.until(ExpectedConditions.urlContains("/dashboard"));
        } catch (Exception e) {
            System.out.println("⚠ Unable to force-navigate to dashboard: " + e.getMessage());
        }
    }

    private void closeOpenForms(String module) {
        try {
            List<WebElement> cancelBtns = driver.findElements(By.xpath("//button[normalize-space()='Cancel' or normalize-space()='Back']"));
            for (WebElement b : cancelBtns) {
                if (b.isDisplayed() && b.isEnabled()) {
                    js.executeScript("arguments[0].click();", b);
                    slowDown();
                    return;
                }
            }

            List<WebElement> closeIcons = driver.findElements(By.cssSelector("button[aria-label='Close'],button[title='Close']"));
            for (WebElement c : closeIcons) {
                if (c.isDisplayed() && c.isEnabled()) {
                    js.executeScript("arguments[0].click();", c);
                    slowDown();
                    return;
                }
            }
        } catch (Exception e) {
            logBug(new Bug(module, "Close Form", e.getMessage()));
        }
    }

    // -------------------- Navigation helpers --------------------
    private void clickMenu(String name) {
        try {
            WebElement menu = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//span[text()='" + name + "']/ancestor::a")));
            js.executeScript("arguments[0].click();", menu);
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//h6")));
        } catch (Exception e) {
            logBug(new Bug("Menu Click", name, e.getMessage()));
        }
    }

    private void tryClickButton(String label) {
        try {
            List<WebElement> buttons = driver.findElements(By.xpath("//button[normalize-space()='" + label + "']"));
            for (WebElement btn : buttons) {
                if (btn.isDisplayed() && btn.isEnabled()) {
                    js.executeScript("arguments[0].click();", btn);
                    waitForForm();
                    return;
                }
            }
        } catch (Exception e) {
            logBug(new Bug("Button Click", label, e.getMessage()));
        }
    }

    private void goToDashboard() {
        try {
            WebElement dashboardMenu = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//span[text()='Dashboard']/ancestor::a")));
            js.executeScript("arguments[0].click();", dashboardMenu);
            wait.until(ExpectedConditions.urlContains("/dashboard"));
            System.out.println("🏠 Returned to Dashboard");
        } catch (Exception e) {
            logBug(new Bug("Dashboard", "Navigation", e.getMessage()));
        }
    }

    private void waitForForm() {
        try {
            wait.withTimeout(Duration.ofSeconds(2))
                .until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//input | //textarea")));
        } finally {
            wait.withTimeout(Duration.ofSeconds(15));
        }
    }

    private void clickSave() {
        try {
            List<WebElement> saveBtns = driver.findElements(By.xpath("//button[normalize-space()='Save']"));
            for (WebElement saveBtn : saveBtns) {
                if (saveBtn.isDisplayed() && saveBtn.isEnabled()) {
                    js.executeScript("arguments[0].click();", saveBtn);
                    waitForSuccessToast();
                    return;
                }
            }
        } catch (Exception e) {
            logBug(new Bug("Save Button", "Save", e.getMessage()));
        }
    }

    private void waitForSuccessToast() {
        try {
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".oxd-toast--success")),
                    ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), "Success")
            ));
        } catch (Exception ignored) {}
    }

    // -------------------- Autofill --------------------
    private void autofillForm(String module) {
        List<WebElement> fields = driver.findElements(By.xpath("//form//input | //form//textarea | //form//select"));
        int count = 0;

        for (WebElement el : fields) {
            try {
                if (!el.isDisplayed() || !el.isEnabled()) continue;

                String tag = el.getTagName().toLowerCase();
                String type = el.getAttribute("type") != null ? el.getAttribute("type").toLowerCase() : "";
                String readonly = el.getAttribute("readonly");
                String placeholder = el.getAttribute("placeholder") != null ? el.getAttribute("placeholder").toLowerCase() : "";

                if ("true".equalsIgnoreCase(readonly) || "hidden".equalsIgnoreCase(type)) continue;

                if (tag.equals("select")) { selectRandomNative(el); continue; }
                if (isCustomDropdown(el)) { selectRandomCustomDropdown(el); continue; }

                if (type.equals("email") || placeholder.contains("email")) safeSendKeys(el, "user" + random.nextInt(9999) + "@example.com");
                else if (type.equals("number") || placeholder.contains("phone") || placeholder.contains("contact")) safeSendKeys(el, String.valueOf(9000000000L + random.nextInt(90_000_000)));
                else if (type.equals("date")) safeSetInputValueWithJS(el, generateRandomDate("1990-01-01", "2002-12-31"));
                else if (type.contains("password")) safeSendKeys(el, PASSWORD);
                else if (placeholder.contains("name")) safeSendKeys(el, VALID_NAMES[random.nextInt(VALID_NAMES.length)]);
                else safeSendKeys(el, "AutoFill_" + (++count));

                Thread.sleep(100); // small delay for stability
            } catch (StaleElementReferenceException sere) {}
            catch (Exception e) { logBug(new Bug(module, "Input Field", e.getMessage())); }
        }
    }

    private boolean isCustomDropdown(WebElement el) {
        try {
            String cls = el.getAttribute("class") != null ? el.getAttribute("class").toLowerCase() : "";
            return cls.contains("oxd-select-text") || cls.contains("oxd-select-input") || "combobox".equalsIgnoreCase(el.getAttribute("role"));
        } catch (Exception e) { return false; }
    }

    private void selectRandomNative(WebElement selectEl) {
        try {
            Select sel = new Select(selectEl);
            List<WebElement> options = sel.getOptions();
            if (options.size() > 1) sel.selectByIndex(random.nextInt(options.size() - 1) + 1);
        } catch (Exception e) { selectRandomCustomDropdown(selectEl); }
    }

    private void selectRandomCustomDropdown(WebElement containerOrInput) {
        try {
            WebElement clickable = containerOrInput;
            String tag = containerOrInput.getTagName().toLowerCase();
            if ("input".equals(tag)) {
                List<WebElement> wrappers = containerOrInput.findElements(By.xpath("./ancestor::div[contains(@class,'oxd-select-wrapper')][1]//div[contains(@class,'oxd-select-text')]"));
                if (!wrappers.isEmpty()) clickable = wrappers.get(0);
            }

            js.executeScript("arguments[0].click();", clickable);
            wait.withTimeout(Duration.ofSeconds(3));

            List<WebElement> options = clickable.findElements(By.xpath(".//following-sibling::div[@role='option']"));
            List<WebElement> visible = new ArrayList<>();
            for (WebElement opt : options) if (opt.isDisplayed()) visible.add(opt);

            if (!visible.isEmpty()) js.executeScript("arguments[0].click();", visible.get(random.nextInt(visible.size())));
            else js.executeScript("arguments[0].click();", clickable);

            wait.withTimeout(Duration.ofSeconds(15));
        } catch (Exception e) {
            logBug(new Bug("Dropdown", "Custom Dropdown", e.getMessage()));
        }
    }

    private void safeSendKeys(WebElement el, String value) {
        try { 
            wait.until(ExpectedConditions.elementToBeClickable(el));
            el.click(); el.clear(); el.sendKeys(value); 
        } catch (ElementNotInteractableException e) { 
            safeSetInputValueWithJS(el, value); 
        } catch (Exception e) {
            logBug(new Bug("SendKeys", el.getAttribute("name") != null ? el.getAttribute("name") : el.toString(), e.getMessage()));
        }
    }

    private void safeSetInputValueWithJS(WebElement el, String value) {
        try { 
            js.executeScript(
                "arguments[0].value = arguments[1];" +
                "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                el, value
            ); 
        } catch (Exception e) { 
            logBug(new Bug("JS SetValue", el.getAttribute("name") != null ? el.getAttribute("name") : el.toString(), e.getMessage())); 
        }
    }

    private String generateRandomDate(String start, String end) {
        LocalDate startDate = LocalDate.parse(start);
        LocalDate endDate = LocalDate.parse(end);
        long days = endDate.toEpochDay() - startDate.toEpochDay();
        long randomDay = startDate.toEpochDay() + random.nextInt((int) days + 1);
        return LocalDate.ofEpochDay(randomDay).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    // -------------------- Leave module --------------------
    private void fillLeaveModule() {
        try {
            tryClickButton("Assign Leave");
            tryClickButton("Add");

            List<WebElement> nameInputs = driver.findElements(By.xpath("//input[@placeholder='Type for hints...' or contains(@placeholder,'employee')]"));
            if (!nameInputs.isEmpty()) {
                WebElement emp = nameInputs.get(0);
                String name = VALID_NAMES[random.nextInt(VALID_NAMES.length)];
                safeSendKeys(emp, name);
                try {
                    WebElement suggestion = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//div[@role='option']//span[text()='" + name + "']")));
                    js.executeScript("arguments[0].click();", suggestion);
                } catch (Exception ignored) {}
            }

            List<WebElement> leaveTypeContainers = driver.findElements(By.xpath("//div[contains(@class,'oxd-select-text')]"));
            if (!leaveTypeContainers.isEmpty()) selectRandomCustomDropdown(leaveTypeContainers.get(0));

            List<WebElement> dateFields = driver.findElements(By.xpath("//input[contains(@placeholder,'yyyy') or contains(@placeholder,'date') or contains(@name,'date')]"));
            if (dateFields.size() >= 2) {
                String from = generateRandomDate("2023-01-01", "2023-12-01");
                String to = generateRandomDate("2023-12-02", "2024-12-31");
                safeSetInputValueWithJS(dateFields.get(0), from);
                safeSetInputValueWithJS(dateFields.get(1), to);
            }

            try {
                WebElement comment = driver.findElement(By.xpath("//textarea[contains(@placeholder,'Comment') or contains(@placeholder,'reason') or contains(@name,'comment')]"));
                safeSendKeys(comment, "Automated leave request for testing.");
            } catch (Exception ignored) {}

            clickSave();
        } catch (Exception e) {
            logBug(new Bug("Leave", "Fill", e.getMessage()));
        }
    }

    // -------------------- Timesheet module --------------------
    private void fillTimesheetModule() {
        try {
            List<WebElement> projectInputs = driver.findElements(By.xpath("//input[contains(@placeholder,'Project')]"));
            List<WebElement> hoursInputs = driver.findElements(By.xpath("//input[contains(@placeholder,'Hours')]"));

            for (int i = 0; i < Math.min(projectInputs.size(), hoursInputs.size()); i++) {
                safeSendKeys(projectInputs.get(i), "Project_" + (i + 1));
                safeSendKeys(hoursInputs.get(i), String.valueOf(6 + random.nextInt(3)));
            }

            clickSave();
        } catch (Exception e) {
            logBug(new Bug("Timesheet", "Fill", e.getMessage()));
        }
    }

    // -------------------- Directory module --------------------
    private void handleDirectoryModule() {
        try {
            clickMenu("Directory");
            slowDown();

            List<WebElement> nameFields = driver.findElements(By.xpath("//input[@placeholder='Type for hints...']"));
            for (WebElement nameField : nameFields) {
                if (nameField.isDisplayed() && nameField.isEnabled()) {
                    nameField.clear();
                    nameField.sendKeys(VALID_NAMES[random.nextInt(VALID_NAMES.length)]);
                }
            }

            List<WebElement> dropdowns = driver.findElements(By.xpath("//div[contains(@class,'oxd-select-text')]"));
            for (WebElement dd : dropdowns) selectRandomCustomDropdown(dd);

            List<WebElement> searchBtns = driver.findElements(By.xpath("//button[normalize-space()='Search']"));
            if (!searchBtns.isEmpty()) js.executeScript("arguments[0].click();", searchBtns.get(0));

            List<WebElement> results = driver.findElements(By.xpath("//div[contains(@class,'oxd-table-card')]"));
            if (results.isEmpty()) logBug(new Bug("Directory", "Search", "No results found for random filters"));
            else System.out.println("✅ Directory search returned " + results.size() + " results.");

            List<WebElement> resetBtns = driver.findElements(By.xpath("//button[normalize-space()='Reset']"));
            if (!resetBtns.isEmpty() && resetBtns.get(0).isDisplayed()) js.executeScript("arguments[0].click();", resetBtns.get(0));
        } catch (Exception e) {
            logBug(new Bug("Directory", "Error", e.getMessage()));
        }
    }

    // -------------------- Teardown --------------------
    private void slowDown() {
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}
    }

    @AfterClass(alwaysRun = true)
    public void teardown() {
        if (driver != null) driver.quit();

        String excelPath = new File("HRMS_Bugs.xlsx").getAbsolutePath();
        try { excelLogger.save(excelPath); }
        catch (Exception e) { System.out.println("⚠ Failed to save Excel: " + e.getMessage()); }

        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(new File(excelPath));
        } catch (Exception e) { System.out.println("⚠ Failed to open Excel: " + e.getMessage()); }

        if (!bugList.isEmpty()) {
            System.out.println("\n=== BUGS DETECTED ===");
            bugList.forEach(System.out::println);
        } else System.out.println("✅ No bugs detected during automation run.");
    }
}
