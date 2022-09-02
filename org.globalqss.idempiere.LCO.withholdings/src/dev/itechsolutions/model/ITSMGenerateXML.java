package dev.itechsolutions.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.Query;

public class ITSMGenerateXML extends X_ITS_generateXML {

	private static final long serialVersionUID = -1681383272706892814L;

	public ITSMGenerateXML(Properties ctx, int ITS_generateXML_ID, String trxName) {
		super(ctx, ITS_generateXML_ID, trxName);
		// TODO Auto-generated constructor stub
	}

	public ITSMGenerateXML(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		// TODO Auto-generated constructor stub
	}

	protected boolean beforeSave (boolean newRecord)
	{
		String where = "ITS_GenerateXML.AD_Client_ID = ? "
				+ "AND ITS_GenerateXML.AD_Org_ID = ? "
				+ "AND (ITS_GenerateXML.ValidFrom BETWEEN ? AND ? "
				+ "OR ITS_GenerateXML.ValidTo BETWEEN ? AND ?) ";
		//
		ITSMGenerateXML data = new Query(getCtx(), ITSMGenerateXML.Table_Name, where, get_TrxName())
				.setOnlyActiveRecords(true)
				.setParameters(getAD_Client_ID(), getAD_Org_ID() 
						, getValidFrom(), getValidTo()
						, getValidFrom(), getValidTo())
				.first();
		
		if(data != null)
			throw new AdempiereException("@ValidateXML@");
			
		return true;
	}
}