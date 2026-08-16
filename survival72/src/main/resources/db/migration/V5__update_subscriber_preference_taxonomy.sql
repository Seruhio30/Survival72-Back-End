ALTER TABLE subscriber_preferences
    DROP CHECK chk_subscriber_preference;

ALTER TABLE content_item_preferences
    DROP CHECK chk_content_item_preference;

UPDATE subscriber_preferences
SET preference = 'PRACTICAL_SKILLS'
WHERE preference = 'EDUCATIONAL_CONTENT';

UPDATE subscriber_preferences
SET preference = 'EVENTS_AND_UPDATES'
WHERE preference = 'EVENTS_AND_TRAINING';

UPDATE content_item_preferences
SET preference = 'PRACTICAL_SKILLS'
WHERE preference = 'EDUCATIONAL_CONTENT';

UPDATE content_item_preferences
SET preference = 'EVENTS_AND_UPDATES'
WHERE preference = 'EVENTS_AND_TRAINING';

ALTER TABLE subscriber_preferences
    ADD CONSTRAINT chk_subscriber_preference
        CHECK (preference IN (
            'GENERAL_PREPAREDNESS',
            'EMERGENCY_KIT',
            'PRACTICAL_SKILLS',
            'EVENTS_AND_UPDATES'
        ));

ALTER TABLE content_item_preferences
    ADD CONSTRAINT chk_content_item_preference
        CHECK (preference IN (
            'GENERAL_PREPAREDNESS',
            'EMERGENCY_KIT',
            'PRACTICAL_SKILLS',
            'EVENTS_AND_UPDATES'
        ));
