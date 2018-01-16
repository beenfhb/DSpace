--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

ALTER TABLE subscription ADD community_id INTEGER;
-- The follow line broke upgrade to a fresh DSpace 7 to a DSpace-CRIS 7
-- ALTER TABLE subscription ADD CONSTRAINT subscription_community_id FOREIGN KEY (community_id) REFERENCES community;