package com.tinyengine.it.dynamic.dao;

import org.apache.ibatis.jdbc.SQL;

import java.util.List;
import java.util.Map;

public class DynamicSqlProvider {

	public String select(Map<String, Object> params) {
		String tableName = (String) params.get("tableName");
		List<String> fields = (List<String>) params.get("fields");
		Map<String, Object> conditions = (Map<String, Object>) params.get("conditions");
		Integer pageNum = (Integer) params.get("pageNum");
		Integer pageSize = (Integer) params.get("pageSize");
		String orderBy = (String) params.get("orderBy");
		String orderType = (String) params.get("orderType");

		SQL sql = new SQL();

		// 选择字段
		if (fields != null && !fields.isEmpty()) {
			for (String field : fields) {
				sql.SELECT(field);
			}
		} else {
			sql.SELECT("*");
		}

		sql.FROM(tableName);

		// 条件
		if (conditions != null && !conditions.isEmpty()) {
			for (Map.Entry<String, Object> entry : conditions.entrySet()) {
				if (entry.getValue() != null) {
					sql.WHERE(entry.getKey() + " = #{conditions." + entry.getKey() + "}");
				}
			}
		}
		// 排序
		if (orderBy != null && !orderBy.isEmpty()) {
			sql.ORDER_BY(orderBy + " " + orderType);
		}

		// 分页
		if (pageNum != null && pageSize != null) {
			return sql.toString() + " LIMIT " + (pageNum - 1) * pageSize + ", " + pageSize;
		}

		return sql.toString();
	}

	public String insert(Map<String, Object> params) {
		String tableName = (String) params.get("tableName");
		Map<String, Object> data = (Map<String, Object>) params.get("data");

		SQL sql = new SQL();
		sql.INSERT_INTO(tableName);

		if (data != null && !data.isEmpty()) {
			for (Map.Entry<String, Object> entry : data.entrySet()) {
				sql.VALUES(entry.getKey(), "#{data." + entry.getKey() + "}");
			}
		}

		return sql.toString();
	}

	public String update(Map<String, Object> params) {
		String tableName = (String) params.get("tableName");
		Map<String, Object> data = (Map<String, Object>) params.get("data");
		Map<String, Object> conditions = (Map<String, Object>) params.get("conditions");

		SQL sql = new SQL();
		sql.UPDATE(tableName);

		if (data != null && !data.isEmpty()) {
			for (Map.Entry<String, Object> entry : data.entrySet()) {
				sql.SET(entry.getKey() + " = #{data." + entry.getKey() + "}");
			}
		}

		if (conditions != null && !conditions.isEmpty()) {
			for (Map.Entry<String, Object> entry : conditions.entrySet()) {
				sql.WHERE(entry.getKey() + " = #{conditions." + entry.getKey() + "}");
			}
		}

		return sql.toString();
	}

	public String delete(Map<String, Object> params) {
		String tableName = (String) params.get("tableName");
		Map<String, Object> conditions = (Map<String, Object>) params.get("conditions");

		SQL sql = new SQL();
		sql.DELETE_FROM(tableName);

		if (conditions != null && !conditions.isEmpty()) {
			for (Map.Entry<String, Object> entry : conditions.entrySet()) {
				sql.WHERE(entry.getKey() + " = #{conditions." + entry.getKey() + "}");
			}
		}

		return sql.toString();
	}
}
