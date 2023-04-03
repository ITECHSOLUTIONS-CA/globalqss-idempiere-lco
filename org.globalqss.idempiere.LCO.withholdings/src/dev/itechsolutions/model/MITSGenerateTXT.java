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
public class MITSGenerateTXT extends X_ITS_GenerateTXT implements ImmutablePOSupport {

	private static final long serialVersionUID = -2390265110024301876L;

	public MITSGenerateTXT(Properties ctx, int ITS_GenerateTXT_ID, String trxName) {
		super(ctx, ITS_GenerateTXT_ID, trxName);
	}

	public MITSGenerateTXT(Properties ctx, int ITS_GenerateTXT_ID, String trxName, String... virtualColumns) {
		super(ctx, ITS_GenerateTXT_ID, trxName, virtualColumns);
	}

	public MITSGenerateTXT(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	protected boolean beforeSave (boolean newRecord)
	{
		StringBuilder where = new StringBuilder ("AD_Client_ID = ?")
				.append(" AND AD_Org_ID = ?")
				.append(" AND ITS_TypeOperation = ?")
				.append(" AND ITS_GenerateTXT_ID <> ?")
				.append(" AND (.ValidFrom BETWEEN ? AND ?")
				.append(" OR ValidTo BETWEEN ? AND ?) ");
		
		MITSGenerateTXT retVal = new Query(getCtx(), MITSGenerateTXT.Table_Name, where.toString(), get_TrxName())
				.setOnlyActiveRecords(true)
				.setParameters(getAD_Client_ID(), getAD_Org_ID()
						, getITS_TypeOperation(), getITS_generateTXT_ID()
						, getValidFrom(), getValidTo()
						, getValidFrom(), getValidTo())
				.first();
		
		if(retVal != null)
			throw new AdempiereException("@ValidateDates@");
		
		return true;
	}

	@Override
	public String toString() {
		return "" + getITS_generateTXT_ID();
	}
	
	/**	Cache					*/
	static private ImmutableIntPOCache<Integer, MITSGenerateTXT>	s_cache = new ImmutableIntPOCache<Integer,MITSGenerateTXT>(Table_Name, 20);
	
	public MITSGenerateTXT(Properties ctx, MITSGenerateTXT copy) 
	{
		this(ctx, copy, (String) null);
	}
	
	public MITSGenerateTXT(Properties ctx, MITSGenerateTXT copy, String trxName) 
	{
		this(ctx, 0, trxName);
		copyPO(copy);
	}
	
	static public MITSGenerateTXT get(int ITS_generateTXT_ID)
	{
		return get(Env.getCtx(), ITS_generateTXT_ID);
	}
	
	static public MITSGenerateTXT get(Properties ctx, int ITS_generateTXT_ID)
	{
		MITSGenerateTXT retValue = s_cache.get(ctx, ITS_generateTXT_ID, e -> new MITSGenerateTXT(ctx, e));
		if (retValue != null)
			return retValue;
		
		retValue = new MITSGenerateTXT (ctx, ITS_generateTXT_ID, (String)null);
		if (retValue.getITS_generateTXT_ID() == ITS_generateTXT_ID)
		{
			s_cache.put(ITS_generateTXT_ID, retValue, e -> new MITSGenerateTXT(Env.getCtx(), e));
			return retValue;
		}
		
		return null;		
	}
	
	@Override
	public PO markImmutable()
	{
		return null;
	}
	
}//	MITSGenerateTXT