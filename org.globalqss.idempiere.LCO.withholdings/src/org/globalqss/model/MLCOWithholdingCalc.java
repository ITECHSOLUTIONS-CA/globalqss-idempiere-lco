package org.globalqss.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Properties;

import org.compiere.model.MConversionRate;
import org.compiere.model.MInvoice;
import org.compiere.model.MPriceList;
import org.compiere.model.PO;
import org.compiere.util.Env;
import org.idempiere.cache.ImmutableIntPOCache;
import org.idempiere.cache.ImmutablePOSupport;

import dev.itechsolutions.exception.NoCurrencyConversionException;

public class MLCOWithholdingCalc extends X_LCO_WithholdingCalc implements ImmutablePOSupport {
	
	private static final long serialVersionUID = 1L;
	
	private static final ImmutableIntPOCache<Integer, MLCOWithholdingCalc> s_cache = new ImmutableIntPOCache<Integer, MLCOWithholdingCalc>(Table_Name, 120, 120);
	
	private HashMap<Integer, BigDecimal> rateCache = new HashMap<Integer, BigDecimal>(5);
	
	private X_LCO_WithholdingType m_type = null;
	
	public MLCOWithholdingCalc(Properties ctx, int LCO_WithholdingCalc_ID, String trxName) {
		super(ctx, LCO_WithholdingCalc_ID, trxName);
	}
	
	public MLCOWithholdingCalc(Properties ctx, int LCO_WithholdingCalc_ID, String trxName, String... virtualColumns) {
		super(ctx, LCO_WithholdingCalc_ID, trxName, virtualColumns);
	}
	
	public MLCOWithholdingCalc(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
	
	public MLCOWithholdingCalc(Properties ctx, MLCOWithholdingCalc copy, String trxName) {
		this(ctx, 0, trxName);
		copyPO(copy);
	}
	
	public MLCOWithholdingCalc(Properties ctx, MLCOWithholdingCalc copy) {
		this(ctx, copy, null);
	}
	
	public MLCOWithholdingCalc(MLCOWithholdingCalc copy) {
		this(Env.getCtx(), copy);
	}
	
	public static MLCOWithholdingCalc get(Properties ctx, int LCO_WithholdingCalc_ID, String trxName) {
		if (LCO_WithholdingCalc_ID == 0)
			return new MLCOWithholdingCalc(ctx, LCO_WithholdingCalc_ID, trxName);
		
		if (s_cache.containsKey(LCO_WithholdingCalc_ID))
			return s_cache.get(LCO_WithholdingCalc_ID);
		
		MLCOWithholdingCalc calc = new MLCOWithholdingCalc(ctx, LCO_WithholdingCalc_ID, trxName);
		
		if (calc.get_ID() == LCO_WithholdingCalc_ID)
		{
			s_cache.put(LCO_WithholdingCalc_ID, calc, e -> new MLCOWithholdingCalc(Env.getCtx(), calc));
			return calc;
		}
		
		return null;
	}
	
	public static MLCOWithholdingCalc get(int LCO_WithholdingCalc_ID, String trxName) {
		return get(Env.getCtx(), LCO_WithholdingCalc_ID, trxName);
	}
	
	public static MLCOWithholdingCalc getCopy(Properties ctx, int LCO_WithholdingCalc_ID, String trxName) {
		MLCOWithholdingCalc retVal = get(LCO_WithholdingCalc_ID, trxName);
		
		if (retVal != null)
			return new MLCOWithholdingCalc(ctx, retVal, trxName);
		return null;
	}
	
	public void setWithholdingType(X_LCO_WithholdingType type) {
		if (type.get_ID() != getLCO_WithholdingType_ID())
			setLCO_WithholdingType_ID(type.get_ID());
		m_type = type;
	}
	
	@Override
	public I_LCO_WithholdingType getLCO_WithholdingType() throws RuntimeException {
		if (m_type != null && getLCO_WithholdingType_ID() != m_type.get_ID())
			m_type = null;
		
		if (m_type == null && getLCO_WithholdingType_ID() > 0)
			m_type = (X_LCO_WithholdingType) super.getLCO_WithholdingType();
		
		return m_type;
	}
	
	public int getC_Currency_ID() {
		
		if (getLCO_WithholdingType_ID() > 0)
			return getLCO_WithholdingType().getC_Currency_ID();
		
		return 0;
	}
	
	public BigDecimal getRate(MInvoice inv) {
		if (rateCache.containsKey(inv.get_ID()))
			return rateCache.get(inv.get_ID());
		
		BigDecimal rate = MConversionRate.getRate(getC_Currency_ID(), inv.getC_Currency_ID()
				, inv.getDateInvoiced(), inv.getC_ConversionType_ID()
				, inv.getAD_Client_ID(), inv.getAD_Org_ID());
		
		if (rate == null)
			throw new NoCurrencyConversionException(getC_Currency_ID(), inv.getC_Currency_ID()
					, inv.getDateInvoiced(), inv.getC_ConversionType_ID()
					, inv.getAD_Client_ID(), inv.getAD_Org_ID());
		
		rateCache.put(inv.get_ID(), rate);
		return rate;
	}
	
	public BigDecimal getConvertedAmount(MInvoice inv, BigDecimal amount) {
		if (amount == null || BigDecimal.ZERO.compareTo(amount) == 0)
			return amount;
		
		BigDecimal rate = getRate(inv);
		
		amount = amount.multiply(rate);
		int stdPrecision = MPriceList.getStandardPrecision(getCtx(), inv.getM_PriceList_ID());
		
		if (amount.scale() > stdPrecision)
			amount = amount.setScale(stdPrecision, RoundingMode.HALF_UP);
		
		return amount;
	}
	
	public BigDecimal getThresholdmin(MInvoice inv) {
		return getConvertedAmount(inv, getThresholdmin());
	}
	
	public BigDecimal getThresholdMax(MInvoice inv) {
		return getConvertedAmount(inv, getThresholdMax());
	}
	
	public BigDecimal getAmountRefunded(MInvoice inv) {
		return getConvertedAmount(inv, getAmountRefunded());
	}
	
	@Override
	public PO markImmutable() {
		
		if (is_Immutable())
			return this;
		
		makeImmutable();
		return this;
	}
}
