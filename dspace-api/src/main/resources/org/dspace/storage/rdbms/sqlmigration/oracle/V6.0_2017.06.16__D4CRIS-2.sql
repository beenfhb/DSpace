--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

--migrate cris_metrics
ALTER TABLE cris_metrics RENAME COLUMN resourceid to resourceid_legacy_id;
ALTER TABLE cris_metrics ADD resourceid RAW(36);
ALTER TABLE CRIS_RPAGE MODIFY (uuid VARCHAR2(36) );
ALTER TABLE CRIS_PROJECT MODIFY (uuid VARCHAR2(36) );
ALTER TABLE CRIS_ORGUNIT MODIFY (uuid VARCHAR2(36) );
ALTER TABLE CRIS_DO MODIFY (uuid VARCHAR2(36) );

UPDATE cris_metrics SET resourceid = (SELECT bitstream.uuid FROM bitstream WHERE cris_metrics.resourceid_legacy_id = bitstream.bitstream_id) WHERE cris_metrics.resourcetypeid = 0;
UPDATE cris_metrics SET resourceid = (SELECT bundle.uuid FROM bundle WHERE cris_metrics.resourceid_legacy_id = bundle.bundle_id) WHERE cris_metrics.resourcetypeid = 1;
UPDATE cris_metrics SET resourceid = (SELECT item.uuid FROM item WHERE cris_metrics.resourceid_legacy_id = item.item_id) WHERE cris_metrics.resourcetypeid = 2;
UPDATE cris_metrics SET resourceid = (SELECT collection.uuid FROM collection WHERE cris_metrics.resourceid_legacy_id = collection.collection_id) WHERE cris_metrics.resourcetypeid = 3;
UPDATE cris_metrics SET resourceid = (SELECT community.uuid FROM community WHERE cris_metrics.resourceid_legacy_id = community.community_id) WHERE cris_metrics.resourcetypeid = 4;
UPDATE cris_metrics SET resourceid = (SELECT epersongroup.uuid FROM epersongroup WHERE cris_metrics.resourceid_legacy_id = epersongroup.eperson_group_id) WHERE cris_metrics.resourcetypeid = 6;
UPDATE cris_metrics SET resourceid = (SELECT eperson.uuid FROM eperson WHERE cris_metrics.resourceid_legacy_id = eperson.eperson_id) WHERE cris_metrics.resourcetypeid = 7;
UPDATE cris_metrics SET resourceid = utl_raw.cast_to_raw(uuid) WHERE cris_metrics.resourcetypeid = 9;
UPDATE cris_metrics SET resourceid = utl_raw.cast_to_raw(uuid) WHERE cris_metrics.resourcetypeid = 10;
UPDATE cris_metrics SET resourceid = utl_raw.cast_to_raw(uuid) WHERE cris_metrics.resourcetypeid = 11;
UPDATE cris_metrics SET resourceid = utl_raw.cast_to_raw(uuid) WHERE cris_metrics.resourcetypeid > 11;
DELETE FROM cris_metrics WHERE resourceid is null;

ALTER TABLE cris_metrics DROP COLUMN resourceid_legacy_id;
CREATE INDEX cris_metrics_resourceid on cris_metrics(resourceid);
CREATE INDEX cris_metrics_uuid on cris_metrics(uuid);
CREATE INDEX cris_metrics_resourceid_resourcetypid on cris_metrics(resourceid, resourcetypeid);
