package com.tinyengine.it.login.service.impl;

import cn.hutool.core.util.ReflectUtil;
import com.tinyengine.it.common.base.Result;
import com.tinyengine.it.common.exception.ExceptionEnum;
import com.tinyengine.it.common.exception.ServiceException;
import com.tinyengine.it.mapper.UserMapper;
import com.tinyengine.it.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import static com.tinyengine.it.login.utils.SM2EncryptionUtil.encrypt;
import static com.tinyengine.it.login.utils.SM2EncryptionUtil.generateSM2KeyPair;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginServiceImplTest {
	@InjectMocks
	private LoginServiceImpl loginService;

	@Mock
	private UserMapper userMapper;


	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		ReflectUtil.setFieldValue(loginService, "baseMapper", userMapper);

	}



	@Test
	void testCreateUser_NullInput() {
		// Act & Assert
		assertThrows(NullPointerException.class, () -> loginService.createUser(null));
	}



	@Test
	void testForgotPassword_InvalidPublicKey() throws Exception {
		// Arrange
		User user = new User();
		user.setUsername("testUser");
		user.setPublicKey("invalidPublicKey");

		when(userMapper.queryUserByCondition(any())).thenReturn(List.of(user));

		// Act
		Result result = loginService.forgotPassword(user);

		// Assert
		assertNotNull(result);
		assertFalse(result.isSuccess());
		assertEquals(ExceptionEnum.CM344.getResultCode(), result.getCode());
	}

	@Test
	void testForgotPassword_MismatchedKeys() throws Exception {
		// Arrange
		User user = new User();
		user.setUsername("testUser");
		user.setPublicKey("testPublicKey");
		user.setSalt("testSalt");

		User existingUser = new User();
		existingUser.setPrivateKey(Base64.getEncoder().encodeToString(new byte[16]));
		existingUser.setSalt("encryptedSalt");

		when(userMapper.queryUserByCondition(any())).thenReturn(List.of(existingUser));

		// Act
		Result result = loginService.forgotPassword(user);

		// Assert
		assertNotNull(result);
		assertFalse(result.isSuccess());
		assertEquals(ExceptionEnum.CM344.getResultCode(), result.getCode());
	}


	@Test
	void testForgotPassword_UserNotFound() throws Exception {
		// Arrange
		User user = new User();
		user.setUsername("nonExistentUser");

		when(userMapper.queryUserByCondition(user)).thenReturn(Collections.emptyList());

		// Act
		Result result = loginService.forgotPassword(user);

		// Assert
		assertNotNull(result);
		assertFalse(result.isSuccess());
		assertEquals(ExceptionEnum.CM345.getResultCode(), result.getCode());
		verify(userMapper, never()).updateUserById(user);
	}



	@Test
	void testForgotPassword_ValidKeys() throws Exception {
		// Arrange
		User user = new User();
		user.setUsername("testUser");
		user.setPublicKey("validPublicKey");
		user.setSalt("newSalt");

		User existingUser = new User();
		existingUser.setPrivateKey(Base64.getEncoder().encodeToString(new byte[16]));
		existingUser.setSalt("encryptedSalt");

		when(userMapper.queryUserByCondition(any())).thenReturn(List.of(existingUser));
		when(userMapper.queryUserById(any())).thenReturn(existingUser);

		// Act
		Result result = loginService.forgotPassword(user);

		// Assert
		assertNotNull(result);
		assertFalse(result.isSuccess());
	}

	@Test
	void testValidatorPublicKey_ValidKeys() throws Exception {
		// Arrange
		String salt = "testSalt";
		KeyPair keyPair = generateSM2KeyPair();
		PublicKey publicKey = keyPair.getPublic();
		PrivateKey privateKey = keyPair.getPrivate();
		String encryptedSalt = encrypt(salt, publicKey);

		// Act
		boolean isValid = loginService.validatorPublicKey(encryptedSalt, publicKey, privateKey);

		// Assert
		assertTrue(isValid);
	}

	@Test
	void testCreateUser_DuplicateUsername() throws Exception {
		// Arrange
		User user = new User();
		user.setUsername("duplicateUser");
		when(userMapper.queryUserByCondition(any())).thenReturn(List.of(user));

		// Act & Assert
		ServiceException exception = assertThrows(ServiceException.class, () -> loginService.createUser(user));
		assertEquals(ExceptionEnum.CM343.getResultCode(), exception.getCode());
	}

	@Test
	void testForgotPassword_NullInput() {
		// Act & Assert
		assertThrows(NullPointerException.class, () -> loginService.forgotPassword(null));
	}


	@Test
	void testCreateUser_EmptyUsername() {
		// Arrange
		User user = new User();
		user.setUsername("");

		// Act & Assert
		NullPointerException exception = assertThrows(NullPointerException.class, () -> loginService.createUser(user));
		assertNotNull(exception);}

	@Test
	void testForgotPassword_InvalidSalt() throws Exception {
		// Arrange
		User user = new User();
		user.setUsername("testUser");
		user.setPublicKey("validPublicKey");
		user.setSalt("invalidSalt");

		User existingUser = new User();
		existingUser.setPrivateKey(Base64.getEncoder().encodeToString(new byte[16]));
		existingUser.setSalt("encryptedSalt");

		when(userMapper.queryUserByCondition(any())).thenReturn(List.of(existingUser));

		// Act
		Result result = loginService.forgotPassword(user);

		// Assert
		assertNotNull(result);
		assertFalse(result.isSuccess());
		assertEquals(ExceptionEnum.CM344.getResultCode(), result.getCode());
	}
	@Test
	void testCreateUser_NullSalt() {
		// Arrange
		User user = new User();
		user.setUsername("testUser");
		user.setSalt(null);

		// Act & Assert
		NullPointerException exception = assertThrows(NullPointerException.class, () -> loginService.createUser(user));
		assertNotNull(exception);
	}

	@Test
	void testForgotPassword_EmptyPublicKey() throws Exception {
		// Arrange
		User user = new User();
		user.setUsername("testUser");
		user.setPublicKey("");

		when(userMapper.queryUserByCondition(any())).thenReturn(List.of(user));

		// Act
		Result result = loginService.forgotPassword(user);

		// Assert
		assertNotNull(result);
		assertFalse(result.isSuccess());
		assertEquals(ExceptionEnum.CM344.getResultCode(), result.getCode());
	}









}