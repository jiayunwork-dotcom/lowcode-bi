package com.lowcode.bi.repository;

import com.lowcode.bi.entity.DimensionHierarchy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DimensionHierarchyRepository extends JpaRepository<DimensionHierarchy, UUID> {

    @Query("SELECT dh FROM DimensionHierarchy dh WHERE dh.dataModel.id = :dataModelId AND dh.deleted = false ORDER BY dh.position ASC")
    List<DimensionHierarchy> findByDataModelId(@Param("dataModelId") UUID dataModelId);

    @Query("SELECT dh FROM DimensionHierarchy dh WHERE dh.dataModel.id = :dataModelId AND dh.name = :name AND dh.deleted = false")
    Optional<DimensionHierarchy> findByDataModelIdAndName(@Param("dataModelId") UUID dataModelId, @Param("name") String name);

    boolean existsByDataModelIdAndNameAndDeletedFalse(UUID dataModelId, String name);
}
