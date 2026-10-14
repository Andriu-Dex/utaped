ALTER TABLE app_user ADD COLUMN username varchar(100);
ALTER TABLE app_user ADD COLUMN first_names varchar(80);
ALTER TABLE app_user ADD COLUMN last_names varchar(80);

-- Existing names remain intact: a display name cannot reliably be split into names.
WITH candidates AS (
    SELECT id, 'user-' || left(regexp_replace(lower(split_part(email,'@',1)), '[^a-z0-9._-]', '-', 'g'),55) AS base
    FROM app_user
), numbered AS (
    SELECT id, base, count(*) OVER (PARTITION BY base) AS duplicates FROM candidates
)
UPDATE app_user u SET username=n.base || CASE WHEN n.duplicates>1 THEN '-' || replace(n.id::text,'-','') ELSE '' END
FROM numbered n WHERE u.id=n.id;
-- Prefer the actual email prefix when it is valid and unambiguous.
UPDATE app_user u SET username=lower(split_part(u.email,'@',1))
WHERE lower(split_part(u.email,'@',1)) ~ '^[a-z0-9][a-z0-9._-]{0,99}$'
AND (SELECT count(*) FROM app_user other WHERE lower(split_part(other.email,'@',1))=lower(split_part(u.email,'@',1)))=1
AND NOT EXISTS (SELECT 1 FROM app_user other WHERE other.id<>u.id AND other.username=lower(split_part(u.email,'@',1)));
ALTER TABLE app_user ADD CONSTRAINT app_user_username_unique UNIQUE (username);
ALTER TABLE app_user ADD CONSTRAINT app_user_username_format CHECK (username IS NULL OR username ~ '^[a-z0-9][a-z0-9._-]{0,99}$');

CREATE TABLE login_captcha (
    id uuid PRIMARY KEY,
    session_hash char(64) NOT NULL UNIQUE,
    answer_hash char(64) NOT NULL,
    expires_at timestamptz NOT NULL
);
CREATE INDEX login_captcha_expiration ON login_captcha(expires_at);
