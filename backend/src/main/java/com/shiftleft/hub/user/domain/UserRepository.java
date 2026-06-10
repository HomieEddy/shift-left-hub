package com.shiftleft.hub.user.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data repository for User entities. */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(UserRole role);

    @Query("SELECT u FROM User u JOIN WorkspaceMember wm"
        + " ON u.id = wm.id.userId WHERE wm.id.workspaceId = :workspaceId")
    List<User> findUsersByWorkspaceId(@Param("workspaceId") UUID workspaceId);

    @Query("SELECT u FROM User u WHERE u.id NOT IN"
        + " (SELECT wm.id.userId FROM WorkspaceMember wm"
        + " WHERE wm.id.workspaceId = :workspaceId)")
    List<User> findUsersNotInWorkspace(@Param("workspaceId") UUID workspaceId);
}
