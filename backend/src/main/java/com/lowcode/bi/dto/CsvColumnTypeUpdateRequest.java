package com.lowcode.bi.dto;

import com.lowcode.bi.common.enums.ColumnDataType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class CsvColumnTypeUpdateRequest {
    @NotNull(message = "数据源ID不能为空")
    private UUID dataSourceId;

    @NotNull(message = "表ID不能为空")
    private UUID tableId;

    @NotEmpty(message = "列类型映射不能为空")
    private Map<String, ColumnDataType> columnTypes;
}
