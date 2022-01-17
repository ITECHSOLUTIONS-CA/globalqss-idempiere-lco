CREATE OR REPLACE FUNCTION getOrgs(p_AD_Org_ID NUMERIC, p_ExcludeParentOrg VARCHAR)
	RETURNS TABLE(ad_org_id NUMERIC)
	LANGUAGE plpgsql
	STABLE
AS $$
DECLARE
	rr RECORD;
	v_IsParent VARCHAR := 'N';
BEGIN
	FOR rr IN
		SELECT
			ch.ad_org_id
		FROM ad_orginfo ao 
		INNER JOIN getOrgs(ao.ad_org_id, 'N') ch ON 1 = 1
		WHERE ao.parent_org_id = p_AD_Org_ID
		AND ao.isactive = 'Y'
	LOOP
		v_IsParent := 'Y';
		ad_org_id := rr.ad_org_id;
		RETURN NEXT;
	END LOOP;
	
	IF COALESCE(p_ExcludeParentOrg, 'N') = 'N' OR v_IsParent = 'N' THEN
		ad_org_id := p_AD_Org_ID;
		RETURN NEXT;
	END IF;
END $$;
