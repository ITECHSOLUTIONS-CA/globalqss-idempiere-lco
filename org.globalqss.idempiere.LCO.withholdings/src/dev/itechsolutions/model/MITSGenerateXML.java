package dev.itechsolutions.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.idempiere.cache.ImmutableIntPOCache;
import org.idempiere.cache.ImmutablePOSupport;

/**
 * 
 * @author José Castañeda
 *
 */
public class MITSGenerateXML extends X_ITS_GenerateXML implements ImmutablePOSupport {

	private static final long serialVersionUID = 9185649253562292738L;

	public MITSGenerateXML(Properties ctx, int ITS_GenerateXML_ID, String trxName) {
		super(ctx, ITS_GenerateXML_ID, trxName);
	}

	public MITSGenerateXML(Properties ctx, int ITS_GenerateXML_ID, String trxName, String... virtualColumns) {
		super(ctx, ITS_GenerateXML_ID, trxName, virtualColumns);
	}

	public MITSGenerateXML(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	protected boolean beforeSave (boolean newRecord)
	{
		StringBuilder where = new StringBuilder("AD_Client_ID = ?")
				.append(" AND AD_Org_ID = ?")
				.append(" AND ITS_generateXML_ID <> ?")
				.append(" AND (ValidFrom BETWEEN ? AND ?")
				.append(" OR ValidTo BETWEEN ? AND ?)");
		
		MITSGenerateXML data = new Query(getCtx(), MITSGenerateXML.Table_Name, where.toString(), get_TrxName())
				.setOnlyActiveRecords(true)
				.setParameters(getAD_Client_ID(), getAD_Org_ID()
						, getITS_GenerateXML_ID()
						, getValidFrom(), getValidTo()
						, getValidFrom(), getValidTo())
				.first();
		
		if(data != null)
			throw new AdempiereException("@ValidateDates@");
			
		return true;
	}
	
	/**	Cache					*/
	static private ImmutableIntPOCache<Integer, MITSGenerateXML>	s_cache = new ImmutableIntPOCache<Integer,MITSGenerateXML>(Table_Name, 20);
	
	public MITSGenerateXML(Properties ctx, MITSGenerateXML copy) 
	{
		this(ctx, copy, (String) null);
	}
	
	public MITSGenerateXML(Properties ctx, MITSGenerateXML copy, String trxName) 
	{
		this(ctx, 0, trxName);
		copyPO(copy);
	}
	
	static public MITSGenerateXML get(int ITS_GenerateXML_ID)
	{
		return get(Env.getCtx(), ITS_GenerateXML_ID);
	}
	
	static public MITSGenerateXML get(Properties ctx, int ITS_GenerateXML_ID)
	{
		MITSGenerateXML retValue = s_cache.get(ctx, ITS_GenerateXML_ID, e -> new MITSGenerateXML(ctx, e));
		if (retValue != null)
			return retValue;
		
		retValue = new MITSGenerateXML (ctx, ITS_GenerateXML_ID, (String)null);
		if (retValue.getITS_GenerateXML_ID() == ITS_GenerateXML_ID)
		{
			s_cache.put(ITS_GenerateXML_ID, retValue, e -> new MITSGenerateXML(Env.getCtx(), e));
			return retValue;
		}
		
		return null;		
	}
	
	@Override
	public PO markImmutable()
	{
		return null;
	}
	
}//	MITSGenerateXML