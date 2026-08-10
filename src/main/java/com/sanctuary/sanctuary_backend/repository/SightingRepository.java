package com.sanctuary.sanctuary_backend.repository;

import com.sanctuary.sanctuary_backend.model.Sighting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;


public interface SightingRepository extends JpaRepository<Sighting, String> {
    List<Sighting> findByRemovedFalse();
}