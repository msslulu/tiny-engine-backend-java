/**
 * Copyright (c) 2023 - present TinyEngine Authors. Copyright (c) 2023 - present Huawei Cloud
 * Computing Technologies Co., Ltd.
 *
 * <p>Use of this source code is governed by an MIT-style license.
 *
 * <p>THE OPEN SOURCE SOFTWARE IN THIS PRODUCT IS DISTRIBUTED IN THE HOPE THAT IT WILL BE USEFUL,
 * BUT WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS FOR A
 * PARTICULAR PURPOSE. SEE THE APPLICABLE LICENSES FOR MORE DETAILS.
 */
package com.tinyengine.it.task;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DatabaseCleanupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseCleanupService.class);
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int EXEC_ID_LENGTH = 8;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Autowired private CleanupProperties cleanupProperties;

    private final Map<String, ExecutionStats> executionStats = new ConcurrentHashMap<>();

    private final AtomicInteger totalExecutions = new AtomicInteger(0);

    // 默认白名单表（如果配置文件未设置）
    private static final List<String> DEFAULT_TABLES =
            Arrays.asList(
                    "t_resource",
                    "t_resource_group",
                    "r_resource_group_resource",
                    "t_app_extension",
                    "t_block",
                    "t_block_carriers_relation",
                    "t_block_group",
                    "t_block_history",
                    "r_material_block",
                    "r_material_history_block",
                    "r_block_group_block",
                    "t_datasource",
                    "t_i18n_entry",
                    "t_model",
                    "t_page",
                    "t_page_history",
                    "t_page_template");

    public DatabaseCleanupService() {
        // Required for Spring field injection.
    }

    /** 每天24:00自动执行清空操作 */
    @Scheduled(cron = "${cleanup.cron-expression:0 0 0 * * ?}")
    public void autoCleanupAtMidnight() {
        if (!cleanupProperties.isEnabled()) {
            logInfo("⏸️ Clearing tasks is disabled, skipping execution");
            return;
        }

        final String executionId = UUID.randomUUID().toString().substring(0, EXEC_ID_LENGTH);
        final String startTime = LocalDateTime.now(ZoneId.systemDefault()).format(FORMATTER);

        logInfo("======= Start executing the database clearing task [{}] =======", executionId);
        logInfo("⏰ Time: {}", startTime);
        logInfo("📋 Tables: {}", getWhitelistTables());

        final ExecutionStats stats = new ExecutionStats(executionId, startTime);
        executionStats.put(executionId, stats);
        totalExecutions.incrementAndGet();

        int successCount = 0;
        int failedCount = 0;
        long totalRowsCleaned = 0L;

        for (String tableName : getWhitelistTables()) {
            try {
                validateTableName(tableName);

                if (!tableExists(tableName)) {
                    logWarn("⚠️  Table {} does not exist, skip", tableName);
                    stats.recordSkipped(tableName, "Table does not exist");
                    continue;
                }

                final long beforeCount = getTableRecordCount(tableName);
                long rowsCleaned;

                if (cleanupProperties.isUseTruncate()) {
                    truncateTable(tableName);
                    rowsCleaned = beforeCount;
                } else {
                    rowsCleaned = clearTableData(tableName);
                }

                totalRowsCleaned += rowsCleaned;
                successCount++;

                logInfo("✅ Table {} cleared: {} records deleted", tableName, rowsCleaned);
                stats.recordSuccess(tableName, rowsCleaned);

            } catch (DataAccessException | IllegalArgumentException exception) {
                failedCount++;
                logError(
                        "❌ Failed to clear table {}: {}",
                        tableName,
                        exception.getMessage(),
                        exception);
                stats.recordFailure(tableName, exception.getMessage());
            }
        }

        final String endTime = LocalDateTime.now(ZoneId.systemDefault()).format(FORMATTER);
        stats.setEndTime(endTime);
        stats.setTotalRowsCleaned(totalRowsCleaned);

        logInfo("📊 ======= Task Completion Statistics [{}] =======", executionId);
        logInfo("✅ Successful table count: {}", successCount);
        logInfo("❌ Failure count: {}", failedCount);
        logInfo("📈 Total deleted records: {}", totalRowsCleaned);
        logInfo("⏰ Time-consuming: {} second", stats.getDurationSeconds());
        logInfo("🕐 Start: {}, End: {}", startTime, endTime);
        logInfo("🎉 ======= Task execution completed =======\n");
    }

    /** 每天23:55发送预警通知 */
    @Scheduled(cron = "0 55 23 * * ?")
    public void sendCleanupWarning() {
        if (!cleanupProperties.isEnabled() || !cleanupProperties.isSendWarning()) {
            return;
        }

        logWarn(
                "⚠️  ⚠️  ⚠️ Important Notice: The database table will be automatically cleared in 5"
                        + " minutes！");
        logWarn("📋 Target table: {}", getWhitelistTables());
        logWarn("⏰ Execution Time: 00:00:00");
        logWarn("💡 If you need to cancel, please change the settings: cleanup.enabled=false");
        logWarn("==========================================");
    }

    /** 应用启动时初始化 */
    @PostConstruct
    public void init() {
        logInfo("🚀 Database auto-clear service initialization completed");
        logInfo("📋 Configuration table: {}", getWhitelistTables());
        logInfo("⏰ Execution time: {}", cleanupProperties.getCronExpression());
        logInfo(
                "🔧 Mode in use: {}", cleanupProperties.isUseTruncate() ? "TRUNCATE" : "DELETE");
        logInfo("✅ Service status: {}", cleanupProperties.isEnabled() ? "Enabled" : "Disabled");
        logInfo("==========================================");
    }

    /**
     * 获取白名单表列表.
     *
     * @return whitelist table names
     */
    public List<String> getWhitelistTables() {
        final List<String> tables = cleanupProperties.getWhitelistTables();
        return tables != null && !tables.isEmpty() ? tables : DEFAULT_TABLES;
    }

    /**
     * 清空表数据（DELETE方式）.
     *
     * @return number of deleted rows
     */
    private long clearTableData(final String tableName) {
        validateTableName(tableName);
        final String sql = "DELETE FROM " + tableName;
        return jdbcTemplate.update(sql);
    }

    /** 清空表数据（TRUNCATE方式） */
    private void truncateTable(final String tableName) {
        validateTableName(tableName);
        final String sql = "TRUNCATE TABLE " + tableName;
        jdbcTemplate.execute(sql);
    }

    /**
     * 检查表是否存在.
     *
     * @return whether the table exists
     */
    public boolean tableExists(final String tableName) {
        boolean tableExists = false;
        try {
            final String sql =
                    "SELECT COUNT(*) FROM information_schema.tables "
                            + "WHERE table_schema = DATABASE() AND table_name = ?";
            final Integer count =
                    jdbcTemplate.queryForObject(sql, Integer.class, tableName.toUpperCase(Locale.ROOT));
            tableExists = count != null && count > 0;
        } catch (DataAccessException | IllegalArgumentException exception) {
            logWarn("The checklist has failed: {}", exception.getMessage());
        }
        return tableExists;
    }

    /**
     * 获取表记录数量.
     *
     * @return record count in the table
     */
    public long getTableRecordCount(final String tableName) {
        long recordCount = -1;
        try {
            validateTableName(tableName);
            final String sql = "SELECT COUNT(*) FROM " + tableName;
            final Long count = jdbcTemplate.queryForObject(sql, Long.class);
            recordCount = count != null ? count : 0;
        } catch (DataAccessException | IllegalArgumentException exception) {
            logError("获取表记录数失败: {}", exception.getMessage());
        }
        return recordCount;
    }

    /** 验证表名安全性 */
    private void validateTableName(final String tableName) {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("Table name cannot be empty");
        }
        if (!tableName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid table name format: " + tableName);
        }
    }

    /**
     * 获取执行统计.
     *
     * @return execution statistics
     */
    public Map<String, ExecutionStats> getExecutionStats() {
        return new LinkedHashMap<>(executionStats);
    }

    public int getTotalExecutions() {
        return totalExecutions.get();
    }

    private static void logInfo(final String message, final Object... arguments) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(message, arguments);
        }
    }

    private static void logWarn(final String message, final Object... arguments) {
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn(message, arguments);
        }
    }

    private static void logError(final String message, final Object... arguments) {
        if (LOGGER.isErrorEnabled()) {
            LOGGER.error(message, arguments);
        }
    }

    /** 执行统计内部类 */
    public static class ExecutionStats {
        private final String executionId;
        private final String startTime;
        private String endTime;
        private long totalRowsCleaned;
        private final Map<String, TableResult> tableResults = new LinkedHashMap<>();

        public ExecutionStats(final String executionId, final String startTime) {
            this.executionId = executionId;
            this.startTime = startTime;
        }

        public void recordSuccess(final String tableName, final long rowsCleaned) {
            tableResults.put(tableName, new TableResult("SUCCESS", rowsCleaned, null));
        }

        public void recordFailure(final String tableName, final String errorMessage) {
            tableResults.put(tableName, new TableResult("FAILED", 0, errorMessage));
        }

        public void recordSkipped(final String tableName, final String reason) {
            tableResults.put(tableName, new TableResult("SKIPPED", 0, reason));
        }

        // Getters and setters
        public String getExecutionId() {
            return executionId;
        }

        public String getStartTime() {
            return startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(final String endTime) {
            this.endTime = endTime;
        }

        public long getTotalRowsCleaned() {
            return totalRowsCleaned;
        }

        public void setTotalRowsCleaned(final long totalRowsCleaned) {
            this.totalRowsCleaned = totalRowsCleaned;
        }

        public Map<String, TableResult> getTableResults() {
            return tableResults;
        }

        public long getDurationSeconds() {
            if (startTime != null && endTime != null) {
                LocalDateTime start = LocalDateTime.parse(startTime, FORMATTER);
                LocalDateTime end = LocalDateTime.parse(endTime, FORMATTER);
                return java.time.Duration.between(start, end).getSeconds();
            }
            return 0;
        }
    }

    /** 表结果内部类 */
    public static class TableResult {
        private final String status;
        private final long rowsCleaned;
        private final String message;

        public TableResult(final String status, final long rowsCleaned, final String message) {
            this.status = status;
            this.rowsCleaned = rowsCleaned;
            this.message = message;
        }

        // Getters
        public String getStatus() {
            return status;
        }

        public long getRowsCleaned() {
            return rowsCleaned;
        }

        public String getMessage() {
            return message;
        }
    }
}
