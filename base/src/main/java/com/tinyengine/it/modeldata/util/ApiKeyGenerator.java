package com.tinyengine.it.modeldata.util;

import java.util.UUID;

public class ApiKeyGenerator {
	public static String generateApiKey() {
		// 生成一个随机的32位长度的API Key
		return java.util.UUID.randomUUID().toString().replace("-", "");
	}
	public static void main(String[] args) {
		// Generate random API key
		String apiKey = UUID.randomUUID().toString().replace("-", "");

		// Generate random secret
		String apiSecret = UUID.randomUUID().toString().replace("-", "");

		System.out.println("API Key: " + apiKey);
		System.out.println("API Secret: " + apiSecret);
	}
}
