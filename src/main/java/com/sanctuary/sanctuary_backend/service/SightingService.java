package com.sanctuary.sanctuary_backend.service;

import com.sanctuary.sanctuary_backend.model.Sighting;
import com.sanctuary.sanctuary_backend.repository.SightingRepository;
import com.sanctuary.sanctuary_backend.exception.SightingNotFoundException;
import com.sanctuary.sanctuary_backend.exception.DuplicateConfirmationException;
import com.sanctuary.sanctuary_backend.exception.UnauthorizedSightingActionException;
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
        return repo.findByRemovedFalse();
    }

    public Sighting create(Sighting sighting) {
        sighting.setStatus("pending");
        // createdAt is set automatically via @PrePersist in the model
        return repo.save(sighting);
    }

    public Sighting confirm(String id, String userId) {
        Sighting s = repo.findById(id)
            .orElseThrow(() -> new SightingNotFoundException("Sighting not found"));

        if (s.getConfirmations().contains(userId)) {
            throw new DuplicateConfirmationException("Already confirmed");
        }

        s.getConfirmations().add(userId);

        // Flip to confirmed at threshold — number stays backend-only
        if (s.getConfirmations().size() >= CONFIRM_THRESHOLD) {
            s.setStatus("confirmed");
        }

        return repo.save(s);
    }

    public void deleteSighting(String id, String userId) {
        Sighting s = repo.findById(id)
            .orElseThrow(() -> new SightingNotFoundException("Sighting not found"));

        if (!s.getReportedBy().equals(userId)) {
            throw new UnauthorizedSightingActionException("You can only delete sightings you reported");
        }

        s.setRemoved(true);
        repo.save(s);
    }
}