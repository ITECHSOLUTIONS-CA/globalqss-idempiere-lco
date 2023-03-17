package dev.itechsolutions.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.Adempiere;
import org.compiere.model.MCity;
import org.compiere.model.MCountry;
import org.compiere.model.MLocation;
import org.compiere.model.MRegion;
import org.compiere.util.CacheMgt;
import org.compiere.util.Env;
import org.idempiere.cache.ImmutableIntPOCache;

import dev.itechsolutions.util.ColumnUtils;

import org.adempiere.base.Model;

/**
 * 
 * @author Argenis Rodríguez
 *
 */
@Model(table = MITSLocation.Table_Name)
public class MITSLocation extends MLocation {
	
	private static final long serialVersionUID = -5879961599817863133L;
	
	public static final String COLUMNNAME_C_Municipality_ID = "C_Municipality_ID";
	public static final String COLUMNNAME_C_Parish_ID = "C_Parish_ID";
	
	private static final ImmutableIntPOCache<Integer, MITSLocation> s_cache = new ImmutableIntPOCache<Integer, MITSLocation>("ITS_Location", 100, 30);
	
	private MMunicipality m_mun = null;
	
	private MParish m_par = null;
	
	public MITSLocation(Properties ctx, int C_Location_ID, String trxName) {
		super(ctx, C_Location_ID, trxName);
	}
	
	public MITSLocation(MCountry country, MRegion region) {
		super(country, region);
	}
	
	public MITSLocation(Properties ctx, int C_Country_ID, int C_Region_ID, String city, String trxName) {
		super(ctx, C_Country_ID, C_Region_ID, city, trxName);
	}
	
	public MITSLocation(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
	
	public MITSLocation(MITSLocation copy) {
		this(Env.getCtx(), copy);
	}
	
	public MITSLocation(Properties ctx, MITSLocation copy) {
		this(ctx, copy, null);
	}
	
	public MITSLocation(Properties ctx, MITSLocation copy, String trxName) {
		super(ctx, copy, trxName);
		m_mun = copy.m_mun != null ? new MMunicipality(ctx, copy.m_mun, trxName) : null;
	}
	
	public static MITSLocation get(Properties ctx, int C_Location_ID, String trxName) {
		
		if (C_Location_ID == 0)
			return new MITSLocation(Env.getCtx(), C_Location_ID, trxName);
		
		MITSLocation retVal = s_cache.get(ctx, C_Location_ID, e -> new MITSLocation(Env.getCtx(), e));
		
		if (retVal != null)
			return retVal;
		
		retVal = new MITSLocation(ctx, C_Location_ID, trxName);
		
		if (retVal.get_ID() == C_Location_ID)
		{
			s_cache.put(retVal.get_ID(), retVal, e -> new MITSLocation(Env.getCtx(), e));
			return retVal;
		}
		
		return null;
	}
	
	public static MITSLocation get(int C_Location_ID, String trxName) {
		return get(Env.getCtx(), C_Location_ID, trxName);
	}
	
	public static MITSLocation getCopy(Properties ctx, int C_Location_ID, String trxName) {
		MITSLocation retVal = get(C_Location_ID, trxName);
		if (retVal != null)
			return new MITSLocation(ctx, retVal, trxName);
		return null;
	}
	
	public static MITSLocation getCopy(Properties ctx, int C_Location_ID) {
		return getCopy(ctx, C_Location_ID, null);
	}
	
	public static MITSLocation getCopy(int C_Location_ID) {
		return getCopy(Env.getCtx(), C_Location_ID);
	}
	
	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		if (!super.afterSave(newRecord, success))
			return false;
		
		if (CacheMgt.get().hasCache("ITS_Location"))
		{
			if (!newRecord)
				Adempiere.getThreadPoolExecutor().submit(() -> CacheMgt.get().reset("ITS_Location", get_ID()));
			else
				Adempiere.getThreadPoolExecutor().submit(() -> CacheMgt.get().newRecord("ITS_Location", get_ID()));
		}
		
		return true;
	}
	
	public int getC_Municipality_ID() {
		return get_ValueAsInt(COLUMNNAME_C_Municipality_ID);
	}
	
	public int getC_Parish_ID() {
		return get_ValueAsInt(COLUMNNAME_C_Parish_ID);
	}
	
	public void setC_Municipality_ID(int C_Municipality_ID) {
		if (C_Municipality_ID > 0)
			set_ValueOfColumn(COLUMNNAME_C_Municipality_ID, C_Municipality_ID);
		else
			set_ValueOfColumn(COLUMNNAME_C_Municipality_ID, null);
	}
	
	public void setC_Parish_ID(int C_Parish_ID) {
		if (C_Parish_ID > 0)
			set_ValueOfColumn(COLUMNNAME_C_Parish_ID, C_Parish_ID);
		else
			set_ValueOfColumn(COLUMNNAME_C_Parish_ID, null);
	}
	
	public void setMunicipality(MMunicipality municipality) {
		if (municipality == null)
		{
			setC_Municipality_ID(0);
			m_mun = null;
		}
		else
		{
			setC_Municipality_ID(municipality.get_ID());
			
			if (municipality.getC_Region_ID() != getC_Region_ID())
			{
				MRegion region = MRegion.get(municipality.getC_Region_ID());
				
				if (region.getC_Country_ID() != getC_Country_ID())
				{
					MCountry country = MCountry.get(region.getC_Country_ID());
					setCountry(country);
				}
				
				setRegion(region);
			}
		}
	}
	
	public MMunicipality getMunicipality() {
		if (m_mun != null && m_mun.get_ID() != getC_Municipality_ID())
			m_mun = null;
		
		if (m_mun == null && getC_Municipality_ID() > 0)
			m_mun = MMunicipality.get(getC_Municipality_ID());
		
		return m_mun;
	}
	
	public void setParish(MParish parish) {
		if (parish == null)
		{
			m_par = null;
			setC_Parish_ID(0);
		}
		else
		{
			setC_Parish_ID(parish.get_ID());
			
			if (parish.getC_Municipality_ID() != getC_Municipality_ID())
				setMunicipality(MMunicipality.get(parish.getC_Municipality_ID()));
		}
	}
	
	public MParish getParish() {
		if (m_par != null && m_par.get_ID() != getC_Parish_ID())
			m_par = null;
		
		if (m_par == null && getC_Parish_ID() > 0)
			m_par = MParish.get(getC_Parish_ID());
		
		return m_par;
	}
	
	public boolean isHasRegion() {
		if (getCountry() == null)
			return false;
		return getCountry().isHasRegion();
	}
	
	public boolean isHasMunicipality() {
		if (getCountry() == null)
			return false;
		return getCountry().get_ValueAsBoolean(ColumnUtils.COLUMNNAME_HasMunicipality);
	}
	
	public boolean isHasParish() {
		if (getCountry() == null)
			return false;
		return getCountry().get_ValueAsBoolean(ColumnUtils.COLUMNNAME_HasParish);
	}
}
