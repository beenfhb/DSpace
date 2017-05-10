--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

ALTER TABLE Subscription RENAME COLUMN community_id to community_legacy_id;
ALTER TABLE Subscription ADD community_id RAW(16) REFERENCES Community(uuid);
CREATE INDEX Subscription_community on Subscription(community_id);
UPDATE Subscription SET community_id = (SELECT community.uuid FROM community WHERE Subscription.community_legacy_id = community.community_id);
ALTER TABLE Subscription DROP COLUMN community_legacy_id;

ALTER TABLE cris_rpage RENAME COLUMN epersonid to epersonid_legacy_id;
ALTER TABLE cris_rpage ADD COLUMN epersonid RAW(16);
UPDATE cris_rpage SET epersonid = (SELECT eperson.uuid FROM eperson WHERE cris_rpage.epersonid_legacy_id = eperson.eperson_id);
ALTER TABLE cris_rpage DROP COLUMN epersonid_legacy_id;

ALTER TABLE cris_relpref RENAME COLUMN itemid to itemid_legacy_id;
ALTER TABLE cris_relpref ADD COLUMN itemid RAW(16);
UPDATE cris_relpref SET itemid = (SELECT item.uuid FROM item WHERE cris_relpref.itemid_legacy_id = item.item_id);
ALTER TABLE cris_relpref DROP COLUMN itemid_legacy_id;

ALTER TABLE cris_relpref RENAME COLUMN itemid to itemid_legacy_id;
ALTER TABLE cris_relpref ADD COLUMN itemid RAW(16);
UPDATE cris_relpref SET itemid = (SELECT item.uuid FROM item WHERE cris_relpref.itemid_legacy_id = item.item_id);
ALTER TABLE cris_relpref DROP COLUMN itemid_legacy_id;
CREATE INDEX cris_relpref_itemid on cris_relpref(itemid);

ALTER TABLE cris_subscription RENAME COLUMN epersonid to epersonid_legacy_id;
ALTER TABLE cris_subscription ADD COLUMN epersonid RAW(16);
UPDATE cris_subscription SET epersonid = (SELECT eperson.uuid FROM eperson WHERE cris_subscription.epersonid_legacy_id = eperson.eperson_id);
ALTER TABLE cris_subscription DROP COLUMN epersonid_legacy_id;

ALTER TABLE cris_statsubscription RENAME COLUMN epersonid to epersonid_legacy_id;
ALTER TABLE cris_statsubscription ADD COLUMN epersonid RAW(16);
UPDATE cris_statsubscription SET epersonid = (SELECT eperson.uuid FROM eperson WHERE cris_statsubscription.epersonid_legacy_id = eperson.eperson_id);
ALTER TABLE cris_statsubscription DROP COLUMN epersonid_legacy_id;
