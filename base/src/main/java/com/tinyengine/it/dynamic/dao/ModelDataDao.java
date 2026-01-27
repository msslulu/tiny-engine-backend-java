package com.tinyengine.it.dynamic.dao;

import com.alibaba.fastjson.JSONObject;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@Mapper
public interface ModelDataDao  {

	@SelectProvider(type = DynamicSqlProvider.class, method = "select")
	List<JSONObject> select(Map<String, Object> params);

	@InsertProvider(type = DynamicSqlProvider.class, method = "insert")
	@Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
	Long insert(Map<String, Object> params);

	@UpdateProvider(type = DynamicSqlProvider.class, method = "update")
	Integer update(Map<String, Object> params);

	@DeleteProvider(type = DynamicSqlProvider.class, method = "delete")
	Integer delete(Map<String, Object> params);

	@Select("SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT " +
		"FROM INFORMATION_SCHEMA.COLUMNS " +
		"WHERE TABLE_NAME = #{tableName} AND TABLE_SCHEMA = DATABASE()")
	List<Map<String, Object>> getTableStructure(String tableName);
}

