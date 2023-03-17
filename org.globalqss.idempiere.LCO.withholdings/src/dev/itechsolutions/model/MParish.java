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

/**
 * 
 * @author Argenis Rodríguez
 *
 */
public class MParish extends X_C_Parish implements ImmutablePOSupport {
	
	private static final long serialVersionUID = 5100993207002960116L;
	
	private static final CLogger s_log = CLogger.getCLogger(MParish.class);
	
	private static final ImmutableIntPOCache<Integer, MParish> s_parishes = new ImmutableIntPOCache<Integer, MParish>(Table_Name, 300, 0);
	
	public MParish(Properties ctx, int C_Parish_ID, String trxName) {
		super(ctx, C_Parish_ID, trxName);
	}
	
	public MParish(Properties ctx, int C_Parish_ID, String trxName, String... virtualColumns) {
		super(ctx, C_Parish_ID, trxName, virtualColumns);
	}
	
	public MParish(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
	
	public MParish(Properties ctx, MParish copy, String trxName) {
		this(ctx, 0, trxName);
		copyPO(copy);
	}
	
	public MParish(Properties ctx, MParish copy) {
		this(ctx, copy, null);
	}
	
	public MParish(MParish copy) {
		this(Env.getCtx(), copy);
	}
	
	public static MParish get(Properties ctx, int C_Parish_ID, String trxName) {
		if (C_Parish_ID == 0)
			new MParish(ctx, C_Parish_ID, trxName);
		
		MParish retVal = s_parishes.get(ctx, C_Parish_ID, e -> new MParish(Env.getCtx(), e));
		
		if (retVal != null)
			return retVal;
		
		retVal = new MParish(ctx, C_Parish_ID, trxName);
		
		if (retVal.get_ID() == C_Parish_ID)
		{
			s_parishes.put(C_Parish_ID, retVal, e -> new MParish(Env.getCtx(), e));
			return retVal;
		}
		
		return null;
	}
	
	public static MParish get(int C_Parish_ID, String trxName) {
		return get(Env.getCtx(), C_Parish_ID, trxName);
	}
	
	public static MParish get(int C_Parish_ID) {
		return get(C_Parish_ID, null);
	}
	
	public static synchronized MParish[] getAllParishes(Comparator<MParish> comparator) {
		comparator = Optional.ofNullable(comparator).orElse(MParish::sortById);
		
		if (s_parishes.size() == 0)
			loadAllParishes();
		
		return s_parishes.values().stream()
				.sorted(comparator)
				.toArray(MParish[]::new);
	}
	
	public static synchronized MParish[] getParishes(int C_Municipality_ID) {
		if (s_parishes.isEmpty())
			loadAllParishes();
		
		if (C_Municipality_ID <= 0)
			return new MParish[0];
		
		return s_parishes.values().stream()
				.filter(parish -> parish.getC_Municipality_ID() == C_Municipality_ID)
				.sorted(MParish::sortByName)
				.toArray(MParish[]::new);
	}
	
	public static int sortByName(MParish p1, MParish p2) {
		return p1.getName().compareTo(p2.getName());
	}
	
	public static int sortById(MParish p1, MParish p2) {
		return p1.get_ID() < p2.get_ID() ? -1
				: (p1.get_ID() == p2.get_ID() ? 0 : 1);
	}
	
	public static synchronized void loadAllParishes() {
		s_parishes.clear();
		List<MParish> parishes = null;
		
		try {
			PO.setCrossTenantSafe();
			parishes = new Query(Env.getCtx(), Table_Name, "", null)
					.setOnlyActiveRecords(true)
					.list();
		} finally {
			PO.clearCrossTenantSafe();
		}
		
		for (MParish parish: parishes)
		{
			parish.markImmutable();
			s_parishes.put(parish.get_ID(), parish, e -> new MParish(Env.getCtx(), e));
			
			if (s_log.isLoggable(Level.FINE)) s_log.fine(s_parishes.size() + " Parishes loaded");
		}
	}
	
	@Override
	public PO markImmutable() {
		if (is_Immutable())
			return this;
		
		makeImmutable();
		return this;
	}
}
