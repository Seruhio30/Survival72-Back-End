CREATE TABLE content_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    type VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000) NULL,
    youtube_video_id VARCHAR(32) NULL,
    published_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_content_item PRIMARY KEY (id),
    CONSTRAINT chk_content_item_type
        CHECK (type IN ('VIDEO', 'ARTICLE')),
    CONSTRAINT chk_content_item_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE content_item_preferences (
    content_item_id BIGINT NOT NULL,
    preference VARCHAR(64) NOT NULL,
    CONSTRAINT pk_content_item_preferences
        PRIMARY KEY (content_item_id, preference),
    CONSTRAINT chk_content_item_preference
        CHECK (preference IN (
            'GENERAL_PREPAREDNESS',
            'EMERGENCY_KIT',
            'EDUCATIONAL_CONTENT',
            'EVENTS_AND_TRAINING'
        )),
    CONSTRAINT fk_content_item_preferences_content_item
        FOREIGN KEY (content_item_id)
        REFERENCES content_item (id)
        ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
