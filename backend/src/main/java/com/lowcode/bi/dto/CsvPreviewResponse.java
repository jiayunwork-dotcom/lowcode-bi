package com.lowcode.bi.dto;

import com.lowcode.bi.common.enums.ColumnDataType;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CsvPreviewResponse {
    private List<String> headers;
    private List<ColumnDataType> columnTypes;
    private List<Map<String, Object>> rows;
    private String charset;
    private int rowCount;
    private int columnCount;
    private String fileName;
    private long fileSize;
}
