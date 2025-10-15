-- Test data for Kutu-Tipp prediction game
-- This migration is only for testing purposes (src/test/resources)

-- ========================================
-- APPARATUS (Geräte)
-- ========================================

-- Men's apparatus (6 apparatus)
INSERT INTO apparatus (id, name, gender, created_at)
VALUES (1, 'Floor', 'M', CURRENT_TIMESTAMP),
       (2, 'Pommel Horse', 'M', CURRENT_TIMESTAMP),
       (3, 'Rings', 'M', CURRENT_TIMESTAMP),
       (4, 'Vault', 'M', CURRENT_TIMESTAMP),
       (5, 'Parallel Bars', 'M', CURRENT_TIMESTAMP),
       (6, 'High Bar', 'M', CURRENT_TIMESTAMP);

-- Women's apparatus (4 apparatus)
INSERT INTO apparatus (id, name, gender, created_at)
VALUES (7, 'Vault', 'F', CURRENT_TIMESTAMP),
       (8, 'Uneven Bars', 'F', CURRENT_TIMESTAMP),
       (9, 'Balance Beam', 'F', CURRENT_TIMESTAMP),
       (10, 'Floor', 'F', CURRENT_TIMESTAMP);

-- Reset sequence
SELECT setval('apparatus_id_seq', 10);

-- ========================================
-- COMPETITIONS
-- ========================================

