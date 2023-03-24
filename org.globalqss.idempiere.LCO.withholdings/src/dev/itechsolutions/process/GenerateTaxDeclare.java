package dev.itechsolutions.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.logging.Level;

import org.adempiere.base.annotation.Parameter;
import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.FillMandatoryException;
import org.compiere.model.MBPartner;
import org.compiere.model.MCharge;
import org.compiere.model.MConversionRate;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MOrgInfo;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.globalqss.model.X_LCO_WithholdingType;

import dev.itechsolutions.exception.NoCurrencyConversionException;
import dev.itechsolutions.model.I_ITS_VoucherWithholding;

@Process
public class GenerateTaxDeclare extends SvrProcess {
	
	@Parameter(name = "AD_Org_ID")
	private int p_AD_Org_ID = -1;
	
	@Parameter(name = "LCO_WithholdingType_ID")
	private int p_LCO_WithholdingType_ID = -1;
	
	@Parameter(name = "DateDoc")
	private Timestamp p_DateDoc = null;
	
	@Parameter(name = "DateFrom")
	private Timestamp p_Date = null;
	
	@Parameter(name = "DateFrom_To")
	private Timestamp p_Date_to = null;
	
	@Parameter(name = "C_Currency_ID")
	private int p_C_Currency_ID = -1;
	
	@Parameter(name = "C_ConversionType_ID")
	private int p_C_ConversionType_ID = -1;
	
	@Parameter(name = "DocAction")
	private String p_DocAction = null;
	
	@Override
	protected void prepare() {/****/}
	
