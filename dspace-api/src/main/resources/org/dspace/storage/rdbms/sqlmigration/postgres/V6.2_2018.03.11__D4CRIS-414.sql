--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

do $$
begin

UPDATE cris_subscription set typedef = typedef +91 WHERE typedef IN (9,10,11);
UPDATE cris_statsubscription set typedef = typedef +91 WHERE typedef IN (9,10,11);
UPDATE cris_metrics set resourcetypeid = resourcetypeid +91 WHERE resourcetypeid IN (9,10,11);

exception when others then
 
    raise notice 'The transaction is in an uncommittable state. '
                     'Transaction was rolled back';
 
    raise notice 'Yo this is good! --> % %', SQLERRM, SQLSTATE;
end;
$$ language 'plpgsql';