INSERT INTO competition (id, name, date, status, created_at, updated_at)
VALUES
    -- Finished competition (30 days ago)
    (1, 'Swiss Cup 2024 - Halbfinal', CURRENT_TIMESTAMP - INTERVAL '30 days', 'finished', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- Live competition (started 1 hour ago)
    (2, 'Swiss Cup 2024 - Final', CURRENT_TIMESTAMP - INTERVAL '1 hour', 'live', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- Upcoming competition (in 2 days)
    (3, 'Swiss Cup 2025 - Halbfinal', CURRENT_TIMESTAMP + INTERVAL '2 days', 'upcoming', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

SELECT setval('competition_id_seq', 3);

-- ========================================
-- GYMNASTS
-- ========================================

-- Men's team from various Swiss clubs
INSERT INTO gymnast (id, name, team_name, gender, created_at, updated_at)
VALUES (1, 'Lucas Müller', 'TV Bern', 'M', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (2, 'Nils Schneider', 'TV Zürich', 'M', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (3, 'Simon Weber', 'STV Luzern', 'M', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (4, 'Marco Fischer', 'TV Bern', 'M', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (5, 'David Steiner', 'TV Basel', 'M', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (6, 'Fabian Meyer', 'TV Zürich', 'M', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Women's team from various Swiss clubs
INSERT INTO gymnast (id, name, team_name, gender, created_at, updated_at)
VALUES (7, 'Laura Moser', 'TV Bern', 'F', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (8, 'Anna Schmid', 'TV Zürich', 'F', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (9, 'Sophie Keller', 'STV Luzern', 'F', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (10, 'Emma Graf', 'TV Basel', 'F', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (11, 'Lena Hofer', 'TV Zürich', 'F', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (12, 'Mia Huber', 'TV Bern', 'F', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

SELECT setval('gymnast_id_seq', 12);

-- ========================================
-- USERS
-- ========================================

-- Password hash for 'password123' (BCrypt)
-- Note: In production, use proper password hashing!
INSERT INTO app_user (id, username, email, password_hash, role, created_at, updated_at)
VALUES
    -- Admin users
    (1, 'admin', 'admin@kututipp.ch', '$2a$12$KPFyReInvLLKfRpJQUV5Q.DeYWqVEYzYoH18c6R3syBoFlvAu369G', 'ADMIN',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'judge', 'judge@kututipp.ch', '$2a$12$KPFyReInvLLKfRpJQUV5Q.DeYWqVEYzYoH18c6R3syBoFlvAu369G', 'ADMIN',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- Regular users (tippers)
    (3, 'tipper1', 'tipper1@example.com', '$2a$12$KPFyReInvLLKfRpJQUV5Q.DeYWqVEYzYoH18c6R3syBoFlvAu369G',
     'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 'tipper2', 'tipper2@example.com', '$2a$12$KPFyReInvLLKfRpJQUV5Q.DeYWqVEYzYoH18c6R3syBoFlvAu369G',
     'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (5, 'tipper3', 'tipper3@example.com', '$2a$12$KPFyReInvLLKfRpJQUV5Q.DeYWqVEYzYoH18c6R3syBoFlvAu369G',
     'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (6, 'fan_zurich', 'fan@zurich.ch', '$2a$12$KPFyReInvLLKfRpJQUV5Q.DeYWqVEYzYoH18c6R3syBoFlvAu369G', 'USER',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (7, 'gym_expert', 'expert@gymnastics.ch', '$2a$12$KPFyReInvLLKfRpJQUV5Q.DeYWqVEYzYoH18c6R3syBoFlvAu369G',
     'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

SELECT setval('app_user_id_seq', 7);

-- ========================================
-- COMPETITION ENTRIES
-- ========================================

-- Competition 1 (Finished - Halbfinal 2024) - Men's entries with actual scores
INSERT INTO competition_entry (id, competition_id, gymnast_id, apparatus_id, actual_score, created_at, updated_at)
VALUES
    -- Lucas Müller on all men's apparatus
    (1, 1, 1, 1, 14.250, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),  -- Floor
    (2, 1, 1, 2, 13.800, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),  -- Pommel Horse
    (3, 1, 1, 3, 14.100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),  -- Rings
    (4, 1, 1, 4, 14.500, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),  -- Vault
    (5, 1, 1, 5, 13.950, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),  -- Parallel Bars
    (6, 1, 1, 6, 14.300, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),  -- High Bar

    -- Nils Schneider on selected apparatus
    (7, 1, 2, 1, 14.100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),  -- Floor
    (8, 1, 2, 3, 14.350, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),  -- Rings
    (9, 1, 2, 6, 14.600, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),  -- High Bar

    -- Simon Weber
    (10, 1, 3, 2, 13.650, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), -- Pommel Horse
    (11, 1, 3, 4, 14.200, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), -- Vault
    (12, 1, 3, 5, 14.050, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
-- Parallel Bars

-- Competition 1 (Finished) - Women's entries with actual scores
INSERT INTO competition_entry (id, competition_id, gymnast_id, apparatus_id, actual_score, created_at, updated_at)
VALUES
    -- Laura Moser
    (13, 1, 7, 7, 13.900, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),  -- Vault
    (14, 1, 7, 8, 13.550, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),  -- Uneven Bars
    (15, 1, 7, 9, 13.800, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),  -- Balance Beam
    (16, 1, 7, 10, 14.100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), -- Floor

    -- Anna Schmid
    (17, 1, 8, 7, 14.200, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),  -- Vault
    (18, 1, 8, 8, 13.850, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),  -- Uneven Bars
    (19, 1, 8, 10, 14.250, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
-- Floor

-- Competition 2 (Live - Final 2024) - Some scores available, some still pending
INSERT INTO competition_entry (id, competition_id, gymnast_id, apparatus_id, actual_score, created_at, updated_at)
VALUES
    -- Marco Fischer - some scores in
    (20, 2, 4, 1, 14.400, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), -- Floor (scored)
    (21, 2, 4, 2, 13.900, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), -- Pommel Horse (scored)
    (22, 2, 4, 3, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),   -- Rings (pending)
    (23, 2, 4, 4, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),   -- Vault (pending)
    (24, 2, 4, 5, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),   -- Parallel Bars (pending)
    (25, 2, 4, 6, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),   -- High Bar (pending)

    -- Sophie Keller - mix of scores
    (26, 2, 9, 7, 13.950, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), -- Vault (scored)
    (27, 2, 9, 8, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),   -- Uneven Bars (pending)
    (28, 2, 9, 9, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),   -- Balance Beam (pending)
    (29, 2, 9, 10, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
-- Floor (pending)

-- Competition 3 (Upcoming - Halbfinal 2025) - No scores yet
INSERT INTO competition_entry (id, competition_id, gymnast_id, apparatus_id, actual_score, created_at, updated_at)
VALUES
    -- David Steiner
    (30, 3, 5, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),  -- Floor
    (31, 3, 5, 3, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),  -- Rings
    (32, 3, 5, 5, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),  -- Parallel Bars
    (33, 3, 5, 6, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),  -- High Bar

    -- Emma Graf
    (34, 3, 10, 7, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), -- Vault
    (35, 3, 10, 8, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), -- Uneven Bars
    (36, 3, 10, 9, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), -- Balance Beam
    (37, 3, 10, 10, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP); -- Floor

SELECT setval('competition_entry_id_seq', 37);

-- ========================================
-- PREDICTIONS
-- ========================================

-- Predictions for Competition 1 (Finished)
-- NOTE: Points are now calculated on-the-fly using the calculate_points() database function
-- Predictions must be created BEFORE competition date (more than 30 minutes before)
-- Competition 1 was 30 days ago, so predictions should be at least 31 days ago
-- tipper1 - good predictions
INSERT INTO prediction (id, user_id, competition_entry_id, predicted_score, created_at, updated_at)
VALUES
    -- Lucas Müller predictions (very accurate)
    (1, 3, 1, 14.250, CURRENT_TIMESTAMP - INTERVAL '31 days', CURRENT_TIMESTAMP - INTERVAL '31 days'),  -- Floor - exact match!
    (2, 3, 2, 13.850, CURRENT_TIMESTAMP - INTERVAL '31 days', CURRENT_TIMESTAMP - INTERVAL '31 days'),  -- Pommel Horse - within 5%
    (3, 3, 3, 14.200, CURRENT_TIMESTAMP - INTERVAL '31 days', CURRENT_TIMESTAMP - INTERVAL '31 days'),  -- Rings - within 5%
    (4, 3, 4, 14.350, CURRENT_TIMESTAMP - INTERVAL '31 days', CURRENT_TIMESTAMP - INTERVAL '31 days'),  -- Vault - within 5%

    -- Laura Moser predictions
    (5, 3, 13, 13.800, CURRENT_TIMESTAMP - INTERVAL '31 days', CURRENT_TIMESTAMP - INTERVAL '31 days'), -- Vault - within 5%
    (6, 3, 14, 13.600, CURRENT_TIMESTAMP - INTERVAL '31 days', CURRENT_TIMESTAMP - INTERVAL '31 days'), -- Uneven Bars - within 5%
    (7, 3, 15, 13.900, CURRENT_TIMESTAMP - INTERVAL '31 days', CURRENT_TIMESTAMP - INTERVAL '31 days');
-- Balance Beam - within 5%

-- tipper2 - moderate predictions
INSERT INTO prediction (id, user_id, competition_entry_id, predicted_score, created_at, updated_at)
VALUES
    -- Lucas Müller predictions (moderate)
    (8, 4, 1, 13.900, CURRENT_TIMESTAMP - INTERVAL '31 days', CURRENT_TIMESTAMP - INTERVAL '31 days'),   -- Floor - within 10%
    (9, 4, 2, 14.200, CURRENT_TIMESTAMP - INTERVAL '31 days', CURRENT_TIMESTAMP - INTERVAL '31 days'),   -- Pommel Horse - within 10%
    (10, 4, 4, 13.200, CURRENT_TIMESTAMP - INTERVAL '31 days', CURRENT_TIMESTAMP - INTERVAL '31 days'),  -- Vault - within 10%

    -- Anna Schmid predictions
    (11, 4, 17, 14.100, CURRENT_TIMESTAMP - INTERVAL '31 days', CURRENT_TIMESTAMP - INTERVAL '31 days'), -- Vault - within 5%
    (12, 4, 18, 13.700, CURRENT_TIMESTAMP - INTERVAL '31 days', CURRENT_TIMESTAMP - INTERVAL '31 days'), -- Uneven Bars - within 5%
    (13, 4, 19, 14.300, CURRENT_TIMESTAMP - INTERVAL '31 days', CURRENT_TIMESTAMP - INTERVAL '31 days');
-- Floor - within 5%

-- tipper3 - mixed accuracy
INSERT INTO prediction (id, user_id, competition_entry_id, predicted_score, created_at, updated_at)
VALUES
    -- Nils Schneider predictions
    (14, 5, 7, 14.100, CURRENT_TIMESTAMP - INTERVAL '31 days', CURRENT_TIMESTAMP - INTERVAL '31 days'), -- Floor - exact!
    (15, 5, 8, 13.800, CURRENT_TIMESTAMP - INTERVAL '31 days', CURRENT_TIMESTAMP - INTERVAL '31 days'), -- Rings - off by >10%
    (16, 5, 9, 14.800, CURRENT_TIMESTAMP - INTERVAL '31 days', CURRENT_TIMESTAMP - INTERVAL '31 days'), -- High Bar - within 10%

    -- Laura Moser
    (17, 5, 16, 13.500, CURRENT_TIMESTAMP - INTERVAL '31 days', CURRENT_TIMESTAMP - INTERVAL '31 days');
-- Floor - off by >10%

-- Predictions for Competition 2 (Live) - points calculated for scored entries only
-- Competition 2 started 1 hour ago, so predictions must be at least 1 hour 30 minutes ago
INSERT INTO prediction (id, user_id, competition_entry_id, predicted_score, created_at, updated_at)
VALUES
    -- Marco Fischer predictions
    (18, 3, 20, 14.450, CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '2 hours'),    -- Floor - within 5% (scored)
    (19, 3, 21, 13.850, CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '2 hours'),    -- Pommel Horse - within 5% (scored)
    (20, 3, 22, 14.300, CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '2 hours'), -- Rings - pending
    (21, 3, 23, 14.600, CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '2 hours'), -- Vault - pending

    -- Sophie Keller
    (22, 4, 26, 14.000, CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '2 hours'),    -- Vault - within 5% (scored)
    (23, 4, 27, 13.700, CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '2 hours'), -- Uneven Bars - pending
    (24, 4, 28, 13.900, CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '2 hours');
-- Balance Beam - pending

-- Predictions for Competition 3 (Upcoming) - no actual scores yet
-- Competition 3 is in 2 days, so predictions can be made now (but not too close to competition time)
INSERT INTO prediction (id, user_id, competition_entry_id, predicted_score, created_at, updated_at)
VALUES
    -- David Steiner predictions (all pending)
    (25, 3, 30, 14.500, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), -- Floor
    (26, 3, 31, 14.200, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), -- Rings
    (27, 3, 32, 14.100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), -- Parallel Bars

    (28, 5, 30, 14.300, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), -- Floor
    (29, 5, 33, 14.450, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), -- High Bar

    -- Emma Graf predictions
    (30, 4, 34, 13.800, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), -- Vault
    (31, 4, 35, 13.600, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), -- Uneven Bars
    (32, 4, 36, 13.750, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), -- Balance Beam
    (33, 4, 37, 14.000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
-- Floor

-- fan_zurich - predictions
INSERT INTO prediction (id, user_id, competition_entry_id, predicted_score, created_at, updated_at)
VALUES
    -- Competition 1 (predictions made 31 days ago)
    (34, 6, 7, 14.150, CURRENT_TIMESTAMP - INTERVAL '31 days', CURRENT_TIMESTAMP - INTERVAL '31 days'),  -- Nils Floor - within 5%
    (35, 6, 17, 14.200, CURRENT_TIMESTAMP - INTERVAL '31 days', CURRENT_TIMESTAMP - INTERVAL '31 days'), -- Anna Vault - exact!

    -- Competition 2 (predictions made 2 hours ago)
    (36, 6, 20, 14.200, CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '2 hours');
-- Marco Floor - within 10%

-- gym_expert - predictions
INSERT INTO prediction (id, user_id, competition_entry_id, predicted_score, created_at, updated_at)
VALUES
    -- Competition 1 - expert predictions (very accurate, made 31 days ago)
    (37, 7, 1, 14.250, CURRENT_TIMESTAMP - INTERVAL '31 days', CURRENT_TIMESTAMP - INTERVAL '31 days'),  -- Lucas Floor - exact!
    (38, 7, 7, 14.100, CURRENT_TIMESTAMP - INTERVAL '31 days', CURRENT_TIMESTAMP - INTERVAL '31 days'),  -- Nils Floor - exact!
    (39, 7, 13, 13.900, CURRENT_TIMESTAMP - INTERVAL '31 days', CURRENT_TIMESTAMP - INTERVAL '31 days'), -- Laura Vault - exact!
    (40, 7, 17, 14.200, CURRENT_TIMESTAMP - INTERVAL '31 days', CURRENT_TIMESTAMP - INTERVAL '31 days'); -- Anna Vault - exact!

SELECT setval('prediction_id_seq', 40);

-- ========================================
-- SUMMARY COMMENTS
-- ========================================

-- Test data summary:
-- - 10 apparatus (6 men's, 4 women's)
-- - 3 competitions (finished, live, upcoming)
-- - 12 gymnasts (6 men, 6 women) from 4 teams
-- - 7 users (2 admins, 5 regular users)
-- - 37 competition entries across all competitions
-- - 40 predictions with varying accuracy levels
--
-- Point distribution (calculated on-the-fly):
-- - 3 points (exact): 7 predictions
-- - 2 points (within 5%): 17 predictions
-- - 1 point (within 10%): 5 predictions
-- - 0 points (>10% off): 3 predictions
-- - Not yet calculated (no actual score): 8 predictions
