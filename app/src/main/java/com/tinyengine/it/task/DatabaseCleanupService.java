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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
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

    /** 每天24:00自动执行清空操作 */
    @Scheduled(cron = "${cleanup.cron-expression:0 0 0 * * ?}")
    public void autoCleanupAtMidnight() {
        if (!cleanupProperties.isEnabled()) {
            LOGGER.info("⏸️ Clearing tasks is disabled, skipping execution");
            return;
        }

        String executionId = UUID.randomUUID().toString().substring(0, EXEC_ID_LENGTH);
        String startTime = LocalDateTime.now(ZoneId.systemDefault()).format(FORMATTER);

        LOGGER.info("======= Start executing the database clearing task [{}] =======", executionId);
        LOGGER.info("⏰ Time: {}", startTime);
        LOGGER.info("📋 Tables: {}", getWhitelistTables());

        ExecutionStats stats = new ExecutionStats(executionId, startTime);
        executionStats.put(executionId, stats);
        totalExecutions.incrementAndGet();

        int successCount = 0;
        int failedCount = 0;
        long totalRowsCleaned = 0L;

        for (String tableName : getWhitelistTables()) {
            try {
                validateTableName(tableName);

                if (!tableExists(tableName)) {
                    LOGGER.warn("⚠️  Table {} does not exist, skip", tableName);
                    stats.recordSkipped(tableName, "Table does not exist");
                    continue;
                }

                long beforeCount = getTableRecordCount(tableName);
                long rowsCleaned;

                if (cleanupProperties.isUseTruncate()) {
                    truncateTable(tableName);
                    rowsCleaned = beforeCount;
                } else {
                    rowsCleaned = clearTableData(tableName);
                }

                totalRowsCleaned += rowsCleaned;
                successCount++;

                LOGGER.info("✅ Table {} cleared: {} records deleted", tableName, rowsCleaned);
                stats.recordSuccess(tableName, rowsCleaned);

            } catch (Exception e) {
                failedCount++;
                LOGGER.error("❌ Failed to clear table {}: {}", tableName, e.getMessage(), e);
                stats.recordFailure(tableName, e.getMessage());
            }
        }

        String endTime = LocalDateTime.now(ZoneId.systemDefault()).format(FORMATTER);
        stats.setEndTime(endTime);
        stats.setTotalRowsCleaned(totalRowsCleaned);

        LOGGER.info("📊 ======= Task Completion Statistics [{}] =======", executionId);
        LOGGER.info("✅ Successful table count: {}", successCount);
        LOGGER.info("❌ Failure count: {}", failedCount);
        LOGGER.info("📈 Total deleted records: {}", totalRowsCleaned);
        LOGGER.info("⏰ Time-consuming: {} second", stats.getDurationSeconds());
        LOGGER.info("🕐 Start: {}, End: {}", startTime, endTime);
        LOGGER.info("🎉 ======= Task execution completed =======\n");
    }

    /** 每天23:55发送预警通知 */
    @Scheduled(cron = "0 55 23 * * ?")
    public void sendCleanupWarning() {
        if (!cleanupProperties.isEnabled() || !cleanupProperties.isSendWarning()) {
            return;
        }

        LOGGER.warn(
                "⚠️  ⚠️  ⚠️ Important Notice: The database table will be automatically cleared in 5"
                        + " minutes！");
        LOGGER.warn("📋 Target table: {}", getWhitelistTables());
        LOGGER.warn("⏰ Execution Time: 00:00:00");
        LOGGER.warn("💡 If you need to cancel, please change the settings: cleanup.enabled=false");
        LOGGER.warn("==========================================");
    }

    /** 应用启动时初始化 */
    @PostConstruct
    public void init() {
        LOGGER.info("🚀 Database auto-clear service initialization completed");
        LOGGER.info("📋 Configuration table: {}", getWhitelistTables());
        LOGGER.info("⏰ Execution time: {}", cleanupProperties.getCronExpression());
        LOGGER.info(
                "🔧 Mode in use: {}", cleanupProperties.isUseTruncate() ? "TRUNCATE" : "DELETE");
        LOGGER.info("✅ Service status: {}", cleanupProperties.isEnabled() ? "Enabled" : "Disabled");
        LOGGER.info("==========================================");
    }

    /**
     * 获取白名单表列表.
     *
     * @return whitelist table names
     */
    public List<String> getWhitelistTables() {
        List<String> tables = cleanupProperties.getWhitelistTables();
        return tables != null && !tables.isEmpty() ? tables : DEFAULT_TABLES;
    }

    /**
     * 清空表数据（DELETE方式）.
     *
     * @return number of deleted rows
     */
    private long clearTableData(String tableName) {
        validateTableName(tableName);
        String sql = "DELETE FROM " + tableName;
        int affectedRows = jdbcTemplate.update(sql);
        return affectedRows;
    }

    /** 清空表数据（TRUNCATE方式） */
    private void truncateTable(String tableName) {
        validateTableName(tableName);
        String sql = "TRUNCATE TABLE " + tableName;
        jdbcTemplate.execute(sql);
    }

    /**
     * 检查表是否存在.
     *
     * @return whether the table exists
     */
    public boolean tableExists(String tableName) {
        try {
            String sql =
                    "SELECT COUNT(*) FROM information_schema.tables "
                            + "WHERE table_schema = DATABASE() AND table_name = ?";
            Integer count =
                    jdbcTemplate.queryForObject(sql, Integer.class, tableName.toUpperCase());
            return count != null && count > 0;
        } catch (Exception e) {
            LOGGER.warn("The checklist has failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取表记录数量.
     *
     * @return record count in the table
     */
    public long getTableRecordCount(String tableName) {
        try {
            validateTableName(tableName);
            String sql = "SELECT COUNT(*) FROM " + tableName;
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            LOGGER.error("获取表记录数失败: {}", e.getMessage());
            return -1;
        }
    }

    /** 验证表名安全性 */
    private void validateTableName(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
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

    /** 执行统计内部类 */
    public static class ExecutionStats {
        private final String executionId;
        private final String startTime;
        private String endTime;
        private long totalRowsCleaned;
        private final Map<String, TableResult> tableResults = new LinkedHashMap<>();

        public ExecutionStats(String executionId, String startTime) {
            this.executionId = executionId;
            this.startTime = startTime;
        }

        public void recordSuccess(String tableName, long rowsCleaned) {
            tableResults.put(tableName, new TableResult("SUCCESS", rowsCleaned, null));
        }

        public void recordFailure(String tableName, String errorMessage) {
            tableResults.put(tableName, new TableResult("FAILED", 0, errorMessage));
        }

        public void recordSkipped(String tableName, String reason) {
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

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }

        public long getTotalRowsCleaned() {
            return totalRowsCleaned;
        }

        public void setTotalRowsCleaned(long totalRowsCleaned) {
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

        public TableResult(String status, long rowsCleaned, String message) {
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
