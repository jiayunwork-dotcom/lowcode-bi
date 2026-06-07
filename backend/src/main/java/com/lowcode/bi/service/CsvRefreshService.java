package com.lowcode.bi.service;

import com.lowcode.bi.common.enums.RefreshInterval;
import com.lowcode.bi.entity.DataSource;

import java.util.UUID;

public interface CsvRefreshService {
    void configureRefresh(UUID dataSourceId, RefreshInterval interval, String directory);

    void executeRefresh(UUID dataSourceId);

    void executeAllDueRefreshes();

    void triggerManualRefresh(UUID dataSourceId);

    void checkAndRefreshCsvFile(DataSource dataSource);
}
