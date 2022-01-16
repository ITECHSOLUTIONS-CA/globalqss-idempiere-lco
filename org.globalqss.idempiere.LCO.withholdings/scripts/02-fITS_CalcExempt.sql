
CREATE OR REPLACE FUNCTION ITS_CalcExempt(p_C_Invoice_ID NUMERIC)
	RETURNS NUMERIC
	LANGUAGE plpgsql
	STABLE
AS $$
DECLARE
	retVal NUMERIC := 0;
BEGIN
	SELECT
		COALESCE(SUM(ci.taxbaseamt), 0)
	INTO
		retVal
	FROM c_invoicetax ci 
	INNER JOIN c_tax ct ON ct.c_tax_id = ci.c_tax_id 
	WHERE ci.c_invoice_id = p_C_Invoice_ID
	AND ct.rate = 0 AND ct.istaxexempt = 'Y';
	
	RETURN retVal;
END; $$
