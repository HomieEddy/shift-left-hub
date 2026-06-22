package com.shiftleft.hub.common.config.seeder;

import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRepository;
import com.shiftleft.hub.workspace.domain.Workspace;
import com.shiftleft.hub.workspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkspaceAssignmentSeeder {

    private static final List<DepartmentAssignment> DEPARTMENT_ASSIGNMENTS = List.of(
        new DepartmentAssignment("human-resources",
            List.of("hr.user@company.com", "hr.tech@company.com")),
        new DepartmentAssignment("legal",
            List.of("legal.user@company.com", "legal.tech@company.com")),
        new DepartmentAssignment("it",
            List.of("it.user@company.com", "it.tech@company.com"))
    );

    private final WorkspaceService workspaceService;
    private final UserRepository userRepository;

    /**
     * Assigns each seed user to the appropriate workspaces (admin to all, department
     * user to their department + Public) and sets every seed user default workspace to Public.
     *
     * @param adminEmail the configured admin email (used to look up the admin user)
     */
    public void assignUsersAndSetDefaults(String adminEmail) {
        Map<String, Workspace> workspacesBySlug = loadWorkspacesBySlug();
        Workspace publicWs = workspacesBySlug.get(WorkspaceSeeder.PUBLIC_SLUG);
        if (publicWs == null) {
            log.warn("Public workspace not found - skipping assignments and default workspace setup");
            return;
        }

        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        if (admin != null) {
            for (Workspace ws : workspacesBySlug.values()) {
                workspaceService.assignUserToWorkspace(ws.getId(), admin.getId(), "ADMIN");
            }
        }

        for (DepartmentAssignment da : DEPARTMENT_ASSIGNMENTS) {
            assignDepartment(da, workspacesBySlug, publicWs);
        }

        setDefaultWorkspaceForAllUsers(adminEmail, publicWs.getId());
    }

    /**
     * Loads all seed workspaces by slug for assignment lookups.
     *
     * @return map of slug to Workspace (only existing workspaces are included)
     */
    private Map<String, Workspace> loadWorkspacesBySlug() {
        Map<String, Workspace> bySlug = new HashMap<>();
        for (WorkspaceSeeder.WorkspaceSeed ws : WorkspaceSeeder.workspaceSeeds()) {
            workspaceService.findBySlug(ws.slug())
                .ifPresent(w -> bySlug.put(ws.slug(), w));
        }
        return bySlug;
    }

    /**
     * Assigns each user in the department to the department workspace (MEMBER)
     * and to the Public workspace (MEMBER).
     *
     * @param da               the department assignment definition
     * @param workspacesBySlug map of slug to Workspace
     * @param publicWs         the Public workspace
     */
    private void assignDepartment(DepartmentAssignment da,
                                  Map<String, Workspace> workspacesBySlug,
                                  Workspace publicWs) {
        Workspace deptWs = workspacesBySlug.get(da.deptSlug());
        if (deptWs == null) {
            log.warn("Department workspace not found for {} - skipping assignments", da.deptSlug());
            return;
        }
        for (String email : da.userEmails()) {
            userRepository.findByEmail(email).ifPresent(user -> {
                workspaceService.assignUserToWorkspace(deptWs.getId(), user.getId(), "MEMBER");
                workspaceService.assignUserToWorkspace(publicWs.getId(), user.getId(), "MEMBER");
            });
        }
    }

    /**
     * Sets every seed user default workspace to Public.
     *
     * @param adminEmail  the admin email (also needs default workspace set)
     * @param publicWsId the Public workspace UUID
     */
    private void setDefaultWorkspaceForAllUsers(String adminEmail, UUID publicWsId) {
        userRepository.findByEmail(adminEmail).ifPresent(user -> setDefault(user, publicWsId));
        List<String> deptEmails = DEPARTMENT_ASSIGNMENTS.stream()
            .flatMap(da -> da.userEmails().stream())
            .collect(Collectors.toList());
        for (String email : deptEmails) {
            userRepository.findByEmail(email).ifPresent(user -> setDefault(user, publicWsId));
        }
    }

    /**
     * Sets the user default workspace to Public if not already set.
     *
     * @param user       the user to update
     * @param publicWsId the Public workspace UUID
     */
    private void setDefault(User user, UUID publicWsId) {
        if (!publicWsId.equals(user.getDefaultWorkspaceId())) {
            user.setDefaultWorkspaceId(publicWsId);
            userRepository.save(user);
            log.debug("Set default workspace for {} to Public", user.getEmail());
        }
    }

    private record DepartmentAssignment(String deptSlug, List<String> userEmails) {
    }
}