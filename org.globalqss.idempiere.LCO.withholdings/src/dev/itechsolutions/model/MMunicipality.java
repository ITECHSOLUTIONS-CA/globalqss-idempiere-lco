package dev.itechsolutions.model;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.MCity;
import org.compiere.model.MRegion;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.idempiere.cache.ImmutablePOCache;
import org.idempiere.cache.ImmutablePOSupport;

import dev.itechsolutions.util.POUtil;

/**
 * 
 * @author Argenis Rodríguez
 *
 */
public class MMunicipality extends X_C_Municipality
	implements ImmutablePOSupport{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/** Municipality Cache */
	private static ImmutablePOCache<Integer, MMunicipality> s_municipalities = new ImmutablePOCache<Integer, MMunicipality>(Table_Name, Table_Name
			, 100, 0, false, 0);
	
	private static CLogger s_log = CLogger.getCLogger(MMunicipality.class);
	
	/** Default Municipality */
	private static MMunicipality s_default = null;
	
	/**
	 * 
	 * @param ctx
	 * @param C_Municipality_ID
	 * @param trxName
	 */
	public MMunicipality(Properties ctx, int C_Municipality_ID, String trxName) {
		super(ctx, C_Municipality_ID, trxName);
	}
	
	/**
	 * 
	 * @param ctx
	 * @param rs
	 * @param trxName
	 */
	public MMunicipality(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
	
	/**
	 * 
	 * @param ctx
	 * @param copy
	 * @param trxName
	 */
	public MMunicipality(Properties ctx, MMunicipality copy, String trxName) {
		this(ctx, 0, trxName);
		copyPO(copy);
	}
	
	/**
	 * 
	 * @param ctx
	 * @param copy
	 */
	public MMunicipality(Properties ctx, MMunicipality copy) {
		this(ctx, copy, null);
	}
	
	/**
	 * 
	 * @param copy
	 */
	public MMunicipality(MMunicipality copy) {
		this(Env.getCtx(), copy);
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @param C_Municipality_ID
	 * @return
	 */
	public static synchronized MMunicipality get(int C_Municipality_ID) {
		return get(Env.getCtx(), C_Municipality_ID);
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @param ctx
	 * @param C_Municipality_ID
	 * @return
	 */
	public static synchronized MMunicipality get(Properties ctx, int C_Municipality_ID) {
		if (s_municipalities.size() == 0)
			loadAllMunicipalities();
		MMunicipality municipality = s_municipalities.get(ctx, C_Municipality_ID, e -> new MMunicipality(ctx, e));
		
		if (municipality != null)
			return municipality;
		
		municipality = new MMunicipality(ctx, C_Municipality_ID, null);
		
		if (municipality.get_ID() == C_Municipality_ID)
		{
			s_municipalities.put(municipality.get_ID(), municipality);
			return municipality;
		}
		
		return null;
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @param region
	 * @return
	 */
	public static synchronized MMunicipality[] getMunicipalities(MRegion region) {
		if (s_municipalities.size() == 0)
			loadAllMunicipalities();
		
		//If Region is null return a empty Array
		if (region == null)
			return new MMunicipality[0];
		
		MMunicipality[] municipalities = s_municipalities.values().stream()
				.filter(municipality -> municipality.getC_Region_ID() == region.get_ID())
				.toArray(MMunicipality[]::new);
		
		Arrays.sort(municipalities, POUtil.getComparator());
		
		return municipalities;
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @param city
	 * @return
	 */
	public static synchronized MMunicipality[] getMunicipalities(MCity city) {
		if (s_municipalities.size() == 0)
			loadAllMunicipalities();
		
		//If City is null return a empty Array
		if (city == null)
			return new MMunicipality[0];
		
		MMunicipality[] municipalities = s_municipalities.values().stream()
				.filter(municipality -> municipality.getC_City_ID() == city.get_ID())
				.toArray(MMunicipality[]::new);
		
		Arrays.sort(municipalities, POUtil.getComparator());
		
		return municipalities;
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @return
	 */
	public static synchronized MMunicipality getDefault() {
		if (s_municipalities.size() == 0)
			loadAllMunicipalities();
		return s_default;
	}
	
	/**
	 * @author Argenis Rodríguez
	 */
	private static void loadAllMunicipalities() {
		s_municipalities.clear();
		List<MMunicipality> municipalities = new ArrayList<MMunicipality>();
		
		try {
			PO.setCrossTenantSafe();
			municipalities = new Query(Env.getCtx(), Table_Name, "", null)
					.setOnlyActiveRecords(true)
					.list();
		} finally {
			PO.clearCrossTenantSafe();
		}
		
		for (MMunicipality municipality: municipalities)
		{
			municipality.markImmutable();
			s_municipalities.put(municipality.get_ID(), municipality);
			
			if (municipality.isDefault())
				s_default = municipality;
		}
		
		if (s_log.isLoggable(Level.FINE))
			s_log.fine(municipalities.size() + " - default=" + s_default);
	}
	
	@Override
	public PO markImmutable() {
		
		if (is_Immutable())
			return this;
		
		makeImmutable();
		return this;
	}
}
