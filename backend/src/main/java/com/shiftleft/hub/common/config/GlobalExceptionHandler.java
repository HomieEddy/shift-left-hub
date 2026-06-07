package com.shiftleft.hub.common.config;

import com.shiftleft.hub.common.DuplicateEmailException;
import com.shiftleft.hub.kcs.domain.KcsDraftingException;
import com.shiftleft.hub.user.domain.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
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

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private final Environment environment;

    public GlobalExceptionHandler(Environment environment) {
        this.environment = environment;
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ProblemDetail handleDuplicateEmail(DuplicateEmailException ex, HttpServletRequest request) {
        log.warn("Duplicate email: {} — {}", request.getRequestURI(), ex.getMessage());
        return buildProblem(HttpStatus.CONFLICT, "Duplicate Entity", ex.getMessage(),
            "urn:shiftleft:problem:duplicate-entity", request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex, HttpServletRequest request) {
        log.warn("User not found: {} — {}", request.getRequestURI(), ex.getMessage());
        return buildProblem(HttpStatus.NOT_FOUND, "Entity Not Found", ex.getMessage(),
            "urn:shiftleft:problem:entity-not-found", request);
    }

    @ExceptionHandler(com.shiftleft.hub.article.domain.ArticleNotFoundException.class)
    public ProblemDetail handleArticleNotFound(
            com.shiftleft.hub.article.domain.ArticleNotFoundException ex, HttpServletRequest request) {
        log.warn("Article not found: {} — {}", request.getRequestURI(), ex.getMessage());
        return buildProblem(HttpStatus.NOT_FOUND, "Entity Not Found", ex.getMessage(),
            "urn:shiftleft:problem:entity-not-found", request);
    }

    @ExceptionHandler(com.shiftleft.hub.tag.domain.TagNotFoundException.class)
    public ProblemDetail handleTagNotFound(
            com.shiftleft.hub.tag.domain.TagNotFoundException ex, HttpServletRequest request) {
        log.warn("Tag not found: {} — {}", request.getRequestURI(), ex.getMessage());
        return buildProblem(HttpStatus.NOT_FOUND, "Entity Not Found", ex.getMessage(),
            "urn:shiftleft:problem:entity-not-found", request);
    }

    @ExceptionHandler(com.shiftleft.hub.tag.domain.TagInUseException.class)
    public ProblemDetail handleTagInUse(
            com.shiftleft.hub.tag.domain.TagInUseException ex, HttpServletRequest request) {
        log.warn("Tag in use: {} — {}", request.getRequestURI(), ex.getMessage());
        return buildProblem(HttpStatus.CONFLICT, "Entity In Use", ex.getMessage(),
            "urn:shiftleft:problem:duplicate-entity", request);
    }

    @ExceptionHandler(com.shiftleft.hub.ticket.domain.TicketNotFoundException.class)
    public ProblemDetail handleTicketNotFound(
            com.shiftleft.hub.ticket.domain.TicketNotFoundException ex, HttpServletRequest request) {
        log.warn("Ticket not found: {} — {}", request.getRequestURI(), ex.getMessage());
        return buildProblem(HttpStatus.NOT_FOUND, "Entity Not Found", ex.getMessage(),
            "urn:shiftleft:problem:entity-not-found", request);
    }

    @ExceptionHandler(KcsDraftingException.class)
    public ProblemDetail handleKcsDraftingError(KcsDraftingException ex, HttpServletRequest request) {
        log.error("KCS drafting error: {} — {}", request.getRequestURI(), ex.getMessage());
        return buildProblem(HttpStatus.INTERNAL_SERVER_ERROR, "Drafting Error", ex.getMessage(),
            "urn:shiftleft:problem:internal-error", request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        log.warn("Illegal state: {} — {}", request.getRequestURI(), ex.getMessage());
        return buildProblem(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(),
            "urn:shiftleft:problem:validation-error", request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Bad credentials: {} — {}", request.getRequestURI(), ex.getMessage());
        return buildProblem(HttpStatus.UNAUTHORIZED, "Authentication Error", ex.getMessage(),
            "urn:shiftleft:problem:auth-error", request);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ProblemDetail handleUsernameNotFound(UsernameNotFoundException ex, HttpServletRequest request) {
        log.warn("Username not found: {} — {}", request.getRequestURI(), ex.getMessage());
        return buildProblem(HttpStatus.NOT_FOUND, "Entity Not Found", ex.getMessage(),
            "urn:shiftleft:problem:entity-not-found", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied: {} — {}", request.getRequestURI(), ex.getMessage());
        return buildProblem(HttpStatus.FORBIDDEN, "Access Denied", ex.getMessage(),
            "urn:shiftleft:problem:access-denied", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {} — {}", request.getRequestURI(), ex.getMessage());
        return buildProblem(HttpStatus.NOT_FOUND, "Resource Not Found", ex.getMessage(),
            "urn:shiftleft:problem:entity-not-found", request);
    }

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

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception: {} — {}", request.getRequestURI(), ex.getMessage(), ex);
        String detail = environment.acceptsProfiles(Profiles.of("dev"))
            ? ex.getMessage() + " — " + ex.getClass().getSimpleName()
            : "Internal server error";
        ProblemDetail pd = buildProblem(HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error", detail,
            "urn:shiftleft:problem:internal-error", request);
        if (environment.acceptsProfiles(Profiles.of("dev"))) {
            StackTraceElement[] stack = ex.getStackTrace();
            if (stack.length > 0) {
                pd.setProperty("stackTrace", stack[0].toString());
            }
        }
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
