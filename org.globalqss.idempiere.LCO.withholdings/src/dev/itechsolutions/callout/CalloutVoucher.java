package dev.itechsolutions.callout;

import java.util.Optional;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MDocType;

import dev.itechsolutions.model.I_ITS_VoucherWithholding;

/**
 * 
 * @author Argenis Rodríguez
 *
 */
public class CalloutVoucher implements IColumnCallout {
	
	/**
	 * @author Argenis Rodríguez
	 * @param mTab
	 * @return
	 */
	private static boolean isCalloutActive(GridTab mTab) {
		
		return mTab != null ? mTab.getActiveCallouts().length > 1 : false;
	}
	
	@Override
	public String start(Properties ctx, int WindowNo
			, GridTab mTab, GridField mField
			, Object value, Object oldValue) {
		
		if (isCalloutActive(mTab))
			return "";
		
		if (I_ITS_VoucherWithholding.COLUMNNAME_C_DocType_ID.equals(mField.getColumnName()))
		{
			int C_DocType_ID = Optional.ofNullable((Integer) value)
					.orElse(0);
			
			if (C_DocType_ID <= 0)
			{
				mTab.setValue(I_ITS_VoucherWithholding.COLUMNNAME_LCO_WithholdingType_ID, null);
				return "";
			}
			
			MDocType dt = new MDocType(ctx, C_DocType_ID, null);
			int LCO_WithholdingType_ID = dt.get_ValueAsInt(I_ITS_VoucherWithholding.COLUMNNAME_LCO_WithholdingType_ID);
			
			mTab.setValue(I_ITS_VoucherWithholding.COLUMNNAME_LCO_WithholdingType_ID, LCO_WithholdingType_ID);
			mTab.setValue(I_ITS_VoucherWithholding.COLUMNNAME_IsSOTrx, dt.isSOTrx());
		}
		
		return "";
	}
}
