--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

do $$
begin

--migrate jdyna_values
ALTER TABLE jdyna_values RENAME COLUMN custompointer to custompointer_legacy_id;
ALTER TABLE jdyna_values ADD COLUMN custompointer UUID;

UPDATE jdyna_values SET custompointer = (SELECT epersongroup.uuid FROM epersongroup WHERE jdyna_values.custompointer_legacy_id = epersongroup.eperson_group_id) WHERE id in 
(select jdv.id from jdyna_values jdv join cris_rp_prop crp on jdv.id = crp.value_id join cris_rp_pdef pdef on crp.typo_id = pdef.id where rendering_id in (select id from cris_wgroup)
UNION
select jdv.id from jdyna_values jdv join cris_pj_prop crp on jdv.id = crp.value_id join cris_pj_pdef pdef on crp.typo_id = pdef.id where rendering_id in (select id from cris_wgroup)
UNION
select jdv.id from jdyna_values jdv join cris_ou_prop crp on jdv.id = crp.value_id join cris_ou_pdef pdef on crp.typo_id = pdef.id where rendering_id in (select id from cris_wgroup)
UNION
select jdv.id from jdyna_values jdv join cris_do_prop crp on jdv.id = crp.value_id join cris_do_pdef pdef on crp.typo_id = pdef.id where rendering_id in (select id from cris_wgroup)
);


UPDATE jdyna_values SET custompointer = (SELECT eperson.uuid FROM eperson WHERE jdyna_values.custompointer_legacy_id = eperson.eperson_id) WHERE id in 
(select jdv.id from jdyna_values jdv join cris_rp_prop crp on jdv.id = crp.value_id join cris_rp_pdef pdef on crp.typo_id = pdef.id where rendering_id in (select id from cris_weperson)
UNION
select jdv.id from jdyna_values jdv join cris_pj_prop crp on jdv.id = crp.value_id join cris_pj_pdef pdef on crp.typo_id = pdef.id where rendering_id in (select id from cris_weperson)
UNION
select jdv.id from jdyna_values jdv join cris_ou_prop crp on jdv.id = crp.value_id join cris_ou_pdef pdef on crp.typo_id = pdef.id where rendering_id in (select id from cris_weperson)
UNION
select jdv.id from jdyna_values jdv join cris_do_prop crp on jdv.id = crp.value_id join cris_do_pdef pdef on crp.typo_id = pdef.id where rendering_id in (select id from cris_weperson)
);

ALTER TABLE jdyna_values DROP COLUMN custompointer_legacy_id;
CREATE INDEX jdyna_values_custompointer on jdyna_values(custompointer);

exception when others then
 
    raise notice 'The transaction is in an uncommittable state. '
                     'Transaction was rolled back';
 
    raise notice 'Yo this is good! --> % %', SQLERRM, SQLSTATE;
end;
$$ language 'plpgsql';
