
CREATE OR REPLACE FUNCTION ITS_ImpBase(typ VARCHAR, p_Record_ID NUMERIC, p_Rate NUMERIC)
	RETURNS NUMERIC
	LANGUAGE plpgsql
	STABLE
AS $$
DECLARE 
	v_RetVal NUMERIC := 0;
BEGIN
	--Type Invoice Base
	IF typ = 'IB' THEN
		SELECT
			ci.taxbaseamt
		INTO
			v_RetVal
		FROM c_invoicetax ci
		INNER JOIN c_tax ct ON ct.c_tax_id = ci.c_tax_id 
		WHERE ci.c_invoice_id = p_Record_ID AND ct.rate = p_Rate;
	--Type Invoice Tax
	ELSIF typ = 'IT' THEN
		SELECT
			ci.taxamt
		INTO
			v_RetVal
		FROM c_invoicetax ci
		INNER JOIN c_tax ct ON ct.c_tax_id = ci.c_tax_id 
		WHERE ci.c_invoice_id = p_Record_ID AND ct.rate = p_Rate;
	--Type Invoice Rate
	ELSIF typ = 'IR' THEN
		SELECT
			ct.rate 
		INTO
			v_RetVal
		FROM c_invoicetax ci
		INNER JOIN c_tax ct ON ct.c_tax_id = ci.c_tax_id 
		WHERE ci.c_invoice_id = p_Record_ID AND ct.rate = p_Rate;
	--Type Order Base
	ELSIF typ = 'OB' THEN
		SELECT
			co.taxbaseamt 
		INTO
			v_RetVal
		FROM c_ordertax co 
		INNER JOIN c_tax ct ON ct.c_tax_id = co.c_tax_id 
		WHERE co.c_order_id = p_Record_ID AND ct.rate = p_Rate;
	--Type Order Tax
	ELSIF typ = 'OT' THEN
		SELECT
			co.taxamt 
		INTO
			v_RetVal
		FROM c_ordertax co 
		INNER JOIN c_tax ct ON ct.c_tax_id = co.c_tax_id 
		WHERE co.c_order_id = p_Record_ID AND ct.rate = p_Rate;
	--Type Order Rate
	ELSIF typ = 'OT' THEN
		SELECT
			ct.rate 
		INTO
			v_RetVal
		FROM c_ordertax co 
		INNER JOIN c_tax ct ON ct.c_tax_id = co.c_tax_id 
		WHERE co.c_order_id = p_Record_ID AND ct.rate = p_Rate;
	END IF;
	
	RETURN COALESCE(v_RetVal, 0);
END; $$