ALTER TABLE activity_catalog ADD COLUMN context jsonb;
CREATE UNIQUE INDEX activity_catalog_source_key ON activity_catalog(group_id,(context->>'sourceKey'),(COALESCE(context->>'periodId','')),(COALESCE(context->>'careerId',''))) WHERE context->>'sourceKey' IS NOT NULL;
