package com.lowcode.bi.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class DataLineageResponse {
    private DataSourceNode dataSource;
    private List<DataModelNode> dataModels;
    private List<DashboardNode> dashboards;

    @Data
    public static class DataSourceNode {
        private UUID id;
        private String name;
        private String type;
    }

    @Data
    public static class DataModelNode {
        private UUID id;
        private String name;
        private String description;
        private UUID dataSourceId;
        private Integer tableCount;
        private Integer measureCount;
        private Integer dimensionCount;
    }

    @Data
    public static class DashboardNode {
        private UUID id;
        private String name;
        private String description;
        private UUID dataModelId;
        private Integer componentCount;
        private Boolean isPublished;
    }
}
