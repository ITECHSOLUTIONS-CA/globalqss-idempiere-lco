package dev.itechsolutions.callout;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MDocType;
import org.compiere.util.Env;

import dev.itechsolutions.util.ColumnUtils;

/**
 * 
 * @author Argenis Rodríguez
 *
 */
public class CalloutDocType implements IColumnCallout {
	
	@Override
	public String start(Properties ctx, int WindowNo
			, GridTab mTab, GridField mField
			, Object value, Object oldValue) {
		
		if (MDocType.Table_Name.equals(mTab.getTableName())
				&& (MDocType.COLUMNNAME_DocBaseType.equals(mField.getColumnName())
						|| MDocType.COLUMNNAME_IsSOTrx.equals(mField.getColumnName())
						|| MDocType.COLUMNNAME_C_DocType_ID.equals(mField.getColumnName())))
			return docType(ctx, WindowNo, mTab, mField, value);
		
		return null;
	}
	
	/**
	 * 
	 * @author Argenis Rodríguez
	 * @param ctx
	 * @param WindowNo
	 * @param mTab
	 * @param mField
	 * @param value
	 * @return
	 */
	private String docType(Properties ctx, int WindowNo
			, GridTab mTab, GridField mField
			, Object value) {
		
		String docBaseType = MDocType.COLUMNNAME_DocBaseType.equals(mField.getColumnName())
				? (String) value
				: mTab.get_ValueAsString(MDocType.COLUMNNAME_DocBaseType);
		
		boolean isSOTrx = MDocType.COLUMNNAME_IsSOTrx.equals(mField.getColumnName())
				? (Boolean) value
				: mTab.getValueAsBoolean(MDocType.COLUMNNAME_IsSOTrx);
		
		Env.setContext(ctx
				, WindowNo
				, ColumnUtils.CONTEXT_IsCustomerShipment
				, MDocType.DOCBASETYPE_MaterialDelivery.equals(docBaseType) && isSOTrx);
		
		Env.setContext(ctx, WindowNo
				, ColumnUtils.CONTEXT_IsVendorReceipt
				, MDocType.DOCBASETYPE_MaterialReceipt.equals(docBaseType) && !isSOTrx);
		
		return null;
	}
}
