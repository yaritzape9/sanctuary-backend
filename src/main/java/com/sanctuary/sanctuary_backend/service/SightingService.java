package com.sanctuary.sanctuary_backend.service;

import com.sanctuary.sanctuary_backend.model.Sighting;
import com.sanctuary.sanctuary_backend.repository.SightingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SightingService {

    private final SightingRepository repo;
    private static final int CONFIRM_THRESHOLD = 4;

    public List<Sighting> getAll() {
        return repo.findAll();
    }

    public Sighting create(Sighting sighting) {
        sighting.setStatus("pending");
        return repo.save(sighting);
    }

    public Sighting confirm(String id, String userId) {
        Sighting s = repo.findById(id)
            .orElseThrow(() -> new RuntimeException("Sighting not found"));

        if (s.getConfirmations().contains(userId)) {
            throw new RuntimeException("Already confirmed");
        }

        s.getConfirmations().add(userId);

        if (s.getConfirmations().size() >= CONFIRM_THRESHOLD) {
            s.setStatus("confirmed");
        }

        return repo.save(s);
    }
}