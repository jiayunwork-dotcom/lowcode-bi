package com.lowcode.bi.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowcode.bi.common.exception.BusinessException;
import com.lowcode.bi.entity.Dashboard;
import com.lowcode.bi.entity.ScheduleConfig;
import com.lowcode.bi.repository.DashboardRepository;
import com.lowcode.bi.security.JwtUtil;
import com.lowcode.bi.security.TenantContext;
import com.lowcode.bi.service.ScreenshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScreenshotServiceImpl implements ScreenshotService {

    private final DashboardRepository dashboardRepository;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.screenshot.query-timeout:30}")
    private int queryTimeout;

    @Value("${app.screenshot.chrome-path:}")
    private String chromePath;

    @Value("${app.screenshot.headless:true}")
    private boolean headless;

    @Override
    public ScreenshotResult captureDashboard(UUID dashboardId, ScheduleConfig config,
                                             Map<String, Object> filters) {
        ScreenshotResult result = new ScreenshotResult();
        long startTime = System.currentTimeMillis();

        UUID tenantId = TenantContext.getTenantId();
        Dashboard dashboard = dashboardRepository.findByIdAndTenantId(dashboardId, tenantId)
                .orElseThrow(() -> new BusinessException("仪表板不存在"));

        String authToken = generateAuthToken(config);
        String dashboardUrl = buildDashboardUrl(dashboardId, config, filters);

        result.setDashboardLink(frontendUrl + "/dashboard/" + dashboardId + "?token=" + authToken);

        return captureUrl(dashboardUrl,
                config.getScreenshotWidth(),
                config.getScreenshotHeight(),
                config.getWaitForRenderMs(),
                authToken);
    }

    @Override
    public ScreenshotResult captureUrl(String url, int width, int height,
                                       int waitMs, String authToken) {
        ScreenshotResult result = new ScreenshotResult();
        long startTime = System.currentTimeMillis();

        WebDriver driver = null;
        try {
            driver = createWebDriver(width, height);

            if (authToken != null) {
                setAuthToken(driver, authToken);
            }

            driver.get(url);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(queryTimeout + 10));

            try {
                wait.until(ExpectedConditions.jsReturnsValue(
                        "return document.readyState === 'complete'"));
            } catch (Exception e) {
                log.warn("等待页面加载超时: {}", e.getMessage());
            }

            checkDataLoading(driver, waitMs);

            boolean dataTimeout = checkDataTimeout(driver);
            result.setDataTimeout(dataTimeout);

            if (dataTimeout) {
                result.setSuccess(false);
                result.setErrorMessage("数据加载超时，请手动查看");
                result.setRenderDurationMs(System.currentTimeMillis() - startTime);
                return result;
            }

            Thread.sleep(Math.max(waitMs, 2000));

            byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);

            result.setSuccess(true);
            result.setImageData(screenshot);
            result.setImageFormat("PNG");
            result.setRenderDurationMs(System.currentTimeMillis() - startTime);

            log.info("截图成功，大小: {} bytes, 耗时: {} ms",
                    screenshot.length, result.getRenderDurationMs());

        } catch (Exception e) {
            log.error("截图失败: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorMessage("截图失败: " + e.getMessage());
            result.setRenderDurationMs(System.currentTimeMillis() - startTime);
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception e) {
                    log.warn("关闭WebDriver失败: {}", e.getMessage());
                }
            }
        }

        return result;
    }

    @Override
    public void cleanup() {
    }

    private WebDriver createWebDriver(int width, int height) {
        ChromeOptions options = new ChromeOptions();

        if (headless) {
            options.addArguments("--headless=new");
        }

        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-web-security");
        options.addArguments("--allow-running-insecure-content");
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--window-size=" + width + "," + height);
        options.addArguments("--user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        if (chromePath != null && !chromePath.isEmpty()) {
            options.setBinary(chromePath);
        }

        ChromeDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(queryTimeout + 10, TimeUnit.SECONDS);
        driver.manage().timeouts().scriptTimeout(queryTimeout + 5, TimeUnit.SECONDS);
        driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);

        return driver;
    }

    private void setAuthToken(WebDriver driver, String authToken) {
        String blankPage = frontendUrl + "/blank";
        driver.get(blankPage);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.localStorage.setItem('authToken', arguments[0]);", authToken);
        js.executeScript("document.cookie = 'authToken=' + arguments[0] + '; path=/;';", authToken);
    }

    private String buildDashboardUrl(UUID dashboardId, ScheduleConfig config,
                                     Map<String, Object> filters) {
        StringBuilder url = new StringBuilder(frontendUrl);
        url.append("/dashboard/").append(dashboardId);
        url.append("?mode=fullscreen");
        url.append("&hideToolbar=true");
        url.append("&hideFilters=true");

        if (config.getTabsToInclude() != null) {
            try {
                @SuppressWarnings("unchecked")
                java.util.List<String> tabs = objectMapper.readValue(
                        config.getTabsToInclude(),
                        objectMapper.getTypeFactory().constructCollectionType(
                                java.util.List.class, String.class));
                if (!tabs.isEmpty()) {
                    url.append("&tabs=").append(String.join(",", tabs));
                }
            } catch (Exception e) {
                log.warn("解析tabs配置失败: {}", e.getMessage());
            }
        }

        if (filters != null && !filters.isEmpty()) {
            try {
                String filtersJson = objectMapper.writeValueAsString(filters);
                url.append("&filters=").append(Base64.getUrlEncoder()
                        .encodeToString(filtersJson.getBytes()));
            } catch (Exception e) {
                log.warn("编码过滤器失败: {}", e.getMessage());
            }
        }

        if (config.getIsDataTimeout() != null && config.getIsDataTimeout()) {
            url.append("&timeout=true");
        }

        return url.toString();
    }

    private String generateAuthToken(ScheduleConfig config) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "schedule");
        claims.put("scheduleId", config.getId().toString());
        claims.put("dashboardId", config.getDashboard().getId().toString());
        claims.put("tenantId", config.getTenant().getId().toString());

        return jwtUtil.generateToken(
                config.getCreatedBy(),
                config.getTenant().getId().toString(),
                "ADMIN",
                claims,
                24 * 60 * 60
        );
    }

    private void checkDataLoading(WebDriver driver, int waitMs) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            for (int i = 0; i < 30; i++) {
                Boolean allLoaded = (Boolean) js.executeScript(
                        "return window.__dashboardLoaded === true || " +
                        "document.querySelectorAll('[data-loading=\"true\"]').length === 0");
                if (Boolean.TRUE.equals(allLoaded)) {
                    break;
                }
                Thread.sleep(500);
            }
        } catch (Exception e) {
            log.debug("检查数据加载状态失败: {}", e.getMessage());
        }
    }

    private boolean checkDataTimeout(WebDriver driver) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Boolean hasTimeout = (Boolean) js.executeScript(
                    "return window.__dataTimeout === true || " +
                    "document.querySelectorAll('[data-timeout=\"true\"]').length > 0");
            return Boolean.TRUE.equals(hasTimeout);
        } catch (Exception e) {
            log.debug("检查数据超时状态失败: {}", e.getMessage());
            return false;
        }
    }
}
