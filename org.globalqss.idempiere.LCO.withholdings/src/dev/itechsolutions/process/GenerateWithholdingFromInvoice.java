package dev.itechsolutions.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MDocType;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MSysConfig;
import org.compiere.model.MTax;
import org.compiere.model.POInfo;
import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.globalqss.model.X_LCO_WithholdingCalc;
import org.globalqss.model.X_LCO_WithholdingRule;
import org.globalqss.model.X_LCO_WithholdingRuleConf;
import org.globalqss.model.X_LCO_WithholdingType;

import dev.itechsolutions.model.ITSMInvoice;
import dev.itechsolutions.model.ITSMLocation;
import dev.itechsolutions.model.I_C_Municipality;
import dev.itechsolutions.model.MITSVoucherWithholding;
import dev.itechsolutions.util.ColumnUtils;

/**
 * 
 * @author José Castañeda - 14/12/2022
 *
 */
public class GenerateWithholdingFromInvoice extends SvrProcess {

	private int p_Record_ID;
	private int p_Currency_ID;

	@Override
	protected void prepare() 
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if(name.equals("C_Currency_ID"))
				p_Currency_ID = para[i].getParameterAsInt();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}

		p_Record_ID = getRecord_ID();
	}

	@Override
	protected String doIt() throws Exception 
	{
		ITSMInvoice inv = new ITSMInvoice(getCtx(), p_Record_ID, get_TrxName());
		
		int nInsert = 0;
		
		// Search withholding types applicable depending on IsSOTrx
		List<X_LCO_WithholdingType> wts = new Query(getCtx(), X_LCO_WithholdingType.Table_Name, "IsSOTrx=?", get_TrxName())
			.setOnlyActiveRecords(true)
			.setClient_ID()
			.setParameters(inv.isSOTrx() ? "Y" : "N")
			.list();
		
		for(X_LCO_WithholdingType wt : wts)
		{
			if(validWithholding(inv, wt) > 0)
			{
				if(voucherWithholding(inv, wt).length > 0)			
					nInsert++;
			}
		}
		
		return "@inserted@ " + nInsert;
	}

	protected ITSMInvoice[] voucherWithholding(ITSMInvoice inv, X_LCO_WithholdingType wt) throws Exception
	{
		List<Object> params = new ArrayList<Object>();
		POInfo invoiceInfo = POInfo.getPOInfo(inv.getCtx(), inv.get_Table_ID());
		StringBuilder sql = invoiceInfo.buildSelect(true, false)
				.append(" INNER JOIN C_DocType dt ON dt.C_DocType_ID = C_Invoice.C_DocType_ID")
				.append(" WHERE C_Invoice.IsSOTrx = ?");

		params.add(inv.isSOTrx());
		
		sql.append(" AND C_Invoice.C_Invoice_ID = ?");
		params.add(inv.getC_Invoice_ID());

		sql.append(" AND ? IN (0, C_Invoice.AD_Org_ID) AND C_Invoice.DocStatus = 'CO'");
		params.add(inv.getAD_Org_ID());
	
		sql.append(" AND dt.GenerateWithholding = 'Y'");
		
		if (!MSysConfig.getBooleanValue(ColumnUtils.SYSCONFIG_LVE_GenerateInvoiceWithholdingIsPaid
				, false, inv.getAD_Client_ID()))
			sql.append(" AND C_Invoice.IsPaid = 'N'");
		
		sql.append(" AND NOT EXISTS(")
			.append("SELECT 1 FROM ITS_VoucherWithholding vw")
			.append(" INNER JOIN LCO_InvoiceWithholding iw ON iw.ITS_VoucherWithholding_ID = vw.ITS_VoucherWithholding_ID")
			.append(" WHERE iw.C_Invoice_ID = C_Invoice.C_Invoice_ID")
			.append(" AND vw.DocStatus NOT IN ('VO', 'RE')")
			.append(" AND vw.LCO_WithholdingType_ID = ?")
		.append(")");
		params.add(wt.getLCO_WithholdingType_ID());
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		List<ITSMInvoice> invoices = new ArrayList<ITSMInvoice>();
		
		try {
			pstmt = DB.prepareStatement(sql.toString(), inv.get_TrxName());
			DB.setParameters(pstmt, params);
			
			rs = pstmt.executeQuery();
			
			while (rs.next())
				invoices.add(new ITSMInvoice(inv.getCtx(), rs, inv.get_TrxName()));
		} catch (SQLException e) {
			throw new AdempiereException(e.getLocalizedMessage(), e);
		} finally {
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		
		if(invoices.isEmpty())
			return invoices.toArray(ITSMInvoice[]::new);
		
		MDocType query = new Query(inv.getCtx(), MDocType.Table_Name, "LCO_WithholdingType_ID = ?", inv.get_TrxName())
				.setOnlyActiveRecords(true)
				.setParameters(wt.getLCO_WithholdingType_ID())
				.first();
		
		MITSVoucherWithholding voucher = new MITSVoucherWithholding(getCtx(), 0, get_TrxName());
		voucher.setAD_Org_ID(inv.getAD_Org_ID());
		voucher.setC_DocType_ID(query.getC_DocType_ID());
		voucher.setLCO_WithholdingType_ID(wt.getLCO_WithholdingType_ID());
		voucher.setDateTrx(inv.getDateInvoiced());
		voucher.setDateAcct(inv.getDateAcct());
		voucher.setC_BPartner_ID(inv.getC_BPartner_ID());
		voucher.setC_Currency_ID(p_Currency_ID);
		voucher.setC_ConversionType_ID(inv.getC_ConversionType_ID());
		voucher.setC_Invoice_ID(inv.getC_Invoice_ID());

		voucher.saveEx();

		inv.recalcWithholdings(voucher);
	
		if (!voucher.processIt(DocAction.ACTION_Complete)) 
		{
			log.warning("Allocation Process Failed: " + voucher + " - " + voucher.getProcessMsg());
			throw new IllegalStateException("Allocation Process Failed: " + voucher + " - " + voucher.getProcessMsg());
				
		}
		
		addLog(voucher.getITS_VoucherWithholding_ID(),null, null,"Retención Generada: " + voucher.getDocumentNo());
		
		return invoices.toArray(ITSMInvoice[]::new);	

	}//	voucherWithholding

	protected int validWithholding(ITSMInvoice inv, X_LCO_WithholdingType wt)
	{
		if (!MSysConfig.getBooleanValue(ColumnUtils.SYSCONFIG_LCO_USE_WITHHOLDINGS
				, true, Env.getAD_Client_ID(Env.getCtx())))
			return 0;
	
		MDocType dt = new MDocType(getCtx(), inv.getC_DocTypeTarget_ID(), get_TrxName());
		String genwh = dt.get_ValueAsString(ColumnUtils.COLUMNNAME_GenerateWithholding);
		
		if (genwh == null || genwh.equals(ColumnUtils.GenerateWithholding_No))
			return 0;
		
		log.info("");
		
		/**		Fill Variables Normally Needed	**/
		/**		BPartner						**/
		MBPartner bp = new MBPartner(getCtx(), inv.getC_BPartner_ID(), get_TrxName());
		int bp_isic_id = bp.get_ValueAsInt(ColumnUtils.COLUMNNAME_LCO_ISIC_ID);
		int bp_taxpayertype_id = bp.get_ValueAsInt(ColumnUtils.COLUMNNAME_LCO_TaxPayerType_ID);
		
		/**		BPartner Location				**/
		MBPartnerLocation mbpl = new MBPartnerLocation(getCtx(), inv.getC_BPartner_Location_ID(), get_TrxName());
		ITSMLocation bpl = ITSMLocation.getCopy(getCtx(), mbpl.getC_Location_ID(), get_TrxName());
		int bp_city_id = bpl.getC_City_ID();
		int bp_Municipality_ID = bpl.get_ValueAsInt(I_C_Municipality.COLUMNNAME_C_Municipality_ID);
		
		/**		OrgInfo							**/
		MOrgInfo oi = MOrgInfo.get(getCtx(), inv.getAD_Org_ID(), get_TrxName());
		int org_isic_id = oi.get_ValueAsInt(ColumnUtils.COLUMNNAME_LCO_ISIC_ID);
		int org_taxpayertype_id = oi.get_ValueAsInt(ColumnUtils.COLUMNNAME_LCO_TaxPayerType_ID);
		ITSMLocation ol = ITSMLocation.getCopy(getCtx(), oi.getC_Location_ID(), get_TrxName());
		int org_city_id = ol.getC_City_ID();
		int org_Municipality_ID = ol.get_ValueAsInt(I_C_Municipality.COLUMNNAME_C_Municipality_ID);
		
		//	Get Withholding Rule Config
		X_LCO_WithholdingRuleConf wrc = new Query(getCtx(),
				X_LCO_WithholdingRuleConf.Table_Name,
				"LCO_WithholdingType_ID=?",
				get_TrxName())
				.setOnlyActiveRecords(true)
				.setParameters(wt.getLCO_WithholdingType_ID())
				.first();
		
		if (wrc == null) {
			log.warning("No LCO_WithholdingRuleConf for LCO_WithholdingType = "+wt.getLCO_WithholdingType_ID());
			return 0;
		}
		
		//	Look For Applicable Rules According To Config Fields (Rule)
		StringBuffer wherer = new StringBuffer(" LCO_WithholdingType_ID=? AND ValidFrom<=? ");
		List<Object> paramsr = new ArrayList<Object>();
		paramsr.add(wt.getLCO_WithholdingType_ID());
		paramsr.add(inv.getDateInvoiced());
		if (wrc.isUseBPISIC()) 
		{

			String validMunicipality = oi.get_ValueAsString(ColumnUtils.COLUMNNAME_ValidMunicipality);

			if (ColumnUtils.ValidMunicipality_None.equals(validMunicipality)
					|| (ColumnUtils.ValidMunicipality_Local.equals(validMunicipality)
							&& bp_Municipality_ID != org_Municipality_ID)
					|| (ColumnUtils.ValidMunicipality_Outsiders.equals(validMunicipality)
							&& bp_Municipality_ID == org_Municipality_ID))
				return 0;

			wherer.append(" AND LCO_BP_ISIC_ID=? ");
			paramsr.add(bp_isic_id);
		}
		if (wrc.isUseBPTaxPayerType()) 
		{
			wherer.append(" AND LCO_BP_TaxPayerType_ID=? ");
			paramsr.add(bp_taxpayertype_id);
		}
		if (wrc.isUseOrgISIC()) 
		{
			wherer.append(" AND LCO_Org_ISIC_ID=? ");
			paramsr.add(org_isic_id);
		}
		if (wrc.isUseOrgTaxPayerType()) 
		{
			wherer.append(" AND LCO_Org_TaxPayerType_ID=? ");
			paramsr.add(org_taxpayertype_id);
		}
		if (wrc.isUseBPCity()) 
		{
			wherer.append(" AND LCO_BP_City_ID=? ");
			paramsr.add(bp_city_id);
			if (bp_city_id <= 0)
				log.warning("Possible configuration error bp city is used but not set");
		}
		if (wrc.isUseOrgCity()) 
		{
			wherer.append(" AND LCO_Org_City_ID=? ");
			paramsr.add(org_city_id);
			if (org_city_id <= 0)
				log.warning("Possible configuration error org city is used but not set");
		}

		//	Add Withholding Categories Of Lines
		if (wrc.isUseWithholdingCategory()) 
		{
			//	Look The Conf Fields
			String sqlwcs =
				"SELECT DISTINCT COALESCE (p.LCO_WithholdingCategory_ID, COALESCE (c.LCO_WithholdingCategory_ID, 0)) "
				+ "  FROM C_InvoiceLine il "
				+ "  LEFT OUTER JOIN M_Product p ON (il.M_Product_ID = p.M_Product_ID) "
				+ "  LEFT OUTER JOIN C_Charge c ON (il.C_Charge_ID = c.C_Charge_ID) "
				+ "  WHERE C_Invoice_ID = ? AND il.IsActive='Y' AND (il.M_Product_ID>0 OR il.C_Charge_ID>0)";
			int[] wcids = DB.getIDsEx(get_TrxName(), sqlwcs, new Object[] {inv.getC_Invoice_ID()});
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
		
		//	Add Tax Categories Of Lines
		if (wrc.isUseProductTaxCategory()) 
		{
			//	Look The Conf Fields
			String sqlwct =
				"SELECT DISTINCT COALESCE (p.C_TaxCategory_ID, COALESCE (c.C_TaxCategory_ID, 0)) "
				+ "  FROM C_InvoiceLine il "
				+ "  LEFT OUTER JOIN M_Product p ON (il.M_Product_ID = p.M_Product_ID) "
				+ "  LEFT OUTER JOIN C_Charge c ON (il.C_Charge_ID = c.C_Charge_ID) "
				+ "  WHERE C_Invoice_ID = ? AND il.IsActive='Y' AND (il.M_Product_ID>0 OR il.C_Charge_ID>0)";
			int[] wcids = DB.getIDsEx(get_TrxName(), sqlwct, new Object[] {inv.getC_Invoice_ID()});
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

		List<X_LCO_WithholdingRule> wrs = new Query(getCtx(), X_LCO_WithholdingRule.Table_Name, wherer.toString(), get_TrxName())
			.setOnlyActiveRecords(true)
			.setParameters(paramsr)
			.list();

		for (X_LCO_WithholdingRule wr : wrs)
		{
			/**
			 * For Each Applicable Rule
			 * Bring Record For Withholding Calculation
			 */
			X_LCO_WithholdingCalc wc = (X_LCO_WithholdingCalc) wr.getLCO_WithholdingCalc();
			if (wc == null || wc.getLCO_WithholdingCalc_ID() == 0) {
				log.severe("Rule without calc " + wr.getLCO_WithholdingRule_ID());
				continue ;
			}
			
			//	Bring Record For Tax
			MTax tax = new MTax(getCtx(), wc.getC_Tax_ID(), get_TrxName());
			
			log.info("WithholdingRule: "+wr.getLCO_WithholdingRule_ID()+"/"+wr.getName()
					+" BaseType:"+wc.getBaseType()
					+" Calc: "+wc.getLCO_WithholdingCalc_ID()+"/"+wc.getName()
					+" CalcOnInvoice:"+wc.isCalcOnInvoice()
					+" Tax: "+tax.getC_Tax_ID()+"/"+tax.getName());
			
			/**
			 * Calc Base
			 * Apply Rule To Calc Base
			 */
			BigDecimal base = null;
			
			if (wc.getBaseType() == null) {
				log.severe("Base Type null in calc record "+wr.getLCO_WithholdingCalc_ID());
			} else if (wc.getBaseType().equals(X_LCO_WithholdingCalc.BASETYPE_Document)) {
				base = inv.getTotalLines();
			} else if (wc.getBaseType().equals(X_LCO_WithholdingCalc.BASETYPE_Line)) {
				
				List<Object> paramslca = new ArrayList<Object>();
				paramslca.add(inv.getC_Invoice_ID());
				String sqllca;				
				if (wrc.isUseWithholdingCategory() && wrc.isUseProductTaxCategory()) 
				{
					//	Base = Lines Of The Withholding Category And Tax Category
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
				} else if (wrc.isUseWithholdingCategory()) 
				{
					//	Base = Lines Of The Withholding Category
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
				} else if (wrc.isUseProductTaxCategory()) 
				{
					//	Base = Lines Of The Product Tax Category
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
				} else 
				{
					//	Base = All Lines
					sqllca =
						"SELECT SUM (LineNetAmt) "
						+ "  FROM C_InvoiceLine il "
						+ " WHERE IsActive='Y' AND C_Invoice_ID = ? ";
				}
				base = DB.getSQLValueBD(get_TrxName(), sqllca, paramslca);
			} else if (wc.getBaseType().equals(X_LCO_WithholdingCalc.BASETYPE_Tax)) 
			{
				//	If Specific Tax
				if (wc.getC_BaseTax_ID() != 0) 
				{
					//	Base = Value Of Specific Tax
					String sqlbst = "SELECT SUM(TaxAmt) "
						+ " FROM C_InvoiceTax "
						+ " WHERE IsActive='Y' AND C_Invoice_ID = ? "
						+ "   AND C_Tax_ID = ?";
					base = DB.getSQLValueBD(get_TrxName(), sqlbst, new Object[] {inv.getC_Invoice_ID(), wc.getC_BaseTax_ID()});
				} else 
				{
					//	Not Specific Tax
					//	Base = Value Of All Taxes
					String sqlbsat = "SELECT SUM(TaxAmt) "
						+ " FROM C_InvoiceTax "
						+ " WHERE IsActive='Y' AND C_Invoice_ID = ? ";
					base = DB.getSQLValueBD(get_TrxName(), sqlbsat, new Object[] {inv.getC_Invoice_ID()});
				}
			}
			log.info("Base: "+base+ " Thresholdmin:"+wc.getThresholdmin());	
			
			if (base != null &&
					base.compareTo(Env.ZERO) != 0 &&
					base.compareTo(wc.getThresholdmin()) >= 0 &&
					(wc.getThresholdMax() == null || wc.getThresholdMax().compareTo(Env.ZERO) == 0 || base.compareTo(wc.getThresholdMax()) <= 0) &&
					tax.getRate() != null &&
					tax.getRate().compareTo(Env.ZERO) != 0) 
			{	
				return 1;
			}
		}
		
		return 0;

	}//	validWithholding

}//	GenerateWithholdingFromInvoice