package com.sanctuary.sanctuary_backend.repository;

import com.sanctuary.sanctuary_backend.model.Sighting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SightingRepository extends JpaRepository<Sighting, String> {}