package com.lowcode.bi.repository;

import com.lowcode.bi.entity.HierarchyLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HierarchyLevelRepository extends JpaRepository<HierarchyLevel, UUID> {

    @Query("SELECT hl FROM HierarchyLevel hl WHERE hl.hierarchy.id = :hierarchyId AND hl.deleted = false ORDER BY hl.level ASC")
    List<HierarchyLevel> findByHierarchyId(@Param("hierarchyId") UUID hierarchyId);
}
