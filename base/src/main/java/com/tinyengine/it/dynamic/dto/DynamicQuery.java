package com.tinyengine.it.dynamic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DynamicQuery {
    @NotBlank(message = "表英文名不能为空")
    @Pattern(regexp = "^[a-zA-Z_][a-zA-Z0-9_]*$", message = "模型名称格式不正确")
	private String nameEn;          // 表名
	private String nameCh;          // 表中文名
	@Pattern(regexp = "^[a-zA-Z_][a-zA-Z0-9_]*$", message = "字段名称格式不正确")
	private List<String> fields;       // 查询字段
	private Map<String, Object> params; // 查询条件
	private Integer currentPage = 1;       // 页码
	private Integer pageSize = 10;     // 每页大小
	@Pattern(regexp = "^[a-zA-Z_][a-zA-Z0-9_]*$", message = "排序字段格式不正确")
	private String orderBy;            // 排序字段
	@Pattern(regexp = "ASC|DESC", message = "排序方式必须为ASC或DESC")
	private String orderType = "ASC";  // 排序方式
}
