package dev.itechsolutions.model;

import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.MAccount;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MCountry;
import org.compiere.model.MLocation;
import org.compiere.model.MRegion;
import org.compiere.model.MSysConfig;
import org.compiere.model.PO;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.idempiere.cache.ImmutablePOCache;

import dev.itechsolutions.util.ColumnUtils;

/**
 * 
 * @author Argenis Rodríguez
 *
 */
public class ITSMLocation extends MLocation {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/** Municipality */
	private MMunicipality m_m = null;
	
	/** Parish */
	private MParish m_p = null;
	
	/** Cache */
	private static ImmutablePOCache<Integer, ITSMLocation>	s_cache = new ImmutablePOCache<Integer, ITSMLocation>(Table_Name, 100, 30);
	
	/**
	 * 
	 * @param country
	 * @param region
	 */
	public ITSMLocation(MCountry country, MRegion region) {
		super(country, region);
	}
	
	/**
	 * 
	 * @param copy
	 */
	public ITSMLocation(MLocation copy) {
		super(copy);
	}
	
	/**
	 * 
	 * @param ctx
	 * @param C_Country_ID
	 * @param C_Region_ID
	 * @param city
	 * @param trxName
	 */
	public ITSMLocation(Properties ctx, int C_Country_ID, int C_Region_ID, String city, String trxName) {
		super(ctx, C_Country_ID, C_Region_ID, city, trxName);
	}
	
	/**
	 * 
	 * @param ctx
	 * @param C_Location_ID
	 * @param trxName
	 */
	public ITSMLocation(Properties ctx, int C_Location_ID, String trxName) {
		super(ctx, C_Location_ID, trxName);
	}
	
	/**
	 * 
	 * @param ctx
	 * @param copy
	 * @param trxName
	 */
	public ITSMLocation(Properties ctx, MLocation copy, String trxName) {
		super(ctx, copy, trxName);
	}
	
	/**
	 * 
	 * @param ctx
	 * @param copy
	 */
	public ITSMLocation(Properties ctx, MLocation copy) {
		super(ctx, copy);
	}
	
