package com.tinyengine.it.modeldata.dao;

import com.tinyengine.it.modeldata.entity.ModelData;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface ModelDataRepository extends JpaRepository<ModelData, Long>, JpaSpecificationExecutor<ModelData> {


	/**
	 * 根据模型ID和租户ID查询所有数据
	 */
	List<ModelData> findByModelIdAndTenantId(Integer modelId, String tenantId);

	ModelData findByModelIdAndTenantIdAndId(Integer modelId, String tenantId, Integer id);


}
