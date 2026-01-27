package com.tinyengine.it.dynamic.dto;

import lombok.Data;

import java.util.Map;
@Data
public class DynamicInsert {
	private String nameEn;
	private Map<String, Object> params;       // 插入/更新数据
}
