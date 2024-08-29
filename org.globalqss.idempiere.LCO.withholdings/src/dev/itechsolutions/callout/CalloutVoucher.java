package dev.itechsolutions.callout;

import org.adempiere.base.annotation.Callout;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.globalqss.model.X_LCO_WithholdingType;

import dev.itechsolutions.annotation.ColumnCallout;
import dev.itechsolutions.base.CustomCallout;
import dev.itechsolutions.model.MITSVoucherWithholding;
import dev.itechsolutions.util.ColumnUtils;

/**
 * 
 * @author Argenis Rodríguez
 *
 */
@Callout(tableName = "ITS_VoucherWithholding"
, columnName = {"C_DocType_ID"
		, "C_Invoice_ID"
		, "DateFrom"
		, "DateTo"})
public class CalloutVoucher extends CustomCallout {
	
	@ColumnCallout(columnName = "C_DocType_ID")
	public String docType() {
		
		if (isCalloutActive())
			return null;
		
		int C_DocType_ID = getValueAsInt();
		
		if (C_DocType_ID <= 0)
		{
			setValue("LCO_WithholdingType_ID", null);
			return null;
		}
		
		MDocType dt = MDocType.get(getCtx(), C_DocType_ID);
		
		int LCO_WithholdingType_ID = dt.get_ValueAsInt(ColumnUtils.COLUMNNAME_LCO_WithholdingType_ID);
		boolean isSOTrx = dt.isSOTrx();
		
		setValue(MITSVoucherWithholding.COLUMNNAME_LCO_WithholdingType_ID
				, LCO_WithholdingType_ID);
		setValue(MITSVoucherWithholding.COLUMNNAME_IsSOTrx, isSOTrx);
		
		if (LCO_WithholdingType_ID <= 0)
		{
			setValue(MITSVoucherWithholding.COLUMNNAME_C_Currency_ID, null);
			return null;
		}
		
		X_LCO_WithholdingType wt = new X_LCO_WithholdingType(getCtx(), LCO_WithholdingType_ID, null);
		
		setValue(MITSVoucherWithholding.COLUMNNAME_C_Currency_ID, wt.getC_Currency_ID());
		
		return null;
	}
	
	@ColumnCallout(columnName = MITSVoucherWithholding.COLUMNNAME_C_Invoice_ID)
	public String invoice() {
		
		int C_Invoice_ID = getValueAsInt();
		
		if (C_Invoice_ID <= 0)
			return null;
		
		MInvoice invoice = MInvoice.get(getCtx(), C_Invoice_ID);
		
		setValue(MITSVoucherWithholding.COLUMNNAME_C_BPartner_ID, invoice.getC_BPartner_ID());
		setValue(MITSVoucherWithholding.COLUMNNAME_DateFrom, null);
		setValue(MITSVoucherWithholding.COLUMNNAME_DateTo, null);
		
		return null;
	}
	
	@ColumnCallout(columnName = {MITSVoucherWithholding.COLUMNNAME_DateFrom
			, MITSVoucherWithholding.COLUMNNAME_DateTo})
	public String date() {
		setValue(MITSVoucherWithholding.COLUMNNAME_C_Invoice_ID, null);
		return null;
	}
}
