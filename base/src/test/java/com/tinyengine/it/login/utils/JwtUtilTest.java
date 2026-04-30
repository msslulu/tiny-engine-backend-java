package com.tinyengine.it.login.utils;

import cn.hutool.core.util.ReflectUtil;
import com.tinyengine.it.login.service.TokenBlacklistService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.core.env.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class JwtUtilTest {

	@Mock
	private TokenBlacklistService tokenBlacklistService;
    @Mock
	private Environment environment;


	@InjectMocks
	private JwtUtil jwtUtil;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);

		ReflectUtil.setFieldValue(jwtUtil, "tokenBlacklistService", tokenBlacklistService);
		ReflectUtil.setFieldValue(jwtUtil, "environment", environment);
		String testSecret = "myTestSecretKeyThatIsLongEnoughForHS256Algorithm";
		ReflectUtil.setFieldValue(jwtUtil, "cachedSecret", testSecret);

		when(environment.getActiveProfiles()).thenReturn("dev".split(","));
		// Also set expiration time if it's not a constant but an injected value

	}
	@Test
	void validateSecretConfigurationThrowsExceptionWhenSecretIsMissingInNonDevProfile() {
		when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

		assertThrows(IllegalStateException.class, jwtUtil::validateSecretConfiguration);
	}

	@Test
	void validateSecretConfigurationGeneratesSecretInDevProfile() {
		when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
		assertDoesNotThrow(jwtUtil::validateSecretConfiguration);
	}


	@Test
	void validateSecretConfigurationThrowsExceptionForInvalidSecret() {
		when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
		System.setProperty("SECRET_STRING", "short");
		assertThrows(IllegalStateException.class, jwtUtil::validateSecretConfiguration);
	}
	@Test
	void getSecretKeyReturnsValidKey() {
		// 使用足够长的密钥（>32字节）
		String testSecret = "myTestSecretKeyThatIsLongEnoughForHS256Algorithm";

		jwtUtil.validateSecretConfiguration();  // 现在不会 NPE
		SecretKey secretKey = jwtUtil.getSecretKey();

		assertNotNull(secretKey);
		assertEquals("HmacSHA256", secretKey.getAlgorithm());

	}

	@Test
	void generateTokenReturnsValidToken() {
		String username = "testUser";
		String roles = "USER";
		String userId = "123";
		Integer platformId = 1;

		String token = jwtUtil.generateToken(username, roles, userId, null, platformId);

		assertNotNull(token);
		assertFalse(token.isEmpty());
		assertEquals(username, jwtUtil.getUsernameFromToken(token));
		assertEquals(roles, jwtUtil.getRolesFromToken(token));
		assertEquals(userId, jwtUtil.getUserIdFromToken(token));
		assertEquals(platformId, jwtUtil.getPlatformIdFromToken(token));
	}

	@Test
	void generateTokenThrowsExceptionForNullUsername() {
		assertThrows(IllegalArgumentException.class, () -> jwtUtil.generateToken(null, "USER", "123", null, 1),
			"Null username should throw IllegalArgumentException");
	}

	@Test
	void generateTokenHandlesEmptyRoles() {
		String token = jwtUtil.generateToken("testUser", "", "123", null, 1);

		assertNotNull(token);
		assertFalse(token.isEmpty());
		assertEquals("", jwtUtil.getRolesFromToken(token));
	}

	@Test
	void generateTokenHandlesNullUserId() {
		// Given
		String username = "testUser";
		String roles = "USER";
		Integer platformId = 1;
		Object tenants = null;  // 显式传入 null
		// When
		String token = jwtUtil.generateToken(username, roles, null, tenants, platformId);

		// Then
		assertNotNull(token);
		assertFalse(token.isEmpty());

		// 使用 assertAll 聚合断言，避免单个失败中断其余检查
		assertAll("Token claims validation",
			() -> assertNull(jwtUtil.getUserIdFromToken(token), "userId should be null"),
			() -> assertEquals(username, jwtUtil.getUsernameFromToken(token), "username mismatch"),
			() -> assertEquals(roles, jwtUtil.getRolesFromToken(token), "roles mismatch"),
			() -> assertEquals(platformId, jwtUtil.getPlatformIdFromToken(token), "platformId mismatch"),
			() -> assertEquals(Collections.emptyList(), jwtUtil.getTenantsFromToken(token), "tenants should be empty list")
		);
	}

	@Test
	void generateTokenHandlesNullTenants() {
		String token = jwtUtil.generateToken("testUser", "USER", "123", null, 1);

		assertNotNull(token);
		assertFalse(token.isEmpty());
		assertTrue(jwtUtil.getTenantsFromToken(token).isEmpty());
	}

	@Test
	void generateTokenHandlesEmptyTenantsList() {
		String token = jwtUtil.generateToken("testUser", "USER", "123", new ArrayList<>(), 1);

		assertNotNull(token);
		assertFalse(token.isEmpty());
		assertTrue(jwtUtil.getTenantsFromToken(token).isEmpty());
	}

	@Test
	void generateTokenHandlesNullPlatformId() {
		String token = jwtUtil.generateToken("testUser", "USER", "123", null, null);

		assertNotNull(token);
		assertFalse(token.isEmpty());
		assertNull(jwtUtil.getPlatformIdFromToken(token));
	}

	@Test
	void generateTokenHandlesSpecialCharactersInUsername() {
		String username = "user!@#$%^&*()";
		String token = jwtUtil.generateToken(username, "USER", "123", null, 1);

		assertNotNull(token);
		assertFalse(token.isEmpty());
		assertEquals(username, jwtUtil.getUsernameFromToken(token));
	}

	@Test
	void generateTokenHandlesLongUsername() {
		String username = "a".repeat(256);
		String token = jwtUtil.generateToken(username, "USER", "123", null, 1);

		assertNotNull(token);
		assertFalse(token.isEmpty());
		assertEquals(username, jwtUtil.getUsernameFromToken(token));
	}

	@Test
	void generateTokenHandlesCombinationOfNullAndValidParameters() {
		assertThrows(IllegalArgumentException.class, () -> jwtUtil.generateToken(null, "USER", "123", null, 1),
			"Null username should throw IllegalArgumentException");

	}

	@Test
	void validateTokenReturnsTrueForValidToken() {
		String token = jwtUtil.generateToken("validUser", "USER", "123", null, 1);
		when(tokenBlacklistService.isTokenBlacklisted(token)).thenReturn(false);

		boolean isValid = jwtUtil.validateToken(token);

		assertTrue(isValid, "Valid token should return true");
	}

	@Test
	void validateTokenReturnsFalseForBlacklistedToken() {
		String token = "blacklistedToken";
		when(tokenBlacklistService.isTokenBlacklisted(token)).thenReturn(true);

		boolean isValid = jwtUtil.validateToken(token);

		assertFalse(isValid, "Blacklisted token should return false");
	}

	@Test
	void validateTokenReturnsFalseForExpiredToken() {
		String expiredToken = createExpiredToken("expiredUser", "USER", "999");

		when(tokenBlacklistService.isTokenBlacklisted(expiredToken)).thenReturn(false);

		// Simulate token expiration by waiting or mocking expiration
		boolean isValid = jwtUtil.validateToken(expiredToken);

		assertFalse(isValid, "Expired token should return false");
	}

	@Test
	void validateTokenReturnsFalseForMalformedToken() {
		String malformedToken = "malformedToken";

		boolean isValid = jwtUtil.validateToken(malformedToken);

		assertFalse(isValid, "Malformed token should return false");
	}

	@Test
	void validateTokenReturnsFalseForInvalidSignature() {
		String token = jwtUtil.generateToken("validUser", "USER", "123", null, 1);
		String tamperedToken = token + "tampered";

		boolean isValid = jwtUtil.validateToken(tamperedToken);

		assertFalse(isValid, "Token with invalid signature should return false");
	}

	@Test
	void validateTokenThrowsExceptionForNullToken() {
		assertThrows(IllegalArgumentException.class, () -> jwtUtil.validateToken(null),
			"Null token should throw IllegalArgumentException");
	}

	@Test
	void validateTokenReturnsFalseForEmptyToken() {
		String emptyToken = "";

		boolean isValid = jwtUtil.validateToken(emptyToken);

		assertFalse(isValid, "Empty token should return false");
	}

	@Test
	void validateTokenReturnsFalseForTokenWithMissingClaims() {
		String token = jwtUtil.generateToken("validUser", null, null, null, null);
		when(tokenBlacklistService.isTokenBlacklisted(token)).thenReturn(true);

		boolean isValid = jwtUtil.validateToken(token);

		assertFalse(isValid, "Token with missing claims should return false");
	}

	@Test
	void validateTokenHandlesTokenWithExtraClaims() {
		String token = jwtUtil.generateToken("testUser", "USER", "123", null, 1);
		// Add extra claims manually
		String tokenWithExtraClaims = token + ".extraClaims";

		boolean isValid = jwtUtil.validateToken(tokenWithExtraClaims);

		assertFalse(isValid, "Token with extra claims should return false");
	}

	@Test
	void validateTokenHandlesTokenWithInvalidExpirationDate() {
		String token = jwtUtil.generateToken("testUser", "USER", "123", null, 1);
		when(tokenBlacklistService.isTokenBlacklisted(token)).thenReturn(true);

		// Simulate invalid expiration date
		String tamperedToken = token.replace("exp", "invalidExp");

		boolean isValid = jwtUtil.validateToken(tamperedToken);

		assertFalse(isValid, "Token with invalid expiration date should return false");
	}

	@Test
	void validateTokenHandlesTokenWithNullClaims() {
		String token = jwtUtil.generateToken("testUser", "USER", "123", null, 1);
		when(tokenBlacklistService.isTokenBlacklisted(token)).thenReturn(true);

		// Simulate null claims
		String tamperedToken = token.replace("claims", "null");

		boolean isValid = jwtUtil.validateToken(tamperedToken);

		assertFalse(isValid, "Token with null claims should return false");
	}

	@Test
	void validateTokenHandlesTokenWithEmptyClaims() {
		String token = jwtUtil.generateToken("testUser", "USER", "123", null, 1);
		when(tokenBlacklistService.isTokenBlacklisted(token)).thenReturn(true);

		// Simulate empty claims
		String tamperedToken = token.replace("claims", "{}");

		boolean isValid = jwtUtil.validateToken(tamperedToken);

		assertFalse(isValid, "Token with empty claims should return false");
	}

	@Test
	void validateTokenHandlesTokenWithInvalidAlgorithm() {
		String token = jwtUtil.generateToken("testUser", "USER", "123", null, 1);
		when(tokenBlacklistService.isTokenBlacklisted(token)).thenReturn(true);

		// Simulate invalid algorithm
		String tamperedToken = token.replace("HS256", "RS256");

		boolean isValid = jwtUtil.validateToken(tamperedToken);

		assertFalse(isValid, "Token with invalid algorithm should return false");
	}

	/**
	 * 辅助方法：生成一个已经过期的JWT（不通过JwtUtil，直接使用相同的密钥）
	 */
	private String createExpiredToken(String username, String roles, String userId) {
		Map<String, Object> claims = new HashMap<>();
		claims.put("username", username);
		claims.put("roles", roles);
		claims.put("userId", userId);
		claims.put("platformId", 1);
		claims.put("tenants", new ArrayList<>());

		// 使用与JwtUtil中完全相同的密钥
		String testSecret = "myTestSecretKeyThatIsLongEnoughForHS256Algorithm";
		SecretKey key = Keys.hmacShaKeyFor(testSecret.getBytes(StandardCharsets.UTF_8));

		return Jwts.builder()
			.claims(claims)
			.subject(username)
			.issuedAt(new Date(System.currentTimeMillis() - 120_000))  // 2分钟前签发
			.expiration(new Date(System.currentTimeMillis() - 60_000)) // 1分钟前过期
			.signWith(key)
			.compact();
	}

}