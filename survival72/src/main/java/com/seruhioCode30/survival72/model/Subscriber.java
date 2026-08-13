package com.seruhioCode30.survival72.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "subscriber")
public class Subscriber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "first_name", length = 255)
    private String firstName;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SubscriberStatus status;

    @Column(name = "subscribed_at", nullable = false)
    private LocalDateTime subscribedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "unsubscribed_at")
    private LocalDateTime unsubscribedAt;

    @Column(name = "management_token_hash", length = 64)
    private String managementTokenHash;

    @ElementCollection
    @CollectionTable(
            name = "subscriber_preferences",
            joinColumns = @JoinColumn(name = "subscriber_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "preference", nullable = false, length = 64)
    private Set<SubscriberPreference> preferences = new LinkedHashSet<>();

    /*
     * Temporary compatibility fields for historical controllers/services.
     * They are intentionally not persisted by the canonical Join model.
     */
    @Transient
    private String lastName;

    @Transient
    private LocalDate subscriptionDate;

    @Transient
    private String topicsOfInterest;

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public SubscriberStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriberStatus status) {
        this.status = status;
    }

    public LocalDateTime getSubscribedAt() {
        return subscribedAt;
    }

    public void setSubscribedAt(LocalDateTime subscribedAt) {
        this.subscribedAt = subscribedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getUnsubscribedAt() {
        return unsubscribedAt;
    }

    public void setUnsubscribedAt(LocalDateTime unsubscribedAt) {
        this.unsubscribedAt = unsubscribedAt;
    }

    public String getManagementTokenHash() {
        return managementTokenHash;
    }

    public void setManagementTokenHash(String managementTokenHash) {
        this.managementTokenHash = managementTokenHash;
    }

    public Set<SubscriberPreference> getPreferences() {
        return preferences;
    }

    public void setPreferences(Set<SubscriberPreference> preferences) {
        this.preferences = preferences == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(preferences);
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getSubscriptionDate() {
        return subscriptionDate;
    }

    public void setSubscriptionDate(LocalDate subscriptionDate) {
        this.subscriptionDate = subscriptionDate;
    }

    public String getTopicsOfInterest() {
        return topicsOfInterest;
    }

    public void setTopicsOfInterest(String topicsOfInterest) {
        this.topicsOfInterest = topicsOfInterest;
    }
}
