WITH normalized_emails AS (
    SELECT
        id,
        lower(btrim(email)) AS normalized_email,
        row_number() OVER (
            PARTITION BY lower(btrim(email))
            ORDER BY id
        ) AS duplicate_rank
    FROM users
    WHERE email IS NOT NULL
      AND btrim(email) <> ''
)
UPDATE users u
SET email = CASE
    WHEN normalized_emails.duplicate_rank = 1 THEN normalized_emails.normalized_email
    ELSE NULL
END
FROM normalized_emails
WHERE u.id = normalized_emails.id;

UPDATE users
SET email = NULL
WHERE email IS NOT NULL
  AND btrim(email) = '';

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email_lower_unique
    ON users ((lower(email)))
    WHERE email IS NOT NULL;
