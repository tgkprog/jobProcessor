package com.example.smallapp;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SmallAppSeleniumTest {

    @LocalServerPort
    private int port;

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void setupTest() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void testSetSchedule() {
        driver.get("http://localhost:" + port);

        // Wait for page to load
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("status-badge")));

        // Check initial status
        WebElement statusBadge = driver.findElement(By.id("status-badge"));
        String statusText = statusBadge.getText().toUpperCase();
        assertTrue(statusText.contains("NOT SET") || statusText.contains("IDLE"));

        // Set a schedule for 1 minute from now
        LocalDateTime future = LocalDateTime.now().plusMinutes(2);
        String futureStr = future.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));

        WebElement datetimeInput = driver.findElement(By.id("schedule-datetime"));
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1]", datetimeInput, futureStr);

        WebElement sleepInput = driver.findElement(By.id("sleep"));
        sleepInput.clear();
        sleepInput.sendKeys("5");

        WebElement randomSleepInput = driver.findElement(By.id("random-sleep"));
        randomSleepInput.clear();
        randomSleepInput.sendKeys("1");

        WebElement setBtn = driver.findElement(By.id("set-schedule-btn"));
        setBtn.click();

        // Handle Alert
        wait.until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().accept();

        // Verify status changes to Scheduled
        wait.until(d -> driver.findElement(By.id("status-badge")).getText().toUpperCase().contains("SCHEDULED"));
        
        WebElement scheduledTime = driver.findElement(By.id("scheduled-time"));
        assertTrue(scheduledTime.getText().contains(future.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
    }

    @Test
    public void testCancelSchedule() {
        driver.get("http://localhost:" + port);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("status-badge")));

        // Set a schedule first
        LocalDateTime future = LocalDateTime.now().plusMinutes(5);
        String futureStr = future.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        driver.findElement(By.id("schedule-datetime")).sendKeys(futureStr);
        driver.findElement(By.id("set-schedule-btn")).click();
        
        wait.until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().accept();

        // Click Cancel
        WebElement cancelBtn = driver.findElement(By.id("cancel-btn"));
        cancelBtn.click();

        wait.until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().accept();

        // Verify status returns to Idle
        wait.until(d -> driver.findElement(By.id("status-badge")).getText().toUpperCase().equals("IDLE"));
        WebElement scheduledTime = driver.findElement(By.id("scheduled-time"));
        assertTrue(scheduledTime.getText().equals("None"));
    }

    @Test
    public void testExecutionStatus() {
        driver.get("http://localhost:" + port);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("status-badge")));

        // Schedule for 2 seconds from now
        LocalDateTime nearFuture = LocalDateTime.now().plusSeconds(2);
        String nearFutureStr = nearFuture.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        
        WebElement datetimeInput = driver.findElement(By.id("schedule-datetime"));
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1]", datetimeInput, nearFutureStr);
        
        driver.findElement(By.id("sleep")).clear();
        driver.findElement(By.id("sleep")).sendKeys("3");
        driver.findElement(By.id("set-schedule-btn")).click();
        
        wait.until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().accept();

        // Wait for it to become Working (polling is 5s, so we might need to wait up to 20s total)
        WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(20));
        longWait.until(d -> driver.findElement(By.id("status-badge")).getText().toUpperCase().contains("WORKING"));
        
        // Wait for it to become Idle again after work
        longWait.until(d -> driver.findElement(By.id("status-badge")).getText().toUpperCase().equals("IDLE"));
    }
}
