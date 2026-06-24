package com.shiftleft.hub.tag.api;

import com.shiftleft.hub.tag.api.dto.CreateTagRequest;
import com.shiftleft.hub.tag.api.dto.TagResponse;
import com.shiftleft.hub.tag.api.dto.UpdateTagRequest;
import com.shiftleft.hub.tag.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/tags")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminTagController {

    private final TagService tagService;

    /**
     * Retrieves a paginated list of tags.
     *
     * @param page the page index (zero-based, defaults to 0)
     * @param size the page size (defaults to 20)
     * @return paginated tag responses
     */
    @GetMapping
    public Page<TagResponse> getAllTags(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return tagService.getAllTags(page, size);
    }

    /**
     * Retrieves a tag by its ID.
     *
     * @param id the tag UUID
     * @return the tag response
     */
    @GetMapping("/{id}")
    public TagResponse getTag(@PathVariable UUID id) {
        return tagService.getTagById(id);
    }

    /**
     * Creates a new tag.
     *
     * @param request the create tag request
     * @return the created tag response
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TagResponse createTag(@Valid @RequestBody CreateTagRequest request) {
        return tagService.createTag(request);
    }

    /**
     * Updates an existing tag.
     *
     * @param id      the tag UUID
     * @param request the update tag request
     * @return the updated tag response
     */
    @PutMapping("/{id}")
    public TagResponse updateTag(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTagRequest request) {
        return tagService.updateTag(id, request);
    }

    /**
     * Deletes a tag.
     *
     * @param id the tag UUID
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTag(@PathVariable UUID id) {
        tagService.deleteTag(id);
    }
}
