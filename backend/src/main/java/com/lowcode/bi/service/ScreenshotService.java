package com.lowcode.bi.service;

import com.lowcode.bi.entity.ScheduleConfig;

import java.util.Map;
import java.util.UUID;

public interface ScreenshotService {

    ScreenshotResult captureDashboard(UUID dashboardId, ScheduleConfig config, Map<String, Object> filters);

    ScreenshotResult captureUrl(String url, int width, int height, int waitMs, String authToken);

    void cleanup();

    class ScreenshotResult {
        private boolean success;
        private byte[] imageData;
        private String imageFormat;
        private long renderDurationMs;
        private String errorMessage;
        private boolean dataTimeout;
        private String dashboardLink;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public byte[] getImageData() { return imageData; }
        public void setImageData(byte[] imageData) { this.imageData = imageData; }
        public String getImageFormat() { return imageFormat; }
        public void setImageFormat(String imageFormat) { this.imageFormat = imageFormat; }
        public long getRenderDurationMs() { return renderDurationMs; }
        public void setRenderDurationMs(long renderDurationMs) { this.renderDurationMs = renderDurationMs; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public boolean isDataTimeout() { return dataTimeout; }
        public void setDataTimeout(boolean dataTimeout) { this.dataTimeout = dataTimeout; }
        public String getDashboardLink() { return dashboardLink; }
        public void setDashboardLink(String dashboardLink) { this.dashboardLink = dashboardLink; }
    }
}
