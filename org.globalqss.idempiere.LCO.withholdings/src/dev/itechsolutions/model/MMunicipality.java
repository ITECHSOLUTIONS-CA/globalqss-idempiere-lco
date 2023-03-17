package dev.itechsolutions.model;

import java.sql.ResultSet;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.idempiere.cache.ImmutableIntPOCache;
import org.idempiere.cache.ImmutablePOSupport;

public class MMunicipality extends X_C_Municipality implements ImmutablePOSupport {
	
	private static final long serialVersionUID = -947266622352105510L;
	
	private static final CLogger s_log = CLogger.getCLogger(MMunicipality.class);
	
	private static final ImmutableIntPOCache<Integer, MMunicipality> s_municipalities = new ImmutableIntPOCache<Integer, MMunicipality>(Table_Name, 250, 0);
	
	public MMunicipality(Properties ctx, int C_Municipality_ID, String trxName) {
		super(ctx, C_Municipality_ID, trxName);
	}
	
	public MMunicipality(Properties ctx, int C_Municipality_ID, String trxName, String... virtualColumns) {
		super(ctx, C_Municipality_ID, trxName, virtualColumns);
	}
	
	public MMunicipality(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
	
	public MMunicipality(Properties ctx, MMunicipality copy, String trxName) {
		this(ctx, 0, trxName);
		copyPO(copy);
	}
	
	public MMunicipality(Properties ctx, MMunicipality copy) {
		this(ctx, copy, null);
	}
	
	public MMunicipality(MMunicipality copy) {
		this(Env.getCtx(), copy);
	}
	
	public static MMunicipality get(Properties ctx, int C_Municipality_ID, String trxName) {
		if (C_Municipality_ID == 0)
			return new MMunicipality(ctx, C_Municipality_ID, trxName);
		
		MMunicipality retVal = s_municipalities.get(ctx, C_Municipality_ID, e -> new MMunicipality(Env.getCtx(), e));
		
		if (retVal != null)
			return retVal;
		
		retVal = new MMunicipality(ctx, C_Municipality_ID, trxName);
		
		if (retVal.get_ID() == C_Municipality_ID)
		{
			s_municipalities.put(C_Municipality_ID, retVal, e -> new MMunicipality(Env.getCtx(), e));
			return retVal;
		}
		
		return null;
	}
	
	public static MMunicipality get(int C_Municipality_ID, String trxName) {
		return get(Env.getCtx(), C_Municipality_ID, trxName);
	}
	
	public static MMunicipality get(int C_Municipality_ID) {
		return get(C_Municipality_ID, null);
	}
	
	public static MMunicipality getCopy(Properties ctx, int C_Municipality_ID, String trxName) {
		MMunicipality retVal = get(C_Municipality_ID, trxName);
		
		if (retVal != null)
			return new MMunicipality(ctx, retVal, trxName);
		return null;
	}
	
	public static synchronized MMunicipality[] getMunicipalities(Comparator<MMunicipality> comparator) {
		comparator = Optional.ofNullable(comparator).orElse(MMunicipality::sortById);
		
		if (s_municipalities.size() == 0)
			loadAllMunicipalities();
		
		return s_municipalities
				.values()
				.stream()
				.sorted(comparator)
				.toArray(MMunicipality[]::new);
	}
	
	public static int sortById(MMunicipality m1, MMunicipality m2) {
		return m1.get_ID() < m2.get_ID() ? -1
				: (m1.get_ID() == m2.get_ID() ? 0 : 1);
	}
	
	public static synchronized MMunicipality[] getMunicipalities(int C_Region_ID) {
		if (s_municipalities.isEmpty())
			loadAllMunicipalities();
		
		if (C_Region_ID <= 0)
			return new MMunicipality[0];
		
		return s_municipalities.values().stream()
				.filter(municipality -> municipality.getC_Region_ID() == C_Region_ID)
				.sorted(MMunicipality::sortByName)
				.toArray(MMunicipality[]::new);
	}
	
	public static int sortByName(MMunicipality m1, MMunicipality m2) {
		return m1.getName().compareTo(m2.getName());
	}
	
	public static synchronized void loadAllMunicipalities() {
		s_municipalities.clear();
		List<MMunicipality> municipalities = null;
		
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
			s_municipalities.put(municipality.get_ID(), municipality, e -> new MMunicipality(Env.getCtx(), e));
		}
		
		if (s_log.isLoggable(Level.FINE)) s_log.fine(s_municipalities.size() + " Municipalities Loaded");
	}
	
	@Override
	public MMunicipality markImmutable() {
		if (is_Immutable())
			return this;
		
		makeImmutable();
		return this;
	}
}
