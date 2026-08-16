package com.seruhioCode30.survival72.repository;

import com.seruhioCode30.survival72.model.ContentItem;
import com.seruhioCode30.survival72.model.ContentStatus;
import com.seruhioCode30.survival72.model.ContentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContentItemRepository extends JpaRepository<ContentItem, Long> {

    Page<ContentItem> findByType(
            ContentType type,
            Pageable pageable
    );

    Page<ContentItem> findByStatus(
            ContentStatus status,
            Pageable pageable
    );

    Page<ContentItem> findByTypeAndStatus(
            ContentType type,
            ContentStatus status,
            Pageable pageable
    );
}
