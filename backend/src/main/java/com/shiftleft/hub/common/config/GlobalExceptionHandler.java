package com.shiftleft.hub.common.config;

import com.shiftleft.hub.common.DuplicateEmailException;
import com.shiftleft.hub.document.domain.DocumentProcessingException;
import com.shiftleft.hub.document.domain.DuplicateDocumentException;
import com.shiftleft.hub.kcs.domain.KcsDraftingException;
import com.shiftleft.hub.user.domain.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Central exception handler that translates domain exceptions to RFC 7807 Problem Details.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private final Environment environment;

    /**
     * Creates a new GlobalExceptionHandler.
     *
     * @param environment the Spring environment for profile detection
     */
    public GlobalExceptionHandler(Environment environment) {
        this.environment = environment;
    }

    /**
     * Handles duplicate email registration attempts.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return a conflict problem detail
     */
    @ExceptionHandler(DuplicateEmailException.class)
    public ProblemDetail handleDuplicateEmail(DuplicateEmailException ex, HttpServletRequest request) {
        log.warn("Duplicate email: {} — {}", request.getRequestURI(), ex.getMessage());
        return buildProblem(HttpStatus.CONFLICT, "Duplicate Entity", ex.getMessage(),
            "urn:shiftleft:problem:duplicate-entity", request);
    }

    /**
     * Handles user not found exceptions.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return a not-found problem detail
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex, HttpServletRequest request) {
        log.warn("User not found: {} — {}", request.getRequestURI(), ex.getMessage());
        return buildProblem(HttpStatus.NOT_FOUND, "Entity Not Found", ex.getMessage(),
            "urn:shiftleft:problem:entity-not-found", request);
    }

    /**
     * Handles article not found exceptions.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return a not-found problem detail
     */
    @ExceptionHandler(com.shiftleft.hub.article.domain.ArticleNotFoundException.class)
    public ProblemDetail handleArticleNotFound(
            com.shiftleft.hub.article.domain.ArticleNotFoundException ex, HttpServletRequest request) {
        log.warn("Article not found: {} — {}", request.getRequestURI(), ex.getMessage());
        return buildProblem(HttpStatus.NOT_FOUND, "Entity Not Found", ex.getMessage(),
            "urn:shiftleft:problem:entity-not-found", request);
    }

    /**
     * Handles tag not found exceptions.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return a not-found problem detail
     */
    @ExceptionHandler(com.shiftleft.hub.tag.domain.TagNotFoundException.class)
    public ProblemDetail handleTagNotFound(
            com.shiftleft.hub.tag.domain.TagNotFoundException ex, HttpServletRequest request) {
        log.warn("Tag not found: {} — {}", request.getRequestURI(), ex.getMessage());
        return buildProblem(HttpStatus.NOT_FOUND, "Entity Not Found", ex.getMessage(),
            "urn:shiftleft:problem:entity-not-found", request);
    }

    /**
     * Handles tag in use exceptions.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return a conflict problem detail
     */
    @ExceptionHandler(com.shiftleft.hub.tag.domain.TagInUseException.class)
    public ProblemDetail handleTagInUse(
            com.shiftleft.hub.tag.domain.TagInUseException ex, HttpServletRequest request) {
        log.warn("Tag in use: {} — {}", request.getRequestURI(), ex.getMessage());
        return buildProblem(HttpStatus.CONFLICT, "Entity In Use", ex.getMessage(),
            "urn:shiftleft:problem:duplicate-entity", request);
    }

    /**
     * Handles ticket not found exceptions.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return a not-found problem detail
     */
    @ExceptionHandler(com.shiftleft.hub.ticket.domain.TicketNotFoundException.class)
    public ProblemDetail handleTicketNotFound(
            com.shiftleft.hub.ticket.domain.TicketNotFoundException ex, HttpServletRequest request) {
        log.warn("Ticket not found: {} — {}", request.getRequestURI(), ex.getMessage());
        return buildProblem(HttpStatus.NOT_FOUND, "Entity Not Found", ex.getMessage(),
            "urn:shiftleft:problem:entity-not-found", request);
    }

    /**
     * Handles document processing errors — validation vs internal.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return a bad request or internal error problem detail
     */
    @ExceptionHandler(DocumentProcessingException.class)
    public ProblemDetail handleDocumentProcessing(DocumentProcessingException ex, HttpServletRequest request) {
        if (ex.getCause() != null) {
            log.error("Document processing error: {} — {}", request.getRequestURI(), ex.getMessage(), ex);
            return buildProblem(HttpStatus.INTERNAL_SERVER_ERROR, "Document Processing Error",
                "Failed to process document", "urn:shiftleft:problem:internal-error", request);
        }
        log.warn("Document validation error: {} — {}", request.getRequestURI(), ex.getMessage());
        return buildProblem(HttpStatus.BAD_REQUEST, "Document Validation Error", ex.getMessage(),
            "urn:shiftleft:problem:validation-error", request);
    }

    /**
     * Handles duplicate document uploads (same content hash).
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return a conflict problem detail
     */
    @ExceptionHandler(DuplicateDocumentException.class)
    public ProblemDetail handleDuplicateDocument(DuplicateDocumentException ex, HttpServletRequest request) {
        log.warn("Duplicate document: {} — {}", request.getRequestURI(), ex.getMessage());
        return buildProblem(HttpStatus.CONFLICT, "Duplicate Document", ex.getMessage(),
            "urn:shiftleft:problem:duplicate-entity", request);
    }

    /**
     * Handles KCS drafting errors.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return an internal error problem detail
     */
    @ExceptionHandler(KcsDraftingException.class)
    public ProblemDetail handleKcsDraftingError(KcsDraftingException ex, HttpServletRequest request) {
        log.error("KCS drafting error: {} — {}", request.getRequestURI(), ex.getMessage());
        return buildProblem(HttpStatus.INTERNAL_SERVER_ERROR, "Drafting Error", ex.getMessage(),
            "urn:shiftleft:problem:internal-error", request);
    }

    /**
     * Handles illegal state exceptions.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return a bad request problem detail
     */
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        log.warn("Illegal state: {} — {}", request.getRequestURI(), ex.getMessage());
        return buildProblem(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(),
            "urn:shiftleft:problem:validation-error", request);
    }

    /**
     * Handles bad credentials exceptions.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return an unauthorized problem detail
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Bad credentials: {} — {}", request.getRequestURI(), ex.getMessage());
        return buildProblem(HttpStatus.UNAUTHORIZED, "Authentication Error", ex.getMessage(),
            "urn:shiftleft:problem:auth-error", request);
    }

    /**
     * Handles username not found exceptions.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return a not-found problem detail
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ProblemDetail handleUsernameNotFound(UsernameNotFoundException ex, HttpServletRequest request) {
        log.warn("Username not found: {} — {}", request.getRequestURI(), ex.getMessage());
        return buildProblem(HttpStatus.NOT_FOUND, "Entity Not Found", ex.getMessage(),
            "urn:shiftleft:problem:entity-not-found", request);
    }

    /**
     * Handles access denied exceptions.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return a forbidden problem detail
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied: {} — {}", request.getRequestURI(), ex.getMessage());
        return buildProblem(HttpStatus.FORBIDDEN, "Access Denied", ex.getMessage(),
            "urn:shiftleft:problem:access-denied", request);
    }

    /**
     * Handles no resource found exceptions (404 for unknown paths).
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return a not-found problem detail
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {} — {}", request.getRequestURI(), ex.getMessage());
        return buildProblem(HttpStatus.NOT_FOUND, "Resource Not Found", ex.getMessage(),
            "urn:shiftleft:problem:entity-not-found", request);
    }

    /**
     * Handles validation errors for @Valid request bodies.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return a validation error problem detail with field-level details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Validation error: {} — {}", request.getRequestURI(), ex.getMessage());
        ProblemDetail pd = buildProblem(HttpStatus.BAD_REQUEST, "Validation Failed",
            "Validation failed for request parameters",
            "urn:shiftleft:problem:validation-error", request);
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        pd.setProperty("fieldErrors", fieldErrors);
        return pd;
    }

    /**
     * Handles malformed JSON request bodies.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return a bad request problem detail
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ProblemDetail handleMalformedBody(
            org.springframework.http.converter.HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        log.warn("Malformed request body: {} — {}", request.getRequestURI(), ex.getMessage());
        return buildProblem(HttpStatus.BAD_REQUEST, "Malformed Request Body",
            "The request body could not be read. Check JSON syntax.",
            "urn:shiftleft:problem:validation-error", request);
    }

    /**
     * Handles HTTP method not supported errors.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return a method not allowed problem detail
     */
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotAllowed(
            org.springframework.web.HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        log.warn("Method not allowed: {} — {}", request.getRequestURI(), ex.getMessage());
        return buildProblem(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed",
            ex.getMessage(),
            "urn:shiftleft:problem:method-not-allowed", request);
    }

    /**
     * Handles all unhandled exceptions as a generic internal error.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return an internal error problem detail
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception ex, HttpServletRequest request) {
        // S-17: never return raw exception class names, messages, or stack
        // frames to the client, regardless of profile. They can echo
        // internals (PII, file paths, class names) that help an attacker
        // fingerprint the stack. The full exception is logged server-side
        // and correlated to the response via a stable errorId.
        String errorId = UUID.randomUUID().toString();
        log.error("Unhandled exception id={} path={} class={} message={}",
            errorId, request.getRequestURI(), ex.getClass().getName(), ex.getMessage(), ex);
        ProblemDetail pd = buildProblem(HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            "An unexpected error occurred. Reference id: " + errorId,
            "urn:shiftleft:problem:internal-error", request);
        pd.setProperty("errorId", errorId);
        return pd;
    }

    private ProblemDetail buildProblem(HttpStatus status, String title, String detail,
            String typeUri, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setType(URI.create(typeUri));
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }
}
