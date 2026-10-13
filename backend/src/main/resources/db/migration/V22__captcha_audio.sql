ALTER TABLE login_captcha ADD COLUMN audio bytea;
ALTER TABLE login_captcha ADD CONSTRAINT login_captcha_audio_size CHECK (audio IS NULL OR octet_length(audio) <= 1048576);
