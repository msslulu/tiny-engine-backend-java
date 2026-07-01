package com.tinyengine.it.common.utils;


import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonUtilsTest {

	@Test
	void testEncode_Success() {
		// Arrange
		Map<String, Object> data = Map.of("key", "value", "number", 123);

		// Act
		String json = JsonUtils.encode(data);

		// Assert
		assertNotNull(json);
		assertTrue(json.contains("\"key\":\"value\""));
		assertTrue(json.contains("\"number\":123"));
	}
	@Test
	void testEncode_Success_with_string2() {
		// Arrange
		Map<String, Object> data = Map.of("key", "value", "string", "2");

		// Act
		String json = JsonUtils.encode(data);

		// Assert
		assertNotNull(json);
		assertTrue(json.contains("\"key\":\"value\""));
		assertTrue(json.contains("\"string\":\"2\""));
	}
	@Test
	void testEncode_Success_with_string3() {
		// Arrange
		Map<String, Object> data = Map.of("key", "value", "string", "3");

		// Act
		String json = JsonUtils.encode(data);

		// Assert
		assertNotNull(json);
		assertTrue(json.contains("\"key\":\"value\""));
		assertTrue(json.contains("\"string\":\"3\""));
	}
	@Test
	void testEncode_Success_with_string4() {
		// Arrange
		Map<String, Object> data = Map.of("key", "value", "string", "4");

		// Act
		String json = JsonUtils.encode(data);

		// Assert
		assertNotNull(json);
		assertTrue(json.contains("\"key\":\"value\""));
		assertTrue(json.contains("\"string\":\"4\""));
	}
	@Test
	void testEncode_Success_with_string5() {
		// Arrange
		Map<String, Object> data = Map.of("key", "value", "string", "5");

		// Act
		String json = JsonUtils.encode(data);

		// Assert
		assertNotNull(json);
		assertTrue(json.contains("\"key\":\"value\""));
		assertTrue(json.contains("\"string\":\"5\""));
	}
	@Test
	void testEncode_Success_with_string() {
		// Arrange
		Map<String, Object> data = Map.of("key", "value", "string", "123");

		// Act
		String json = JsonUtils.encode(data);

		// Assert
		assertNotNull(json);
		assertTrue(json.contains("\"key\":\"value\""));
		assertTrue(json.contains("\"string\":\"123\""));
	}

	@Test
	void testEncode_NullInput() {
		// Act
		String json = JsonUtils.encode(null);

		// Assert
		assertEquals("null", json);
	}

	@Test
	void testDecode_Success() {

			// Arrange: Prepare a map and encode it to JSON
			Map<String, Object> data = Map.of("key", "value", "number", 123);
			String json = JsonUtils.encode(data);

			// Act: Decode the JSON back to a map
			Map<String, Object> result = JsonUtils.decode(json, Map.class);

			// Assert: Verify the decoded map matches the original data
			assertNotNull(result, "Decoded result should not be null");
			assertEquals(2, result.size(), "Result size should match the original map");
			assertEquals("value", result.get("key"), "Key 'key' should have value 'value'");
			assertEquals(123, result.get("number"), "Key 'number' should have value 123");
	}



	@Test
	void testDecode_InvalidJson() {
		// Arrange
		String invalidJson = "{key:value}";

		// Act & Assert
		Exception exception = assertThrows(Exception.class, () -> JsonUtils.decode(invalidJson, Map.class));
		assertFalse(exception.getMessage().contains("Unrecognized token"));
	}



	@Test
	void testGetList_FromString() {
		// Arrange
		String input = "a, b, c";

		// Act
		List<String> result = JsonUtils.getList(input);

		// Assert
		assertEquals(3, result.size());
		assertEquals("a", result.get(0));
		assertEquals("b", result.get(1));
		assertEquals("c", result.get(2));
	}

	@Test
	void testGetList_FromArray() {
		// Arrange
		String[] input = {"a", "b", "c"};

		// Act
		List<String> result = JsonUtils.getList(input);

		// Assert
		assertEquals(3, result.size());
		assertEquals("a", result.get(0));
		assertEquals("b", result.get(1));
		assertEquals("c", result.get(2));
	}

	@Test
	void testGetList_FromNull() {
		// Act
		List<String> result = JsonUtils.getList(null);

		// Assert
		assertTrue(result.isEmpty());
	}

	@Test
	void testEncode_EmptyMap() {
		// Arrange
		Map<String, Object> data = Map.of();

		// Act
		String json = JsonUtils.encode(data);

		// Assert
		assertNotNull(json);
		assertEquals("{}", json);
	}

	@Test
	void testDecode_NestedJson() {
		// Arrange
		String nestedJson = "{\"key\":\"value\",\"nested\":{\"innerKey\":\"innerValue\"}}";

		// Act
		Map<String, Object> result = JsonUtils.decode(nestedJson, Map.class);

		// Assert
		assertNotNull(result);
		assertEquals("value", result.get("key"));
		Map<String, Object> nested = (Map<String, Object>) result.get("nested");
		assertNotNull(nested);
		assertEquals("innerValue", nested.get("innerKey"));
	}

	@Test
	void testGetList_UnsupportedType() {
		// Arrange
		Object unsupportedInput = 12345;

		// Act
		List<String> result = JsonUtils.getList(unsupportedInput);

		// Assert
		assertTrue(result.isEmpty());
	}
	@Test
	void testEncode_List() {
		// Arrange
		List<String> data = List.of("item1", "item2", "item3");

		// Act
		String json = JsonUtils.encode(data);

		// Assert
		assertNotNull(json);
		assertTrue(json.contains("[\"item1\",\"item2\",\"item3\"]"));
	}

	@Test
	void testDecode_InvalidJsonStructure() {
		// Arrange
		String invalidJson = "[{\"key\":\"value\"}, {\"key2\":}]";

		// Act & Assert
		Exception exception = assertThrows(Exception.class, () -> JsonUtils.decode(invalidJson, List.class));
		assertTrue(exception.getMessage().contains("Unexpected character"));
	}

	@Test
	void testGetList_MixedDataTypes() {
		// Arrange
		List<Object> input = List.of("string", 123, true);

		// Act
		List<String> result = JsonUtils.getList(input);

		// Assert
		assertEquals(3, result.size());
		assertEquals("string", result.get(0));
		assertEquals("123", result.get(1));
		assertEquals("true", result.get(2));
	}

	@Test
	void testEncodePrettily_ComplexObject() {
		// Arrange
		Map<String, Object> data = Map.of(
			"key", "value",
			"nested", Map.of("innerKey", "innerValue"),
			"list", List.of(1, 2, 3)
		);

		// Act
		String prettyJson = JsonUtils.encodePrettily(data);

		// Assert
		assertNotNull(prettyJson);
		assertTrue(prettyJson.contains("\n")); // Pretty JSON should have line breaks
		assertTrue(prettyJson.contains("\"key\" : \"value\""));
		assertTrue(prettyJson.contains("\"innerKey\" : \"innerValue\""));
	}

	@Test
	void testDecode_ByteArray() {
		// Arrange
		String json = "{\"key\":\"value\",\"number\":123}";
		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

		// Act
		Map<String, Object> result = JsonUtils.decode(jsonBytes, Map.class);

		// Assert
		assertNotNull(result);
		assertEquals("value", result.get("key"));
		assertEquals(123, result.get("number"));
	}

	@Test
	void testConvertValue_NestedObjects() {
		// Arrange
		Map<String, Object> data = Map.of(
			"key", "value",
			"nested", Map.of("innerKey", "innerValue")
		);

		// Act
		NestedObject result = JsonUtils.convertValue(data, NestedObject.class);

		// Assert
		assertNotNull(result);
		assertEquals("value", result.getKey());
		assertNotNull(result.getNested());
		assertEquals("innerValue", result.getNested().getInnerKey());
	}

	// Helper class for nested object conversion
	static class NestedObject {
		private String key;
		private InnerNestedObject nested;

		// Getters and setters
		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public InnerNestedObject getNested() {
			return nested;
		}

		public void setNested(InnerNestedObject nested) {
			this.nested = nested;
		}

		static class InnerNestedObject {
			private String innerKey;

			public String getInnerKey() {
				return innerKey;
			}

			public void setInnerKey(String innerKey) {
				this.innerKey = innerKey;
			}
		}
	}

	@Test
	void testDecode_DeeplyNestedJson() {
		// Arrange
		String deeplyNestedJson = "{\"level1\":{\"level2\":{\"level3\":{\"key\":\"value\"}}}}";

		// Act
		Map<String, Object> result = JsonUtils.decode(deeplyNestedJson, Map.class);

		// Assert
		assertNotNull(result);
		Map<String, Object> level1 = (Map<String, Object>) result.get("level1");
		assertNotNull(level1);
		Map<String, Object> level2 = (Map<String, Object>) level1.get("level2");
		assertNotNull(level2);
		Map<String, Object> level3 = (Map<String, Object>) level2.get("level3");
		assertNotNull(level3);
		assertEquals("value", level3.get("key"));
	}



	@Test
	void testConvertValue_ComplexObjectWithListsAndMaps() {
		// Arrange
		Map<String, Object> data = Map.of(
			"key", "value",
			"list", List.of(1, 2, 3),
			"map", Map.of("innerKey", "innerValue")
		);

		// Act
		ComplexObject result = JsonUtils.convertValue(data, ComplexObject.class);

		// Assert
		assertNotNull(result);
		assertEquals("value", result.getKey());
		assertEquals(List.of(1, 2, 3), result.getList());
		assertNotNull(result.getMap());
		assertEquals("innerValue", result.getMap().get("innerKey"));
	}

	@Test
	void testDecode_MalformedByteArray() {
		// Arrange
		byte[] malformedBytes = new byte[]{0x01, 0x02, 0x03};

		// Act & Assert
		Exception exception = assertThrows(Exception.class, () -> JsonUtils.decode(malformedBytes, Map.class));
		assertFalse(exception.getMessage().contains("Cannot deserialize"));
	}

	@Test
	void testGetList_DeeplyNestedList() {
		// Arrange
		List<Object> input = List.of(
			"value1",
			List.of("nested1", "nested2"),
			"value2"
		);

		// Act
		List<String> result = JsonUtils.getList(input);

		// Assert
		assertEquals(3, result.size());
		assertEquals("value1", result.get(0));
		assertEquals("[nested1, nested2]", result.get(1)); // Nested list converted to string
		assertEquals("value2", result.get(2));
	}

	@Test
	void testConvertValue_MapWithMixedDataTypes() {
		// Arrange
		Map<String, Object> data = Map.of(
			"stringKey", "stringValue",
			"intKey", 123,
			"booleanKey", true,
			"listKey", List.of(1, 2, 3)
		);

		// Act
		MixedTypeObject result = JsonUtils.convertValue(data, MixedTypeObject.class);

		// Assert
		assertNotNull(result);
		assertEquals("stringValue", result.getStringKey());
		assertEquals(123, result.getIntKey());
		assertTrue(result.isBooleanKey());
		assertEquals(List.of(1, 2, 3), result.getListKey());
	}

	@Test
	void testEncode_MapWithSpecialCharacters() {
		// Arrange
		Map<String, Object> data = Map.of(
			"key", "value",
			"specialChars", "!@#$%^&*()_+"
		);

		// Act
		String json = JsonUtils.encode(data);

		// Assert
		assertNotNull(json);
		assertTrue(json.contains("\"key\":\"value\""));
		assertTrue(json.contains("\"specialChars\":\"!@#$%^&*()_+\""));
	}





	// Helper class for mixed data type conversion
	static class MixedTypeObject {
		private String stringKey;
		private int intKey;
		private boolean booleanKey;
		private List<Integer> listKey;

		// Getters and setters
		public String getStringKey() {
			return stringKey;
		}

		public void setStringKey(String stringKey) {
			this.stringKey = stringKey;
		}

		public int getIntKey() {
			return intKey;
		}

		public void setIntKey(int intKey) {
			this.intKey = intKey;
		}

		public boolean isBooleanKey() {
			return booleanKey;
		}

		public void setBooleanKey(boolean booleanKey) {
			this.booleanKey = booleanKey;
		}

		public List<Integer> getListKey() {
			return listKey;
		}

		public void setListKey(List<Integer> listKey) {
			this.listKey = listKey;
		}
	}

	// Helper class for complex object conversion
	static class ComplexObject {
		private String key;
		private List<Integer> list;
		private Map<String, String> map;

		// Getters and setters
		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public List<Integer> getList() {
			return list;
		}

		public void setList(List<Integer> list) {
			this.list = list;
		}

		public Map<String, String> getMap() {
			return map;
		}

		public void setMap(Map<String, String> map) {
			this.map = map;
		}
	}
	@Test
	void testEncode_MapWithNestedLists() {
		// Arrange
		Map<String, Object> data = Map.of(
			"key", "value",
			"nestedList", List.of(
				Map.of("innerKey1", "innerValue1"),
				Map.of("innerKey2", "innerValue2")
			)
		);

		// Act
		String json = JsonUtils.encode(data);

		// Assert
		assertNotNull(json);
		assertTrue(json.contains("\"key\":\"value\""));
		assertTrue(json.contains("\"nestedList\":[{\"innerKey1\":\"innerValue1\"},{\"innerKey2\":\"innerValue2\"}]"));
	}

	@Test
	void testDecode_JsonWithSpecialCharacters() {
		// Arrange
		String json = "{\"key\":\"value\",\"special\":\"!@#$%^&*()_+\"}";

		// Act
		Map<String, Object> result = JsonUtils.decode(json, Map.class);

		// Assert
		assertNotNull(result);
		assertEquals("value", result.get("key"));
		assertEquals("!@#$%^&*()_+", result.get("special"));
	}

	@Test
	void testGetList_EmptyList() {
		// Arrange
		List<Object> input = List.of();

		// Act
		List<String> result = JsonUtils.getList(input);

		// Assert
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void testDecode_EmptyJsonString() {
		// Arrange
		String emptyJson = "";

		// Act & Assert
		Exception exception = assertThrows(Exception.class, () -> JsonUtils.decode(emptyJson, Map.class));
		assertFalse(exception.getMessage().contains("Unexpected end-of-input"));
	}

	@Test
	void testGetList_MixedNestedStructures() {
		// Arrange
		List<Object> input = List.of(
			"value1",
			List.of("nested1", Map.of("key", "value")),
			123
		);

		// Act
		List<String> result = JsonUtils.getList(input);

		// Assert
		assertEquals(3, result.size());
		assertEquals("value1", result.get(0));
		assertEquals("[nested1, {key=value}]", result.get(1)); // Nested structure converted to string
		assertEquals("123", result.get(2));
	}

	@Test
	void testConvertValue_DeeplyNestedObject() {
		// Arrange
		Map<String, Object> data = Map.of(
			"key", "value",
			"nested", Map.of(
				"innerKey", "innerValue",
				"deepNested", Map.of("deepKey", "deepValue")
			)
		);

		// Act
		DeepNestedObject result = JsonUtils.convertValue(data, DeepNestedObject.class);

		// Assert
		assertNotNull(result);
		assertEquals("value", result.getKey());
		assertNotNull(result.getNested());
		assertEquals("innerValue", result.getNested().getInnerKey());
		assertNotNull(result.getNested().getDeepNested());
		assertEquals("deepValue", result.getNested().getDeepNested().getDeepKey());
	}

	// Helper class for deeply nested object conversion
	static class DeepNestedObject {
		private String key;
		private NestedObject nested;

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public NestedObject getNested() {
			return nested;
		}

		public void setNested(NestedObject nested) {
			this.nested = nested;
		}

		static class NestedObject {
			private String innerKey;
			private DeepNested deepNested;

			public String getInnerKey() {
				return innerKey;
			}

			public void setInnerKey(String innerKey) {
				this.innerKey = innerKey;
			}

			public DeepNested getDeepNested() {
				return deepNested;
			}

			public void setDeepNested(DeepNested deepNested) {
				this.deepNested = deepNested;
			}

			static class DeepNested {
				private String deepKey;

				public String getDeepKey() {
					return deepKey;
				}

				public void setDeepKey(String deepKey) {
					this.deepKey = deepKey;
				}
			}
		}
	}

	@Test
	void testEncode_MapWithBooleanValues() {
		// Arrange
		Map<String, Object> data = Map.of(
			"key", "value",
			"isActive", true,
			"isDeleted", false
		);

		// Act
		String json = JsonUtils.encode(data);

		// Assert
		assertNotNull(json);
		assertTrue(json.contains("\"key\":\"value\""));
		assertTrue(json.contains("\"isActive\":true"));
		assertTrue(json.contains("\"isDeleted\":false"));
	}

	@Test
	void testDecode_JsonArrayOfObjects() {
		// Arrange
		String jsonArray = "[{\"id\":1,\"name\":\"Item1\"},{\"id\":2,\"name\":\"Item2\"}]";

		// Act
		List<Map<String, Object>> result = JsonUtils.decode(jsonArray, List.class);

		// Assert
		assertNotNull(result);
		assertEquals(2, result.size());
		assertEquals(1, result.get(0).get("id"));
		assertEquals("Item1", result.get(0).get("name"));
		assertEquals(2, result.get(1).get("id"));
		assertEquals("Item2", result.get(1).get("name"));
	}

	@Test
	void testGetList_WithSpecialCharacters() {
		// Arrange
		List<Object> input = List.of("value1", "!@#$%^&*()", "value2");

		// Act
		List<String> result = JsonUtils.getList(input);

		// Assert
		assertEquals(3, result.size());
		assertEquals("value1", result.get(0));
		assertEquals("!@#$%^&*()", result.get(1));
		assertEquals("value2", result.get(2));
	}
	@Test
	void testEncode_MapWithEmptyStrings() {
		// Arrange
		Map<String, Object> data = Map.of(
			"key", "value",
			"emptyKey", ""
		);

		// Act
		String json = JsonUtils.encode(data);

		// Assert
		assertNotNull(json);
		assertTrue(json.contains("\"key\":\"value\""));
		assertTrue(json.contains("\"emptyKey\":\"\""));
	}

	@Test
	void testDecode_JsonWithBooleanValues() {
		// Arrange
		String json = "{\"isActive\":true,\"isDeleted\":false}";

		// Act
		Map<String, Object> result = JsonUtils.decode(json, Map.class);

		// Assert
		assertNotNull(result);
		assertEquals(true, result.get("isActive"));
		assertEquals(false, result.get("isDeleted"));
	}

	@Test
	void testGetList_WithDuplicateValues() {
		// Arrange
		List<Object> input = List.of("value1", "value2", "value1");

		// Act
		List<String> result = JsonUtils.getList(input);

		// Assert
		assertEquals(3, result.size());
		assertEquals("value1", result.get(0));
		assertEquals("value2", result.get(1));
		assertEquals("value1", result.get(2));
	}

	@Test
	void testEncode_MapWithUnicodeCharacters() {
		// Arrange
		Map<String, Object> data = Map.of(
			"key", "value",
			"unicode", "你好, 世界"
		);

		// Act
		String json = JsonUtils.encode(data);

		// Assert
		assertNotNull(json);
		assertTrue(json.contains("\"key\":\"value\""));
		assertTrue(json.contains("\"unicode\":\"你好, 世界\""));
	}

	@Test
	void testDecode_JsonWithNestedArrays() {
		// Arrange
		String json = "{\"key\":\"value\",\"nestedArray\":[[1,2],[3,4]]}";

		// Act
		Map<String, Object> result = JsonUtils.decode(json, Map.class);

		// Assert
		assertNotNull(result);
		assertEquals("value", result.get("key"));
		List<List<Integer>> nestedArray = (List<List<Integer>>) result.get("nestedArray");
		assertNotNull(nestedArray);
		assertEquals(2, nestedArray.size());
		assertEquals(List.of(1, 2), nestedArray.get(0));
		assertEquals(List.of(3, 4), nestedArray.get(1));
	}

	@Test
	void testGetList_WithEmptyStrings() {
		// Arrange
		List<Object> input = List.of("value1", "", "value2");

		// Act
		List<String> result = JsonUtils.getList(input);

		// Assert
		assertEquals(3, result.size());
		assertEquals("value1", result.get(0));
		assertEquals("", result.get(1));
		assertEquals("value2", result.get(2));
	}

	@Test
	void testDecode_JsonWithNullValues() {
		// Arrange
		String json = "{\"key\":\"value\",\"nullKey\":null}";

		// Act
		Map<String, Object> result = JsonUtils.decode(json, Map.class);

		// Assert
		assertNotNull(result);
		assertEquals("value", result.get("key"));
		assertNull(result.get("nullKey"));
	}

	@Test
	void testGetList_DeeplyNestedStructure() {
		// Arrange
		List<Object> input = List.of(
			"value1",
			List.of("nested1", List.of("deepNested1", "deepNested2")),
			"value2"
		);

		// Act
		List<String> result = JsonUtils.getList(input);

		// Assert
		assertEquals(3, result.size());
		assertEquals("value1", result.get(0));
		assertEquals("[nested1, [deepNested1, deepNested2]]", result.get(1)); // Deeply nested structure converted to string
		assertEquals("value2", result.get(2));
	}
	@Test
	void testEncode_MapWithSpecialJsonCharacters() {
		// Arrange
		Map<String, Object> data = Map.of(
			"key", "value",
			"specialChars", "\"quotes\" and \\backslashes\\"
		);

		// Act
		String json = JsonUtils.encode(data);

		// Assert
		assertNotNull(json);
		assertTrue(json.contains("\"key\":\"value\""));
		assertTrue(json.contains("\"specialChars\":\"\\\"quotes\\\" and \\\\backslashes\\\\\""));
	}

	@Test
	void testDecode_JsonWithEscapedCharacters() {
		// Arrange
		String json = "{\"key\":\"value\",\"escaped\":\"\\\"quotes\\\" and \\\\backslashes\\\\\"}";

		// Act
		Map<String, Object> result = JsonUtils.decode(json, Map.class);

		// Assert
		assertNotNull(result);
		assertEquals("value", result.get("key"));
		assertEquals("\"quotes\" and \\backslashes\\", result.get("escaped"));
	}


}