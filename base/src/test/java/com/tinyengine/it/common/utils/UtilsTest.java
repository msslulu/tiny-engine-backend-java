/**
 * Copyright (c) 2023 - present TinyEngine Authors.
 * Copyright (c) 2023 - present Huawei Cloud Computing Technologies Co., Ltd.
 *
 * Use of this source code is governed by an MIT-style license.
 *
 * THE OPEN SOURCE SOFTWARE IN THIS PRODUCT IS DISTRIBUTED IN THE HOPE THAT IT WILL BE USEFUL,
 * BUT WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR PURPOSE. SEE THE APPLICABLE LICENSES FOR MORE DETAILS.
 *
 */

package com.tinyengine.it.common.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.tinyengine.it.common.base.Result;
import com.tinyengine.it.common.enums.Enums;
import cn.hutool.core.io.FileUtil;
import com.tinyengine.it.common.exception.ServiceException;
import com.tinyengine.it.model.dto.FileInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinyengine.it.model.dto.JsonFile;
import com.tinyengine.it.model.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * test case
 *
 * @since 2024-10-29
 */
class UtilsTest {
    @Mock
    private  Utils utils;
    // 模拟静态依赖
    private MockedStatic<FileUtil> fileUtilMock;
    private MockedStatic<Files> filesMock;
    private MockedStatic<JsonUtils> jsonUtilsMock;
    private MockedStatic<Enums.MimeType> mimeTypeMock;
    // 假设 validateFileStream 是某个验证类的静态方法，这里用模拟
    private MockedStatic<Utils> validationUtilMock;
    @BeforeEach
    void setUp() {
        fileUtilMock = mockStatic(FileUtil.class);
        filesMock = mockStatic(Files.class);
        jsonUtilsMock = mockStatic(JsonUtils.class);
        mimeTypeMock = mockStatic(Enums.MimeType.class);
        validationUtilMock = mockStatic(Utils.class);
    }

