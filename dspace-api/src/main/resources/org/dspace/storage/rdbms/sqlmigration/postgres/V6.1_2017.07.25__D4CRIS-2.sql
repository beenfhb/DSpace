--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

do $$
begin

--migrate cris_orcid
ALTER TABLE cris_orcid_history RENAME COLUMN entityid to entityid_legacy_id;
ALTER TABLE cris_orcid_history ADD COLUMN entityid UUID;

UPDATE cris_orcid_history SET entityid = (SELECT item.uuid FROM item WHERE cris_orcid_history.entityid_legacy_id = item.item_id) WHERE cris_orcid_history.typeid = 2;
UPDATE cris_orcid_history SET entityid = entityuuid::uuid WHERE cris_orcid_history.typeid = 9;
UPDATE cris_orcid_history SET entityid = entityuuid::uuid WHERE cris_orcid_history.typeid = 10;
UPDATE cris_orcid_history SET entityid = entityuuid::uuid WHERE cris_orcid_history.typeid = 11;
UPDATE cris_orcid_history SET entityid = entityuuid::uuid WHERE cris_orcid_history.typeid > 11;
DELETE FROM cris_orcid_history WHERE entityid is null;

ALTER TABLE cris_orcid_history DROP COLUMN entityid_legacy_id;
CREATE INDEX cris_orcid_history_id on cris_orcid_history(entityid);
CREATE INDEX cris_orcid_history_uuid on cris_orcid_history(entityuuid);
CREATE INDEX cris_orcid_history_id_type on cris_orcid_history(entityid, typeid);

ALTER TABLE cris_orcid_queue RENAME COLUMN entityid to entityid_legacy_id;
ALTER TABLE cris_orcid_queue ADD COLUMN entityid UUID;

UPDATE cris_orcid_queue SET entityid = (SELECT item.uuid FROM item WHERE cris_orcid_queue.entityid_legacy_id = item.item_id) WHERE cris_orcid_queue.typeid = 2;
UPDATE cris_orcid_queue SET entityid = (SELECT uuid::uuid FROM cris_rpage WHERE cris_orcid_queue.entityid_legacy_id = cris_rpage.id) WHERE cris_orcid_queue.typeid = 9;
UPDATE cris_orcid_queue SET entityid = (SELECT uuid::uuid FROM cris_project WHERE cris_orcid_queue.entityid_legacy_id = cris_project.id) WHERE cris_orcid_queue.typeid = 10;
UPDATE cris_orcid_queue SET entityid = (SELECT uuid::uuid FROM cris_orgunit WHERE cris_orcid_queue.entityid_legacy_id = cris_orgunit.id) WHERE cris_orcid_queue.typeid = 11;
UPDATE cris_orcid_queue SET entityid = (SELECT uuid::uuid FROM cris_do WHERE cris_orcid_queue.entityid_legacy_id = cris_do.id) WHERE cris_orcid_queue.typeid > 11;
DELETE FROM cris_orcid_queue WHERE entityid is null;

ALTER TABLE cris_orcid_queue DROP COLUMN entityid_legacy_id;
CREATE INDEX cris_orcid_queue_id on cris_orcid_queue(entityid);
CREATE INDEX cris_orcid_queue_id_type on cris_orcid_queue(entityid, typeid);

exception when others then
 
    raise notice 'The transaction is in an uncommittable state. '
                     'Transaction was rolled back';
 
    raise notice 'Yo this is good! --> % %', SQLERRM, SQLSTATE;
end;
$$ language 'plpgsql';