	@Override
	protected String doIt() throws Exception
	{	
		//Validate Parameters
		if (p_AD_Org_ID <= 0)
			throw new FillMandatoryException("AD_Org_ID");
		else if (p_LCO_WithholdingType_ID <= 0)
			throw new FillMandatoryException("LCO_WithholdingType_ID");
		else if (p_DateDoc == null)
			throw new FillMandatoryException("DateDoc");
		else if (p_C_Currency_ID <= 0)
			throw new FillMandatoryException("C_Currency_ID");
		else if (Util.isEmpty(p_DocAction, true))
			throw new FillMandatoryException("DocAction");
		else if (p_Date == null || p_Date_to == null)
			throw new FillMandatoryException("Date");
		
		X_LCO_WithholdingType wt = new X_LCO_WithholdingType(getCtx(), p_LCO_WithholdingType_ID, get_TrxName());
		MOrgInfo oInfo = MOrgInfo.getCopy(getCtx(), p_AD_Org_ID, get_TrxName());
		
		int C_DocTypeDeclare_ID = wt.getC_DocTypeDeclare_ID();
		int C_Charge_ID = wt.getC_Charge_ID();
		
		int C_BPartner_ID = Optional.of(wt.getC_BPartner_ID())
				.filter(cID -> cID > 0)
				.orElse(oInfo.get_ValueAsInt(X_LCO_WithholdingType.COLUMNNAME_C_BPartner_ID));
		
		if (C_DocTypeDeclare_ID <= 0)
			throw new AdempiereException("@C_DocTypeDeclare_ID@ @NotFound@");
		
		if (C_Charge_ID <= 0)
			throw new AdempiereException("@C_Charge_ID@ @NotFound@");
		
		if (C_BPartner_ID <= 0)
			throw new AdempiereException("@BPartnerDeclareNotFound@");
		
		int C_BPartner_Location_ID = DB.getSQLValue(get_TrxName()
				, "SELECT C_BPartner_Location_ID"
				  + " FROM C_BPartner_Location WHERE C_BPartner_ID = ? AND IsBillTo = 'Y'"
				, C_BPartner_ID);
		
		if (C_BPartner_Location_ID <= 0)
			throw new AdempiereException("@C_BPartner_Location_ID@ @NotFound@");
		
		int C_UOM_ID = DB.getSQLValue(get_TrxName()
				, "SELECT C_UOM_ID FROM C_UOM WHERE X12DE355='EA'");
		
		if (C_UOM_ID <= 0)
			throw new AdempiereException("@C_UOM_ID@ @NotFound@");
		
		MCharge charge = MCharge.getCopy(getCtx(), C_Charge_ID, get_TrxName());
		MBPartner bpartner = new MBPartner(getCtx(), C_BPartner_ID, get_TrxName());
		
		int C_Tax_ID = DB.getSQLValue(get_TrxName()
				, "SELECT C_Tax_ID FROM C_Tax WHERE C_TaxCategory_ID = ?"
				  + " ORDER BY IsDefault DESC"
				, charge.getC_TaxCategory_ID());
		
		if (C_Tax_ID <= 0)
			throw new AdempiereException("@C_Tax_ID@ @NotFound@");
		
		int AD_User_ID = DB.getSQLValue(get_TrxName()
				, "SELECT AD_User_ID FROM AD_User WHERE C_BPartner_ID = ? ORDER BY IsBillTo DESC"
				, C_BPartner_ID);
		
		if (AD_User_ID <= 0)
			throw new AdempiereException("@AD_User_ID@ @NotFound@");
		
		int M_PriceList_ID = DB.getSQLValue(get_TrxName()
				, "SELECT CASE WHEN dt.IsSOTrx = 'Y' THEN bp.M_PriceList_ID ELSE bp.PO_PriceList_ID END FROM C_BPartner bp"
				  + " INNER JOIN C_DocType dt ON dt.C_DocType_ID = ?"
				  + " WHERE bp.C_BPartner_ID = ?"
				, C_DocTypeDeclare_ID, C_BPartner_ID);
		
		if (M_PriceList_ID <= 0)
			throw new AdempiereException("@M_PriceList_ID@ @NotFound@");
		
		StringBuilder sql = new StringBuilder("SELECT")
					.append(" vw.ITS_VoucherWithholding_ID")
					.append(", vw.DocumentNo")
					.append(", vw.C_Currency_ID")
					.append(", COALESCE(SUM(")
						.append("CASE")
							.append(" WHEN CHARAT(dt.DocBaseType, 3) = 'C' THEN iw.ConvertedTaxAmt * -1")
							.append(" ELSE iw.ConvertedTaxAmt")
						.append(" END")
					.append("), 0) TaxAmt")
				.append(" FROM ITS_VoucherWithholding vw")
				.append(" INNER JOIN LCO_InvoiceWithholding iw ON iw.ITS_VoucherWithholding_ID = vw.ITS_VoucherWithholding_ID")
				.append(" INNER JOIN C_Invoice ci ON ci.C_Invoice_ID = iw.C_Invoice_ID")
				.append(" INNER JOIN C_DocType dt ON dt.C_DocType_ID = ci.C_DocType_ID")
				.append(" WHERE vw.DocStatus = 'CO' AND vw.DateTrx BETWEEN ? AND ?")
				.append(" AND vw.LCO_WithholdingType_ID = ? AND iw.AD_Org_ID = ?")
				.append(" AND NOT EXISTS(")
					.append("SELECT 1 FROM C_InvoiceLine il")
					.append(" INNER JOIN C_Invoice inv ON inv.C_Invoice_ID = il.C_Invoice_ID")
					.append(" WHERE inv.DocStatus NOT IN ('VO', 'RE')")
					.append(" AND il.ITS_VoucherWithholding_ID = vw.ITS_VoucherWithholding_ID")
					.append(" AND inv.AD_Org_ID = iw.AD_Org_ID")
				.append(")")
				.append(" GROUP BY vw.ITS_VoucherWithholding_ID");
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		int declared = 0;
		
		MDocType dtDeclare = MDocType.get(getCtx(), C_DocTypeDeclare_ID);
		MInvoice invoice = null;
		
		try
		{
			int index = 1;
			
			pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
			pstmt.setTimestamp(index++, p_Date);
			pstmt.setTimestamp(index++, p_Date_to);
			pstmt.setInt(index++, p_LCO_WithholdingType_ID);
			pstmt.setInt(index++, p_AD_Org_ID);
			
			rs = pstmt.executeQuery();
			
			while (rs.next())
			{
				if (invoice == null)
				{
					invoice = new MInvoice(getCtx(), 0, get_TrxName());
					invoice.setAD_Org_ID(p_AD_Org_ID);
					invoice.setC_BPartner_ID(C_BPartner_ID);
					invoice.setC_BPartner_Location_ID(C_BPartner_Location_ID);
					invoice.setC_DocTypeTarget_ID(C_DocTypeDeclare_ID);
					invoice.setDateInvoiced(p_DateDoc);
					invoice.setDateAcct(p_DateDoc);
					invoice.setIsSOTrx(dtDeclare.isSOTrx());
					invoice.setM_PriceList_ID(M_PriceList_ID);
					invoice.setC_Currency_ID(p_C_Currency_ID);
					invoice.setC_ConversionType_ID(p_C_ConversionType_ID);
					invoice.setPaymentRule(invoice.isSOTrx() ? bpartner.getPaymentRule() : bpartner.getPaymentRulePO());
					invoice.setC_PaymentTerm_ID(invoice.isSOTrx() ? bpartner.getC_PaymentTerm_ID(): bpartner.getPO_PaymentTerm_ID());
					invoice.setAD_User_ID(AD_User_ID);
					
					invoice.saveEx();
				}
				
				MInvoiceLine invLine = new MInvoiceLine(invoice);
				invLine.setC_Charge_ID(C_Charge_ID);
				invLine.setQty(BigDecimal.ONE);
				
				BigDecimal price = MConversionRate.convert(getCtx(), rs.getBigDecimal("TaxAmt")
						, rs.getInt("C_Currency_ID"), invoice.getC_Currency_ID()
						, invoice.getDateInvoiced(), invoice.getC_ConversionType_ID()
						, invoice.getAD_Client_ID(), invoice.getAD_Org_ID());
				
				if (price == null)
					throw new NoCurrencyConversionException(rs.getInt("C_Currency_ID"), invoice.getC_Currency_ID()
							, invoice.getDateInvoiced(), invoice.getC_ConversionType_ID()
							, invoice.getAD_Client_ID(), invoice.getAD_Org_ID());
				
				invLine.setPrice(price);
				invLine.setC_UOM_ID(C_UOM_ID);
				invLine.setC_Tax_ID(C_Tax_ID);
				invLine.setDescription(Msg.parseTranslation(getCtx()
						, "@ITS_VoucherWithholding_ID@: " + rs.getString("DocumentNo")));
				invLine.set_ValueOfColumn(I_ITS_VoucherWithholding.COLUMNNAME_ITS_VoucherWithholding_ID
						, rs.getInt("ITS_VoucherWithholding_ID"));
				invLine.saveEx();
				declared++;
			}
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		
		if (invoice != null)
		{
			invoice.load(get_TrxName());
			if (!invoice.processIt(p_DocAction))
				throw new AdempiereException(invoice.getProcessMsg());
			invoice.saveEx();
			
			addLog(invoice.get_ID(), invoice.getDateInvoiced()
					, invoice.getGrandTotal(), Msg.parseTranslation(getCtx(), "@C_Invoice_ID@: " + invoice.getDocumentNo())
					, MInvoice.Table_ID, invoice.get_ID());
		}
		
		return "@ITS_VoucherWithholding_ID@ @Declared@ " + declared;
	}
}