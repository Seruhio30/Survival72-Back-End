package com.seruhioCode30.survival72.repository;

import com.seruhioCode30.survival72.model.Newsletter;
import com.seruhioCode30.survival72.model.NewsletterStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NewsletterRepository extends JpaRepository<Newsletter, Long> {

    Page<Newsletter> findByStatus(
            NewsletterStatus status,
            Pageable pageable
    );
}
