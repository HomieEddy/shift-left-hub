package com.shiftleft.hub.tag.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {
}
