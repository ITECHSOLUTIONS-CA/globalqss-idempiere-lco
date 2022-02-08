
CREATE OR REPLACE FUNCTION currencyConvertNegotiationType(p_C_Invoice_ID NUMERIC, p_C_CurrencyTo_ID NUMERIC
															, p_Amt NUMERIC, p_ConversionDate TIMESTAMP)
	RETURNS NUMERIC
	LANGUAGE plpgsql
	STABLE
AS $$
DECLARE
	v_NegotiationCurrency_ID NUMERIC := 0;
	v_AD_Client_ID NUMERIC := 0;
	v_AD_Org_ID NUMERIC := 0;
	v_ConversionType_ID NUMERIC := 0;
BEGIN
	BEGIN
		SELECT
			cb.c_currency_id 
			, ci.c_conversiontype_id 
			, ci.ad_client_id 
			, ci.ad_org_id 
		INTO
			v_NegotiationCurrency_ID 
			, v_ConversionType_ID 
			, v_AD_Client_ID 
			, v_AD_Org_ID 
		FROM c_invoice ci 
		INNER JOIN c_bpartner cb ON cb.c_bpartner_id = ci.c_bpartner_id 
		WHERE ci.c_invoice_id = p_C_Invoice_ID;
	EXCEPTION
		WHEN OTHERS THEN
			RAISE NOTICE 'Currency Convert Negotiation Type %', SQLERRM;
		RETURN NULL;
	END;
	
	IF (COALESCE(v_NegotiationCurrency_ID, 0) = 0) THEN
		RAISE NOTICE 'Negotiation Currency Not Found';
		RETURN NULL;
	END IF;
	
	RETURN currencyConvert(p_Amt
				, v_NegotiationCurrency_ID, p_C_CurrencyTo_ID
				, p_ConversionDate, v_ConversionType_ID
				, v_AD_Client_ID, v_AD_Org_ID);
END $$;
