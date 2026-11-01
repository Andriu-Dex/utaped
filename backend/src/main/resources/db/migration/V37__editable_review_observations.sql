ALTER TABLE review_observation ADD COLUMN updated_at TIMESTAMPTZ;
ALTER TABLE review_observation ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE review_observation ADD COLUMN deleted_by UUID REFERENCES app_user(id);
ALTER TABLE review_observation ADD CONSTRAINT review_observation_deletion_coherence
    CHECK ((deleted_at IS NULL AND deleted_by IS NULL) OR (deleted_at IS NOT NULL AND deleted_by IS NOT NULL));

CREATE TABLE review_observation_history (
    id UUID PRIMARY KEY,
    observation_id UUID NOT NULL REFERENCES review_observation(id) ON DELETE CASCADE,
    task_id UUID NOT NULL REFERENCES review_task(id) ON DELETE CASCADE,
    actor_id UUID NOT NULL REFERENCES app_user(id),
    operation VARCHAR(10) NOT NULL CHECK (operation IN ('UPDATE', 'DELETE')),
    before_state JSONB NOT NULL,
    after_state JSONB,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX review_observation_active_idx ON review_observation(review_id) WHERE deleted_at IS NULL;
CREATE INDEX review_observation_task_active_idx ON review_observation(task_id) WHERE deleted_at IS NULL;
CREATE INDEX review_observation_history_obs_idx ON review_observation_history(observation_id);

