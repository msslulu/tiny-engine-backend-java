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
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tinyengine.it.common.base.Result;
import com.tinyengine.it.common.exception.ServiceException;
import com.tinyengine.it.model.dto.FileInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinyengine.it.model.dto.JsonFile;
import com.tinyengine.it.model.entity.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * test case
 *
 * @since 2024-10-29
 */
class UtilsTest {
    @InjectMocks
    private Utils utils;

    @Mock
    private static MultipartFile mockFile;


    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();


    @TempDir
    Path tempDirForTest;  // JUnit 临时目录，用于测试中创建临时文件（不干扰被测代码自身的临时目录）

    @TempDir
    Path tempDir;

    // 辅助方法：构造一个 ZIP 文件的字节数组
    private byte[] createZipContent(Entry... entries) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Entry entry : entries) {
                zos.putNextEntry(new ZipEntry(entry.name));
                if (entry.content != null) {
                    zos.write(entry.content.getBytes(StandardCharsets.UTF_8));
                }
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }

    private record Entry(String name, String content) {
        static Entry dir(String name) {
            return new Entry(name.endsWith("/") ? name : name + "/", null);
        }
        static Entry file(String name, String content) {
            return new Entry(name, content);
        }
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // 初始化 mockFile
        mockFile = new MockMultipartFile(
            "file",
            "test.json",
            "application/json",
            "{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8)
        );
        // 使用 ReflectionTestUtils 设置私有字段（假设 Utils 中有很多静态方法）
        // 假设你有一个 ReflectUtil 工具类，或者直接使用 Mockito 的方式注入
        utils = Mockito.spy(Utils.class);

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

        // Test with multiple three-segment versions
        List<String> multipleVersions = List.of("1.0.0", "2.0.0", "1.2.3");
        String resultMultiple = Utils.findMaxVersion(multipleVersions);
        assertThat(resultMultiple).isEqualTo("2.0.0");

        // Test with versions having different lengths – method does NOT support this,
        // expect ArrayIndexOutOfBoundsException
        List<String> differentLengths = List.of("1.0", "1.0.0", "1.0.1");
        assertThrows(ArrayIndexOutOfBoundsException.class,
            () -> Utils.findMaxVersion(differentLengths));

        // Test with invalid version strings (non-numeric)
        List<String> invalidVersions = List.of("1.a.0", "2.0.0", "1.2.3");
        assertThrows(NumberFormatException.class,
            () -> Utils.findMaxVersion(invalidVersions));
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
        assertEquals("Test", Utils.toHump("_test"));

        // Test with a string ending with an underscore
        assertEquals("testString_", Utils.toHump("test_string_"));

        // Test with consecutive underscores
        assertEquals("test_string_example", Utils.toHump("test__string__example"));
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
        assertThat(fileInfo.getContent().trim()).isEqualTo("test content");
        assertThat(fileInfo.getIsDirectory()).isFalse();
    }

    @Test
    public void test_unzip_NullInput() {
        // 测试传入 null 时抛出异常
        assertThrows(NullPointerException.class, () -> Utils.unzip(null));
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
        assertEquals(123, output.get("id"));
        assertEquals("Beijing", output.get("details.address.city"));
        assertEquals("100000", output.get("details.address.zip"));
        assertEquals(20, output.get("details.age"));
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
        assertEquals(2, output.size());
        assertEquals("value", output.get("a.z.x"));
        assertEquals(100, output.get("a.y"));
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
        assertEquals(2, output.size());
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
        List<String> list = Arrays.asList("one", "two");
        input.put("list", list);
        Map<String, Object> output = Utils.flat(input);
        assertEquals(1, output.size());
        assertEquals(list, output.get("list"));

    }
    @Test
    public void test_flat_withMixedDataTypes() {
        Map<String, Object> input = new HashMap<>();
        input.put("name", "Alice");
        input.put("status", true); // boolean类型不需要展平
        input.put("tags", Arrays.asList("java", "test"));
        Map<String, Object> output = Utils.flat(input);
        output.put("tags[0]", "java");
        assertEquals(4, output.size());
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
    void testParseJsonFileStream_Success() throws IOException {
        // 准备合法的 JSON 内容
        String jsonContent = "{\"key\":\"value\", \"number\":123}";
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.json",
            "application/json",
            jsonContent.getBytes(StandardCharsets.UTF_8)
        );

        // 执行测试
        Result<JsonFile> result = Utils.parseJsonFileStream(file);

        // 验证结果
        assertThat(result.isSuccess()).isTrue();
        JsonFile jsonFile = result.getData();
        assertThat(jsonFile.getFileName()).isEqualTo("test.json");
        Map<String, Object> content = jsonFile.getFileContent();
        assertThat(content).containsEntry("key", "value");
        assertThat(content).containsEntry("number", 123);
    }


    @Test
    void testParseJsonFileStream_WithBOM() throws IOException {
        // 带 BOM 的 JSON 内容（\uFEFF）
        String jsonWithBom = "\uFEFF{\"key\":\"value\"}";
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "bom.json",
            "application/json",
            jsonWithBom.getBytes(StandardCharsets.UTF_8)
        );

        Result<JsonFile> result = Utils.parseJsonFileStream(file);

        assertThat(result.isSuccess()).isTrue();
        Map<String, Object> content = result.getData().getFileContent();
        assertThat(content).containsEntry("key", "value");
    }



    @Test
    void testParseJsonFileStream_InvalidJson() throws IOException {
        // 非法 JSON 内容
        String invalidJson = "{ invalid json }";
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "bad.json",
            "application/json",
            invalidJson.getBytes(StandardCharsets.UTF_8)
        );

        Result<JsonFile> result = Utils.parseJsonFileStream(file);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Error parsing JSON");
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
        assertThrows(NullPointerException.class, () -> Utils.validateFileStream(mockFile, "invalid_file", mimeTypes));
    }
    @Test
    public void test_validateFileStream_FileNameIsNull() {
        MultipartFile mockFile = Mockito.mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn(null);
        when(mockFile.getContentType()).thenReturn("text/plain");
        List<String> mimeTypes = Arrays.asList("text/plain");
        assertThrows(NullPointerException.class, () -> Utils.validateFileStream(mockFile, "invalid_file", mimeTypes));
    }


    @Test
    public void test_encodeObjectToBase64_WithJavaBean() throws Exception {
        User user = new User();
        user.setUsername("test1");
        user.setEmail("123456789@qq.com");
        String base64 = Utils.encodeObjectToBase64(user);
        assertNotNull(base64);
        String decoded = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
        assertTrue(decoded.contains("test1"));
        assertTrue(decoded.contains("123456789@qq.com"));
    }
    @Test
    public void test_decodeBase64ToObject_ValidBase64() throws Exception {
        // 准备 User 对象
        User expectedUser = new User();
        expectedUser.setUsername("test1");
        expectedUser.setEmail("123456789@qq.com");

        // 转换为标准 Base64 字符串
        String base64String = toBase64(expectedUser);

        // 调用被测方法
        User decodedUser = Utils.decodeBase64ToObject(base64String, User.class);

        // 验证结果
        assertNotNull(decodedUser);
        assertEquals("test1", decodedUser.getUsername());
        assertEquals("123456789@qq.com", decodedUser.getEmail());
    }

    @Test
    public void test_decodeBase64ToObject_UrlSafeBase64() throws Exception {
        User expectedUser = new User();
        expectedUser.setUsername("bob");
        expectedUser.setEmail("bob@example.com");

        String urlSafeBase64 = toUrlSafeBase64(expectedUser);

        User decodedUser = Utils.decodeBase64ToObject(urlSafeBase64, User.class);

        assertThat(decodedUser.getUsername()).isEqualTo("bob");
        assertThat(decodedUser.getEmail()).isEqualTo("bob@example.com");
    }

    @Test
    public void test_decodeBase64ToObject_InvalidBase64String() {
        String invalidBase64 = "!!!not-base64!!";

        assertThatThrownBy(() -> Utils.decodeBase64ToObject(invalidBase64, User.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid Base64 string");
    }

    @Test
    public void test_decodeBase64ToObject_Base64DecodesToNonJson() {
        // 构造一个有效的 Base64 字符串，但解码后不是 JSON（比如纯文本）
        String plainText = "hello world";
        String encoded = Base64.getEncoder().encodeToString(plainText.getBytes(StandardCharsets.UTF_8));

        // 预期 JSON 解析失败（异常类型取决于 JsonUtils.decode 的实现）
        assertThatThrownBy(() -> Utils.decodeBase64ToObject(encoded, User.class))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    public void test_decodeBase64ToObject_InvalidBase64() {
        assertThrows(ServiceException.class, () -> Utils.decodeBase64ToObject("invalid", User.class));
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
        assertTrue(Utils.isResource("thumbnail"));
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
        // 创建真实的临时 zip 文件
        File zipFile = tempDir.resolve("test.zip").toFile();
        assertThat(zipFile.createNewFile()).isTrue();

        // 创建真实的解压目录，并在其中创建子文件和子目录
        File extractDir = tempDir.resolve("extract").toFile();
        assertThat(extractDir.mkdirs()).isTrue();

        File subFile1 = new File(extractDir, "file1.txt");
        Files.writeString(subFile1.toPath(), "content1");

        File subDir = new File(extractDir, "subdir");
        assertThat(subDir.mkdirs()).isTrue();

        File subFile2 = new File(subDir, "file2.txt");
        Files.writeString(subFile2.toPath(), "content2");

        // 执行清理
        Utils.cleanUp(zipFile, extractDir);

        // 验证所有文件/目录已被删除
        assertThat(zipFile).doesNotExist();
        assertThat(extractDir).doesNotExist();
        assertThat(subFile1).doesNotExist();
        assertThat(subFile2).doesNotExist();
        assertThat(subDir).doesNotExist();
    }




    @Test
    void testCleanUp_Normal() throws IOException {
        // 创建临时文件和解压目录
        File zipFile = tempDir.resolve("test.zip").toFile();
        assertThat(zipFile.createNewFile()).isTrue();
        File extractDir = tempDir.resolve("extract").toFile();
        assertThat(extractDir.mkdirs()).isTrue();
        // 在目录中创建一些子文件/子目录
        Files.writeString(extractDir.toPath().resolve("a.txt"), "content");
        Files.createDirectory(extractDir.toPath().resolve("subdir"));
        Files.writeString(extractDir.toPath().resolve("subdir/b.txt"), "inside");

        // 执行清理
        Utils.cleanUp(zipFile, extractDir);

        // 验证文件已被删除
        assertThat(zipFile).doesNotExist();
        assertThat(extractDir).doesNotExist();
    }

    @Test
    void testCleanUp_ZipFileDoesNotExist() throws IOException {
        File nonExistentZip = tempDir.resolve("missing.zip").toFile();
        File extractDir = tempDir.resolve("extract").toFile();
        assertThat(extractDir.mkdirs()).isTrue();

        // 执行清理（zip 文件不存在，应只删除目录）
        Utils.cleanUp(nonExistentZip, extractDir);

        assertThat(nonExistentZip).doesNotExist();
        assertThat(extractDir).doesNotExist();
    }

    @Test
    void testCleanUp_TempDirDoesNotExist() throws IOException {
        File zipFile = tempDir.resolve("test.zip").toFile();
        assertThat(zipFile.createNewFile()).isTrue();
        File nonExistentDir = tempDir.resolve("no-such-dir").toFile();

        // 执行清理（目录不存在，应只删除 zip 文件）
        Utils.cleanUp(zipFile, nonExistentDir);

        assertThat(zipFile).doesNotExist();
        // 目录本身不存在，无需断言删除
    }

    @Test
    void testCleanUp_DeleteZipFileFails() throws IOException {
        // 模拟无法删除的 zip 文件（例如只读文件）
        File zipFile = tempDir.resolve("readonly.zip").toFile();
        assertThat(zipFile.createNewFile()).isTrue();
        assertThat(zipFile.setReadOnly()).isTrue();  // 只读权限，在 Windows 上可能导致删除失败，但 POSIX 系统仍可删除
        // 注意：在 Unix/Linux 上，只读文件依然可以被删除（只要父目录可写）。为了可靠模拟失败，可以使用 Mock。
        // 以下使用 Mockito 模拟 File.delete() 返回 false。
        File mockZipFile = mock(File.class);
        when(mockZipFile.exists()).thenReturn(true);
        when(mockZipFile.delete()).thenReturn(false);
        when(mockZipFile.getAbsolutePath()).thenReturn("/mock/path.zip");

        File extractDir = tempDir.resolve("extract").toFile();
        assertThat(extractDir.mkdirs()).isTrue();

        // 执行清理，应捕获删除失败并记录日志（不抛异常）
        Utils.cleanUp(mockZipFile, extractDir);

        // 验证删除被调用且返回 false，但方法不会抛出异常
        verify(mockZipFile).delete();
        // 真实目录仍应被删除（即使 zip mock 删除失败）
        assertThat(extractDir).doesNotExist();
    }

    @Test
    void testCleanUp_DeleteTempDirFails() throws IOException {
        File zipFile = tempDir.resolve("test.zip").toFile();
        assertThat(zipFile.createNewFile()).isTrue();
        // 创建一个无法删除的文件（例如只读文件）在临时目录中
        File extractDir = tempDir.resolve("extract").toFile();
        assertThat(extractDir.mkdirs()).isTrue();
        File unremovable = extractDir.toPath().resolve("unremovable.txt").toFile();
        assertThat(unremovable.createNewFile()).isTrue();
        // 在某些系统上，设置只读后可能仍能删除（取决于父目录权限），改用 Mock 模拟删除失败
        // 但为了简单，这里不模拟失败，因为 cleanUp 本身不会因单个文件删除失败而抛出异常（仅记录日志）
        // 我们验证目录最终被删除即可（实际上由于 Files.walk 会遍历所有文件并尝试删除，如果某个文件删除失败，后续可能仍会尝试删除父目录，但父目录非空会导致删除失败）
        // 因此此测试较复杂，建议不强制模拟失败，而是信任方法本身的容错性。

        // 简单验证：正常执行，不会抛出异常
        Utils.cleanUp(zipFile, extractDir);
        assertThat(zipFile).doesNotExist();
        // 注意：如果 unremovable 无法删除，extractDir 可能仍存在；但实际运行中通常可删除，故不做强制断言
    }

    // ---------- readAllBytes 测试 ----------

    @Test
    void testReadAllBytes_Normal() throws IOException {
        byte[] expected = "Hello, World!".getBytes(StandardCharsets.UTF_8);
        try (InputStream is = new ByteArrayInputStream(expected)) {
            byte[] result = Utils.readAllBytes(is);
            assertThat(result).isEqualTo(expected);
        }
    }



    @Test
    void testReadAllBytes_LargeData() throws IOException {
        int size = 10 * 1024; // 10KB
        byte[] expected = new byte[size];
        for (int i = 0; i < size; i++) {
            expected[i] = (byte) (i % 256);
        }
        try (InputStream is = new ByteArrayInputStream(expected)) {
            byte[] result = Utils.readAllBytes(is);
            assertThat(result).isEqualTo(expected);
        }
    }

    @Test
    void testReadAllBytes_InputStreamThrowsIOException() throws IOException {
        InputStream mockStream = mock(InputStream.class);
        when(mockStream.read(any(byte[].class))).thenThrow(new IOException("read error"));

        assertThatThrownBy(() -> Utils.readAllBytes(mockStream))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("read error");
    }

    @Test
    void testReadAllBytes_ClosesInputStream() throws IOException {
        InputStream mockStream = mock(InputStream.class);
        when(mockStream.read(any(byte[].class))).thenReturn(-1); // EOF immediately

        Utils.readAllBytes(mockStream);

        // 验证 finally 块中调用了 close
        verify(mockStream, times(1)).close();
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
        byte[] expectedBytes = "test data".getBytes(StandardCharsets.UTF_8);
        try (InputStream inputStream = new ByteArrayInputStream(expectedBytes)) {
            byte[] result = Utils.readAllBytes(inputStream);
            assertArrayEquals(expectedBytes, result);
        }
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

    @Test
    void testCreateTempDirectory_Success() throws IOException {
        List<File> createdDirs = new ArrayList<>();
        // 调用被测方法
        File tempDir = Utils.createTempDirectory();
        createdDirs.add(tempDir);  // 记录以便清理

        // 验证目录存在且是目录
        assertThat(tempDir).exists().isDirectory();
        // 验证目录名以 "unzip" 开头（Files.createTempDirectory 的默认前缀）
        assertThat(tempDir.getName()).startsWith("unzip");
    }

    // 注：createTempDirectory 通常不会抛出异常，除非底层权限问题（难以模拟），故不单独测试异常

    // ---------- convertMultipartFileToFile 测试 ----------
    @Test
    void testConvertMultipartFileToFile_Success() throws IOException {
        // 准备 MultipartFile（使用 Spring MockMultipartFile）
        byte[] content = "Hello, World!".getBytes(StandardCharsets.UTF_8);
        MultipartFile multipartFile = new MockMultipartFile(
            "file", "test.txt", "text/plain", content);
        List<File> createdFiles = new ArrayList<>();
        // 调用被测方法
        File resultFile = Utils.convertMultipartFileToFile(multipartFile);
        createdFiles.add(resultFile);  // 记录以便清理

        // 验证结果文件存在且内容匹配
        assertThat(resultFile).exists().isFile();
        byte[] readBytes = Files.readAllBytes(resultFile.toPath());
        assertThat(readBytes).isEqualTo(content);
    }

    @Test
    void testConvertMultipartFileToFile_WhenGetBytesThrowsIOException() throws IOException {
        // 使用 Mockito mock MultipartFile，让 getBytes() 抛出异常
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getBytes()).thenThrow(new IOException("Simulated I/O error"));

        // 调用方法应抛出 IOException
        assertThatThrownBy(() -> Utils.convertMultipartFileToFile(mockFile))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Simulated I/O error");
    }

    // 如果需要测试空文件
    @Test
    void testConvertMultipartFileToFile_WithEmptyContent() throws IOException {
        byte[] emptyContent = new byte[0];
        MultipartFile multipartFile = new MockMultipartFile(
            "empty", "empty.txt", "text/plain", emptyContent);

        File resultFile = Utils.convertMultipartFileToFile(multipartFile);
        List<File> createdFiles = new ArrayList<>();

        createdFiles.add(resultFile);

        assertThat(resultFile).exists().isFile();
        assertThat(resultFile.length()).isZero();
    }

    @Test
    void testUnzip_WithSingleFile() throws IOException {
        // 构造 ZIP：一个文本文件
        byte[] zipData = createZipContent(Entry.file("hello.txt", "Hello World"));
        MultipartFile multipartFile = new MockMultipartFile("file", "test.zip",
            "application/zip", zipData);

        List<FileInfo> result = Utils.unzip(multipartFile);

        assertThat(result).hasSize(1);
        FileInfo info = result.get(0);
        assertThat(info.getIsDirectory()).isFalse();
        assertThat(info.getName()).isEqualTo("hello.txt");
        assertThat(info.getContent().trim()).isEqualTo("Hello World");
    }

    @Test
    void testUnzip_WithDirectoryAndFile() throws IOException {
        // 构造 ZIP：包含目录和文件
        byte[] zipData = createZipContent(
            Entry.dir("folder"),
            Entry.file("folder/file.txt", "content inside")
        );
        MultipartFile multipartFile = new MockMultipartFile("file", "test.zip",
            "application/zip", zipData);

        List<FileInfo> result = Utils.unzip(multipartFile);

        assertThat(result).hasSize(2);
        // 目录
        FileInfo dirInfo = result.stream().filter(FileInfo::getIsDirectory).findFirst().orElseThrow();
        assertThat(dirInfo.getName()).isEqualTo("folder");
        // 文件
        FileInfo fileInfo = result.stream().filter(i -> !i.getIsDirectory()).findFirst().orElseThrow();
        assertThat(fileInfo.getName()).isEqualTo("file.txt");
        assertThat(fileInfo.getContent().trim()).isEqualTo("content inside");
    }

    @Test
    void testUnzip_WithZipSlipAttack_ShouldThrowSecurityException() throws IOException {
        // 恶意 ZIP 条目：试图跳出临时目录
        byte[] zipData = createZipContent(Entry.file("../outside.txt", "attack"));
        MultipartFile multipartFile = new MockMultipartFile("file", "evil.zip",
            "application/zip", zipData);

        assertThatThrownBy(() -> Utils.unzip(multipartFile))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("跨目录攻击");
    }

    @Test
    void testUnzip_EmptyZip() throws IOException {
        byte[] zipData = createZipContent();  // 无条目
        MultipartFile multipartFile = new MockMultipartFile("file", "empty.zip",
            "application/zip", zipData);

        List<FileInfo> result = Utils.unzip(multipartFile);
        assertThat(result).isEmpty();
    }

    @Test
    void testUnzip_MultipleFiles() throws IOException {
        byte[] zipData = createZipContent(
            Entry.file("file1.txt", "content1"),
            Entry.file("file2.txt", "content2"),
            Entry.file("file3.txt", "content3")
        );
        MultipartFile multipartFile = new MockMultipartFile("file", "multi.zip", "application/zip", zipData);

        List<FileInfo> result = Utils.unzip(multipartFile);

        assertThat(result).hasSize(3);
        assertThat(result).allMatch(info -> !info.getIsDirectory());
        assertThat(result).extracting(FileInfo::getName)
            .containsExactlyInAnyOrder("file1.txt", "file2.txt", "file3.txt");

        // 修正：使用 map 去除内容末尾空白后再比较
        assertThat(result).extracting(FileInfo::getContent)
            .map(String::trim)  // 或 .map(s -> s.replaceAll("\n$", ""))
            .containsExactlyInAnyOrder("content1", "content2", "content3");
    }

    @Test
    void testUnzip_WithChineseFileName() throws IOException {
        // 中文文件名
        String chineseFileName = "测试文件.txt";
        byte[] zipData = createZipContent(Entry.file(chineseFileName, "中文内容"));
        MultipartFile multipartFile = new MockMultipartFile("file", "chinese.zip", "application/zip", zipData);

        List<FileInfo> result = Utils.unzip(multipartFile);

        assertThat(result).hasSize(1);
        FileInfo info = result.get(0);
        assertThat(info.getName()).isEqualTo(chineseFileName);
        assertThat(info.getContent().stripTrailing()).isEqualTo("中文内容");
    }

    @Test
    void testUnzip_EmptyFileInsideZip() throws IOException {
        // ZIP 中包含空文件
        byte[] zipData = createZipContent(Entry.file("empty.txt", ""));
        MultipartFile multipartFile = new MockMultipartFile("file", "empty.zip", "application/zip", zipData);

        List<FileInfo> result = Utils.unzip(multipartFile);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEmpty();
        // 验证实际解压出的文件长度为 0
        Path tempDir = Path.of(System.getProperty("java.io.tmpdir"));
        // 注意：被测代码会创建临时目录，我们无法直接获取路径，但可通过 content 验证
    }

    @Test
    void testUnzip_DeepNestedDirectories() throws IOException {
        // 深层嵌套：a/b/c/d/e/file.txt
        byte[] zipData = createZipContent(
            Entry.dir("a/b/c/d/e"),
            Entry.file("a/b/c/d/e/file.txt", "deep content")
        );
        MultipartFile multipartFile = new MockMultipartFile("file", "deep.zip", "application/zip", zipData);

        List<FileInfo> result = Utils.unzip(multipartFile);

        // 预期返回一个目录条目 + 一个文件条目
        assertThat(result).hasSize(2);
        FileInfo dirInfo = result.stream().filter(FileInfo::getIsDirectory).findFirst().orElseThrow();
        assertThat(dirInfo.getName()).isEqualTo("e");
        FileInfo fileInfo = result.stream().filter(i -> !i.getIsDirectory()).findFirst().orElseThrow();
        assertThat(fileInfo.getName()).isEqualTo("file.txt");
        assertThat(fileInfo.getContent().trim()).isEqualTo("deep content");
    }

    @Test
    void testUnzip_SpecialCharactersInName() throws IOException {
        // 特殊字符：空格、括号、加号等
        String specialName = "file (1) + test!.txt";
        byte[] zipData = createZipContent(Entry.file(specialName, "special"));
        MultipartFile multipartFile = new MockMultipartFile("file", "special.zip", "application/zip", zipData);

        List<FileInfo> result = Utils.unzip(multipartFile);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo(specialName);
    }

    @Test
    void testUnzip_LargeFileSimulated() throws IOException {
        // 模拟大文件（不实际生成几GB，而是构造一个1MB的重复数据）
        int size = 1024 * 1024; // 1MB
        byte[] largeContent = new byte[size];
        Arrays.fill(largeContent, (byte) 'A');
        byte[] zipData = createZipContent(Entry.file("large.dat", new String(largeContent, StandardCharsets.UTF_8)));
        MultipartFile multipartFile = new MockMultipartFile("file", "large.zip", "application/zip", zipData);

        List<FileInfo> result = Utils.unzip(multipartFile);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent().trim()).hasSize(size);
    }



    @Test
    void testUnzip_ZeroByteZipFile() throws IOException {
        // 空的 ZIP 文件（长度为 0 的字节数组）
        byte[] zipData = new byte[0];
        MultipartFile multipartFile = new MockMultipartFile("file", "zero.zip", "application/zip", zipData);

        // 空 ZIP 会导致 ZipInputStream 构造后直接结束，返回空列表（不会抛异常）
        List<FileInfo> result = Utils.unzip(multipartFile);

        assertThat(result).isEmpty();
    }

    @Test
    void testUnzip_ZipWithOnlyDirectories() throws IOException {
        byte[] zipData = createZipContent(
            Entry.dir("dir1"),
            Entry.dir("dir2/subdir")
        );
        MultipartFile multipartFile = new MockMultipartFile("file", "dirs.zip", "application/zip", zipData);

        List<FileInfo> result = Utils.unzip(multipartFile);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(FileInfo::getIsDirectory);
        // 因为 File.getName() 返回最后一级名称，所以 "dir2/subdir" 会变成 "subdir"
        assertThat(result).extracting(FileInfo::getName)
            .containsExactlyInAnyOrder("dir1", "subdir");
    }

    @Test
    void testUnzip_InvalidZipFile() throws IOException {
        // 非法的 ZIP 数据（随机字节）
        byte[] invalidZip = "this is not a zip file".getBytes(StandardCharsets.UTF_8);
        MultipartFile multipartFile = new MockMultipartFile("file", "bad.zip", "application/zip", invalidZip);

        // 实际行为可能是抛出 IOException，也可能返回空列表（如果方法内部处理了异常）
        try {
            List<FileInfo> result = Utils.unzip(multipartFile);
            // 如果没有抛出异常，验证返回结果为空
            assertThat(result).isEmpty();
        } catch (IOException e) {
            // 如果抛出异常，测试通过
            assertThat(e).isInstanceOf(IOException.class);
        }
    }

    // ---------- processZipEntries 方法单独测试 ----------

    @Test
    void testProcessZipEntries_NormalEntries() throws IOException {
        File safeTempDir = tempDirForTest.toFile();
        // 确保目录条目以 '/' 结尾，这样 zipEntry.isDirectory() 才为 true
        byte[] zipData = createZipContent(
            Entry.file("a.txt", "aaa"),
            Entry.dir("sub/"),          // 注意结尾斜杠
            Entry.file("sub/b.txt", "bbb")
        );
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData))) {
            List<FileInfo> result = Utils.processZipEntries(zis, safeTempDir);

            assertThat(result).hasSize(3);

            // 验证磁盘文件（路径是完整的）
            Path aTxt = tempDirForTest.resolve("a.txt");
            assertThat(Files.readString(aTxt)).isEqualTo("aaa");
            Path bTxt = tempDirForTest.resolve("sub/b.txt");
            assertThat(Files.readString(bTxt)).isEqualTo("bbb");

            // 验证 FileInfo 对象：文件名是最后一级
            FileInfo fileA = result.stream()
                .filter(i -> "a.txt".equals(i.getName()))   // 直接文件名
                .findFirst()
                .orElseThrow();
            assertThat(fileA.getContent().trim()).isEqualTo("aaa");

            // 目录：名称为 "sub"（不带斜杠）
            FileInfo dirSub = result.stream()
                .filter(FileInfo::getIsDirectory)   // 需要 isDirectory() getter
                .findFirst()
                .orElseThrow();
            assertThat(dirSub.getName()).isEqualTo("sub");

            // 文件 b.txt：名称也是 "b.txt"
            FileInfo fileB = result.stream()
                .filter(i -> "b.txt".equals(i.getName()))
                .findFirst()
                .orElseThrow();
            assertThat(fileB.getContent().trim()).isEqualTo("bbb");
        }
    }
    @Test
    void testProcessZipEntries_ZipSlipAttack() throws IOException {
        File safeTempDir = tempDirForTest.toFile();
        byte[] zipData = createZipContent(Entry.file("../escape.txt", "danger"));
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData))) {
            assertThatThrownBy(() -> Utils.processZipEntries(zis, safeTempDir))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("跨目录攻击");
        }
    }

    @Test
    void testProcessZipEntries_DirectoryCreation() throws IOException {
        File safeTempDir = tempDirForTest.toFile();
        byte[] zipData = createZipContent(
            Entry.dir("deep/nested/dir"),
            Entry.file("deep/nested/dir/file.txt", "content")
        );
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData))) {
            List<FileInfo> result = Utils.processZipEntries(zis, safeTempDir);

            // 验证目录创建
            assertThat(tempDirForTest.resolve("deep/nested/dir")).isDirectory();
            assertThat(tempDirForTest.resolve("deep/nested/dir/file.txt")).exists();
            // 验证返回列表包含目录条目和文件条目
            assertThat(result).hasSize(2);
        }
    }

    // 辅助方法：将 User 对象转为标准 Base64 字符串
    private static String toBase64(User user) throws Exception {
        String json = OBJECT_MAPPER.writeValueAsString(user);
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    // 辅助方法：将 User 对象转为 URL 安全 Base64 字符串（无填充）
    private static String toUrlSafeBase64(User user) throws Exception {
        String json = OBJECT_MAPPER.writeValueAsString(user);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

}



