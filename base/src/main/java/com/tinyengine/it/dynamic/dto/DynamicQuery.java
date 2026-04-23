package com.tinyengine.it.dynamic.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DynamicQuery {

	private String nameEn;          // 表名
	private String nameCh;          // 表中文名
	private Map<String, Object> params; // 查询条件
	private Integer currentPage = 1;       // 页码
	private Integer pageSize = 10;     // 每页大小

}
