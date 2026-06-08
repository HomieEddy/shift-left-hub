package com.shiftleft.hub.ticket.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Ticket} entities.
 */
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    List<Ticket> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Ticket> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, TicketStatus status);

    long countByStatus(TicketStatus status);

    /**
     * Finds a ticket by ID with a pessimistic write lock.
     * <p>Acquires a database-level row lock to prevent concurrent modifications,
     * primarily used during the claim flow to prevent double-claiming.</p>
     *
     * @param id the ticket UUID
     * @return an {@link Optional} containing the ticket if found, or empty
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Ticket t where t.id = :id")
    Optional<Ticket> findByIdForUpdate(@Param("id") UUID id);
}
