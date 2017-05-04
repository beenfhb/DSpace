--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

ALTER TABLE Subscription RENAME COLUMN community_id to community_legacy_id;
ALTER TABLE Subscription ADD COLUMN community_id UUID REFERENCES Community(uuid);
UPDATE Subscription SET community_id = (SELECT community.uuid FROM community WHERE Subscription.community_legacy_id = community.community_id);
ALTER TABLE Subscription DROP COLUMN community_legacy_id;
CREATE INDEX Subscription_community on Subscription(community_id);