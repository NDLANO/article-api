CREATE TABLE conceptdata (
  id BIGSERIAL PRIMARY KEY,
  external_subject_id TEXT[],
  document JSONB
);
