package com.prephub.api.repository;

import com.prephub.api.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {

    Optional<Company> findBySlug(String slug);

    Optional<Company> findByNameIgnoreCase(String name);

    @Query(value = "SELECT * FROM companies WHERE name ILIKE CONCAT('%', :query, '%') OR :query = ANY(aliases) ORDER BY name ASC LIMIT 20", nativeQuery = true)
    List<Company> searchByNameOrAlias(@Param("query") String query);

    @Query(value = "SELECT * FROM companies WHERE similarity(name, :companyName) > 0.4 ORDER BY similarity(name, :companyName) DESC LIMIT 1", nativeQuery = true)
    Optional<Company> findMostSimilarByName(@Param("companyName") String companyName);
}

