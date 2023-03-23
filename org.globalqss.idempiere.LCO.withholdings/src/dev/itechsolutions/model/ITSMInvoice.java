package dev.itechsolutions.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.MBPartner;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MConversionRate;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceBatch;
import org.compiere.model.MInvoiceBatchLine;
import org.compiere.model.MOrder;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MPriceList;
import org.compiere.model.MSysConfig;
import org.compiere.model.MTax;
import org.compiere.model.POInfo;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfo;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.globalqss.model.LCO_MInvoice;
import org.globalqss.model.MLCOInvoiceWithholding;
import org.globalqss.model.MLCOWithholdingCalc;
import org.globalqss.model.X_LCO_WithholdingCalc;
import org.globalqss.model.X_LCO_WithholdingRule;
import org.globalqss.model.X_LCO_WithholdingRuleConf;
import org.globalqss.model.X_LCO_WithholdingType;

import dev.itechsolutions.exception.NoCurrencyConversionException;
import dev.itechsolutions.util.ColumnUtils;
import dev.itechsolutions.util.POUtil;
import dev.itechsolutions.util.ProcessInfoUtil;
import dev.itechsolutions.util.TimestampUtil;

/**
 * 
 * @author Argenis Rodríguez
 *
 */
public class ITSMInvoice extends LCO_MInvoice {
	
	private static final long serialVersionUID = -9042047457042108286L;
	
	private static CLogger s_log = CLogger.getCLogger(ITSMInvoice.class);
	
	public ITSMInvoice(MInOut ship, Timestamp invoiceDate) {
		super(ship, invoiceDate);
	}
	
	public ITSMInvoice(MInvoice copy) {
		super(copy);
	}
	
	public ITSMInvoice(MInvoiceBatch batch, MInvoiceBatchLine line) {
		super(batch, line);
	}
	
	public ITSMInvoice(MOrder order, int C_DocTypeTarget_ID, Timestamp invoiceDate) {
		super(order, C_DocTypeTarget_ID, invoiceDate);
	}
	
	public ITSMInvoice(Properties ctx, int C_Invoice_ID, String trxName, String... virtualColumns) {
		super(ctx, C_Invoice_ID, trxName, virtualColumns);
	}
	
	public ITSMInvoice(Properties ctx, int C_Invoice_ID, String trxName) {
		super(ctx, C_Invoice_ID, trxName);
	}
	
	public ITSMInvoice(Properties ctx, MInvoice copy, String trxName) {
		super(ctx, copy, trxName);
	}
	
	public ITSMInvoice(Properties ctx, MInvoice copy) {
		super(ctx, copy);
	}
	
	public ITSMInvoice(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
	
	public static ITSMInvoice[] getOfVoucherWithholding(MITSVoucherWithholding voucher) {
		ArrayList<Object> params = new ArrayList<Object>();
		POInfo info = POInfo.getPOInfo(voucher.getCtx(), Table_ID);
		
		StringBuilder sql = info.buildSelect(true, false)
				.append(" INNER JOIN C_DocType dt ON dt.C_DocType_ID = C_Invoice.C_DocType_ID")
				.append(" LEFT JOIN LCO_InvoiceWithholding iw ON iw.C_Invoice_ID = C_Invoice.C_Invoice_ID"
						+ " AND iw.LCO_WithholdingType_ID = ?");
		
		sql = new StringBuilder(POUtil.addColumnsToSql(sql.toString()
				, "iw.LCO_InvoiceWithholding_ID"));
		
		sql.append(" WHERE C_Invoice.IsSOTrx = ?");
		params.add(voucher.getLCO_WithholdingType_ID());
		params.add(voucher.isSOTrx());
		
		ArrayList<ITSMInvoice> invoices = new ArrayList<ITSMInvoice>();
		
		if (voucher.getC_Invoice_ID() > 0)
		{
			sql.append(" AND C_Invoice.C_Invoice_ID = ?");
			params.add(voucher.getC_Invoice_ID());
		}
		else
		{
			if (voucher.getC_BPartner_ID() > 0)
			{
				sql.append(" AND C_Invoice.C_BPartner_ID = ?");
				params.add(voucher.getC_BPartner_ID());
			}
			
			if (voucher.getDateFrom() != null && voucher.getDateTo() != null)
			{
				Timestamp dateFrom = TimestampUtil.startOfDay(voucher.getDateFrom());
				Timestamp dateTo = TimestampUtil.endOfDay(voucher.getDateTo());
				
				sql.append(" AND C_Invoice.DateInvoiced BETWEEN ? AND ?");
				params.add(dateFrom);
				params.add(dateTo);
			}
			else if (voucher.getDateFrom() != null)
			{
				Timestamp dateFrom = TimestampUtil.startOfDay(voucher.getDateFrom());
				
				sql.append(" AND C_Invoice.DateInvoiced >= ?");
				params.add(dateFrom);
			}
			else if (voucher.getDateTo() != null)
			{
				Timestamp dateTo = TimestampUtil.endOfDay(voucher.getDateTo());
				
				sql.append(" AND C_Invoice.DateInvoiced <= ?");
				params.add(dateTo);
			}
		}
		
		if (voucher.getAD_Org_ID() > 0)
		{
			sql.append(" AND C_Invoice.AD_Org_ID = ?");
			params.add(voucher.getAD_Org_ID());
		}
		
		sql.append(" AND C_Invoice.DocStatus = 'CO' AND dt.GenerateWithholding IN ('Y', 'A')");
		
		if (!MSysConfig.getBooleanValue(ColumnUtils.SYSCONFIG_LVE_GenerateInvoiceWithholdingIsPaid
				, false, voucher.getAD_Client_ID()))
			sql.append(" AND (C_Invoice.IsPaid = 'N' OR iw.LCO_InvoiceWithholding_ID IS NOT NULL)");
		
		sql.append(" AND NOT EXISTS(")
			.append("SELECT 1 FROM ITS_VoucherWithholding vw")
			.append(" INNER JOIN LCO_InvoiceWithholding iwh ON iwh.ITS_VoucherWithholding_ID = vw.ITS_VoucherWithholding_ID")
			.append(" WHERE iwh.C_Invoice_ID = C_Invoice.C_Invoice_ID")
			.append(" AND vw.DocStatus NOT IN ('VO', 'RE')")
			.append(" AND vw.LCO_WithholdingType_ID = ?")
		.append(")");
		params.add(voucher.getLCO_WithholdingType_ID());
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			pstmt = DB.prepareStatement(sql.toString(), voucher.get_TrxName());
			DB.setParameters(pstmt, params);
			
			rs = pstmt.executeQuery();
			
			while (rs.next())
			{
				ITSMInvoice invoice = new ITSMInvoice(voucher.getCtx(), rs, voucher.get_TrxName());
				invoice.set_Attribute(ColumnUtils.ATTRIBUTE_LCO_InvoiceWithholding_ID, rs.getInt("LCO_InvoiceWithholding_ID"));
				
				invoices.add(invoice);
			}
		} catch (SQLException e) {
			s_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		} finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		
		return invoices.toArray(ITSMInvoice[]::new);
	}
	
