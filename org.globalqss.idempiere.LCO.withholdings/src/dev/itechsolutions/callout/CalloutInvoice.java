package dev.itechsolutions.callout;

import java.util.Optional;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.I_C_Invoice;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.util.Env;

import dev.itechsolutions.util.ColumnUtils;

/**
 * 
 * @author Argenis Rodríguez
 *
 */
public class CalloutInvoice implements IColumnCallout {
	
	private static boolean isCalloutActive(GridTab mTab) {
		
		return mTab != null ? mTab.getActiveCallouts().length > 1 : false;
	}
	
	@Override
	public String start(Properties ctx, int WindowNo
			, GridTab mTab, GridField mField
			, Object value, Object oldValue) {
		
		if (isCalloutActive(mTab))
			return null;
		
		if (I_C_Invoice.Table_Name.equals(mTab.getTableName())
				&& (I_C_Invoice.COLUMNNAME_C_DocTypeTarget_ID.equals(mField.getColumnName())
						|| I_C_Invoice.COLUMNNAME_C_Invoice_ID.equals(mField.getColumnName())))
			return docType(ctx, WindowNo, mTab, mField, value);
		
		return null;
	}
	
	/**
	 * 
	 * @author Argenis Rodríguez
	 * @param mTab
	 * @param columnName
	 * @return
	 */
	private static int getValueAsInt(GridTab mTab, String columnName) {
		return Optional.ofNullable((Integer) mTab.getValue(columnName))
				.orElse(0);
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
		
		int C_DocType_ID = MInvoice.COLUMNNAME_C_DocTypeTarget_ID.equals(mField.getColumnName())
				? Optional.ofNullable((Integer) value).orElse(0)
				: getValueAsInt(mTab, MInvoice.COLUMNNAME_C_DocTypeTarget_ID);
		
		C_DocType_ID = Optional.of(C_DocType_ID)
				.filter(docType -> docType > 0)
				.orElseGet(() -> getValueAsInt(mTab, MInvoice.COLUMNNAME_C_DocType_ID));
		
		if (C_DocType_ID <= 0)
		{
			Env.setContext(ctx, WindowNo, ColumnUtils.COLUMNNAME_IsControlNoDocument, false);
			mTab.setValue(ColumnUtils.COLUMNNAME_ITS_ControlNumber, null);
			mTab.setValue(ColumnUtils.COLUMNNAME_ITS_POInvoiceNo, null);
			return null;
		}
		
		MDocType dt = MDocType.get(ctx, C_DocType_ID);
		boolean isControlNoDocument = dt.get_ValueAsBoolean(ColumnUtils.COLUMNNAME_IsControlNoDocument);
		
		Env.setContext(ctx, WindowNo
				, ColumnUtils.COLUMNNAME_IsControlNoDocument
				, isControlNoDocument);
		
		if (!isControlNoDocument)
		{
			mTab.setValue(ColumnUtils.COLUMNNAME_ITS_ControlNumber, null);
			mTab.setValue(ColumnUtils.COLUMNNAME_ITS_POInvoiceNo, null);
		}
		
		return null;
	}
}
