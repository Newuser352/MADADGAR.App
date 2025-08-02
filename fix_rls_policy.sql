-- Fix the RLS policy for user_notification table
-- This should be run in Supabase SQL Editor

-- 1. Remove the incorrect policy
drop policy if exists "Everyone but uploader can read" on public.user_notification;

-- 2. Create the correct policy - users can read their own notifications
create policy "Users can view their own notifications"
  on public.user_notification
  for select
  using ( auth.uid() = user_id );

-- 3. Verify the policy is correctly applied
select 
    policy_name,
    policy_cmd,
    policy_expr
from pg_policies 
where tablename = 'user_notification';