	public static int recalcWithholdings(MInvoice invoice) {
		return recalcWithholdings(invoice, null, null);
	}
	
	public static MLCOInvoiceWithholding recalculateInvoiceWithholding(MLCOInvoiceWithholding iwh, ProcessInfo pInfo) {
		
		if (!MSysConfig.getBooleanValue(ColumnUtils.SYSCONFIG_LCO_USE_WITHHOLDINGS
				, true, iwh.getAD_Client_ID()))
			return iwh;
		
		MInvoice invoice = iwh.getC_Invoice();
		MDocType dt = MDocType.get(invoice.getCtx(), invoice.getC_DocTypeTarget_ID());
		String genWh = Optional.ofNullable(dt.get_ValueAsString(ColumnUtils.COLUMNNAME_GenerateWithholding))
				.orElse(ColumnUtils.GENERATEWITHHOLDING_No);
		
		if (ColumnUtils.GENERATEWITHHOLDING_No.equals(genWh))
			return iwh;
		
		s_log.info("");
		BigDecimal totwith = BigDecimal.ZERO;
		
		// Fill variables normally needed
		// BP variables
		MBPartner bp = new MBPartner(invoice.getCtx(), invoice.getC_BPartner_ID(), invoice.get_TrxName());
		int bp_isic_id = bp.get_ValueAsInt(ColumnUtils.COLUMNNAME_LCO_ISIC_ID);
		int bp_taxpayertype_id = bp.get_ValueAsInt(ColumnUtils.COLUMNNAME_LCO_TaxPayerType_ID);
		MBPartnerLocation mbpl = new MBPartnerLocation(invoice.getCtx(), invoice.getC_BPartner_Location_ID(), invoice.get_TrxName());
		MITSLocation bpl = MITSLocation.getCopy(invoice.getCtx(), mbpl.getC_Location_ID(), invoice.get_TrxName());
		int bp_city_id = bpl.getC_City_ID();
		int bp_Municipality_ID = bpl.getC_Municipality_ID();
		// OrgInfo variables
		MOrgInfo oi = MOrgInfo.getCopy(invoice.getCtx(), invoice.getAD_Org_ID(), invoice.get_TrxName());
		int org_isic_id = oi.get_ValueAsInt(ColumnUtils.COLUMNNAME_LCO_ISIC_ID);
		int org_taxpayertype_id = oi.get_ValueAsInt(ColumnUtils.COLUMNNAME_LCO_TaxPayerType_ID);
		MITSLocation ol = MITSLocation.getCopy(invoice.getCtx(), oi.getC_Location_ID(), invoice.get_TrxName());
		int org_city_id = ol.getC_City_ID();
		int org_Municipality_ID = ol.getC_Municipality_ID();
		
		StringBuilder where = new StringBuilder("IsSOTrx = ? AND LCO_WithholdingType_ID = ?");
		ArrayList<Object> params = new ArrayList<Object>();
		params.add(invoice.isSOTrx());
		params.add(iwh.getLCO_WithholdingType_ID());
		
		// Search withholding types applicable depending on IsSOTrx
		List<X_LCO_WithholdingType> wts = new Query(invoice.getCtx(), X_LCO_WithholdingType.Table_Name, where.toString(), invoice.get_TrxName())
			.setOnlyActiveRecords(true)
			.setClient_ID()
			.setParameters(params)
			.list();
		for (X_LCO_WithholdingType wt : wts)
		{
			// For each applicable withholding
			s_log.info("Withholding Type: "+wt.getLCO_WithholdingType_ID()+"/"+wt.getName());
			
			X_LCO_WithholdingRuleConf wrc = new Query(invoice.getCtx(),
					X_LCO_WithholdingRuleConf.Table_Name,
					"LCO_WithholdingType_ID=?",
					invoice.get_TrxName())
					.setOnlyActiveRecords(true)
					.setParameters(wt.getLCO_WithholdingType_ID())
					.first();
			if (wrc == null) {
				if (pInfo == null)
					s_log.warning("No LCO_WithholdingRuleConf for LCO_WithholdingType = "+wt.getLCO_WithholdingType_ID());
				else
					ProcessInfoUtil.addLog(pInfo, wt.get_ID(), TimestampUtil.now()
							, BigDecimal.ZERO
							, Msg.getMsg(invoice.getCtx(), "WithholdingRuleConfigNotFound"
									, new Object[] {wt.getName()})
							, X_LCO_WithholdingType.Table_ID, wt.get_ID());
				continue;
			}
			
			// look for applicable rules according to config fields (rule)
			StringBuffer wherer = new StringBuffer(" LCO_WithholdingRule_ID=? AND ValidFrom<=? ");
			List<Object> paramsr = new ArrayList<Object>();
			paramsr.add(iwh.getLCO_WithholdingRule_ID());
			paramsr.add(invoice.getDateInvoiced());
			if (wrc.isUseBPISIC()) {
				
				String validMunicipality = oi.get_ValueAsString(ColumnUtils.COLUMNNAME_ValidMunicipality);
				
				if (ColumnUtils.VALIDATEMUNICIPALITY_None.equals(validMunicipality)
						|| (ColumnUtils.VALIDATEMUNICIPALITY_Local.equals(validMunicipality)
								&& bp_Municipality_ID != org_Municipality_ID)
						|| (ColumnUtils.VALIDATEMUNICIPALITY_OutSiders.equals(validMunicipality)
								&& bp_Municipality_ID == org_Municipality_ID))
					continue;
				
				wherer.append(" AND LCO_BP_ISIC_ID=? ");
				paramsr.add(bp_isic_id);
			}
			if (wrc.isUseBPTaxPayerType()) {
				wherer.append(" AND LCO_BP_TaxPayerType_ID=? ");
				paramsr.add(bp_taxpayertype_id);
			}
			if (wrc.isUseOrgISIC()) {
				wherer.append(" AND LCO_Org_ISIC_ID=? ");
				paramsr.add(org_isic_id);
			}
			if (wrc.isUseOrgTaxPayerType()) {
				wherer.append(" AND LCO_Org_TaxPayerType_ID=? ");
				paramsr.add(org_taxpayertype_id);
			}
			if (wrc.isUseBPCity()) {
				wherer.append(" AND LCO_BP_City_ID=? ");
				paramsr.add(bp_city_id);
				if (bp_city_id <= 0)
				{
					if (pInfo == null)
						s_log.warning("Possible configuration error bp city is used but not set");
					else
						ProcessInfoUtil.addLog(pInfo, invoice.get_ID()
								, TimestampUtil.now(), BigDecimal.ZERO
								, Msg.getMsg(invoice.getCtx(), "BPCityNotSet"), Table_ID, invoice.get_ID());
				}
			}
			if (wrc.isUseOrgCity()) {
				wherer.append(" AND LCO_Org_City_ID=? ");
				paramsr.add(org_city_id);
				if (org_city_id <= 0)
				{
					if (pInfo == null)
						s_log.warning("Possible configuration error org city is used but not set");
					else
						ProcessInfoUtil.addLog(pInfo, invoice.get_ID()
								, TimestampUtil.now(), BigDecimal.ZERO
								, Msg.getMsg(invoice.getCtx(), "OrgCityNotSet"), Table_ID, invoice.get_ID());
				}
			}
			
			// Add withholding categories of lines
			if (wrc.isUseWithholdingCategory()) {
				// look the conf fields
				String sqlwcs =
					"SELECT DISTINCT COALESCE (p.LCO_WithholdingCategory_ID, COALESCE (c.LCO_WithholdingCategory_ID, 0)) "
					+ "  FROM C_InvoiceLine il "
					+ "  LEFT OUTER JOIN M_Product p ON (il.M_Product_ID = p.M_Product_ID) "
					+ "  LEFT OUTER JOIN C_Charge c ON (il.C_Charge_ID = c.C_Charge_ID) "
					+ "  WHERE C_Invoice_ID = ? AND il.IsActive='Y' AND (il.M_Product_ID>0 OR il.C_Charge_ID>0)";
				int[] wcids = DB.getIDsEx(invoice.get_TrxName(), sqlwcs, new Object[] {invoice.getC_Invoice_ID()});
				boolean addedlines = false;
				for (int i = 0; i < wcids.length; i++) {
					int wcid = wcids[i];
					if (wcid > 0) {
						if (! addedlines) {
							wherer.append(" AND LCO_WithholdingCategory_ID IN (");
							addedlines = true;
						} else {
							wherer.append(",");
						}
						wherer.append(wcid);
					}
				}
				if (addedlines)
					wherer.append(") ");
			}
			
			// Add tax categories of lines
			if (wrc.isUseProductTaxCategory()) {
				// look the conf fields
				String sqlwct =
					"SELECT DISTINCT COALESCE (p.C_TaxCategory_ID, COALESCE (c.C_TaxCategory_ID, 0)) "
					+ "  FROM C_InvoiceLine il "
					+ "  LEFT OUTER JOIN M_Product p ON (il.M_Product_ID = p.M_Product_ID) "
					+ "  LEFT OUTER JOIN C_Charge c ON (il.C_Charge_ID = c.C_Charge_ID) "
					+ "  WHERE C_Invoice_ID = ? AND il.IsActive='Y' AND (il.M_Product_ID>0 OR il.C_Charge_ID>0)";
				int[] wcids = DB.getIDsEx(invoice.get_TrxName(), sqlwct, new Object[] {invoice.getC_Invoice_ID()});
				boolean addedlines = false;
				for (int i = 0; i < wcids.length; i++) {
					int wcid = wcids[i];
					if (wcid > 0) {
						if (! addedlines) {
							wherer.append(" AND C_TaxCategory_ID IN (");
							addedlines = true;
						} else {
							wherer.append(",");
						}
						wherer.append(wcid);
					}
				}
				if (addedlines)
					wherer.append(") ");
			}
			
			List<X_LCO_WithholdingRule> wrs = new Query(invoice.getCtx(), X_LCO_WithholdingRule.Table_Name, wherer.toString(), invoice.get_TrxName())
				.setOnlyActiveRecords(true)
				.setParameters(paramsr)
				.list();
			
			if(wrs.isEmpty())
			{
				iwh.setTaxBaseAmt(BigDecimal.ZERO);
				iwh.setTaxAmt(BigDecimal.ZERO);
			}
			
			for (X_LCO_WithholdingRule wr : wrs)
			{
				// for each applicable rule
				// bring record for withholding calculation
				//X_LCO_WithholdingCalc wc = (X_LCO_WithholdingCalc) wr.getLCO_WithholdingCalc();
				MLCOWithholdingCalc wc = MLCOWithholdingCalc.getCopy(invoice.getCtx(), wr.getLCO_WithholdingCalc_ID(), invoice.get_TrxName());
				if (wc == null || wc.getLCO_WithholdingCalc_ID() == 0) {
					s_log.severe("Rule without calc " + wr.getLCO_WithholdingRule_ID());
					continue;
				}
				wc.setWithholdingType(wt);
				
				// bring record for tax
				MTax tax = new MTax(invoice.getCtx(), wc.getC_Tax_ID(), invoice.get_TrxName());
				
				s_log.info("WithholdingRule: "+wr.getLCO_WithholdingRule_ID()+"/"+wr.getName()
						+" BaseType:"+wc.getBaseType()
						+" Calc: "+wc.getLCO_WithholdingCalc_ID()+"/"+wc.getName()
						+" CalcOnInvoice:"+wc.isCalcOnInvoice()
						+" Tax: "+tax.getC_Tax_ID()+"/"+tax.getName());
				
				// calc base
				// apply rule to calc base
				BigDecimal base = null;
				
				if (wc.getBaseType() == null) {
					s_log.severe("Base Type null in calc record "+wr.getLCO_WithholdingCalc_ID());
				} else if (wc.getBaseType().equals(X_LCO_WithholdingCalc.BASETYPE_Document)) {
					base = invoice.getTotalLines();
				} else if (wc.getBaseType().equals(X_LCO_WithholdingCalc.BASETYPE_Line)) {
					List<Object> paramslca = new ArrayList<Object>();
					paramslca.add(invoice.getC_Invoice_ID());
					String sqllca;
					if (wrc.isUseWithholdingCategory() && wrc.isUseProductTaxCategory()) {
						// base = lines of the withholding category and tax category
						sqllca =
							"SELECT SUM (LineNetAmt) "
							+ "  FROM C_InvoiceLine il "
							+ " WHERE IsActive='Y' AND C_Invoice_ID = ? "
							+ "   AND (   EXISTS ( "
							+ "              SELECT 1 "
							+ "                FROM M_Product p "
							+ "               WHERE il.M_Product_ID = p.M_Product_ID "
							+ "                 AND p.C_TaxCategory_ID = ? "
							+ "                 AND p.LCO_WithholdingCategory_ID = ?) "
							+ "        OR EXISTS ( "
							+ "              SELECT 1 "
							+ "                FROM C_Charge c "
							+ "               WHERE il.C_Charge_ID = c.C_Charge_ID "
							+ "                 AND c.C_TaxCategory_ID = ? "
							+ "                 AND c.LCO_WithholdingCategory_ID = ?) "
							+ "       ) ";
						paramslca.add(wr.getC_TaxCategory_ID());
						paramslca.add(wr.getLCO_WithholdingCategory_ID());
						paramslca.add(wr.getC_TaxCategory_ID());
						paramslca.add(wr.getLCO_WithholdingCategory_ID());
					} else if (wrc.isUseWithholdingCategory()) {
						// base = lines of the withholding category
						sqllca =
							"SELECT SUM (LineNetAmt) "
							+ "  FROM C_InvoiceLine il "
							+ " WHERE IsActive='Y' AND C_Invoice_ID = ? "
							+ "   AND (   EXISTS ( "
							+ "              SELECT 1 "
							+ "                FROM M_Product p "
							+ "               WHERE il.M_Product_ID = p.M_Product_ID "
							+ "                 AND p.LCO_WithholdingCategory_ID = ?) "
							+ "        OR EXISTS ( "
							+ "              SELECT 1 "
							+ "                FROM C_Charge c "
							+ "               WHERE il.C_Charge_ID = c.C_Charge_ID "
							+ "                 AND c.LCO_WithholdingCategory_ID = ?) "
							+ "       ) ";
						paramslca.add(wr.getLCO_WithholdingCategory_ID());
						paramslca.add(wr.getLCO_WithholdingCategory_ID());
					} else if (wrc.isUseProductTaxCategory()) {
						// base = lines of the product tax category
						sqllca =
							"SELECT SUM (LineNetAmt) "
							+ "  FROM C_InvoiceLine il "
							+ " WHERE IsActive='Y' AND C_Invoice_ID = ? "
							+ "   AND (   EXISTS ( "
							+ "              SELECT 1 "
							+ "                FROM M_Product p "
							+ "               WHERE il.M_Product_ID = p.M_Product_ID "
							+ "                 AND p.C_TaxCategory_ID = ?) "
							+ "        OR EXISTS ( "
							+ "              SELECT 1 "
							+ "                FROM C_Charge c "
							+ "               WHERE il.C_Charge_ID = c.C_Charge_ID "
							+ "                 AND c.C_TaxCategory_ID = ?) "
							+ "       ) ";
						paramslca.add(wr.getC_TaxCategory_ID());
						paramslca.add(wr.getC_TaxCategory_ID());
					} else {
						// base = all lines
						sqllca =
							"SELECT SUM (LineNetAmt) "
							+ "  FROM C_InvoiceLine il "
							+ " WHERE IsActive='Y' AND C_Invoice_ID = ? ";
					}
					base = DB.getSQLValueBD(invoice.get_TrxName(), sqllca, paramslca);
				} else if (wc.getBaseType().equals(X_LCO_WithholdingCalc.BASETYPE_Tax)) {
					// if specific tax
					if (wc.getC_BaseTax_ID() != 0) {
						// base = value of specific tax
						String sqlbst = "SELECT SUM(TaxAmt) "
							+ " FROM C_InvoiceTax "
							+ " WHERE IsActive='Y' AND C_Invoice_ID = ? "
							+ "   AND C_Tax_ID = ?";
						base = DB.getSQLValueBD(invoice.get_TrxName(), sqlbst, new Object[] {invoice.getC_Invoice_ID(), wc.getC_BaseTax_ID()});
					} else {
						// not specific tax
						// base = value of all taxes
						String sqlbsat = "SELECT SUM(TaxAmt) "
							+ " FROM C_InvoiceTax "
							+ " WHERE IsActive='Y' AND C_Invoice_ID = ? ";
						base = DB.getSQLValueBD(invoice.get_TrxName(), sqlbsat, new Object[] {invoice.getC_Invoice_ID()});
					}
				}
				s_log.info("Base: "+base+ " Thresholdmin:"+wc.getThresholdmin());
				
				// if base between thresholdmin and thresholdmax inclusive
				// if thresholdmax = 0 it is ignored
				if (base != null &&
						base.compareTo(Env.ZERO) != 0 &&
								base.compareTo(wc.getThresholdmin(invoice)) >= 0 &&
						(wc.getThresholdMax(invoice).compareTo(Env.ZERO) == 0 || base.compareTo(wc.getThresholdMax(invoice)) <= 0) &&
						tax.getRate() != null &&
						tax.getRate().compareTo(Env.ZERO) != 0) {
					
					// insert new withholding record
					// with: type, tax, base amt, percent, tax amt, trx date, acct date, rule
					//MLCOInvoiceWithholding iwh = new MLCOInvoiceWithholding(invoice.getCtx(), 0, invoice.get_TrxName());
					iwh.setAD_Org_ID(invoice.getAD_Org_ID());
					iwh.setInvoice(invoice);
					/*iwh.setDateAcct(invoice.getDateAcct());
					iwh.setDateTrx(invoice.getDateInvoiced());*/
					iwh.setIsTaxIncluded(false);
					iwh.setWithholdingRule(wr);
					iwh.setWithholdingCalc(wc);
					iwh.setLCO_WithholdingType_ID(wt.getLCO_WithholdingType_ID());
					iwh.setC_Tax_ID(tax.getC_Tax_ID());
					iwh.setPercent(tax.getRate());
					iwh.setProcessed(false);
					int stdPrecision = MPriceList.getStandardPrecision(invoice.getCtx(), invoice.getM_PriceList_ID());
					BigDecimal taxamt = tax.calculateTax(base, false, stdPrecision);
					BigDecimal amountRefunded = wc.getAmountRefunded(invoice);
					if (amountRefunded.compareTo(Env.ZERO) > 0) {
						taxamt = taxamt.subtract(wc.getAmountRefunded(invoice));
						iwh.setAmountRefunded(amountRefunded);
					}
					iwh.setTaxAmt(taxamt);
					iwh.setTaxBaseAmt(base);
					if (    (  invoice.isSOTrx() && MSysConfig.getBooleanValue("QSSLCO_GenerateWithholdingInactiveSO", false, invoice.getAD_Client_ID(), invoice.getAD_Org_ID()) )
						 || ( !invoice.isSOTrx() && MSysConfig.getBooleanValue("QSSLCO_GenerateWithholdingInactivePO", false, invoice.getAD_Client_ID(), invoice.getAD_Org_ID()) )) {
						iwh.setIsActive(false);
					}
					
					List<MLCOInvoiceWithholding> withholdings = POUtil.getAttribute(invoice
							, ColumnUtils.ATTRIBUTE_LCO_InvoiceWithholdings
							, ArrayList::new);
					
					withholdings.add(iwh);
					
					totwith = totwith.add(taxamt);
					s_log.info("LCO_InvoiceWithholding saved:"+iwh.getTaxAmt());
				}
				else
				{
					iwh.setTaxBaseAmt(BigDecimal.ZERO);
					iwh.setTaxAmt(BigDecimal.ZERO);
				}
			} // while each applicable rule
		} // while type
		
		return iwh;
	}
	
