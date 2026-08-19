package com.atlas.core.repository;

import com.atlas.core.model.Chunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChunkRepository extends JpaRepository<Chunk, UUID> {

    boolean existsByContentHashAndVersion(String contentHash, String version);
}
