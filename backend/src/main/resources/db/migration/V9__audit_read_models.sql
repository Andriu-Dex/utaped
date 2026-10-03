ALTER TABLE audit_event ADD COLUMN actor_display_name VARCHAR(160);
CREATE INDEX audit_event_target_time_idx ON audit_event(target_id, occurred_at DESC, id);
CREATE INDEX audit_event_actor_time_idx ON audit_event(actor_id, occurred_at DESC, id);
CREATE INDEX audit_event_action_time_idx ON audit_event(action, occurred_at DESC, id);
