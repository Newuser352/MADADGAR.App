-- Migration: Add latitude and longitude columns to items table
-- Description: Enable location-based features by adding coordinate fields

-- Add latitude column
ALTER TABLE items 
ADD COLUMN IF NOT EXISTS latitude FLOAT8;

-- Add longitude column  
ALTER TABLE items 
ADD COLUMN IF NOT EXISTS longitude FLOAT8;

-- Add comments for documentation
COMMENT ON COLUMN items.latitude IS 'Latitude coordinate for item location';
COMMENT ON COLUMN items.longitude IS 'Longitude coordinate for item location';

-- Optional: Add index for location-based queries (for better performance)
CREATE INDEX IF NOT EXISTS idx_items_location ON items (latitude, longitude);

-- Optional: Add constraint to ensure valid latitude range (-90 to 90)
ALTER TABLE items 
ADD CONSTRAINT IF NOT EXISTS chk_latitude_range 
CHECK (latitude IS NULL OR (latitude >= -90 AND latitude <= 90));

-- Optional: Add constraint to ensure valid longitude range (-180 to 180)
ALTER TABLE items 
ADD CONSTRAINT IF NOT EXISTS chk_longitude_range 
CHECK (longitude IS NULL OR (longitude >= -180 AND longitude <= 180));