	/**
	 * 
	 * @param ctx
	 * @param rs
	 * @param trxName
	 */
	public ITSMLocation(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @return
	 */
	public MMunicipality getMunicipality() {
		return getMunicipality(false);
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @param C_Location_ID
	 * @return
	 */
	public static ITSMLocation get(int C_Location_ID) {
		return get(C_Location_ID, null);
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @param C_Location_ID
	 * @param trxName
	 * @return
	 */
	public static ITSMLocation get(int C_Location_ID, String trxName) {
		return get(Env.getCtx(), C_Location_ID, trxName);
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @param ctx
	 * @param C_Location_ID
	 * @param trxName
	 * @return
	 */
	public static ITSMLocation get(Properties ctx, int C_Location_ID, String trxName) {
		//new
		if (C_Location_ID == 0)
			return new ITSMLocation(Env.getCtx(), C_Location_ID, trxName);
		
		ITSMLocation retVal = s_cache.get(ctx, C_Location_ID, e -> new ITSMLocation(ctx, e));
		
		if (retVal != null)
			return retVal;
		
		retVal = new ITSMLocation(ctx, C_Location_ID, trxName);
		
		if (retVal.get_ID() == C_Location_ID)
		{
			s_cache.put(retVal.get_ID(), retVal, e -> new ITSMLocation(Env.getCtx(), e));
			return retVal;
		}
		
		return null;
	}
	
	/**
	 * 
	 * @author Argenis Rodríguez
	 * @param ctx
	 * @param C_Location_ID
	 * @param trxName
	 * @return
	 */
	public static ITSMLocation getCopy(Properties ctx, int C_Location_ID, String trxName) {
		
		ITSMLocation loc = get(C_Location_ID, trxName);
		
		if (loc != null && loc.get_ID() > 0)
			loc = new ITSMLocation(ctx, loc, trxName);
		
		return loc;
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @param requery
	 * @return
	 */
	public MMunicipality getMunicipality(boolean requery) {
		
		int C_Municipality_ID = get_ValueAsInt(MMunicipality.COLUMNNAME_C_Municipality_ID);
		
		if (m_m != null && C_Municipality_ID != m_m.get_ID())
			m_m = null;
		
		if ((m_m == null || requery) && C_Municipality_ID > 0)
			m_m = MMunicipality.get(C_Municipality_ID);
		return m_m;
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @return
	 */
	public MParish getParish() {
		return getParish(false);
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @param requery
	 * @return
	 */
	public MParish getParish(boolean requery) {
		
		int C_Parish_ID = get_ValueAsInt(MParish.COLUMNNAME_C_Parish_ID);
		
		if (m_p != null && C_Parish_ID != m_p.get_ID())
			m_p = null;
		
		if ((m_p == null || requery) && C_Parish_ID > 0)
			m_p = MParish.get(C_Parish_ID);
		return m_p;
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @return
	 */
	public boolean isHasRegion() {
		if (getCountry() == null)
			return false;
		return getCountry().isHasRegion();
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @return
	 */
	public boolean isHasMunicipality() {
		if (getCountry() == null)
			return false;
		return getCountry().get_ValueAsBoolean(ColumnUtils.COLUMNNAME_HasMunicipality);
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @return
	 */
	public boolean isHasParish() {
		if (getCountry() == null)
			return false;
		return getCountry().get_ValueAsBoolean(ColumnUtils.COLUMNNAME_HasParish);
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @param municipality
	 */
	public void setMunicipality(MMunicipality municipality) {
		m_m = municipality;
		
		if (municipality == null)
		{
			set_ValueOfColumn(MMunicipality.COLUMNNAME_C_Municipality_ID, null);
			setParish(null);
		}
		else
		{
			set_ValueOfColumn(MMunicipality.COLUMNNAME_C_Municipality_ID, municipality.get_ID());
			if (m_m.getC_Region_ID() != getC_Region_ID())
			{
				if (log.isLoggable(Level.INFO))
					log.info("Municipality (" + municipality + ") C_Region_ID=" + municipality.getC_Region_ID()
							+ " - From C_Region_ID=" + getC_Region_ID());
				setC_Region_ID(m_m.getC_Region_ID());
			}
		}
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @param parish
	 */
	public void setParish(MParish parish) {
		m_p = parish;
		
		if (parish == null)
			set_ValueOfColumn(MParish.COLUMNNAME_C_Parish_ID, null);
		else
		{
			set_ValueOfColumn(MParish.COLUMNNAME_C_Parish_ID, parish.get_ID());
			int C_Municipality_ID = get_ValueAsInt(MMunicipality.COLUMNNAME_C_Municipality_ID);
			if (m_p.getC_Municipality_ID() != C_Municipality_ID)
			{
				if (log.isLoggable(Level.INFO))
					log.info("Parish (" + parish + ") C_Municipality_ID=" + parish.getC_Municipality_ID()
							+ " - From C_Municipality_ID=" + C_Municipality_ID);
				setC_Municipality_ID(m_p.getC_Municipality_ID());
			}
		}
	}
	
	@Override
	public void setRegion(MRegion region) {
		
		if (region == null)
			setMunicipality(null);
		
		super.setRegion(region);
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @param C_Municipality_ID
	 */
	public void setC_Municipality_ID(int C_Municipality_ID) {
		
		if (C_Municipality_ID == 0)
			setMunicipality(null);
		//Region Defined
		else if (getC_Region_ID() != 0)
		{
			MCountry country = getCountry();
			MRegion region = getRegion();
			
			if (isValidMunicipality(country, region, C_Municipality_ID))
				set_ValueOfColumn(MMunicipality.COLUMNNAME_C_Municipality_ID, C_Municipality_ID);
			else
				setMunicipality(null);
		}
		else
			setMunicipality(MMunicipality.get(C_Municipality_ID));
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @param C_Parish_ID
	 */
	public void setC_Parish_ID(int C_Parish_ID) {
		if (C_Parish_ID == 0)
			setParish(null);
		//Municipality Defined
		else if (get_ValueAsInt(MMunicipality.COLUMNNAME_C_Municipality_ID) != 0)
		{
			MCountry country = getCountry();
			int C_Municipality_ID = get_ValueAsInt(MMunicipality.COLUMNNAME_C_Municipality_ID);
			
			if (isValidParish(country, C_Municipality_ID, C_Parish_ID))
				set_ValueOfColumn(MParish.COLUMNNAME_C_Parish_ID, C_Parish_ID);
			else
				setParish(null);
		}
		else
			setParish(MParish.get(C_Parish_ID));
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @param country
	 * @param C_Municipality_ID
	 * @param C_Parish_ID
	 * @return
	 */
	public static boolean isValidParish(MCountry country, int C_Municipality_ID
			, int C_Parish_ID) {
		if (C_Parish_ID == 0
				|| country.get_ID() == 0
				|| !country.get_ValueAsBoolean(ColumnUtils.COLUMNNAME_HasParish))
			return false;
		
		return Arrays.stream(MParish.getParishes(C_Municipality_ID))
				.anyMatch(parish -> parish.get_ID() == C_Parish_ID);
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @param country
	 * @param C_Region_ID
	 * @param C_Municipality_ID
	 * @return
	 */
	public static boolean isValidMunicipality(MCountry country, MRegion region
			, int C_Municipality_ID) {
		if (C_Municipality_ID == 0
				|| country.get_ID() == 0
				|| !country.get_ValueAsBoolean(ColumnUtils.COLUMNNAME_HasMunicipality))
			return false;
		
		return Arrays.stream(MMunicipality.getMunicipalities(region))
				.anyMatch(municipality -> municipality.get_ID() == C_Municipality_ID);
	}
	
	@Override
	protected boolean beforeSave(boolean newRecord) {
		
		MCountry m_c = getCountry();
		
		if (getAD_Org_ID() != 0)
			setAD_Org_ID(0);
		//	Region Check
		if (getC_Region_ID() != 0)
		{
			if (m_c == null || m_c.getC_Country_ID() != getC_Country_ID())
				getCountry();
			if (!m_c.isHasRegion())
				setC_Region_ID(0);
		} else {
			setRegionName(null);
		}
		if (getC_City_ID() <= 0 && getCity() != null && getCity().length() > 0) {
			int city_id = DB.getSQLValue(
					get_TrxName(),
					"SELECT C_City_ID FROM C_City WHERE C_Country_ID=? AND COALESCE(C_Region_ID,0)=? AND Name=? AND AD_Client_ID IN (0,?)",
					new Object[] {getC_Country_ID(), getC_Region_ID(), getCity(), getAD_Client_ID()});
			if (city_id > 0)
				setC_City_ID(city_id);
		}
		
		//check city
		if (m_c != null && !m_c.isAllowCitiesOutOfList() && getC_City_ID()<=0) {
			log.saveError("CityNotFound", Msg.translate(getCtx(), "CityNotFound"));
			return false;
		}
		
		//check city id
		if (m_c != null && !m_c.isAllowCitiesOutOfList() && getC_City_ID() > 0) {
			int city_id = DB.getSQLValue(get_TrxName(),
										"SELECT C_City_ID "+
										"  FROM C_City "+
										" WHERE C_Country_ID=? "+
										"   AND COALESCE(C_Region_ID,0)=? " +
										"   AND C_City_ID =?",
										new Object[] {getC_Country_ID(), getC_Region_ID(), getC_City_ID()});
			
			if (city_id<0){
			    log.saveError("CityNotFound",Msg.translate(getCtx(), "CityNotFound")+" C_City_ID["+getC_City_ID()+"]");
			    return false;
			}
		}
		
		return true;
	}
	
	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		
		if (!success)
			return success;
		//	Value/Name change in Account
		if (!newRecord
			&& ("Y".equals(Env.getContext(getCtx(), "$Element_LF")) 
				|| "Y".equals(Env.getContext(getCtx(), "$Element_LT")))
			&& (is_ValueChanged("Postal") || is_ValueChanged("City"))
			){
			StringBuilder msgup = new StringBuilder(
					"(C_LocFrom_ID=").append(getC_Location_ID()) 
					.append(" OR C_LocTo_ID=").append(getC_Location_ID()).append(")");
			MAccount.updateValueDescription(getCtx(), msgup.toString(), get_TrxName());
		}
		
		//Update BP_Location name IDEMPIERE 417
		if (get_TrxName().startsWith(PO.LOCAL_TRX_PREFIX)) { // saved without trx
			int bplID = DB.getSQLValueEx(get_TrxName(), updateBPLocName, getC_Location_ID());
			if (bplID>0)
			{
				// just trigger BPLocation name change when the location change affects the name:
				// START_VALUE_BPLOCATION_NAME
				// 0 - City
				// 1 - City + Address1
				// 2 - City + Address1 + Address2
				// 3 - City + Address1 + Address2 + Region
				// 4 - City + Address1 + Address2 + Region + ID
				int bplocname = MSysConfig.getIntValue(MSysConfig.START_VALUE_BPLOCATION_NAME, 0, getAD_Client_ID(), getAD_Org_ID());
				if (bplocname < 0 || bplocname > 4)
					bplocname = 0;
				if (   is_ValueChanged(COLUMNNAME_City)
					|| is_ValueChanged(COLUMNNAME_C_City_ID)
					|| (bplocname >= 1 && is_ValueChanged(COLUMNNAME_Address1))
					|| (bplocname >= 2 && is_ValueChanged(COLUMNNAME_Address2))
					|| (bplocname >= 3 && (is_ValueChanged(COLUMNNAME_RegionName) || is_ValueChanged(COLUMNNAME_C_Region_ID)))
					) {
					MBPartnerLocation bpl = new MBPartnerLocation(getCtx(), bplID, get_TrxName());
					bpl.setName(bpl.getBPLocName(this));
					bpl.saveEx();
				}
			}
		}
		
		return success;
	}
}
