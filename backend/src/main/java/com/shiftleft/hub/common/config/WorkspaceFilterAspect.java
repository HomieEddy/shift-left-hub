package com.shiftleft.hub.common.config;

import com.shiftleft.hub.common.domain.WorkspaceContextHolder;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

/**
 * AOP aspect that automatically enables the Hibernate workspaceFilter
 * on all Repository method executions.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkspaceFilterAspect {

    private final EntityManager entityManager;

    /**
     * Enables the workspaceFilter on repository queries when a workspace context is active.
     *
     * @param joinPoint the join point for the repository method execution
     * @return the result of the repository method
     * @throws Throwable if the repository method throws
     */
    @Around("execution(* com.shiftleft.hub.*.domain.*Repository.*(..))")
    public Object applyWorkspaceFilter(ProceedingJoinPoint joinPoint) throws Throwable {
        if (WorkspaceContextHolder.hasCurrentWorkspaceId()) {
            Session session = entityManager.unwrap(Session.class);
            org.hibernate.Filter filter = session.enableFilter("workspaceFilter");
            filter.setParameter("workspaceId", WorkspaceContextHolder.getCurrentWorkspaceId());
            log.trace("Enabled workspaceFilter for workspace {}", WorkspaceContextHolder.getCurrentWorkspaceId());
        }
        try {
            return joinPoint.proceed();
        } finally {
            if (WorkspaceContextHolder.hasCurrentWorkspaceId()) {
                entityManager.unwrap(Session.class).disableFilter("workspaceFilter");
            }
        }
    }
}
