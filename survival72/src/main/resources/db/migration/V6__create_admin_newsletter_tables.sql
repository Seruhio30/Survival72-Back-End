CREATE TABLE newsletter (
    id BIGINT NOT NULL AUTO_INCREMENT,
    subject VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    sent_at DATETIME(6) NULL,
    CONSTRAINT pk_newsletter PRIMARY KEY (id),
    CONSTRAINT chk_newsletter_status
        CHECK (status IN (
            'DRAFT',
            'READY_TO_SEND',
            'SENT'
        ))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE newsletter_preferences (
    newsletter_id BIGINT NOT NULL,
    preference VARCHAR(64) NOT NULL,
    CONSTRAINT pk_newsletter_preferences
        PRIMARY KEY (newsletter_id, preference),
    CONSTRAINT chk_newsletter_preference
        CHECK (preference IN (
            'GENERAL_PREPAREDNESS',
            'EMERGENCY_KIT',
            'PRACTICAL_SKILLS',
            'EVENTS_AND_UPDATES'
        )),
    CONSTRAINT fk_newsletter_preferences_newsletter
        FOREIGN KEY (newsletter_id)
        REFERENCES newsletter (id)
        ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