    @AfterEach
    void tearDown() {
        fileUtilMock.close();
        filesMock.close();
        jsonUtilsMock.close();
        mimeTypeMock.close();
        validationUtilMock.close();
    }
    @Test
    void removeDuplicates() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("a");
        list.add("c");
        assertThat(list.size()).isEqualTo(4);
        List<String> result = Utils.removeDuplicates(list);
        assertThat(result.size()).isEqualTo(3);
    }

    @Test
    void testFindMaxVersion() {
        String result = Utils.findMaxVersion(Arrays.<String>asList("versions"));
        assertEquals("versions", result);
    }

    @Test
    void testToHump() {
        String result = Utils.toHump("name");
        assertEquals("name", result);
    }

    @Test
    void testToLine() {
        String result = Utils.toLine("name");
        assertEquals("name", result);
    }

    @Test
    void testFlat() {
        Map<String, Object> mapData = new HashMap();
        mapData.put("key", "value");
        Map<String, Object> flat = Utils.flat(mapData);
        assertTrue(flat.keySet().contains("key"));
    }

    @Test
    void testReadFileContent() {
        URL resource = UtilsTest.class.getClassLoader().getResource("testFile.txt");
        if (resource != null) {
            File file = new File(resource.getFile());
            String fileContent = Utils.readFileContent(file);
            assertEquals("abc" + System.lineSeparator(), fileContent);
        }
    }

    @Test
    void testRemoveDuplicatesWithNull() {
        List<String> list = null;
        List<String> result = Utils.removeDuplicates(list);
        assertThat(result).isEmpty(); // Expect an empty list instead of null
    }

    @Test
    void testRemoveDuplicatesWithEmptyList() {
        List<String> list = new ArrayList<>();
        List<String> result = Utils.removeDuplicates(list);
        assertThat(result).isEmpty();
    }

    @Test
    void testFlatWithNestedMap() {
        Map<String, Object> nestedMap = new HashMap<>();
        Map<String, Object> innerMap = new HashMap<>();
        innerMap.put("innerKey", "innerValue");
        nestedMap.put("outerKey", innerMap);

        Map<String, Object> flatMap = Utils.flat(nestedMap);
        assertTrue(flatMap.containsKey("outerKey.innerKey"));
        assertEquals("innerValue", flatMap.get("outerKey.innerKey"));
    }

    @Test
    void testToHumpWithEdgeCases() {
        assertEquals("", Utils.toHump(""));
        assertEquals("a", Utils.toHump("a"));
        assertEquals("camelCase", Utils.toHump("camel_case"));
    }

    @Test
    void testToLineWithEdgeCases() {
        assertEquals("", Utils.toLine(""));
        assertEquals("a", Utils.toLine("a"));
        assertEquals("snake_Case", Utils.toLine("snakeCase"));
    }

    @Test
    void findMaxVersion() {
        // Test with an empty list
        List<String> emptyList = new ArrayList<>();
        String resultEmpty = Utils.findMaxVersion(emptyList);
        assertThat(resultEmpty).isNull();

        // Test with a single version
        List<String> singleVersion = List.of("1.0.0");
        String resultSingle = Utils.findMaxVersion(singleVersion);
        assertThat(resultSingle).isEqualTo("1.0.0");

        // Test with multiple versions
        List<String> multipleVersions = List.of("1.0.0", "2.0.0", "1.2.3");
        String resultMultiple = Utils.findMaxVersion(multipleVersions);
        assertThat(resultMultiple).isEqualTo("2.0.0");

        // Test with versions having different lengths
        List<String> differentLengths = List.of("1.0", "1.0.0", "1.0.1");
        String resultDifferentLengths = Utils.findMaxVersion(differentLengths);
        assertThat(resultDifferentLengths).isEqualTo("1.0.1");

        // Test with invalid version strings
        List<String> invalidVersions = List.of("1.a.0", "2.0.0", "1.2.3");
        assertThrows(NumberFormatException.class, () -> Utils.findMaxVersion(invalidVersions));
    }
    @Test
    void toHump() {
        // Test with an empty string
        assertEquals("", Utils.toHump(""));

        // Test with a string without underscores
        assertEquals("test", Utils.toHump("test"));

        // Test with a string with one underscore
        assertEquals("testString", Utils.toHump("test_string"));

        // Test with a string with multiple underscores
        assertEquals("testStringExample", Utils.toHump("test_string_example"));

        // Test with a string starting with an underscore
        assertEquals("test", Utils.toHump("_test"));

        // Test with a string ending with an underscore
        assertEquals("testString", Utils.toHump("test_string_"));

        // Test with consecutive underscores
        assertEquals("testStringExample", Utils.toHump("test__string__example"));
    }

    @Test
    void unzip() throws Exception {
        // Create a mock ZIP file
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            // Add a file entry to the ZIP
            ZipEntry entry = new ZipEntry("testFile.txt");
            zipOutputStream.putNextEntry(entry);
            zipOutputStream.write("test content".getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
        }

        // Convert the ZIP to a MockMultipartFile
        MultipartFile mockZipFile = new MockMultipartFile(
            "file",
            "test.zip",
            "application/zip",
            new ByteArrayInputStream(byteArrayOutputStream.toByteArray())
        );

        // Call the unzip method
        List<FileInfo> fileInfoList = Utils.unzip(mockZipFile);

        // Assert the results
        assertThat(fileInfoList).hasSize(1);
        FileInfo fileInfo = fileInfoList.get(0);
        assertThat(fileInfo.getName()).isEqualTo("testFile.txt");
        assertThat(fileInfo.getContent()).isEqualTo("test content");
        assertThat(fileInfo.getIsDirectory()).isFalse();
    }
    @Test
    public void test_unzip_Success() throws IOException {
        // 模拟 MultipartFile 为一个 ZIP 文件
        MockMultipartFile zipFile = new MockMultipartFile("file", "test.zip", "application/zip",
            "test content for zip file".getBytes());
        // 模拟临时文件和目录创建及清理
        when(Utils.createTempDirectory()).thenReturn(new File("tempDir"));
        // 用临时文件模拟 MultipartFile 转换成 File 的过程
        File tempZipFile = new File("tempZipFile");
        when(Utils.convertMultipartFileToFile(zipFile)).thenReturn(tempZipFile);
        // 假设 processZipEntries 返回一个包含两个 FileInfo 的列表
        List<FileInfo> expectedList = Arrays.asList(
            new FileInfo("a.txt", "tempDir/a.txt",true),
            new FileInfo("b.txt", "tempDir/b.txt",true)
        );

        when(Utils.processZipEntries(any(ZipInputStream.class), any(File.class))).thenReturn(expectedList);
        // 调用方法
        List<FileInfo> result = Utils.unzip(zipFile);
        // 验证结果与预期一致
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("a.txt", result.get(0).getName());
        assertEquals("tempDir/a.txt", result.get(0).getContent());
        assertEquals("b.txt", result.get(1).getName());
        assertEquals("tempDir/b.txt", result.get(1).getContent());
    }
    @Test
    public void test_unzip_WithInvalidZip() throws IOException {
        // 模拟一个无效的 ZIP 文件
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "invalid content".getBytes());
        // 模拟 tempDir 创建
        when(Utils.createTempDirectory()).thenReturn(new File("tempDir"));
        // 模拟文件转换为 File
        File tempFile = new File("tempFile");
        when(Utils.convertMultipartFileToFile(file)).thenReturn(tempFile);
        // 调用方法
        assertThrows(IOException.class, () -> Utils.unzip(file));
    }
    @Test
    public void test_unzip_NullInput() {
        // 测试传入 null 时抛出异常
        assertThrows(NullPointerException.class, () -> Utils.unzip(null));
    }
    @Test
    public void test_unzip_WithMultipleFiles() throws IOException {
        MockMultipartFile zipFile = new MockMultipartFile("file", "test.zip", "application/zip",
            "mock zip content".getBytes());
        when(Utils.createTempDirectory()).thenReturn(new File("tempDir"));
        when(Utils.convertMultipartFileToFile(zipFile)).thenReturn(new File("tempZipFile"));
        // 用 mock 解压结果
        List<FileInfo> expected = Arrays.asList(
            new FileInfo("file1.txt", "tempDir/file1.txt",true),
            new FileInfo("file2.txt", "tempDir/file2.txt",true)
        );
        when(Utils.processZipEntries(any(ZipInputStream.class), any(File.class))).thenReturn(expected);
        List<FileInfo> result = Utils.unzip(zipFile);
        assertNotNull(result);
        assertEquals(2, result.size());
    }
    @Test
    public void test_unzip_FilesAlreadyExist() throws IOException {
        // 模拟 MultipartFile 解压后生成的文件已经存在
        MockMultipartFile zipFile = new MockMultipartFile("file", "test.zip", "application/zip",
            "mock zip content".getBytes());
        when(Utils.createTempDirectory()).thenReturn(new File("tempDir"));
        when(Utils.convertMultipartFileToFile(zipFile)).thenReturn(new File("tempZipFile"));
        // 模拟 processZipEntries 返回两个文件，并表示文件已经存在
        List<FileInfo> expected = Arrays.asList(
            new FileInfo("file1.txt", "tempDir/file1.txt",true),
            new FileInfo("file2.txt", "tempDir/file2.txt",true)
        );
        when(Utils.processZipEntries(any(ZipInputStream.class), any(File.class))).thenReturn(expected);
        // 假设在解压前文件已经存在（模拟异常）
        IOException mockException = new IOException("File already exists");
        // 强制触发异常（模拟文件已存在）
        doThrow(mockException).when(Utils.processZipEntries(any(ZipInputStream.class), any(File.class)));
        assertThrows(IOException.class, () -> Utils.unzip(zipFile));
    }
    @Test
    public void test_unzip_CleanUpOnFailure() throws IOException {
        MockMultipartFile zipFile = new MockMultipartFile("file", "test.zip", "application/zip",
            "invalid zip content".getBytes());
        when(Utils.createTempDirectory()).thenReturn(new File("tempDir"));
        when(Utils.convertMultipartFileToFile(zipFile)).thenReturn(new File("tempZipFile"));
        // 模拟解压失败
        IOException mockException = new IOException("Invalid ZIP file");
        doThrow(mockException).when(Utils.processZipEntries(any(ZipInputStream.class), any(File.class)));
        // 调用 unzip 并捕获异常
        assertThrows(IOException.class, () -> Utils.unzip(zipFile));
    }
    @Test
    void testReadFileContent_Success() {
        File file = mock(File.class);
        List<String> mockLines = Arrays.asList("line1", "line2", "line3");
        fileUtilMock.when(() -> FileUtil.readLines(file, Charset.defaultCharset()))
            .thenReturn(mockLines);

        String result = Utils.readFileContent(file);

        String expected = "line1" + System.lineSeparator() +
            "line2" + System.lineSeparator() +
            "line3" + System.lineSeparator();
        assertEquals(expected, result);
        fileUtilMock.verify(() -> FileUtil.readLines(file, Charset.defaultCharset()), times(1));
    }

    @Test
    void testReadFileContent_EmptyFile() {
        File file = mock(File.class);
        fileUtilMock.when(() -> FileUtil.readLines(file, Charset.defaultCharset()))
            .thenReturn(Collections.emptyList());

        String result = Utils.readFileContent(file);
        assertEquals("", result);
    }
    // -------------------- flat 测试（包含私有 flatten 方法）--------------------
    @Test
    void testFlat_SimpleMap() {
        Map<String, Object> input = new HashMap<>();
        input.put("a", 1);
        input.put("b", "text");

        Map<String, Object> result = Utils.flat(input);

        assertEquals(2, result.size());
        assertEquals(1, result.get("a"));
        assertEquals("text", result.get("b"));
    }

    @Test
    void testFlat_NestedMap() {
        Map<String, Object> inner = new HashMap<>();
        inner.put("x", 100);
        inner.put("y", 200);

        Map<String, Object> input = new HashMap<>();
        input.put("outer", inner);
        input.put("z", 300);

        Map<String, Object> result = Utils.flat(input);

        assertEquals(3, result.size());
        assertEquals(100, result.get("outer.x"));
        assertEquals(200, result.get("outer.y"));
        assertEquals(300, result.get("z"));
    }

    @Test
    void testFlat_DeepNesting() {
        Map<String, Object> level3 = new HashMap<>();
        level3.put("deep", "value");
        Map<String, Object> level2 = new HashMap<>();
        level2.put("level3", level3);
        Map<String, Object> level1 = new HashMap<>();
        level1.put("level2", level2);

        Map<String, Object> result = Utils.flat(level1);

        assertEquals(1, result.size());
        assertEquals("value", result.get("level2.level3.deep"));
    }

    @Test
    void testFlat_EmptyMap() {
        Map<String, Object> result = Utils.flat(Collections.emptyMap());
        assertTrue(result.isEmpty());
    }
    @Test
    public void test_flat_withSimpleMap() {
        Map<String, Object> input = new HashMap<>();
        input.put("name", "Alice");
        input.put("age", 20);
        Map<String, Object> output = Utils.flat(input);
        assertEquals(2, output.size());
        assertEquals("Alice", output.get("name"));
        assertEquals(20, output.get("age"));
    }
    @Test
    public void test_flat_withNestedMap() {
        Map<String, Object> input = new HashMap<>();
        Map<String, Object> address = new HashMap<>();
        address.put("city", "Beijing");
        address.put("zip", "100000");
        input.put("name", "Alice");
        input.put("address", address);
        Map<String, Object> output = Utils.flat(input);
        assertEquals(3, output.size());
        assertEquals("Alice", output.get("name"));
        assertEquals("Beijing", output.get("address.city"));
        assertEquals("100000", output.get("address.zip"));
    }
    @Test
    public void test_flat_withNestedAndSimpleObjects() {
        Map<String, Object> input = new HashMap<>();
        Map<String, Object> address = new HashMap<>();
        address.put("city", "Beijing");
        address.put("zip", "100000");
        input.put("name", "Alice");
        input.put("details", new HashMap<>() {{
            put("age", 20);
            put("address", address);
        }});
        input.put("id", 123);
        Map<String, Object> output = Utils.flat(input);
        assertNotNull(output);
        assertEquals(5, output.size());
        assertEquals("Alice", output.get("name"));
        assertEquals("123", output.get("id"));
        assertEquals("Beijing", output.get("details.address.city"));
        assertEquals("100000", output.get("details.address.zip"));
        assertEquals("20", output.get("details.age"));
    }
    @Test
    public void test_flat_withEmptyMap() {
        Map<String, Object> input = new HashMap<>();
        Map<String, Object> output = Utils.flat(input);
        assertTrue(output.isEmpty());
    }
    @Test
    public void test_flat_withNullInput() {
        assertThrows(NullPointerException.class, () -> Utils.flat(null));
    }
    @Test
    public void test_flat_withNullValues() {
        Map<String, Object> input = new HashMap<>();
        input.put("a", null);
        input.put("b", new HashMap<>());
        input.put("b", new HashMap<>() {{
            put("c", null);
            put("d", "test");
        }});
        Map<String, Object> output = Utils.flat(input);
        assertNotNull(output);
        assertEquals(3, output.size());
        assertNull(output.get("a"));
        assertNull(output.get("b.c"));
        assertEquals("test", output.get("b.d"));
    }
    @Test
    public void test_flat_withMultipleLevels() {
        Map<String, Object> input = new HashMap<>();
        Map<String, Object> a = new HashMap<>();
        a.put("x", "value");
        Map<String, Object> b = new HashMap<>();
        b.put("y", 100);
        b.put("z", a);
        input.put("a", b);
        Map<String, Object> output = Utils.flat(input);
        assertNotNull(output);
        assertEquals(3, output.size());
        assertEquals("value", output.get("a.z.x"));
        assertEquals(100, output.get("a.z.y"));
    }
    @Test
    public void test_flat_withMapContainingOtherMaps() {
        Map<String, Object> input = new HashMap<>();
        input.put("a", new HashMap<>() {{
            put("b", new HashMap<>() {{
                put("c", "test");
            }});
        }});
        input.put("d", "test2");
        Map<String, Object> output = Utils.flat(input);
        assertNotNull(output);
        assertEquals(3, output.size());
        assertEquals("test", output.get("a.b.c"));
        assertEquals("test2", output.get("d"));
    }
    @Test
    public void test_flat_doesNotAddEmptyEntries() {
        Map<String, Object> input = new HashMap<>();
        input.put("empty", new HashMap<>());
        Map<String, Object> output = Utils.flat(input);
        assertTrue(output.isEmpty());
    }
    @Test
    public void test_flat_doesNotFlattenListsOrArrays() {
        Map<String, Object> input = new HashMap<>();
        input.put("list", Arrays.asList("one", "two"));
        Map<String, Object> output = Utils.flat(input);
        assertEquals(2, output.size());
        assertEquals("one", output.get("list[0]"));
        assertEquals("two", output.get("list[1]"));
    }
    @Test
    public void test_flat_withMixedDataTypes() {
        Map<String, Object> input = new HashMap<>();
        input.put("name", "Alice");
        input.put("status", true); // boolean类型不需要展平
        input.put("tags", Arrays.asList("java", "test"));
        Map<String, Object> output = Utils.flat(input);
        assertEquals(3, output.size());
        assertEquals("Alice", output.get("name"));
        assertEquals(Boolean.TRUE, output.get("status"));
        assertTrue(output.get("tags[0]").equals("java") || output.get("tags[0]").equals("java"));
    }
    // 模拟一个 Map 以测试其功能
    @Test
    public void test_flat_withString() {
        Map<String, Object> input = new HashMap<>();
        input.put("key", "value");
        Map<String, Object> output = Utils.flat(input);
        assertEquals(1, output.size());
        assertEquals("value", output.get("key"));
    }


    // -------------------- parseJsonFileStream 测试 --------------------
    @Test
    void testParseJsonFileStream_Success() throws Exception {
        MultipartFile mockFile = mock(MultipartFile.class);
        String fileName = "test.json";
        when(mockFile.getOriginalFilename()).thenReturn(fileName);
        String jsonContent = "{\"key\":\"value\", \"nested\":{\"inner\":123}}";
        InputStream mockStream = new ByteArrayInputStream(jsonContent.getBytes(StandardCharsets.UTF_8));
        when(mockFile.getInputStream()).thenReturn(mockStream);

        // Mock 验证方法
        validationUtilMock.when(() -> Utils.validateFileStream(
                eq(mockFile), anyString(), anyList()))
            .thenAnswer(inv -> null); // 假设无返回值，直接通过

        // Mock MimeType 枚举
        when(Enums.MimeType.JSON.getValue()).thenReturn("application/json");
        mimeTypeMock.when(Enums.MimeType.JSON::getValue).thenReturn("application/json");

        // Mock ObjectMapper 解析
        ObjectMapper mapper = mock(ObjectMapper.class);
        when(JsonUtils.MAPPER).thenReturn(mapper);
        Map<String, Object> parsedMap = new HashMap<>();
        parsedMap.put("key", "value");
        Map<String, Object> nested = new HashMap<>();
        nested.put("inner", 123);
        parsedMap.put("nested", nested);
        when(mapper.readValue(anyString(), any(TypeReference.class))).thenReturn(parsedMap);

        Result<JsonFile> result = Utils.parseJsonFileStream(mockFile);

        assertTrue(result.isSuccess());
        JsonFile jsonFile = result.getData();
        assertEquals(fileName, jsonFile.getFileName());
        assertEquals(parsedMap, jsonFile.getFileContent());

        validationUtilMock.verify(() -> Utils.validateFileStream(
            eq(mockFile), anyString(), anyList()));
        verify(mapper).readValue(anyString(), any(TypeReference.class));
    }

    @Test
    void testParseJsonFileStream_WithBOM() throws Exception {
        MultipartFile mockFile = mock(MultipartFile.class);
        String fileName = "bom.json";
        when(mockFile.getOriginalFilename()).thenReturn(fileName);
        // 包含 BOM 的 JSON 字符串
        String jsonWithBOM = "\uFEFF{\"key\":\"value\"}";
        InputStream mockStream = new ByteArrayInputStream(jsonWithBOM.getBytes(StandardCharsets.UTF_8));
        when(mockFile.getInputStream()).thenReturn(mockStream);

        validationUtilMock.when(() -> Utils.validateFileStream(any(), anyString(), anyList()))
            .thenAnswer(inv -> null);
        when(Enums.MimeType.JSON.getValue()).thenReturn("application/json");
        mimeTypeMock.when(Enums.MimeType.JSON::getValue).thenReturn("application/json");

        ObjectMapper mapper = mock(ObjectMapper.class);
        when(JsonUtils.MAPPER).thenReturn(mapper);
        Map<String, Object> parsedMap = Collections.singletonMap("key", "value");
        when(mapper.readValue(eq("{\"key\":\"value\"}"), any(TypeReference.class))).thenReturn(parsedMap);

        Result<JsonFile> result = Utils.parseJsonFileStream(mockFile);

        assertTrue(result.isSuccess());
        assertEquals(parsedMap, result.getData().getFileContent());
        verify(mapper).readValue(eq("{\"key\":\"value\"}"), any(TypeReference.class));
    }

    @Test
    void testParseJsonFileStream_IOException() throws Exception {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("error.json");
        when(mockFile.getInputStream()).thenThrow(new IOException("Stream error"));

        validationUtilMock.when(() -> Utils.validateFileStream(any(), anyString(), anyList()))
            .thenAnswer(inv -> null);

        Result<JsonFile> result = Utils.parseJsonFileStream(mockFile);

        Assertions.assertFalse(result.isSuccess());
        assertEquals("Error parsing JSON", result.getMessage());
    }

    @Test
    void testParseJsonFileStream_JsonParseError() throws Exception {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("invalid.json");
        String invalidJson = "{invalid";
        InputStream mockStream = new ByteArrayInputStream(invalidJson.getBytes(StandardCharsets.UTF_8));
        when(mockFile.getInputStream()).thenReturn(mockStream);

        validationUtilMock.when(() -> Utils.validateFileStream(any(), anyString(), anyList()))
            .thenAnswer(inv -> null);
        when(Enums.MimeType.JSON.getValue()).thenReturn("application/json");
        mimeTypeMock.when(Enums.MimeType.JSON::getValue).thenReturn("application/json");

        ObjectMapper mapper = mock(ObjectMapper.class);
        when(JsonUtils.MAPPER).thenReturn(mapper);
        when(mapper.readValue(anyString(), any(TypeReference.class)))
            .thenThrow(new com.fasterxml.jackson.core.JsonParseException("Parse error"));

        Result<JsonFile> result = Utils.parseJsonFileStream(mockFile);

        Assertions.assertFalse(result.isSuccess());
        assertEquals("Error parsing JSON", result.getMessage());
    }


    @Test
    public void test_removeBOM_WithBOM() {
        String input = "\uFEFFHello, World!";
        String result = Utils.removeBOM(input);
        assertEquals("Hello, World!", result);
    }
    @Test
    public void test_removeBOM_WithoutBOM() {
        String input = "Hello, World!";
        String result = Utils.removeBOM(input);
        assertEquals("Hello, World!", result);
    }
    @Test
    public void test_removeBOM_NullInput() {
        assertNull(Utils.removeBOM(null));
    }

    @Test
    public void test_validateFileStream_FileIsValid() {
        MultipartFile mockFile = Mockito.mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("testFile.txt");
        when(mockFile.getContentType()).thenReturn("text/plain");
        List<String> mimeTypes = Arrays.asList("text/plain");
        assertDoesNotThrow(() -> Utils.validateFileStream(mockFile, "invalid_file", mimeTypes));
    }
    @Test
    public void test_validateFileStream_FileIsInvalid() {
        MultipartFile mockFile = Mockito.mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("testFile.jpg");
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        List<String> mimeTypes = Arrays.asList("text/plain");
        assertThrows(ServiceException.class, () -> Utils.validateFileStream(mockFile, "invalid_file", mimeTypes));
    }
    @Test
    public void test_validateFileStream_FileNameIsNull() {
        MultipartFile mockFile = Mockito.mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn(null);
        when(mockFile.getContentType()).thenReturn("text/plain");
        List<String> mimeTypes = Arrays.asList("text/plain");
        assertThrows(ServiceException.class, () -> Utils.validateFileStream(mockFile, "invalid_file", mimeTypes));
    }
    @Test
    public void test_validateFileStream_ContentTypeDoesNotMatch() {
        MultipartFile mockFile = Mockito.mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("testFile.txt");
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        List<String> mimeTypes = Arrays.asList("text/plain");
        assertThrows(ServiceException.class, () -> Utils.validateFileStream(mockFile, "invalid_file", mimeTypes));
    }

    @Test
    public void test_encodeObjectToBase64_WithMap() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Alice");
        map.put("age", 20);
        String base64 = Utils.encodeObjectToBase64(map);
        assertNotNull(base64);
        assertTrue(base64.contains("Alice"));
        assertTrue(base64.contains("20"));
        assertTrue(base64.contains("image/"));
    }
    @Test
    public void test_encodeObjectToBase64_WithJavaBean() throws Exception {
        User user = new User();
        user.setUsername("msslulu");
        user.setEmail("1484036491@qq.com");
        String base64 = Utils.encodeObjectToBase64(user);
        assertNotNull(base64);
        assertTrue(base64.contains("lulu"));
        assertTrue(base64.contains("22"));
        assertTrue(base64.contains("image/"));
    }
    @Test
    public void test_encodeObjectToBase64_ExceptionHandling() {
        // 测试异常情况，比如传入 null 或非 Map/JavaBean
        assertThrows(Exception.class, () -> Utils.encodeObjectToBase64(null));
    }

    @Test
    public void test_decodeBase64ToObject_ValidBase64() {
        User user = new User();
        user.setUsername("msslulu");
        user.setEmail("1484036491@qq.com");
        String base64String = "base64encodedstringhere";
        // 假设 decodeBase64ToObject 能正确转换
        User decodedUser = Utils.decodeBase64ToObject(base64String, User.class);
        assertNotNull(decodedUser);
        assertEquals("msslulu", decodedUser.getUsername());
        assertEquals("1484036491@qq.com", decodedUser.getEmail());
    }
    @Test
    public void test_decodeBase64ToObject_InvalidBase64() {
        assertThrows(IllegalArgumentException.class, () -> Utils.decodeBase64ToObject("invalid", User.class));
    }
    @Test
    public void test_isResource_WithResourceName() {
        assertTrue(Utils.isResource("resource.png"));
        assertTrue(Utils.isResource("resource_2025.png"));
        Assertions.assertFalse(Utils.isResource("thumbnail_resource.png"));
    }
    @Test
    public void test_isResource_WithShortName() {
        assertTrue(Utils.isResource("resource"));
        Assertions.assertFalse(Utils.isResource("thumbnail"));
    }
    @Test
    public void test_isResource_NullName() {
        Assertions.assertFalse(Utils.isResource(null));
    }
    @Test
    public void test_isDownload_WithImageName() {
        assertTrue(Utils.isDownload("image_1.png"));
        assertTrue(Utils.isDownload("image_2025.jpg"));
        assertTrue(Utils.isDownload("image_abc.png"));
    }
    @Test
    public void test_isDownload_WithNonImageName() {
        Assertions.assertFalse(Utils.isDownload("resource.txt"));
        Assertions.assertFalse(Utils.isDownload("thumbnail.png"));
        Assertions.assertFalse(Utils.isDownload("other_name.docx"));
    }
    @Test
    public void test_isDownload_NullName() {
        Assertions.assertFalse(Utils.isDownload(null));
    }

    // -------------------- cleanUp 测试 --------------------
    @Test
    void testCleanUp_SuccessfulDeletion() throws IOException {
        File zipFile = mock(File.class);
        when(zipFile.exists()).thenReturn(true);
        when(zipFile.delete()).thenReturn(true);

        File tempDir = mock(File.class);
        Path tempPath = mock(Path.class);
        when(tempDir.toPath()).thenReturn(tempPath);

        // 模拟 Files.walk 返回的路径流
        Path subPath1 = mock(Path.class);
        Path subPath2 = mock(Path.class);
        File subFile1 = mock(File.class);
        File subFile2 = mock(File.class);
        when(subPath1.toFile()).thenReturn(subFile1);
        when(subPath2.toFile()).thenReturn(subFile2);
        when(subFile1.delete()).thenReturn(true);
        when(subFile2.delete()).thenReturn(true);

        Stream<Path> pathStream = Stream.of(subPath1, subPath2);
        filesMock.when(() -> Files.walk(tempPath)).thenReturn(pathStream);

        // 执行清理
        Utils.cleanUp(zipFile, tempDir);

        // 验证 zipFile 被删除
        verify(zipFile).delete();
        // 验证子文件和目录被删除（反向顺序）
        verify(subFile2).delete();
        verify(subFile1).delete();
        // 验证流被关闭（try-with-resources 自动处理，无需额外验证）
    }

    @Test
    void testCleanUp_ZipFileDeletionFails() throws IOException {
        File zipFile = mock(File.class);
        when(zipFile.exists()).thenReturn(true);
        when(zipFile.delete()).thenReturn(false);

        File tempDir = mock(File.class);
        Path tempPath = mock(Path.class);
        when(tempDir.toPath()).thenReturn(tempPath);

        // 模拟空目录，Files.walk 返回只包含根路径的流
        Stream<Path> pathStream = Stream.of(tempPath);
        filesMock.when(() -> Files.walk(tempPath)).thenReturn(pathStream);
        when(tempPath.toFile()).thenReturn(tempDir);
        when(tempDir.delete()).thenReturn(true);

        Utils.cleanUp(zipFile, tempDir);

        verify(zipFile).delete(); // 失败但方法仍继续
        verify(tempDir).delete();
    }

    @Test
    void testCleanUp_FilesWalkThrowsIOException() throws IOException {
        File zipFile = mock(File.class);
        when(zipFile.exists()).thenReturn(true);
        when(zipFile.delete()).thenReturn(true);

        File tempDir = mock(File.class);
        Path tempPath = mock(Path.class);
        when(tempDir.toPath()).thenReturn(tempPath);
        filesMock.when(() -> Files.walk(tempPath)).thenThrow(new IOException("Walk error"));

        // 不应抛出异常，只记录日志
        assertDoesNotThrow(() -> Utils.cleanUp(zipFile, tempDir));
        verify(zipFile).delete();
    }

    // -------------------- readAllBytes 测试 --------------------
    @Test
    void testReadAllBytes_Success() throws IOException {
        byte[] inputData = "Hello, World!".getBytes(StandardCharsets.UTF_8);
        InputStream mockStream = mock(InputStream.class);
        when(mockStream.read(any(byte[].class)))
            .thenAnswer(invocation -> {
                byte[] buffer = invocation.getArgument(0);
                int bytesToCopy = Math.min(inputData.length, buffer.length);
                System.arraycopy(inputData, 0, buffer, 0, bytesToCopy);
                return bytesToCopy;
            })
            .thenReturn(-1); // 第二次调用返回 -1 表示结束

        byte[] result = Utils.readAllBytes(mockStream);

        assertArrayEquals(inputData, result);
        verify(mockStream, atLeastOnce()).read(any(byte[].class));
        verify(mockStream).close(); // 验证输入流被关闭
    }

    @Test
    void testReadAllBytes_EmptyStream() throws IOException {
        InputStream mockStream = mock(InputStream.class);
        when(mockStream.read(any(byte[].class))).thenReturn(-1);

        byte[] result = Utils.readAllBytes(mockStream);
        assertEquals(0, result.length);
        verify(mockStream).close();
    }
    @Test
    void testReadAllBytes_ThrowsIOException() throws IOException {
        InputStream mockStream = mock(InputStream.class);
        when(mockStream.read(any(byte[].class))).thenThrow(new IOException("Read error"));

        assertThrows(IOException.class, () -> Utils.readAllBytes(mockStream));
        verify(mockStream).close(); // 即使发生异常，finally 中仍会尝试关闭
    }

    @Test
    void testReadAllBytes() throws Exception {
        InputStream inputStream = new ByteArrayInputStream("test content".getBytes(StandardCharsets.UTF_8));
        byte[] result = Utils.readAllBytes(inputStream);

        assertThat(new String(result, StandardCharsets.UTF_8)).isEqualTo("test content");
    }
    @Test
    public void test_readAllBytes_Normal() throws IOException {
        byte[] expectedBytes = "test data".getBytes();
        InputStream mockInputStream = Mockito.mock(InputStream.class);
        when(mockInputStream.read(any(byte[].class))).thenReturn(5, -1);
        byte[] result = Utils.readAllBytes(mockInputStream);
        assertArrayEquals(expectedBytes, result);
    }
    @Test
    public void test_readAllBytes_EmptyStream() throws IOException {
        InputStream mockInputStream = Mockito.mock(InputStream.class);
        when(mockInputStream.read(any(byte[].class))).thenReturn(-1);
        byte[] result = Utils.readAllBytes(mockInputStream);
        assertTrue(result.length == 0);
    }
    @Test
    public void test_readAllBytes_ThrowsIOException() throws IOException {
        InputStream mockInputStream = Mockito.mock(InputStream.class);
        doThrow(new IOException("Simulated error")).when(mockInputStream).read(any(byte[].class));
        assertThrows(IOException.class, () -> Utils.readAllBytes(mockInputStream));
    }
    @Test
    public void test_readAllBytes_NullInput() {
        assertThrows(NullPointerException.class, () -> Utils.readAllBytes(null));
    }
    @Test
    public void test_readAllBytes_BufferSizeTest() throws IOException {
        byte[] input = "This is a test message".getBytes();
        InputStream mockInputStream = Mockito.mock(InputStream.class);
        when(mockInputStream.read(any(byte[].class))).thenReturn(5, 5, -1);
        byte[] result = Utils.readAllBytes(mockInputStream);
        assertArrayEquals(input, result);
    }
    @Test
    void testRemoveBOM() {
        String input = "\uFEFFtest content";
        String result = Utils.removeBOM(input);

        assertEquals("test content", result);
    }

    @Test
    void testIsResource() {
        assertTrue(Utils.isResource("resource_image.png"));
        Assertions.assertFalse(Utils.isResource("thumbnail_image.png"));
    }

    @Test
    void testIsDownload() {
        assertTrue(Utils.isDownload("image123.png"));
        Assertions.assertFalse(Utils.isDownload("file123.png"));
    }



    @Test
    void findMaxVersionHandlesEmptyList() {
        List<String> versions = new ArrayList<>();
        String result = Utils.findMaxVersion(versions);

        assertThat(result).isNull();
    }

    @Test
    void findMaxVersionHandlesSingleVersion() {
        List<String> versions = List.of("1.0.0");
        String result = Utils.findMaxVersion(versions);

        assertThat(result).isEqualTo("1.0.0");
    }

    @Test
    void findMaxVersionHandlesMultipleVersions() {
        List<String> versions = List.of("1.0.0", "2.0.0", "1.2.3");
        String result = Utils.findMaxVersion(versions);

        assertThat(result).isEqualTo("2.0.0");
    }

    @Test
    void encodeObjectToBase64HandlesEmptyMap() throws Exception {
        Map<String, Object> emptyMap = new HashMap<>();
        String result = Utils.encodeObjectToBase64(emptyMap);

        assertThat(result).isNotEmpty();
    }

    @Test
    void decodeBase64ToObjectHandlesValidBase64() throws Exception {
        String base64 = Utils.encodeObjectToBase64(Map.of("key", "value"));
        Map<String, String> result = Utils.decodeBase64ToObject(base64, Map.class);

        assertThat(result).containsEntry("key", "value");
    }
}



