CREATE TABLE incidents (
    id UUID PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    description VARCHAR(5000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT incidents_title_not_blank
        CHECK (title ~ '[^[:space:]]'),

    CONSTRAINT incidents_description_not_blank
        CHECK (description ~ '[^[:space:]]'),

    CONSTRAINT incidents_status_valid
        CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED')),

    CONSTRAINT incidents_timestamps_valid
        CHECK (updated_at >= created_at)
);