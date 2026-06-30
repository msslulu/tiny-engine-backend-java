package com.tinyengine.it.modeldata.service;

import com.tinyengine.it.modeldata.dao.ApiKeyRepository;
import com.tinyengine.it.modeldata.entity.ApiKeyEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
@Service
public class ApiKeyService {
	@Autowired
	private ApiKeyRepository repository;

	public ApiKeyEntity getValidKey(String key) {
		Optional<ApiKeyEntity> opt = repository.findByApiKeyAndStatus(key, 1);
		if (opt.isEmpty()) return null;
		ApiKeyEntity entity = opt.get();
		if (entity.getExpireTime() != null && entity.getExpireTime().isBefore(LocalDateTime.now())) {
			return null;
		}
		return entity;
	}
}
