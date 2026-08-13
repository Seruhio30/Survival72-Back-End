ALTER TABLE subscriber
    MODIFY deslizamiento BIT(1) NULL,
    MODIFY huracan BIT(1) NULL,
    MODIFY inundacion BIT(1) NULL,
    MODIFY terremoto BIT(1) NULL,
    ADD COLUMN first_name VARCHAR(255) NULL,
    ADD COLUMN country_code VARCHAR(2) NOT NULL,
    ADD COLUMN status VARCHAR(32) NOT NULL,
    ADD COLUMN subscribed_at DATETIME(6) NOT NULL,
    ADD COLUMN updated_at DATETIME(6) NOT NULL,
    ADD COLUMN unsubscribed_at DATETIME(6) NULL,
    ADD COLUMN management_token_hash CHAR(64) NULL,
    ADD CONSTRAINT uk_subscriber_email UNIQUE (email),
    ADD CONSTRAINT uk_subscriber_management_token_hash UNIQUE (management_token_hash),
    ADD CONSTRAINT chk_subscriber_country_code
        CHECK (CHAR_LENGTH(country_code) = 2),
    ADD CONSTRAINT chk_subscriber_status
        CHECK (status IN ('ACTIVE', 'UNSUBSCRIBED'));

ALTER TABLE subscriber
    MODIFY email VARCHAR(255) NOT NULL;

CREATE TABLE subscriber_preferences (
    subscriber_id BIGINT NOT NULL,
    preference VARCHAR(64) NOT NULL,
    CONSTRAINT pk_subscriber_preferences
        PRIMARY KEY (subscriber_id, preference),
    CONSTRAINT chk_subscriber_preference
        CHECK (preference IN (
            'GENERAL_PREPAREDNESS',
            'EMERGENCY_KIT',
            'EDUCATIONAL_CONTENT',
            'EVENTS_AND_TRAINING'
        )),
    CONSTRAINT fk_subscriber_preferences_subscriber
        FOREIGN KEY (subscriber_id)
        REFERENCES subscriber (id)
        ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
