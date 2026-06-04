package com.shiftleft.hub.ticket.domain;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface TicketNumberSequenceRepository extends JpaRepository<TicketNumberSequence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from TicketNumberSequence s where s.id = 1")
    Optional<TicketNumberSequence> findByIdWithLock(Long id);
}
