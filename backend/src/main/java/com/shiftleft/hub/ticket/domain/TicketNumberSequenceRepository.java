package com.shiftleft.hub.ticket.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/** Spring Data JPA repository for {@link TicketNumberSequence} with pessimistic locking. */
public interface TicketNumberSequenceRepository extends JpaRepository<TicketNumberSequence, Long> {

    /**
     * Finds the sequence row with a pessimistic write lock.
     *
     * @param id the sequence row ID (always 1)
     * @return the sequence with a lock held
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from TicketNumberSequence s where s.id = 1")
    Optional<TicketNumberSequence> findByIdWithLock(Long id);
}
