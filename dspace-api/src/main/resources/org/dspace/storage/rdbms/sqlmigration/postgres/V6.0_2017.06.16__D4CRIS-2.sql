--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

do $$
begin

--migrate cris_metrics
ALTER TABLE cris_metrics RENAME COLUMN resourceid to resourceid_legacy_id;
ALTER TABLE cris_metrics ADD COLUMN resourceid UUID;
UPDATE cris_metrics SET resourceid = (SELECT bitstream.uuid FROM bitstream WHERE cris_metrics.resourceid_legacy_id = bitstream.bitstream_id) WHERE cris_metrics.resourcetypeid = 0;
UPDATE cris_metrics SET resourceid = (SELECT bundle.uuid FROM bundle WHERE cris_metrics.resourceid_legacy_id = bundle.bundle_id) WHERE cris_metrics.resourcetypeid = 1;
UPDATE cris_metrics SET resourceid = (SELECT item.uuid FROM item WHERE cris_metrics.resourceid_legacy_id = item.item_id) WHERE cris_metrics.resourcetypeid = 2;
UPDATE cris_metrics SET resourceid = (SELECT collection.uuid FROM collection WHERE cris_metrics.resourceid_legacy_id = collection.eperson_id) WHERE cris_metrics.resourcetypeid = 3;
UPDATE cris_metrics SET resourceid = (SELECT community.uuid FROM community WHERE cris_metrics.resourceid_legacy_id = community.community_id) WHERE cris_metrics.resourcetypeid = 4;
UPDATE cris_metrics SET resourceid = (SELECT site.uuid FROM site WHERE cris_metrics.resourceid_legacy_id = site.site_id) WHERE cris_metrics.resourcetypeid = 5;
UPDATE cris_metrics SET resourceid = (SELECT epersongroup.uuid FROM epersongroup WHERE cris_metrics.resourceid_legacy_id = epersongroup.epersongroup_id) WHERE cris_metrics.resourcetypeid = 6;
UPDATE cris_metrics SET resourceid = (SELECT eperson.uuid FROM eperson WHERE cris_metrics.resourceid_legacy_id = eperson.eperson_id) WHERE cris_metrics.resourcetypeid = 7;
UPDATE cris_metrics SET resourceid = (SELECT cris_rpage.uuid FROM cris_rpage WHERE cris_metrics.resourceid_legacy_id = cris_rpage.id)::uuid WHERE cris_metrics.resourcetypeid = 9;
UPDATE cris_metrics SET resourceid = (SELECT cris_project.uuid FROM cris_project WHERE cris_metrics.resourceid_legacy_id = cris_project.id)::uuid WHERE cris_metrics.resourcetypeid = 10;
UPDATE cris_metrics SET resourceid = (SELECT cris_orgunit.uuid FROM cris_orgunit WHERE cris_metrics.resourceid_legacy_id = cris_orgunit.id)::uuid WHERE cris_metrics.resourcetypeid = 11;
UPDATE cris_metrics SET resourceid = (SELECT cris_do.uuid FROM cris_do WHERE cris_metrics.resourceid_legacy_id = cris_do.id)::uuid WHERE cris_metrics.resourcetypeid > 11;

ALTER TABLE cris_metrics DROP COLUMN resourceid_legacy_id;
CREATE INDEX cris_metrics_resourceid on doi2item(resourceid);
CREATE INDEX cris_metrics_uuid on doi2item(uuid);
CREATE INDEX cris_metrics_resourceid_resourcetypid on doi2item(resourceid, resourcetypeid);


exception when others then
 
    raise notice 'The transaction is in an uncommittable state. '
                     'Transaction was rolled back';
 
    raise notice 'Yo this is good! --> % %', SQLERRM, SQLSTATE;
end;
$$ language 'plpgsql';
