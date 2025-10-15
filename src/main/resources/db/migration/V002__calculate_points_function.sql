-- Create function to calculate points based on prediction accuracy
-- Implements the same logic as the previous Java implementation:
-- - Exact match (< 0.001 difference): 3 points
-- - Within 5% deviation: 2 points
-- - Within 10% deviation: 1 point
-- - More than 10% deviation: 0 points

CREATE OR REPLACE FUNCTION calculate_points(
    predicted NUMERIC(5, 3),
    actual NUMERIC(5, 3)
) RETURNS INTEGER AS
$$
DECLARE
    difference    NUMERIC;
    percentage    NUMERIC;
    min_score     CONSTANT NUMERIC := 0.0;
    max_score     CONSTANT NUMERIC := 20.0;
    exact_threshold CONSTANT NUMERIC := 0.001;
BEGIN
    -- Validate scores are within valid range (BR-003)
    IF actual IS NULL OR predicted IS NULL THEN
        RETURN 0;
    END IF;

    IF actual < min_score OR actual > max_score OR predicted < min_score OR predicted > max_score THEN
        RETURN 0;
    END IF;

    -- Handle edge case where actual score is zero
    IF actual < exact_threshold THEN
        -- If actual is essentially zero, only exact match gets points
        IF ABS(predicted - actual) < exact_threshold THEN
            RETURN 3;
        ELSE
            RETURN 0;
        END IF;
    END IF;

    -- Calculate absolute difference
    difference := ABS(predicted - actual);

    -- Check for exact match (BR-001)
    IF difference < exact_threshold THEN
        RETURN 3;
    END IF;

    -- Calculate percentage deviation
    percentage := (difference / actual) * 100.0;

    -- Award points based on deviation thresholds (BR-001)
    IF percentage <= 5.0 THEN
        RETURN 2;
    ELSIF percentage <= 10.0 THEN
        RETURN 1;
    ELSE
        RETURN 0;
    END IF;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Add comment explaining the function
COMMENT ON FUNCTION calculate_points(NUMERIC, NUMERIC) IS
    'Calculates prediction points: 3 (exact), 2 (within 5%), 1 (within 10%), 0 (more than 10% off)';

-- Drop the points_earned column and its index since points are now calculated on-the-fly
DROP INDEX IF EXISTS idx_prediction_points;
ALTER TABLE prediction DROP COLUMN IF EXISTS points_earned;

-- Update table comment to reflect the change
COMMENT ON TABLE prediction IS 'User predictions for competition entries. Points are calculated on-the-fly using calculate_points() function.';
