--Procedura
-- si parte da un  database vuoto!!
-- dopo bisognerà integrare le tabelle metadatavalue, item, metadatafieldregistry  create da questa procedura nel db completo.

-- crera tabella metadatavalue_ts
CREATE TABLE metadatavalue_ts
(
  metadata_value_id integer NOT NULL
  resource_id integer NOT NULL,
  metadata_field_id integer,
  text_value text,
  text_lang character varying(24),
  place integer,
  authority character varying(100),
  confidence integer DEFAULT (-1),
  resource_type_id integer NOT NULL,
  CONSTRAINT metadatavalue_pkey PRIMARY KEY (metadata_value_id),

)
WITH (
  OIDS=FALSE
);
ALTER TABLE metadatavalue
  OWNER TO dspace;


CREATE INDEX metadatavalue_field_fk_idx
  ON metadatavalue
  USING btree
  (metadata_field_id);


CREATE INDEX metadatavalue_item_idx
  ON ir.metadatavalue
  USING btree
  (resource_id);

CREATE INDEX metadatavalue_item_idx2
  ON ir.metadatavalue
  USING btree
  (resource_id, metadata_field_id);


-- eseguire i due kettle meta_trieste e meta_udine (importano in metadatavalue_ts)
-- ci mettono poco: una mezz'oretta

-- controllare con queste due query:
--meta trieste
select count(*) 
from metadatavalue_ts m
where  m.metadata_value_id<=2833585
;
--1173793

--meta udine
select count(*) 
from metadatavalue_ts m
where  m.metadata_value_id>2833585
;
--1211383

-- creare le tabell item_ts, item_ud, e item:
-- (NB: item_ts e item_ud non hanno boolean ma integer)

CREATE TABLE item
(
  item_id integer NOT NULL,
  submitter_id integer,
  in_archive boolean,
  withdrawn boolean,
  last_modified timestamp with time zone,
  owning_collection integer,
  discoverable boolean,
  CONSTRAINT item_pkey PRIMARY KEY (item_id)
  
)
WITH (
  OIDS=FALSE
);
ALTER TABLE item
  OWNER TO dspace;

CREATE INDEX item_submitter_fk_idx
  ON item
  USING btree
  (submitter_id);

CREATE  TABLE item_ts
(
  item_id integer NOT NULL,
  submitter_id integer,
  in_archive integer,
  withdrawn integer,
  last_modified timestamp with time zone,
  owning_collection integer,
  discoverable integer,
  CONSTRAINT item_pkey_ts PRIMARY KEY (item_id)

)
WITH (
  OIDS=FALSE
);
ALTER TABLE item_ts
  OWNER TO dspace;

CREATE  TABLE item_ud
(
  item_id integer NOT NULL,
  submitter_id integer,
  in_archive integer,
  withdrawn integer,
  last_modified timestamp with time zone,
  owning_collection integer,
  discoverable integer,
  CONSTRAINT item_pkey_ud PRIMARY KEY (item_id)

)
WITH (
  OIDS=FALSE
);
ALTER TABLE item_ud
  OWNER TO dspace;

delete from item_ts;
delete from item_ts;

-- eseguire kettle item trieste e item udine

-- controllare con queste due query:
--item trieste
select count(*)
from item
where item_id<=62406;
-- 54757

