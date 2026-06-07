package com.lowcode.bi.dto;

import com.lowcode.bi.common.enums.RefreshInterval;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CsvRefreshConfigRequest {
    @NotNull(message = "数据源ID不能为空")
    private UUID dataSourceId;

    @NotNull(message = "刷新间隔不能为空")
    private RefreshInterval refreshInterval;

    @NotBlank(message = "刷新目录不能为空")
    private String refreshDirectory;
}
