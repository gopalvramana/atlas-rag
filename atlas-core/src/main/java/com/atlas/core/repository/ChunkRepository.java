package com.atlas.core.repository;

import com.atlas.core.model.Chunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ChunkRepository extends JpaRepository<Chunk, UUID> {

    boolean existsByContentHashAndVersion(String contentHash, String version);

    @Query("SELECT MAX(c.version) FROM Chunk c")
    Optional<String> findLatestVersion();
}
