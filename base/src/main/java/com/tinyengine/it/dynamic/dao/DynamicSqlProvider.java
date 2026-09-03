package com.tinyengine.it.dynamic.dao;

import com.tinyengine.it.common.utils.SqlIdentifierValidator;
import org.apache.ibatis.jdbc.SQL;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DynamicSqlProvider {

    private static final String COUNT_SELECT = "COUNT(*) AS count";
    private static final String COUNT_SELECT_LEGACY = "COUNT(*) as count";

    public String select(Map<String, Object> params) {
        String tableName = requireIdentifier(params.get("tableName"), "tableName");
        List<?> fields = getList(params.get("fields"));
        Map<?, ?> conditions = getMap(params.get("conditions"));
        Integer pageNum = (Integer) params.get("pageNum");
        Integer pageSize = (Integer) params.get("pageSize");
        String orderBy = getOptionalIdentifier(params.get("orderBy"), "orderBy");
        String orderType = getOrderType(params.get("orderType"));

        SQL sql = new SQL();

        if (fields != null && !fields.isEmpty()) {
            for (Object field : fields) {
                sql.SELECT(getSelectField(field));
            }
        } else {
            sql.SELECT("*");
        }

        sql.FROM(tableName);

        if (conditions != null && !conditions.isEmpty()) {
            List<Object> conditionValues = new ArrayList<>();
            int index = 0;
            for (Map.Entry<?, ?> entry : conditions.entrySet()) {
                if (entry.getValue() != null) {
                    String columnName = requireIdentifier(entry.getKey(), "condition key");
                    conditionValues.add(entry.getValue());
                    sql.WHERE(columnName + " = #{conditionValues[" + index + "]}");
                    index++;
                }
            }
            params.put("conditionValues", conditionValues);
        }

        if (orderBy != null && !orderBy.isEmpty()) {
            sql.ORDER_BY(orderBy + " " + orderType);
        }

        if (pageNum != null && pageSize != null) {
            int safePageNum = requirePositiveInt(pageNum, "pageNum");
            int safePageSize = requirePositiveInt(pageSize, "pageSize");
            params.put("offset", (safePageNum - 1) * safePageSize);
            params.put("limit", safePageSize);
            return sql + " LIMIT #{offset}, #{limit}";
        }

        return sql.toString();
    }

    public String insert(Map<String, Object> params) {
        String tableName = requireIdentifier(params.get("tableName"), "tableName");
        Map<?, ?> data = getRequiredMap(params.get("data"), "data");
        List<Object> dataValues = new ArrayList<>();

        SQL sql = new SQL();
        sql.INSERT_INTO(tableName);

        int index = 0;
        for (Map.Entry<?, ?> entry : data.entrySet()) {
            String columnName = requireIdentifier(entry.getKey(), "data key");
            dataValues.add(entry.getValue());
            sql.VALUES(columnName, "#{dataValues[" + index + "]}");
            index++;
        }
        params.put("dataValues", dataValues);

        return sql.toString();
    }

    public String update(Map<String, Object> params) {
        String tableName = requireIdentifier(params.get("tableName"), "tableName");
        Map<?, ?> data = getRequiredMap(params.get("data"), "data");
        Map<?, ?> conditions = getRequiredMap(params.get("conditions"), "conditions");
        List<Object> dataValues = new ArrayList<>();
        List<Object> conditionValues = new ArrayList<>();

        SQL sql = new SQL();
        sql.UPDATE(tableName);

        int dataIndex = 0;
        for (Map.Entry<?, ?> entry : data.entrySet()) {
            String columnName = requireIdentifier(entry.getKey(), "data key");
            dataValues.add(entry.getValue());
            sql.SET(columnName + " = #{dataValues[" + dataIndex + "]}");
            dataIndex++;
        }
        params.put("dataValues", dataValues);

        int conditionIndex = 0;
        for (Map.Entry<?, ?> entry : conditions.entrySet()) {
            if (entry.getValue() != null) {
                String columnName = requireIdentifier(entry.getKey(), "condition key");
                conditionValues.add(entry.getValue());
                sql.WHERE(columnName + " = #{conditionValues[" + conditionIndex + "]}");
                conditionIndex++;
            }
        }
        if (conditionValues.isEmpty()) {
            throw new IllegalArgumentException("conditions cannot be empty");
        }
        params.put("conditionValues", conditionValues);

        return sql.toString();
    }

    public String delete(Map<String, Object> params) {
        String tableName = requireIdentifier(params.get("tableName"), "tableName");
        Map<?, ?> conditions = getRequiredMap(params.get("conditions"), "conditions");
        List<Object> conditionValues = new ArrayList<>();

        SQL sql = new SQL();
        sql.DELETE_FROM(tableName);

        int index = 0;
        for (Map.Entry<?, ?> entry : conditions.entrySet()) {
            if (entry.getValue() != null) {
                String columnName = requireIdentifier(entry.getKey(), "condition key");
                conditionValues.add(entry.getValue());
                sql.WHERE(columnName + " = #{conditionValues[" + index + "]}");
                index++;
            }
        }
        if (conditionValues.isEmpty()) {
            throw new IllegalArgumentException("conditions cannot be empty");
        }
        params.put("conditionValues", conditionValues);

        return sql.toString();
    }

    private String getSelectField(Object field) {
        if (field instanceof String && COUNT_SELECT_LEGACY.equalsIgnoreCase(((String) field).trim())) {
            return COUNT_SELECT;
        }
        return requireIdentifier(field, "field");
    }

    private String getOptionalIdentifier(Object value, String name) {
        if (value == null) {
            return null;
        }
        String identifier = requireString(value, name);
        if (identifier.isEmpty()) {
            return null;
        }
        return SqlIdentifierValidator.requireValidIdentifier(identifier);
    }

    private String requireIdentifier(Object value, String name) {
        return SqlIdentifierValidator.requireValidIdentifier(requireString(value, name));
    }

    private String requireString(Object value, String name) {
        if (!(value instanceof String)) {
            throw new IllegalArgumentException(name + " must be a string");
        }
        return (String) value;
    }

    private String getOrderType(Object value) {
        if (value == null || (value instanceof String && ((String) value).isEmpty())) {
            return "ASC";
        }
        return SqlIdentifierValidator.requireValidOrderType(requireString(value, "orderType"));
    }

    private int requirePositiveInt(Integer value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private List<?> getList(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof List<?>)) {
            throw new IllegalArgumentException("fields must be a list");
        }
        return (List<?>) value;
    }

    private Map<?, ?> getMap(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("conditions must be a map");
        }
        return (Map<?, ?>) value;
    }

    private Map<?, ?> getRequiredMap(Object value, String name) {
        if (!(value instanceof Map<?, ?>) || ((Map<?, ?>) value).isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be empty");
        }
        return (Map<?, ?>) value;
    }
}
