-- Database trigger to prevent users from receiving notifications about their own posts
-- This trigger checks if the notification is for a post created by the same user
-- and prevents the insertion if it is.

-- First, create a function that will be called by the trigger
CREATE OR REPLACE FUNCTION prevent_self_notifications()
RETURNS TRIGGER AS $$
BEGIN
    -- Check if this is a new_listing notification
    IF NEW.type = 'new_listing' THEN
        -- Extract the uploader_id from the payload JSON
        IF NEW.payload ? 'uploader_id' THEN
            -- Compare the notification recipient (user_id) with the uploader_id from payload
            IF NEW.user_id::text = (NEW.payload->>'uploader_id') THEN
                -- Log the prevention for debugging
                RAISE NOTICE 'Preventing self-notification: user_id=%, uploader_id=%', 
                    NEW.user_id, (NEW.payload->>'uploader_id');
                
                -- Return NULL to prevent the insertion
                RETURN NULL;
            END IF;
        END IF;
    END IF;
    
    -- For all other cases, allow the insertion
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Drop the trigger if it exists
DROP TRIGGER IF EXISTS prevent_self_notifications_trigger ON public.user_notifications;

-- Create the trigger that calls our function before each INSERT
CREATE TRIGGER prevent_self_notifications_trigger
    BEFORE INSERT ON public.user_notifications
    FOR EACH ROW
    EXECUTE FUNCTION prevent_self_notifications();

-- Add a comment to document the trigger
COMMENT ON FUNCTION prevent_self_notifications() IS 'Prevents users from receiving notifications about their own posts by checking uploader_id in payload';

-- Test the trigger with some sample data (optional - you can remove this section)
/*
-- This should be inserted (different user)
INSERT INTO public.user_notifications (user_id, type, title, body, payload)
VALUES (
    '11111111-1111-1111-1111-111111111111'::uuid,
    'new_listing',
    'Test Notification',
    'This should be inserted',
    '{"uploader_id": "22222222-2222-2222-2222-222222222222", "item_id": "123"}'::jsonb
);

-- This should be blocked (same user)
INSERT INTO public.user_notifications (user_id, type, title, body, payload)
VALUES (
    '11111111-1111-1111-1111-111111111111'::uuid,
    'new_listing',
    'Self Notification',
    'This should be blocked',
    '{"uploader_id": "11111111-1111-1111-1111-111111111111", "item_id": "456"}'::jsonb
);

-- Check the results
SELECT * FROM public.user_notifications WHERE title IN ('Test Notification', 'Self Notification');
*/
