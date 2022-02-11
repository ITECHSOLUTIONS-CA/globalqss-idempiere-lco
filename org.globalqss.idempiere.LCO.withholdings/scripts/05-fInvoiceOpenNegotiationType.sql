
CREATE OR REPLACE FUNCTION invoiceOpenNegotiationType(p_C_Invoice_ID NUMERIC
											, p_C_InvoicePaySchedule_ID NUMERIC)
	RETURNS NUMERIC
	LANGUAGE plpgsql
	STABLE
AS $$
DECLARE
	v_Currency_ID NUMERIC := 0;
	v_TotalOpenAmt NUMERIC := 0;
	v_PaidAmt NUMERIC := 0;
	v_Remaining NUMERIC := 0;
	v_MultiplierAP NUMERIC := 0;
	v_MultiplierCM NUMERIC := 0;
	v_Temp NUMERIC := 0;
	v_Precision NUMERIC := 0;
	v_Min NUMERIC := 0;
	v_LCO_InvoiceWithholding_ID NUMERIC := 0;
	v_ConversionDate TIMESTAMP;
	ar RECORD;
	s RECORD;
	v_IsPaid VARCHAR;
	v_WriteOffAmt NUMERIC := 0;
BEGIN
	BEGIN
		SELECT
			MAX(cb.c_currency_id)
			, SUM(currencyConvert(civ.grandtotal
						, civ.c_currency_id, cb.c_currency_id
						, civ.dateinvoiced, civ.c_conversiontype_id
						, civ.ad_client_id, civ.ad_org_id))
			, MAX(civ.multiplierap), MAX(civ.multiplier), MAX(civ.ispaid)
		INTO
			v_Currency_ID
			, v_TotalOpenAmt
			, v_MultiplierAP, v_MultiplierCM, v_IsPaid
		FROM c_invoice_v civ 
		INNER JOIN c_bpartner cb ON cb.c_bpartner_id = civ.c_bpartner_id 
		WHERE civ.c_invoice_id = p_C_Invoice_ID;
	EXCEPTION
		WHEN OTHERS THEN
			RAISE NOTICE 'Invoice Open Negotiation Type %', SQLERRM;
		RETURN NULL;
	END;
	
	IF COALESCE(v_Currency_ID, 0) = 0 THEN
		RAISE NOTICE 'Negotiation Currency Not Found';
		RETURN NULL;
	END IF;
	
	IF v_IsPaid = 'Y' THEN
		RETURN 0;
	END IF;
	
	SELECT
		cc.stdprecision 
	INTO
		v_Precision
	FROM c_currency cc 
	WHERE cc.c_currency_id = v_Currency_ID;
	
	SELECT 1/10^v_Precision INTO v_Min;
	
	--Must be installed LCO Withholding Plugin for use LCO_InvoiceWithholding Table
	FOR ar IN
		SELECT
			ah.ad_client_id 
			, ah.ad_org_id 
			, al.amount 
			, al.discountamt 
			, al.writeoffamt
			, ah.c_currency_id 
			, ah.datetrx 
			, ci.dateinvoiced
			, al.c_allocationline_id 
			, pr.requesttype 
		FROM c_allocationline al
		INNER JOIN c_allocationhdr ah ON ah.c_allocationhdr_id = al.c_allocationhdr_id 
		INNER JOIN c_invoice ci ON ci.c_invoice_id = al.c_invoice_id 
		LEFT JOIN c_payselectionline psl ON psl.c_allocationline_id = al.c_allocationline_id 
		LEFT JOIN its_paymentrequestline prl ON prl.its_paymentrequestline_id = psl.its_paymentrequestline_id 
		LEFT JOIN its_paymentrequest pr ON pr.its_paymentrequest_id = prl.its_paymentrequest_id 
		WHERE al.c_invoice_id = p_C_Invoice_ID
		AND ah.isactive = 'Y'
	LOOP
		v_WriteOffAmt := 0;
		SELECT
			li.lco_invoicewithholding_id 
		INTO
			v_LCO_InvoiceWithholding_ID
		FROM lco_invoicewithholding li 
		WHERE li.c_allocationline_id = ar.c_allocationline_id
		LIMIT 1;
		
		IF COALESCE(v_LCO_InvoiceWithholding_ID, 0) = 0 AND ar.requesttype <> 'PRT' THEN
			v_ConversionDate := ar.datetrx;
		ELSE
			v_ConversionDate := ar.dateinvoiced;
			IF COALESCE(v_LCO_InvoiceWithholding_ID, 0) != 0 THEN
				v_WriteOffAmt := ar.writeoffamt;
			END IF;
		END IF;
		
		v_Temp := ar.amount + ar.discountamt + v_WriteOffAmt;
		v_PaidAmt := v_PaidAmt + currencyConvert(v_Temp * v_MultiplierAP, ar.c_currency_id, v_Currency_ID
										, v_ConversionDate, NULL, ar.ad_client_id, ar.ad_org_id);
		RAISE NOTICE 'Paid=% Allocation=%*%', v_PaidAmt, v_Temp, v_MultiplierAP;
	END LOOP;
	
	--Do we Have a Payment Schedule?
	IF COALESCE(p_C_InvoicePaySchedule_ID, 0) > 0 THEN
		v_Remaining := v_PaidAmt;
		
		FOR s IN
			SELECT
				psi.c_invoicepayschedule_id 
				, currencyConvert(psi.dueamt 
							, ci.c_currency_id, v_Currency_ID
							, ci.dateinvoiced, ci.c_conversiontype_id
							, ci.ad_client_id, ci.ad_org_id) dueamt
			FROM c_invoicepayschedule psi
			INNER JOIN c_invoice ci ON ci.c_invoice_id = psi.c_invoice_id 
			WHERE psi.c_invoice_id = p_C_Invoice_ID
			AND psi.isvalid = 'Y'
			ORDER BY psi.duedate
		LOOP
			
			IF s.c_invoicepayschedule_id = p_C_InvoicePaySchedule_ID THEN
				v_TotalOpenAmt := (s.dueamt * v_MultiplierCM) - v_Remaining;
				
				IF (s.dueamt - v_Remaining < 0) THEN
					v_TotalOpenAmt := 0;
				END IF;
			ELSE
				v_Remaining := v_Remaining - s.dueamt;
				
				IF v_Remaining < 0 THEN
					v_Remaining := 0;
				END IF;
			END IF;
		END LOOP;
	ELSE
		v_TotalOpenAmt := v_TotalOpenAmt - v_PaidAmt;
	END IF;
	
	--Ignore Rounding
	IF (v_TotalOpenAmt > -v_Min AND v_TotalOpenAmt < v_Min) THEN
		v_TotalOpenAmt := 0;
	END IF;
	
	--Round to Currency Precision
	RETURN ROUND(COALESCE(v_TotalOpenAmt, 0), v_Precision);
END $$;
