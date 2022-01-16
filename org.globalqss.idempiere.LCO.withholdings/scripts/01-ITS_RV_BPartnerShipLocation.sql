CREATE OR REPLACE VIEW adempiere.its_rv_bpartnershiplocation
AS SELECT a.c_bpartner_id,
    min(a.c_location_id) AS c_location_id,
    min(a.address_shipto) AS address_shipto,
    min(a.address_billto) AS address_billto,
    min(a.phoneshipto) AS phoneshipto,
    min(a.phoneshipto2) AS phoneshipto2,
    min(a.phonebillto) AS phonebillto,
    min(a.phonebillto2) AS phonebillto2
   FROM ( SELECT a_1.c_bpartner_id,
            a_1.c_location_id,
                CASE
                    WHEN a_1.isshipto = 'Y'::bpchar THEN (((((btrim(clorg.address1::text) || COALESCE(' '::text || btrim(clorg.address2::text), ''::text)) || COALESCE(' '::text || btrim(clorg.address3::text), ''::text)) || COALESCE(' '::text || btrim(clorg.address4::text), ''::text)) || COALESCE(' '::text || btrim(clorg.city::text), ''::text)) || COALESCE(' '::text || btrim(d.name::text), ''::text)) || COALESCE(' '::text || btrim(c.name::text), ''::text)
                    ELSE NULL::text
                END AS address_shipto,
                CASE
                    WHEN a_1.isshipto = 'Y'::bpchar THEN a_1.phone::text
                    ELSE NULL::text
                END AS phoneshipto,
                CASE
                    WHEN a_1.isshipto = 'Y'::bpchar THEN a_1.phone2::text
                    ELSE NULL::text
                END AS phoneshipto2,
                CASE
                    WHEN a_1.isbillto = 'Y'::bpchar THEN (((((btrim(clorg.address1::text) || COALESCE(' '::text || btrim(clorg.address2::text), ''::text)) || COALESCE(' '::text || btrim(clorg.address3::text), ''::text)) || COALESCE(' '::text || btrim(clorg.address4::text), ''::text)) || COALESCE(' '::text || btrim(clorg.city::text), ''::text)) || COALESCE(' '::text || btrim(d.name::text), ''::text)) || COALESCE(' '::text || btrim(c.name::text), ''::text)
                    ELSE NULL::text
                END AS address_billto,
                CASE
                    WHEN a_1.isbillto = 'Y'::bpchar THEN a_1.phone::text
                    ELSE NULL::text
                END AS phonebillto,
                CASE
                    WHEN a_1.isbillto = 'Y'::bpchar THEN a_1.phone2::text
                    ELSE NULL::text
                END AS phonebillto2
           FROM c_bpartner_location a_1
             JOIN c_location clorg ON a_1.c_location_id = clorg.c_location_id
             LEFT JOIN c_country c ON c.c_country_id = clorg.c_country_id
             LEFT JOIN c_region d ON d.c_region_id = clorg.c_region_id
          WHERE clorg.isactive = 'Y'::bpchar
          ORDER BY a_1.c_bpartner_id) a
  GROUP BY a.c_bpartner_id;
 
 