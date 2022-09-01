package dev.itechsolutions.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.Query;

/**
 * 
 * @author José Castañeda
 *
 */
public class ITSMGenerateTXT extends X_ITS_generateTXT {

	private static final long serialVersionUID = 6856991921861561515L;

	public ITSMGenerateTXT(Properties ctx, int ITS_generateTXT_ID, String trxName) {
		super(ctx, ITS_generateTXT_ID, trxName);
	}

	public ITSMGenerateTXT(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
	

	protected boolean beforeSave (boolean newRecord)
	{
		String where = "ITS_GenerateTxT.AD_Client_ID = ? "
				+ "AND ITS_GenerateTxT.AD_Org_ID = ? "
				+ "AND (ITS_GenerateTxT.ValidFrom BETWEEN ? AND ? "
				+ "OR ITS_GenerateTxT.ValidTo BETWEEN ? AND ?) ";
		//
		ITSMGenerateTXT data = new Query(getCtx(), ITSMGenerateTXT.Table_Name, where, get_TrxName())
				.setOnlyActiveRecords(true)
				.setParameters(getAD_Client_ID(), getAD_Org_ID() 
						, getValidFrom(), getValidTo()
						, getValidFrom(), getValidTo())
				.first();
		
		if(data != null)
			throw new AdempiereException("@ValidateTXT@");
			
		return true;
	}

}// ITSMGenerateTxT