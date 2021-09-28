package dev.itechsolutions.model;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.idempiere.cache.ImmutablePOCache;
import org.idempiere.cache.ImmutablePOSupport;

import dev.itechsolutions.util.POUtil;

/**
 * 
 * @author Argenis Rodríguez
 *
 */
public class MParish extends X_C_Parish implements ImmutablePOSupport {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/** Parish Cache */
	private static ImmutablePOCache<Integer, MParish> s_parishes = new ImmutablePOCache<Integer, MParish>(Table_Name, Table_Name, 100, 0, false, 0);
	
	/** Default Parish */
	private static MParish s_default = null;
	
	/**
	 * 
	 * @param ctx
	 * @param C_Parish_ID
	 * @param trxName
	 */
	public MParish(Properties ctx, int C_Parish_ID, String trxName) {
		super(ctx, C_Parish_ID, trxName);
	}
	
	/**
	 * 
	 * @param ctx
	 * @param rs
	 * @param trxName
	 */
	public MParish(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
	
	/**
	 * 
	 * @param copy
	 */
	public MParish(MParish copy) {
		this(Env.getCtx(), copy);
	}
	
	/**
	 * 
	 * @param ctx
	 * @param copy
	 */
	public MParish(Properties ctx, MParish copy) {
		this(ctx, copy, null);
	}
	
	/**
	 * 
	 * @param ctx
	 * @param copy
	 * @param trxName
	 */
	public MParish(Properties ctx, MParish copy, String trxName) {
		this(ctx, 0, trxName);
		copyPO(copy);
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @param C_Parish_ID
	 * @return
	 */
	public static synchronized MParish get(int C_Parish_ID) {
		return get(Env.getCtx(), C_Parish_ID);
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @param ctx
	 * @param C_Parish_ID
	 * @return
	 */
	public static synchronized MParish get(Properties ctx, int C_Parish_ID) {
		if (s_parishes.size() == 0)
			loadAllParishes();
		
		MParish parish = s_parishes.get(ctx, C_Parish_ID, e -> new MParish(ctx, e));
		
		if (parish != null)
			return parish;
		
		parish = new MParish(ctx, C_Parish_ID, null);
		
		if (parish.get_ID() == C_Parish_ID)
		{
			s_parishes.put(parish.get_ID(), parish);
			return parish;
		}
		
		return null;
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @return
	 */
	public static synchronized MParish getDefault() {
		return s_default;
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @param C_Municipality_ID
	 * @return
	 */
	public static synchronized MParish[] getParishes(int C_Municipality_ID) {
		
		if (s_parishes.size() == 0)
			loadAllParishes();
		
		MParish[] parishes = s_parishes.values().stream()
				.filter(parish -> parish.getC_Municipality_ID() == C_Municipality_ID)
				.toArray(MParish[]::new);
		
		Arrays.sort(parishes, POUtil.getComparator());
		
		return parishes;
	}
	
	/**
	 * @author Argenis Rodríguez
	 */
	private static void loadAllParishes() {
		s_parishes.clear();
		List<MParish> parishes = new ArrayList<MParish>();
		
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
			s_parishes.put(parish.get_ID(), parish);
			
			if (parish.isDefault())
				s_default = parish;
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
