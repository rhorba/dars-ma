-- Additive columns needed for admin review (not in the original DB doc schema):
-- mime_type lets the admin UI render/download the document correctly;
-- original_filename is stored for display only, never trusted for storage paths.
ALTER TABLE verification_documents ADD COLUMN mime_type VARCHAR(100) NOT NULL DEFAULT 'application/octet-stream';
ALTER TABLE verification_documents ADD COLUMN original_filename VARCHAR(255);
ALTER TABLE verification_documents ALTER COLUMN mime_type DROP DEFAULT;
