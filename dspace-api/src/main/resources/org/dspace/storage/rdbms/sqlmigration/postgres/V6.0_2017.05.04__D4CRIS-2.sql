--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

do $$
begin
	
-- migrate cris_rpage	
ALTER TABLE cris_rpage RENAME COLUMN epersonid to epersonid_legacy_id;
ALTER TABLE cris_rpage ADD COLUMN epersonid UUID;
UPDATE cris_rpage SET epersonid = (SELECT eperson.uuid FROM eperson WHERE cris_rpage.epersonid_legacy_id = eperson.eperson_id);
ALTER TABLE cris_rpage DROP COLUMN epersonid_legacy_id;
CREATE INDEX cris_rpage_eperson on cris_rpage(epersonid);

-- migrate reletionpref
ALTER TABLE cris_relpref RENAME COLUMN itemid to itemid_legacy_id;
ALTER TABLE cris_relpref ADD COLUMN itemid UUID;
UPDATE cris_relpref SET itemid = (SELECT item.uuid FROM item WHERE cris_relpref.itemid_legacy_id = item.item_id);
ALTER TABLE cris_relpref DROP COLUMN itemid_legacy_id;
CREATE INDEX cris_relpref_itemid on cris_relpref(itemid);

-- migrate subscription
ALTER TABLE Subscription RENAME COLUMN community_id to community_legacy_id;
ALTER TABLE Subscription ADD COLUMN community_id UUID REFERENCES Community(uuid);
UPDATE Subscription SET community_id = (SELECT community.uuid FROM community WHERE Subscription.community_legacy_id = community.community_id);
ALTER TABLE Subscription DROP COLUMN community_legacy_id;
CREATE INDEX Subscription_community on Subscription(community_id);

ALTER TABLE cris_subscription RENAME COLUMN epersonid to epersonid_legacy_id;
ALTER TABLE cris_subscription ADD COLUMN epersonid UUID;
UPDATE cris_subscription SET epersonid = (SELECT eperson.uuid FROM eperson WHERE cris_subscription.epersonid_legacy_id = eperson.eperson_id);
ALTER TABLE cris_subscription DROP COLUMN epersonid_legacy_id;
CREATE INDEX cris_subscription_epersonid on cris_subscription(epersonid);

ALTER TABLE cris_statsubscription RENAME COLUMN epersonid to epersonid_legacy_id;
ALTER TABLE cris_statsubscription ADD COLUMN epersonid UUID;
UPDATE cris_statsubscription SET epersonid = (SELECT eperson.uuid FROM eperson WHERE cris_statsubscription.epersonid_legacy_id = eperson.eperson_id);
ALTER TABLE cris_statsubscription DROP COLUMN epersonid_legacy_id;
CREATE INDEX cris_statsubscription_epersonid on cris_statsubscription(epersonid);

-- migrate deduplication
DELETE FROM dedup_reject WHERE first_item_id is null OR second_item_id is null;
ALTER TABLE dedup_reject RENAME TO cris_deduplication;
ALTER SEQUENCE dedup_reject_seq RENAME TO cris_deduplication_seq;

ALTER TABLE cris_deduplication RENAME COLUMN dedup_reject_id to id;
ALTER TABLE cris_deduplication RENAME COLUMN eperson_id to eperson_legacy_id;
ALTER TABLE cris_deduplication RENAME COLUMN admin_id to admin_legacy_id;
ALTER TABLE cris_deduplication RENAME COLUMN reader_id to reader_legacy_id;
ALTER TABLE cris_deduplication RENAME COLUMN first_item_id to first_item_legacy_id;
ALTER TABLE cris_deduplication RENAME COLUMN second_item_id to second_item_legacy_id;

ALTER TABLE cris_deduplication ADD COLUMN eperson_id UUID;
ALTER TABLE cris_deduplication ADD COLUMN admin_id UUID;
ALTER TABLE cris_deduplication ADD COLUMN reader_id UUID;
ALTER TABLE cris_deduplication ADD COLUMN first_item_id varchar(255);
ALTER TABLE cris_deduplication ADD COLUMN second_item_id varchar(255);

UPDATE cris_deduplication SET eperson_id = (SELECT eperson.uuid FROM eperson WHERE cris_deduplication.eperson_legacy_id = eperson.eperson_id);
UPDATE cris_deduplication SET admin_id = (SELECT eperson.uuid FROM eperson WHERE cris_deduplication.admin_legacy_id = eperson.eperson_id);
UPDATE cris_deduplication SET reader_id = (SELECT eperson.uuid FROM eperson WHERE cris_deduplication.reader_legacy_id = eperson.eperson_id);
UPDATE cris_deduplication SET first_item_id = (SELECT item.uuid FROM item WHERE cris_deduplication.first_item_legacy_id = item.item_id);
UPDATE cris_deduplication SET second_item_id = (SELECT item.uuid FROM item WHERE cris_deduplication.second_item_legacy_id = item.item_id);
ALTER TABLE cris_deduplication DROP COLUMN eperson_legacy_id;
ALTER TABLE cris_deduplication DROP COLUMN admin_legacy_id;
ALTER TABLE cris_deduplication DROP COLUMN reader_legacy_id;
ALTER TABLE cris_deduplication DROP COLUMN first_item_legacy_id;
ALTER TABLE cris_deduplication DROP COLUMN second_item_legacy_id;


exception when others then
 
    raise notice 'The transaction is in an uncommittable state. '
                     'Transaction was rolled back';
 
    raise notice 'Yo this is good! --> % %', SQLERRM, SQLSTATE;
end;
$$ language 'plpgsql';