--item udine
select count(*)
from item
where item_id>62406;
-- 51292


    
--riversare item_ts e item_ud in item 
--(NB: qui viene trasformato l' integer in boolean)

delete from item;
insert into item
select item_id, submitter_id, in_archive=1, withdrawn=1, last_modified, 
            owning_collection, discoverable=1
from item_ts;            
insert into item
select item_id, submitter_id, in_archive=1, withdrawn=1, last_modified, 
            owning_collection, discoverable=1
from item_ud;      


-- creare  le tabelle metadatafieldregistry_ts e metadatafieldregistry_ud:
CREATE TABLE metadatafieldregistry_ts
(
  metadata_field_id integer NOT NULL, 
  metadata_schema_id integer NOT NULL,
  element character varying(64),
  qualifier character varying(64),
  scope_note text,
  CONSTRAINT metadatafieldregistry_pkeyts PRIMARY KEY (metadata_field_id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE metadatafieldregistry_ts
  OWNER TO dspace;

CREATE INDEX metadatafield_schema_idx_ts
  ON metadatafieldregistry_ts
  USING btree
  (metadata_schema_id);

CREATE TABLE metadatafieldregistry_ud
(
  metadata_field_id integer NOT NULL, 
  metadata_schema_id integer NOT NULL,
  element character varying(64),
  qualifier character varying(64),
  scope_note text,
  CONSTRAINT metadatafieldregistry_pkeyud PRIMARY KEY (metadata_field_id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE metadatafieldregistry_ud
  OWNER TO dspace;

CREATE INDEX metadatafield_schema_idx_ud
  ON metadatafieldregistry_ud
  USING btree
  (metadata_schema_id);


-- creare la seq metadatafieldregistry_seq e la tabella metadatafieldregistry

CREATE SEQUENCE metadatafieldregistry_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 142
  CACHE 1;
ALTER TABLE metadatafieldregistry_seq
  OWNER TO dspace;



CREATE TABLE metadatafieldregistry
(
  metadata_field_id integer NOT NULL DEFAULT nextval('metadatafieldregistry_seq'::regclass),
  metadata_schema_id integer NOT NULL,
  element character varying(64),
  qualifier character varying(64),
  scope_note text,
  CONSTRAINT metadatafieldregistry_pkey PRIMARY KEY (metadata_field_id)

)
WITH (
  OIDS=FALSE
);
ALTER TABLE metadatafieldregistry
  OWNER TO dspace;

X ir.metadatafield_schema_idx;

CREATE INDEX metadatafield_schema_idx
  ON metadatafieldregistry
  USING btree
  (metadata_schema_id);


-- importare i registry:

delete from metadatafieldregistry;
delete from metadatafieldregistry_ts;
delete from metadatafieldregistry_ud;

-- eseguire gli script sql registry-trieste.sql e registry-udine.sql

-- fondere i registry
select setval('metadatafieldregistry_seq',max(metadata_field_id)+1) from metadatafieldregistry where metadata_field_id<8000;

insert into metadatafieldregistry select * from metadatafieldregistry_ts;
insert into metadatafieldregistry 
select nextval('metadatafieldregistry_seq'),ud.metadata_schema_id,ud.element,ud.qualifier,ud.scope_note
from metadatafieldregistry_ud ud
left join  metadatafieldregistry_ts ts on 
(ts.element=ud.element or (ts.element is null and ud.element is null)) and (ts.qualifier=ud.qualifier or (ts.qualifier is null and ud.qualifier is null)) 
where ts.metadata_field_id is null
;
-- Ps l' ultima insert non inserta niente :) 

-- aggiornare il metadata_field_id dei metadata

update metadatavalue_ts m
set metadata_field_id = ud.metadata_field_id
from metadatafieldregistry_ts ts,metadatafieldregistry_ud ud
where m.resource_id>62406 and
      ud.metadata_field_id=m.metadata_field_id and
     (ts.element=ud.element or (ts.element is null and ud.element is null)) and (ts.qualifier=ud.qualifier or (ts.qualifier is null and ud.qualifier is null)) 
;


-- TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO 

--rinomina metadatavalue_ts in metadatavalue
-- spostare le tabelle  metadatavalue, item, metadatafieldregistry nel db completo
--(copiare solo i dati o spostare le tabelle???)
-- NB: se sposti la atbelle bisogna alterarle perché quelle create qui mancano di alcuni vincoli

-- alter metadatavalue per mettare questo default sulla colonna metadata_value_id di metadatavlaue
 DEFAULT nextval('metadatavalue_seq'::regclass)
 
-- aggiornare la seq
select setval('metadatavalue_seq',max(metadata_value_id)+1) from metadatavalue;

-- aggiungere il vincolo sulla tabella metadatavalue
  CONSTRAINT metadatavalue_metadata_field_id_fkey FOREIGN KEY (metadata_field_id)
      REFERENCES metadatafieldregistry (metadata_field_id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION

-- aggiungere vincolo su tabella item:
CONSTRAINT item_submitter_id_fkey FOREIGN KEY (submitter_id)
      REFERENCES eperson (eperson_id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION

  CONSTRAINT metadatafieldregistry_metadata_schema_id_fkey FOREIGN KEY (metadata_schema_id)
      REFERENCES metadataschemaregistry (metadata_schema_id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
--- FINE VINCOLI      


-- aggiungere ad ogni item i metadati aggiuntivi rigaurdanti la provenienza  e il link (creare il meta nel registry se necessario)
-- ricordarsi che gli item con id (metadati con resource_id) <=62406 sono di trieste gli altri di udine


-- export RP with build_csv_UNITS.ktr and build_csv_UNIUD.ktr (warning!!! the database to retrieve the data point up the PREPROD!!!) 

-- Aggiornare gli "autori" degli item (praticamete solo i metadati authority.people) 
-- ricordarsi che gli item con id (metadati con resource_id) <=62406 sono di trieste gli altri di udine

-- generare gli handle fittizi se serve

-- importare i csv di openstarts e sissa

-- FINE (si spera)

-- per ogni problema mandare una email a .....
