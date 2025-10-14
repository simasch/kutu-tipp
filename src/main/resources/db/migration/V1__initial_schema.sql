-- Initial schema for Kutu-Tipp prediction game

-- Competition status type
CREATE TYPE competition_status AS ENUM ('upcoming', 'live', 'finished');

-- Gender type
CREATE TYPE gender_type AS ENUM ('M', 'F');

-- User role type
CREATE TYPE user_role AS ENUM ('USER', 'ADMIN');

-- Competitions (Wettkämpfe)
CREATE TABLE competition
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255)             NOT NULL,
    date       TIMESTAMP WITH TIME ZONE NOT NULL,
    status     competition_status       NOT NULL DEFAULT 'upcoming',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_competition_date ON competition (date);
CREATE INDEX idx_competition_status ON competition (status);

-- Gymnasts (Turner/Turnerinnen)
CREATE TABLE gymnast
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255)             NOT NULL,
    team_name  VARCHAR(255)             NOT NULL,
    gender     gender_type              NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_gymnast_gender ON gymnast (gender);
CREATE INDEX idx_gymnast_team ON gymnast (team_name);

-- Apparatus (Geräte)
CREATE TABLE apparatus
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100)             NOT NULL,
    gender     gender_type              NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (name, gender)
);

-- Users (Benutzer/Tipper)
CREATE TABLE app_user
(
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(100)             NOT NULL UNIQUE,
    email         VARCHAR(255)             NOT NULL UNIQUE,
    password_hash VARCHAR(255)             NOT NULL,
    role          user_role                NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_username ON app_user (username);
CREATE INDEX idx_user_email ON app_user (email);
CREATE INDEX idx_user_role ON app_user (role);

-- Competition entries (Turner/Gerät Kombinationen für einen Wettkampf)
CREATE TABLE competition_entry
(
    id             BIGSERIAL PRIMARY KEY,
    competition_id BIGINT                   NOT NULL,
    gymnast_id     BIGINT                   NOT NULL,
    apparatus_id   BIGINT                   NOT NULL,
    actual_score   NUMERIC(5, 3), -- NULL until result is available, e.g. 14.500
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_competition_entry_competition FOREIGN KEY (competition_id) REFERENCES competition (id) ON DELETE CASCADE,
    CONSTRAINT fk_competition_entry_gymnast FOREIGN KEY (gymnast_id) REFERENCES gymnast (id) ON DELETE CASCADE,
    CONSTRAINT fk_competition_entry_apparatus FOREIGN KEY (apparatus_id) REFERENCES apparatus (id) ON DELETE CASCADE,
    CONSTRAINT uq_competition_gymnast_apparatus UNIQUE (competition_id, gymnast_id, apparatus_id),
    CONSTRAINT check_actual_score_positive CHECK (actual_score IS NULL OR actual_score >= 0)
);

CREATE INDEX idx_competition_entry_competition ON competition_entry (competition_id);
CREATE INDEX idx_competition_entry_gymnast ON competition_entry (gymnast_id);
CREATE INDEX idx_competition_entry_apparatus ON competition_entry (apparatus_id);

-- Predictions (Tipps)
CREATE TABLE prediction
(
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT                   NOT NULL,
    competition_entry_id BIGINT                   NOT NULL,
    predicted_score      NUMERIC(5, 3)            NOT NULL, -- e.g. 14.500
    points_earned        INTEGER,                           -- NULL until calculated, 0-3 points
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prediction_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_prediction_competition_entry FOREIGN KEY (competition_entry_id) REFERENCES competition_entry (id) ON DELETE CASCADE,
    CONSTRAINT uq_user_competition_entry UNIQUE (user_id, competition_entry_id),
    CONSTRAINT check_predicted_score_positive CHECK (predicted_score >= 0),
    CONSTRAINT check_points_earned_range CHECK (points_earned IS NULL OR (points_earned >= 0 AND points_earned <= 3))
);

CREATE INDEX idx_prediction_user ON prediction (user_id);
CREATE INDEX idx_prediction_competition_entry ON prediction (competition_entry_id);
CREATE INDEX idx_prediction_points ON prediction (points_earned);

-- Comments
COMMENT
ON TABLE competition IS 'Swiss Cup gymnastics competitions (Halbfinal, Final)';
COMMENT
ON TABLE gymnast IS 'Individual gymnasts participating in competitions';
COMMENT
ON TABLE apparatus IS 'Gymnastics apparatus by gender (Reck, Boden, etc.)';
COMMENT
ON TABLE app_user IS 'Application users who make predictions';
COMMENT
ON COLUMN app_user.role IS 'User role: USER (regular tipper) or ADMIN (can manage competitions and entries)';
COMMENT
ON TABLE competition_entry IS 'Links gymnasts to apparatus for specific competitions, stores actual scores';
COMMENT
ON TABLE prediction IS 'User predictions for competition entries with earned points';

COMMENT
ON COLUMN competition.status IS 'Competition status: upcoming (accepting predictions), live (in progress), finished (results final)';
COMMENT
ON COLUMN competition_entry.actual_score IS 'NULL until competition results are available';
COMMENT
ON COLUMN prediction.points_earned IS 'Points earned: 3 (exact), 2 (within 5%), 1 (within 10%), 0 (more than 10% off)';