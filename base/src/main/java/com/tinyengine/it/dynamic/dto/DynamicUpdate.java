package com.tinyengine.it.dynamic.dto;

import lombok.Data;

import java.util.Map;
@Data
public class DynamicUpdate {
	private String nameEn;
	private Map<String, Object> data;
	private Map<String, Object> params;// 查询条件
}
