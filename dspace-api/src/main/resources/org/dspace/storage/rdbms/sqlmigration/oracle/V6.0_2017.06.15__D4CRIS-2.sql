--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- migrate doi2item	
ALTER TABLE doi2item RENAME COLUMN eperson_id to eperson_id_legacy_id;
ALTER TABLE doi2item ADD eperson_id RAW(16);
UPDATE doi2item SET eperson_id = (SELECT eperson.uuid FROM eperson WHERE doi2item.eperson_id_legacy_id = eperson.eperson_id);
ALTER TABLE doi2item DROP COLUMN eperson_id_legacy_id;
CREATE INDEX doi2item_eperson on doi2item(eperson_id);

ALTER TABLE doi2item RENAME COLUMN item_id to item_id_legacy_id;
ALTER TABLE doi2item ADD item_id RAW(16);
UPDATE doi2item SET item_id = (SELECT item.uuid FROM item WHERE doi2item.item_id_legacy_id = item.item_id);
ALTER TABLE doi2item DROP COLUMN item_id_legacy_id;
CREATE INDEX doi2item_item on doi2item(item_id);