	public static int recalcWithholdings(MInvoice invoice, MITSVoucherWithholding voucher, ProcessInfo pInfo) {
		
		if (!MSysConfig.getBooleanValue(ColumnUtils.SYSCONFIG_LCO_USE_WITHHOLDINGS
				, true, invoice.getAD_Client_ID()))
			return 0;
		
		MDocType dt = MDocType.get(invoice.getCtx(), invoice.getC_DocTypeTarget_ID());
		String genWh = Optional.ofNullable(dt.get_ValueAsString(ColumnUtils.COLUMNNAME_GenerateWithholding))
				.orElse(ColumnUtils.GENERATEWITHHOLDING_No);
		
		if (ColumnUtils.GENERATEWITHHOLDING_No.equals(genWh))
			return 0;
		
		if (voucher != null
				&& POUtil.getAttribute(invoice, ColumnUtils.ATTRIBUTE_LCO_InvoiceWithholding_ID, 0) != 0)
		{
			int LCO_InvoiceWithholding_ID = POUtil.getAttribute(invoice, ColumnUtils.ATTRIBUTE_LCO_InvoiceWithholding_ID, 0);
			MLCOInvoiceWithholding iwh = MLCOInvoiceWithholding.getCopy(invoice.getCtx(), LCO_InvoiceWithholding_ID, invoice.get_TrxName());
			
			BigDecimal rate = MConversionRate.getRate(invoice.getC_Currency_ID(), voucher.getC_Currency_ID()
					, invoice.getDateInvoiced(), invoice.getC_ConversionType_ID()
					, invoice.getAD_Client_ID(), invoice.getAD_Org_ID());
			
			if (rate == null)
			{
				if (pInfo != null)
					ProcessInfoUtil.addLog(pInfo, invoice.get_ID(), TimestampUtil.now()
							, BigDecimal.ZERO, NoCurrencyConversionException.buildMessage(invoice.getC_Currency_ID(), voucher.getC_Currency_ID()
									, invoice.getDateInvoiced(), invoice.getC_ConversionType_ID()
									, invoice.getAD_Client_ID(), invoice.getAD_Org_ID())
							, Table_ID, invoice.get_ID());
				return 0;
			}
			
			//int stdPrecision = MPriceList.getStandardPrecision(invoice.getCtx(), invoice.getM_PriceList_ID());
			MPriceList pl = MPriceList.get(invoice.getM_PriceList_ID());
			int stdPrecision = pl.getPricePrecision();
			
			BigDecimal convertedTaxBaseAmt = iwh.getTaxBaseAmt().multiply(rate);
			BigDecimal convertedTaxAmt = iwh.getTaxAmt().multiply(rate);
			BigDecimal convertedAmountRefunded = iwh.getAmountRefunded().multiply(rate);
			
			if (convertedTaxAmt.scale() > stdPrecision)
				convertedTaxAmt = convertedTaxAmt.setScale(stdPrecision, RoundingMode.HALF_UP);
			
			if (convertedTaxBaseAmt.scale() > stdPrecision)
				convertedTaxBaseAmt = convertedTaxBaseAmt.setScale(stdPrecision, RoundingMode.HALF_UP);
			
			if (convertedAmountRefunded.compareTo(Env.ZERO) > 0)
			{
				if (convertedAmountRefunded.scale() > stdPrecision)
					convertedAmountRefunded = convertedAmountRefunded.setScale(stdPrecision, RoundingMode.HALF_UP);
				
				iwh.setConvertedAmountRefunded(convertedAmountRefunded);
			}
			
			setVoucherWithholding(iwh, voucher.get_ID()
					, convertedTaxBaseAmt
					, convertedTaxAmt);
			iwh.saveEx();
			
			return 0;
		}
		
		int noins = 0;
		s_log.info("");
		BigDecimal totwith = BigDecimal.ZERO;
		
		// Fill variables normally needed
		// BP variables
		MBPartner bp = new MBPartner(invoice.getCtx(), invoice.getC_BPartner_ID(), invoice.get_TrxName());
		int bp_isic_id = bp.get_ValueAsInt(ColumnUtils.COLUMNNAME_LCO_ISIC_ID);
		int bp_taxpayertype_id = bp.get_ValueAsInt(ColumnUtils.COLUMNNAME_LCO_TaxPayerType_ID);
		MBPartnerLocation mbpl = new MBPartnerLocation(invoice.getCtx(), invoice.getC_BPartner_Location_ID(), invoice.get_TrxName());
		MITSLocation bpl = MITSLocation.getCopy(invoice.getCtx(), mbpl.getC_Location_ID(), invoice.get_TrxName());
		int bp_city_id = bpl.getC_City_ID();
		int bp_Municipality_ID = bpl.getC_Municipality_ID();
		// OrgInfo variables
		MOrgInfo oi = MOrgInfo.getCopy(invoice.getCtx(), invoice.getAD_Org_ID(), invoice.get_TrxName());
		int org_isic_id = oi.get_ValueAsInt(ColumnUtils.COLUMNNAME_LCO_ISIC_ID);
		int org_taxpayertype_id = oi.get_ValueAsInt(ColumnUtils.COLUMNNAME_LCO_TaxPayerType_ID);
		MITSLocation ol = MITSLocation.getCopy(invoice.getCtx(), oi.getC_Location_ID(), invoice.get_TrxName());
		int org_city_id = ol.getC_City_ID();
		int org_Municipality_ID = ol.getC_Municipality_ID();
		
		StringBuilder where = new StringBuilder("IsSOTrx = ?");
		ArrayList<Object> params = new ArrayList<Object>();
		params.add(invoice.isSOTrx());
		
		if (voucher != null)
		{
			where.append(" AND LCO_WithholdingType_ID = ?");
			params.add(voucher.getLCO_WithholdingType_ID());
		}
		
		// Search withholding types applicable depending on IsSOTrx
		List<X_LCO_WithholdingType> wts = new Query(invoice.getCtx(), X_LCO_WithholdingType.Table_Name, where.toString(), invoice.get_TrxName())
			.setOnlyActiveRecords(true)
			.setClient_ID()
			.setParameters(params)
			.list();
		for (X_LCO_WithholdingType wt : wts)
		{
			// For each applicable withholding
			s_log.info("Withholding Type: "+wt.getLCO_WithholdingType_ID()+"/"+wt.getName());
			
			X_LCO_WithholdingRuleConf wrc = new Query(invoice.getCtx(),
					X_LCO_WithholdingRuleConf.Table_Name,
					"LCO_WithholdingType_ID=?",
					invoice.get_TrxName())
					.setOnlyActiveRecords(true)
					.setParameters(wt.getLCO_WithholdingType_ID())
					.first();
			if (wrc == null) {
				if (pInfo == null)
					s_log.warning("No LCO_WithholdingRuleConf for LCO_WithholdingType = "+wt.getLCO_WithholdingType_ID());
				else
					ProcessInfoUtil.addLog(pInfo, wt.get_ID(), TimestampUtil.now()
							, BigDecimal.ZERO
							, Msg.getMsg(invoice.getCtx(), "WithholdingRuleConfigNotFound"
									, new Object[] {wt.getName()})
							, X_LCO_WithholdingType.Table_ID, wt.get_ID());
				continue;
			}
			
			// look for applicable rules according to config fields (rule)
			StringBuffer wherer = new StringBuffer(" LCO_WithholdingType_ID=? AND ValidFrom<=? ");
			List<Object> paramsr = new ArrayList<Object>();
			paramsr.add(wt.getLCO_WithholdingType_ID());
			paramsr.add(invoice.getDateInvoiced());
			if (wrc.isUseBPISIC()) {
				
				String validMunicipality = oi.get_ValueAsString(ColumnUtils.COLUMNNAME_ValidMunicipality);
				
				if (ColumnUtils.VALIDATEMUNICIPALITY_None.equals(validMunicipality)
						|| (ColumnUtils.VALIDATEMUNICIPALITY_Local.equals(validMunicipality)
								&& bp_Municipality_ID != org_Municipality_ID)
						|| (ColumnUtils.VALIDATEMUNICIPALITY_OutSiders.equals(validMunicipality)
								&& bp_Municipality_ID == org_Municipality_ID))
					continue;
				
				wherer.append(" AND LCO_BP_ISIC_ID=? ");
				paramsr.add(bp_isic_id);
			}
			if (wrc.isUseBPTaxPayerType()) {
				wherer.append(" AND LCO_BP_TaxPayerType_ID=? ");
				paramsr.add(bp_taxpayertype_id);
			}
			if (wrc.isUseOrgISIC()) {
				wherer.append(" AND LCO_Org_ISIC_ID=? ");
				paramsr.add(org_isic_id);
			}
			if (wrc.isUseOrgTaxPayerType()) {
				wherer.append(" AND LCO_Org_TaxPayerType_ID=? ");
				paramsr.add(org_taxpayertype_id);
			}
			if (wrc.isUseBPCity()) {
				wherer.append(" AND LCO_BP_City_ID=? ");
				paramsr.add(bp_city_id);
				if (bp_city_id <= 0)
				{
					if (pInfo == null)
						s_log.warning("Possible configuration error bp city is used but not set");
					else
						ProcessInfoUtil.addLog(pInfo, invoice.get_ID()
								, TimestampUtil.now(), BigDecimal.ZERO
								, Msg.getMsg(invoice.getCtx(), "BPCityNotSet"), Table_ID, invoice.get_ID());
				}
			}
			if (wrc.isUseOrgCity()) {
				wherer.append(" AND LCO_Org_City_ID=? ");
				paramsr.add(org_city_id);
				if (org_city_id <= 0)
				{
					if (pInfo == null)
						s_log.warning("Possible configuration error org city is used but not set");
					else
						ProcessInfoUtil.addLog(pInfo, invoice.get_ID()
								, TimestampUtil.now(), BigDecimal.ZERO
								, Msg.getMsg(invoice.getCtx(), "OrgCityNotSet"), Table_ID, invoice.get_ID());
				}
			}
			
			// Add withholding categories of lines
			if (wrc.isUseWithholdingCategory()) {
				// look the conf fields
				String sqlwcs =
					"SELECT DISTINCT COALESCE (p.LCO_WithholdingCategory_ID, COALESCE (c.LCO_WithholdingCategory_ID, 0)) "
					+ "  FROM C_InvoiceLine il "
					+ "  LEFT OUTER JOIN M_Product p ON (il.M_Product_ID = p.M_Product_ID) "
					+ "  LEFT OUTER JOIN C_Charge c ON (il.C_Charge_ID = c.C_Charge_ID) "
					+ "  WHERE C_Invoice_ID = ? AND il.IsActive='Y' AND (il.M_Product_ID>0 OR il.C_Charge_ID>0)";
				int[] wcids = DB.getIDsEx(invoice.get_TrxName(), sqlwcs, new Object[] {invoice.getC_Invoice_ID()});
				boolean addedlines = false;
				for (int i = 0; i < wcids.length; i++) {
					int wcid = wcids[i];
					if (wcid > 0) {
						if (! addedlines) {
							wherer.append(" AND LCO_WithholdingCategory_ID IN (");
							addedlines = true;
						} else {
							wherer.append(",");
						}
						wherer.append(wcid);
					}
				}
				if (addedlines)
					wherer.append(") ");
			}
			
			// Add tax categories of lines
			if (wrc.isUseProductTaxCategory()) {
				// look the conf fields
				String sqlwct =
					"SELECT DISTINCT COALESCE (p.C_TaxCategory_ID, COALESCE (c.C_TaxCategory_ID, 0)) "
					+ "  FROM C_InvoiceLine il "
					+ "  LEFT OUTER JOIN M_Product p ON (il.M_Product_ID = p.M_Product_ID) "
					+ "  LEFT OUTER JOIN C_Charge c ON (il.C_Charge_ID = c.C_Charge_ID) "
					+ "  WHERE C_Invoice_ID = ? AND il.IsActive='Y' AND (il.M_Product_ID>0 OR il.C_Charge_ID>0)";
				int[] wcids = DB.getIDsEx(invoice.get_TrxName(), sqlwct, new Object[] {invoice.getC_Invoice_ID()});
				boolean addedlines = false;
				for (int i = 0; i < wcids.length; i++) {
					int wcid = wcids[i];
					if (wcid > 0) {
						if (! addedlines) {
							wherer.append(" AND C_TaxCategory_ID IN (");
							addedlines = true;
						} else {
							wherer.append(",");
						}
						wherer.append(wcid);
					}
				}
				if (addedlines)
					wherer.append(") ");
			}
			
			List<X_LCO_WithholdingRule> wrs = new Query(invoice.getCtx(), X_LCO_WithholdingRule.Table_Name, wherer.toString(), invoice.get_TrxName())
				.setOnlyActiveRecords(true)
				.setParameters(paramsr)
				.list();
			for (X_LCO_WithholdingRule wr : wrs)
			{
				// for each applicable rule
				// bring record for withholding calculation
				//X_LCO_WithholdingCalc wc = (X_LCO_WithholdingCalc) wr.getLCO_WithholdingCalc();
				MLCOWithholdingCalc wc = MLCOWithholdingCalc.getCopy(invoice.getCtx(), wr.getLCO_WithholdingCalc_ID(), invoice.get_TrxName());
				if (wc == null || wc.getLCO_WithholdingCalc_ID() == 0) {
					s_log.severe("Rule without calc " + wr.getLCO_WithholdingRule_ID());
					continue;
				}
				wc.setWithholdingType(wt);
				
				// bring record for tax
				MTax tax = new MTax(invoice.getCtx(), wc.getC_Tax_ID(), invoice.get_TrxName());
				
				s_log.info("WithholdingRule: "+wr.getLCO_WithholdingRule_ID()+"/"+wr.getName()
						+" BaseType:"+wc.getBaseType()
						+" Calc: "+wc.getLCO_WithholdingCalc_ID()+"/"+wc.getName()
						+" CalcOnInvoice:"+wc.isCalcOnInvoice()
						+" Tax: "+tax.getC_Tax_ID()+"/"+tax.getName());
				
				// calc base
				// apply rule to calc base
				BigDecimal base = null;
				
				if (wc.getBaseType() == null) {
					s_log.severe("Base Type null in calc record "+wr.getLCO_WithholdingCalc_ID());
				} else if (wc.getBaseType().equals(X_LCO_WithholdingCalc.BASETYPE_Document)) {
					base = invoice.getTotalLines();
				} else if (wc.getBaseType().equals(X_LCO_WithholdingCalc.BASETYPE_Line)) {
					List<Object> paramslca = new ArrayList<Object>();
					paramslca.add(invoice.getC_Invoice_ID());
					String sqllca;
					if (wrc.isUseWithholdingCategory() && wrc.isUseProductTaxCategory()) {
						// base = lines of the withholding category and tax category
						sqllca =
							"SELECT SUM (LineNetAmt) "
							+ "  FROM C_InvoiceLine il "
							+ " WHERE IsActive='Y' AND C_Invoice_ID = ? "
							+ "   AND (   EXISTS ( "
							+ "              SELECT 1 "
							+ "                FROM M_Product p "
							+ "               WHERE il.M_Product_ID = p.M_Product_ID "
							+ "                 AND p.C_TaxCategory_ID = ? "
							+ "                 AND p.LCO_WithholdingCategory_ID = ?) "
							+ "        OR EXISTS ( "
							+ "              SELECT 1 "
							+ "                FROM C_Charge c "
							+ "               WHERE il.C_Charge_ID = c.C_Charge_ID "
							+ "                 AND c.C_TaxCategory_ID = ? "
							+ "                 AND c.LCO_WithholdingCategory_ID = ?) "
							+ "       ) ";
						paramslca.add(wr.getC_TaxCategory_ID());
						paramslca.add(wr.getLCO_WithholdingCategory_ID());
						paramslca.add(wr.getC_TaxCategory_ID());
						paramslca.add(wr.getLCO_WithholdingCategory_ID());
					} else if (wrc.isUseWithholdingCategory()) {
						// base = lines of the withholding category
						sqllca =
							"SELECT SUM (LineNetAmt) "
							+ "  FROM C_InvoiceLine il "
							+ " WHERE IsActive='Y' AND C_Invoice_ID = ? "
							+ "   AND (   EXISTS ( "
							+ "              SELECT 1 "
							+ "                FROM M_Product p "
							+ "               WHERE il.M_Product_ID = p.M_Product_ID "
							+ "                 AND p.LCO_WithholdingCategory_ID = ?) "
							+ "        OR EXISTS ( "
							+ "              SELECT 1 "
							+ "                FROM C_Charge c "
							+ "               WHERE il.C_Charge_ID = c.C_Charge_ID "
							+ "                 AND c.LCO_WithholdingCategory_ID = ?) "
							+ "       ) ";
						paramslca.add(wr.getLCO_WithholdingCategory_ID());
						paramslca.add(wr.getLCO_WithholdingCategory_ID());
					} else if (wrc.isUseProductTaxCategory()) {
						// base = lines of the product tax category
						sqllca =
							"SELECT SUM (LineNetAmt) "
							+ "  FROM C_InvoiceLine il "
							+ " WHERE IsActive='Y' AND C_Invoice_ID = ? "
							+ "   AND (   EXISTS ( "
							+ "              SELECT 1 "
							+ "                FROM M_Product p "
							+ "               WHERE il.M_Product_ID = p.M_Product_ID "
							+ "                 AND p.C_TaxCategory_ID = ?) "
							+ "        OR EXISTS ( "
							+ "              SELECT 1 "
							+ "                FROM C_Charge c "
							+ "               WHERE il.C_Charge_ID = c.C_Charge_ID "
							+ "                 AND c.C_TaxCategory_ID = ?) "
							+ "       ) ";
						paramslca.add(wr.getC_TaxCategory_ID());
						paramslca.add(wr.getC_TaxCategory_ID());
					} else {
						// base = all lines
						sqllca =
							"SELECT SUM (LineNetAmt) "
							+ "  FROM C_InvoiceLine il "
							+ " WHERE IsActive='Y' AND C_Invoice_ID = ? ";
					}
					base = DB.getSQLValueBD(invoice.get_TrxName(), sqllca, paramslca);
				} else if (wc.getBaseType().equals(X_LCO_WithholdingCalc.BASETYPE_Tax)) {
					// if specific tax
					if (wc.getC_BaseTax_ID() != 0) {
						// base = value of specific tax
						String sqlbst = "SELECT SUM(TaxAmt) "
							+ " FROM C_InvoiceTax "
							+ " WHERE IsActive='Y' AND C_Invoice_ID = ? "
							+ "   AND C_Tax_ID = ?";
						base = DB.getSQLValueBD(invoice.get_TrxName(), sqlbst, new Object[] {invoice.getC_Invoice_ID(), wc.getC_BaseTax_ID()});
					} else {
						// not specific tax
						// base = value of all taxes
						String sqlbsat = "SELECT SUM(TaxAmt) "
							+ " FROM C_InvoiceTax "
							+ " WHERE IsActive='Y' AND C_Invoice_ID = ? ";
						base = DB.getSQLValueBD(invoice.get_TrxName(), sqlbsat, new Object[] {invoice.getC_Invoice_ID()});
					}
				}
				s_log.info("Base: "+base+ " Thresholdmin:"+wc.getThresholdmin());
				
				// if base between thresholdmin and thresholdmax inclusive
				// if thresholdmax = 0 it is ignored
				if (base != null &&
						base.compareTo(Env.ZERO) != 0 &&
								base.compareTo(wc.getThresholdmin(invoice)) >= 0 &&
						(wc.getThresholdMax(invoice).compareTo(Env.ZERO) == 0 || base.compareTo(wc.getThresholdMax(invoice)) <= 0) &&
						tax.getRate() != null &&
						tax.getRate().compareTo(Env.ZERO) != 0) {
					
					BigDecimal rate = BigDecimal.ONE;
					
					if (voucher != null)
					{
						rate = MConversionRate.getRate(invoice.getC_Currency_ID(), voucher.getC_Currency_ID()
							, invoice.getDateInvoiced(), invoice.getC_ConversionType_ID()
							, invoice.getAD_Client_ID(), invoice.getAD_Org_ID());
						
						if (rate == null)
						{
							if (pInfo != null)
								ProcessInfoUtil.addLog(pInfo, invoice.get_ID()
										, TimestampUtil.now(), BigDecimal.ZERO
										, NoCurrencyConversionException.buildMessage(invoice.getC_Currency_ID(), voucher.getC_Currency_ID()
												, invoice.getDateInvoiced(), invoice.getC_ConversionType_ID()
												, invoice.getAD_Client_ID(), invoice.getAD_Org_ID())
										, Table_ID, invoice.get_ID());
							continue;
						}
					}
					
					// insert new withholding record
					// with: type, tax, base amt, percent, tax amt, trx date, acct date, rule
					MLCOInvoiceWithholding iwh = new MLCOInvoiceWithholding(invoice.getCtx(), 0, invoice.get_TrxName());
					iwh.setAD_Org_ID(invoice.getAD_Org_ID());
					iwh.setInvoice(invoice);
					iwh.setDateAcct(invoice.getDateAcct());
					iwh.setDateTrx(invoice.getDateInvoiced());
					iwh.setIsTaxIncluded(false);
					iwh.setWithholdingRule(wr);
					iwh.setWithholdingCalc(wc);
					iwh.setLCO_WithholdingType_ID(wt.getLCO_WithholdingType_ID());
					iwh.setC_Tax_ID(tax.getC_Tax_ID());
					iwh.setPercent(tax.getRate());
					//iwh.setProcessed(false);
					iwh.setProcessed(true);
					//int stdPrecision = MPriceList.getStandardPrecision(invoice.getCtx(), invoice.getM_PriceList_ID());
					MPriceList pl = MPriceList.get(invoice.getM_PriceList_ID());
					int stdPrecision = pl.getPricePrecision();
					
					BigDecimal taxamt = tax.calculateTax(base, false, stdPrecision);
					BigDecimal amountRefunded = wc.getAmountRefunded(invoice);
					if (amountRefunded.compareTo(Env.ZERO) > 0) {
						taxamt = taxamt.subtract(wc.getAmountRefunded(invoice));
						iwh.setAmountRefunded(amountRefunded);
					}
					iwh.setTaxAmt(taxamt);
					iwh.setTaxBaseAmt(base);
					if (    (  invoice.isSOTrx() && MSysConfig.getBooleanValue("QSSLCO_GenerateWithholdingInactiveSO", false, invoice.getAD_Client_ID(), invoice.getAD_Org_ID()) )
						 || ( !invoice.isSOTrx() && MSysConfig.getBooleanValue("QSSLCO_GenerateWithholdingInactivePO", false, invoice.getAD_Client_ID(), invoice.getAD_Org_ID()) )) {
						iwh.setIsActive(false);
					}
					
					if (voucher != null)
					{
						BigDecimal convertedBase = base.multiply(rate);
						BigDecimal convertedTaxAmt = taxamt.multiply(rate);
						BigDecimal convertedAmountRefunded = amountRefunded.multiply(rate);
						
						if (convertedBase.scale() > stdPrecision)
							convertedBase = convertedBase.setScale(stdPrecision, RoundingMode.HALF_UP);
						
						if (convertedTaxAmt.scale() > stdPrecision)
							convertedTaxAmt = convertedTaxAmt.setScale(stdPrecision, RoundingMode.HALF_UP);
						
						if (convertedAmountRefunded.compareTo(Env.ZERO) > 0)
						{
							if (convertedAmountRefunded.scale() > stdPrecision)
								convertedAmountRefunded = convertedAmountRefunded.setScale(stdPrecision, RoundingMode.HALF_UP);
							
							iwh.setConvertedAmountRefunded(convertedAmountRefunded);
						}
						
						setVoucherWithholding(iwh, voucher.get_ID()
								, convertedBase, convertedTaxAmt);
					}
					
					iwh.saveEx();
					List<MLCOInvoiceWithholding> withholdings = POUtil.getAttribute(invoice
							, ColumnUtils.ATTRIBUTE_LCO_InvoiceWithholdings
							, ArrayList::new);
					
					withholdings.add(iwh);
					
					totwith = totwith.add(taxamt);
					noins++;
					s_log.info("LCO_InvoiceWithholding saved:"+iwh.getTaxAmt());
				}
			} // while each applicable rule
		} // while type
		LCO_MInvoice.updateHeaderWithholding(invoice.getC_Invoice_ID(), invoice.get_TrxName());
		invoice.saveEx();
		
		return noins;
	}
	
	public static void setVoucherWithholding(MLCOInvoiceWithholding iwh, int ITS_VoucherWithholding_ID
			, BigDecimal taxBaseAmt, BigDecimal taxAmt) {
		iwh.setITS_VoucherWithholding_ID(ITS_VoucherWithholding_ID);
		iwh.setConvertedTaxBaseAmt(taxBaseAmt);
		iwh.setConvertedTaxAmt(taxAmt);
	}
}
