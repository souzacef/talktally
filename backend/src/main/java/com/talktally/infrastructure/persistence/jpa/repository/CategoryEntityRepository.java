package com.talktally.infrastructure.persistence.jpa.repository;

import com.talktally.infrastructure.persistence.jpa.entity.CategoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryEntityRepository extends JpaRepository<CategoryJpaEntity, UUID> {

	@Query("""
			select category
			from CategoryJpaEntity category
			where category.id = :categoryId
			  and (category.ownerUserId is null or category.ownerUserId = :ownerId)
			""")
	Optional<CategoryJpaEntity> findVisibleById(
			@Param("ownerId") UUID ownerId,
			@Param("categoryId") UUID categoryId);

	@Query("""
			select category
			from CategoryJpaEntity category
			where category.ownerUserId is null or category.ownerUserId = :ownerId
			order by
			  case when category.builtIn = true then 0 else 1 end,
			  category.id
			""")
	List<CategoryJpaEntity> findAllVisible(@Param("ownerId") UUID ownerId);

	Optional<CategoryJpaEntity> findByCodeAndOwnerUserIdIsNull(String code);
}
