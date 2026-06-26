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

    // Threshold is intentionally not exposed to the frontend
    private static final int CONFIRM_THRESHOLD = 10;

    public List<Sighting> getAll() {
        return repo.findAll();
    }

    public Sighting create(Sighting sighting) {
        sighting.setStatus("pending");
        // createdAt is set automatically via @PrePersist in the model
        return repo.save(sighting);
    }

    public Sighting confirm(String id, String userId) {
        Sighting s = repo.findById(id)
            .orElseThrow(() -> new RuntimeException("Sighting not found"));

        // Block duplicate confirmations — one per user enforced here, not in the DB
        if (s.getConfirmations().contains(userId)) {
            throw new RuntimeException("Already confirmed");
        }

        s.getConfirmations().add(userId);

        // Flip to confirmed at threshold — number stays backend-only
        if (s.getConfirmations().size() >= CONFIRM_THRESHOLD) {
            s.setStatus("confirmed");
        }

        return repo.save(s);
    }
}