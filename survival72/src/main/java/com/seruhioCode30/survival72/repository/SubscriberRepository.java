package com.seruhioCode30.survival72.repository;

import com.seruhioCode30.survival72.model.Subscriber;
import com.seruhioCode30.survival72.model.SubscriberPreference;
import com.seruhioCode30.survival72.model.SubscriberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {

    Optional<Subscriber> findByEmail(String email);

    Optional<Subscriber> findByManagementTokenHash(String managementTokenHash);

    Page<Subscriber> findByStatus(
            SubscriberStatus status,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT DISTINCT s
                    FROM Subscriber s
                    JOIN s.preferences preference
                    WHERE preference = :preference
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT s.id)
                    FROM Subscriber s
                    JOIN s.preferences preference
                    WHERE preference = :preference
                    """
    )
    Page<Subscriber> findByPreference(
            @Param("preference") SubscriberPreference preference,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT DISTINCT s
                    FROM Subscriber s
                    JOIN s.preferences preference
                    WHERE s.status = :status
                      AND preference = :preference
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT s.id)
                    FROM Subscriber s
                    JOIN s.preferences preference
                    WHERE s.status = :status
                      AND preference = :preference
                    """
    )
    Page<Subscriber> findByStatusAndPreference(
            @Param("status") SubscriberStatus status,
            @Param("preference") SubscriberPreference preference,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT DISTINCT s
                    FROM Subscriber s
                    JOIN s.preferences preference
                    WHERE s.status = :status
                      AND preference IN :preferences
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT s.id)
                    FROM Subscriber s
                    JOIN s.preferences preference
                    WHERE s.status = :status
                      AND preference IN :preferences
                    """
    )
    Page<Subscriber> findDistinctByStatusAndPreferencesIn(
            @Param("status") SubscriberStatus status,
            @Param("preferences") Set<SubscriberPreference> preferences,
            Pageable pageable
    );
}
