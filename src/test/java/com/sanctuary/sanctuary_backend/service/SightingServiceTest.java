package com.sanctuary.sanctuary_backend.service;

import com.sanctuary.sanctuary_backend.model.Sighting;
import com.sanctuary.sanctuary_backend.repository.SightingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SightingServiceTest {

    @Mock
    private SightingRepository repo;

    @InjectMocks
    private SightingService service;

    private Sighting sighting;

    @BeforeEach
    void setUp() {
        sighting = new Sighting();
        sighting.setId("sighting-1");
        sighting.setLocation("Test Location");
        sighting.setDescription("Test description");
        sighting.setLat(37.77);
        sighting.setLng(-122.42);
        sighting.setStatus("pending");
        sighting.setReportedBy("user-reporter");
        sighting.setConfirmations(new ArrayList<>());
    }

    @Test
    void getAll_returnsAllSightingsFromRepo() {
        when(repo.findByRemovedFalse()).thenReturn(List.of(sighting));

        List<Sighting> result = service.getAll();

        assertEquals(1, result.size());
        assertEquals("sighting-1", result.get(0).getId());
        verify(repo, times(1)).findByRemovedFalse();
    }

    @Test
    void create_setsStatusToPendingRegardlessOfInput() {
        sighting.setStatus("confirmed"); // simulate bad/malicious input
        when(repo.save(any(Sighting.class))).thenAnswer(inv -> inv.getArgument(0));

        Sighting result = service.create(sighting);

        assertEquals("pending", result.getStatus());
        verify(repo, times(1)).save(sighting);
    }

    @Test
    void confirm_throwsWhenSightingNotFound() {
        when(repo.findById("missing-id")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> service.confirm("missing-id", "user-1"));

        assertEquals("Sighting not found", ex.getMessage());
        verify(repo, never()).save(any());
    }

    @Test
    void confirm_throwsOnDuplicateConfirmationFromSameUser() {
        sighting.getConfirmations().add("user-1");
        when(repo.findById("sighting-1")).thenReturn(Optional.of(sighting));

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> service.confirm("sighting-1", "user-1"));

        assertEquals("Already confirmed", ex.getMessage());
        verify(repo, never()).save(any());
    }

    @Test
    void confirm_addsUserAndStaysPendingBelowThreshold() {
        // 8 existing confirmations -> this will be the 9th, still below threshold of 10
        for (int i = 0; i < 8; i++) {
            sighting.getConfirmations().add("user-" + i);
        }
        when(repo.findById("sighting-1")).thenReturn(Optional.of(sighting));
        when(repo.save(any(Sighting.class))).thenAnswer(inv -> inv.getArgument(0));

        Sighting result = service.confirm("sighting-1", "user-9th");

        assertEquals(9, result.getConfirmations().size());
        assertEquals("pending", result.getStatus());
    }

    @Test
    void confirm_flipsToConfirmedExactlyAtThreshold() {
        // 9 existing confirmations -> this will be the 10th, hitting the threshold
        for (int i = 0; i < 9; i++) {
            sighting.getConfirmations().add("user-" + i);
        }
        when(repo.findById("sighting-1")).thenReturn(Optional.of(sighting));
        when(repo.save(any(Sighting.class))).thenAnswer(inv -> inv.getArgument(0));

        Sighting result = service.confirm("sighting-1", "user-10th");

        assertEquals(10, result.getConfirmations().size());
        assertEquals("confirmed", result.getStatus());
    }

    @Test
    void confirm_staysConfirmedWhenAlreadyPastThreshold() {
        // 10 existing confirmations, already flipped to confirmed
        for (int i = 0; i < 10; i++) {
            sighting.getConfirmations().add("user-" + i);
        }
        sighting.setStatus("confirmed");
        when(repo.findById("sighting-1")).thenReturn(Optional.of(sighting));
        when(repo.save(any(Sighting.class))).thenAnswer(inv -> inv.getArgument(0));

        Sighting result = service.confirm("sighting-1", "user-11th");

        assertEquals(11, result.getConfirmations().size());
        assertEquals("confirmed", result.getStatus());
    }

    @Test
    void deleteSighting_ownerDeletesOwnSighting_setsRemovedTrue() {
        when(repo.findById("sighting-1")).thenReturn(Optional.of(sighting));
        when(repo.save(any(Sighting.class))).thenAnswer(inv -> inv.getArgument(0));

        service.deleteSighting("sighting-1", "user-reporter");

        assertTrue(sighting.getRemoved());
        verify(repo, times(1)).save(sighting);
    }

    @Test
    void deleteSighting_nonOwnerAttemptsDelete_throwsUnauthorized() {
        when(repo.findById("sighting-1")).thenReturn(Optional.of(sighting));

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> service.deleteSighting("sighting-1", "user-not-the-reporter"));

        assertEquals("You can only delete sightings you reported", ex.getMessage());
        verify(repo, never()).save(any());
    }

    @Test
    void deleteSighting_sightingDoesNotExist_throwsSightingNotFound() {
        when(repo.findById("missing-id")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> service.deleteSighting("missing-id", "user-reporter"));

        assertEquals("Sighting not found", ex.getMessage());
        verify(repo, never()).save(any());
    }
}