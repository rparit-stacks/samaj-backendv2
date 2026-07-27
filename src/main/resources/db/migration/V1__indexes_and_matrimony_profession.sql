-- V1: searchable matrimony.profession column + hot-path indexes
-- Safe on existing Hibernate-managed schemas (IF NOT EXISTS).

ALTER TABLE samaj_matrimony_profiles
    ADD COLUMN IF NOT EXISTS profession varchar(255);

-- Backfill profession from detail_json when it looks like a JSON object.
UPDATE samaj_matrimony_profiles
SET profession = NULLIF(TRIM(detail_json::json ->> 'profession'), '')
WHERE profession IS NULL
  AND detail_json IS NOT NULL
  AND detail_json ~ '^\s*\{'
  AND detail_json::json ->> 'profession' IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_matrimony_profiles_search
    ON samaj_matrimony_profiles (status, visible_in_search, gender);

CREATE INDEX IF NOT EXISTS idx_matrimony_profiles_owner
    ON samaj_matrimony_profiles (owner_user_id);

CREATE INDEX IF NOT EXISTS idx_matrimony_profiles_city
    ON samaj_matrimony_profiles (city);

CREATE INDEX IF NOT EXISTS idx_matrimony_profiles_dob
    ON samaj_matrimony_profiles (date_of_birth);

CREATE INDEX IF NOT EXISTS idx_matrimony_profiles_profession
    ON samaj_matrimony_profiles (profession);

CREATE INDEX IF NOT EXISTS idx_matrimony_blocks_blocked
    ON samaj_matrimony_blocks (blocked_user_id);

CREATE INDEX IF NOT EXISTS idx_matrimony_interests_from
    ON samaj_matrimony_interests (from_profile_id);

CREATE INDEX IF NOT EXISTS idx_matrimony_interests_to
    ON samaj_matrimony_interests (to_profile_id);

CREATE INDEX IF NOT EXISTS idx_matrimony_interests_status
    ON samaj_matrimony_interests (status);

CREATE INDEX IF NOT EXISTS idx_notifications_user_created
    ON samaj_notifications (user_id, created_at);

CREATE INDEX IF NOT EXISTS idx_users_status
    ON samaj_users (status);

CREATE INDEX IF NOT EXISTS idx_users_role
    ON samaj_users (role);

CREATE INDEX IF NOT EXISTS idx_user_settings_directory
    ON samaj_user_settings (show_in_directory);

CREATE INDEX IF NOT EXISTS idx_directory_settings_visible
    ON samaj_directory_settings (visible);

CREATE INDEX IF NOT EXISTS idx_user_profiles_full_name
    ON samaj_user_profiles (full_name);

CREATE INDEX IF NOT EXISTS idx_user_profiles_city
    ON samaj_user_profiles (city);

CREATE INDEX IF NOT EXISTS idx_community_posts_created
    ON samaj_community_posts (created_at);

CREATE INDEX IF NOT EXISTS idx_community_posts_author
    ON samaj_community_posts (author_id);
