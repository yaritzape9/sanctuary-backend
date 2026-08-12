package com.sanctuary.sanctuary_backend.service;

import com.sanctuary.sanctuary_backend.model.Sighting;
import com.sanctuary.sanctuary_backend.repository.SightingRepository;
import com.sanctuary.sanctuary_backend.exception.SightingNotFoundException;
import com.sanctuary.sanctuary_backend.exception.DuplicateConfirmationException;
import com.sanctuary.sanctuary_backend.exception.UnauthorizedSightingActionException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.sanctuary.sanctuary_backend.dto.SightingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SightingService {

    private final SightingRepository repo;
    private final SimpMessagingTemplate messagingTemplate;

    // Threshold is intentionally not exposed to the frontend
    private static final int CONFIRM_THRESHOLD = 10;

    public List<Sighting> getAll() {
        return repo.findByRemovedFalse();
    }

    public Sighting create(Sighting sighting) {
        sighting.setStatus("pending");
        // createdAt is set automatically via @PrePersist in the model
        Sighting saved = repo.save(sighting);
        broadcastAfterCommit("/topic/sightings/create", SightingResponse.from(saved));
        return saved;
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

        Sighting saved = repo.save(s);
        broadcastAfterCommit("/topic/sightings/confirm", SightingResponse.from(saved)); 
        return saved;
    }

    public void deleteSighting(String id, String userId) {
        Sighting s = repo.findById(id)
            .orElseThrow(() -> new SightingNotFoundException("Sighting not found"));

        if (!s.getReportedBy().equals(userId)) {
            throw new UnauthorizedSightingActionException("You can only delete sightings you reported");
        }

        s.setRemoved(true);
        Sighting saved = repo.save(s);
        broadcastAfterCommit("/topic/sightings/delete", SightingResponse.from(saved));
    }

    private void broadcastAfterCommit(String topic, SightingResponse response) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messagingTemplate.convertAndSend(topic, response);
            }
        });
    } else {
        messagingTemplate.convertAndSend(topic, response);
    }
}
}