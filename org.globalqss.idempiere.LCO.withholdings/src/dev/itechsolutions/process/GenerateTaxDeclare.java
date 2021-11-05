package dev.itechsolutions.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.FillMandatoryException;
import org.compiere.model.MBPartner;
import org.compiere.model.MCharge;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MOrgInfo;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.globalqss.model.X_LCO_WithholdingType;

import dev.itechsolutions.exception.NoCurrencyConversionException;
import dev.itechsolutions.model.ITSMConversionRate;
import dev.itechsolutions.model.I_ITS_VoucherWithholding;

/**
 * 
 * @author Argenis Rodríguez arodriguez@itechsolutions.dev
 *
 */
public class GenerateTaxDeclare extends SvrProcess {
	
	//Process Parameters
	private int p_AD_Org_ID = -1;
	private int p_LCO_WithholdingType_ID = -1;
	private Timestamp p_DateDoc = null;
	private Timestamp p_DateFrom = null;
	private Timestamp p_DateTo = null;
	private int p_C_Currency_ID = -1;
	private int p_C_ConversionType_ID = -1;
	private String p_DocAction = null;
	
	@Override
	protected void prepare() {
		
		for (ProcessInfoParameter parameter: getParameter())
		{
			String name = parameter.getParameterName();
			
			if (Util.isEmpty(name, true))
				continue;
			
			if ("AD_Org_ID".equals(name))
				p_AD_Org_ID = parameter.getParameterAsInt();
			else if ("LCO_WithholdingType_ID".equals(name))
				p_LCO_WithholdingType_ID = parameter.getParameterAsInt();
			else if ("DateDoc".equals(name))
				p_DateDoc = parameter.getParameterAsTimestamp();
			else if ("DateFrom".equals(name))
			{
				p_DateFrom = parameter.getParameterAsTimestamp();
				p_DateTo = parameter.getParameter_ToAsTimestamp();
			}
			else if ("C_Currency_ID".equals(name))
				p_C_Currency_ID = parameter.getParameterAsInt();
			else if ("C_ConversionType_ID".equals(name))
				p_C_ConversionType_ID = parameter.getParameterAsInt();
			else if ("DocAction".equals(name))
				p_DocAction = parameter.getParameterAsString();
		}
	}
	
	@Override
	protected String doIt() throws Exception {
		
		//Validate Parameters
		if (p_AD_Org_ID <= 0)
			throw new FillMandatoryException("AD_Org_ID");
		else if (p_LCO_WithholdingType_ID <= 0)
			throw new FillMandatoryException("LCO_WithholdingType_ID");
		else if (p_DateDoc == null)
			throw new FillMandatoryException("DateDoc");
		else if (p_DateFrom == null || p_DateTo == null)
			throw new FillMandatoryException("DateFrom");
		else if (p_C_Currency_ID <= 0)
			throw new FillMandatoryException("C_Currency_ID");
		else if (p_C_ConversionType_ID <= 0)
			throw new FillMandatoryException("C_ConversionType_ID");
		if (Util.isEmpty(p_DocAction, true))
			throw new FillMandatoryException("DocAction");
		
		X_LCO_WithholdingType whType = new X_LCO_WithholdingType(getCtx(), p_LCO_WithholdingType_ID, get_TrxName());
		MOrgInfo oInfo = MOrgInfo.getCopy(getCtx(), p_AD_Org_ID, get_TrxName());
		
		int C_DocTypeDeclare_ID = whType.getC_DocTypeDeclare_ID();
		int C_Charge_ID = whType.getC_Charge_ID();
		
		//Get BPartner from Withholding Type or Org Info
		int C_BPartner_ID = Optional.ofNullable(whType.getC_BPartner_ID())
				.filter(bpartnerId -> bpartnerId > 0)
				.orElse(oInfo.get_ValueAsInt(X_LCO_WithholdingType.COLUMNNAME_C_BPartner_ID));
		
		if (C_DocTypeDeclare_ID <= 0)
			throw new AdempiereException("@DocTypeDeclareNotFound@");
		
		if (C_Charge_ID <= 0)
			throw new AdempiereException("@ChargeDeclareNotFound@");
		
		if (C_BPartner_ID <= 0)
			throw new AdempiereException("@BPartnerDeclareNotFound@");
		
		int C_BPartner_Location_ID = DB.getSQLValue(get_TrxName()
				, "SELECT C_BPartner_Location_ID FROM C_BPartner_Location"
				 + " WHERE C_BPartner_ID = ? AND IsBillTo = 'Y'"
				, C_BPartner_ID);
		
		if (C_BPartner_Location_ID <= 0)
			throw new AdempiereException("@C_BPartner_Location_ID@ @NotFound@");
		
		int C_UOM_ID = DB.getSQLValue(get_TrxName()
				, "SELECT C_UOM_ID FROM C_UOM WHERE X12DE355='EA'");
		
		if (C_UOM_ID <= 0)
			throw new AdempiereException("@C_UOM_ID@ @NotFound@");
		
		MCharge charge = new MCharge(getCtx(), C_Charge_ID, get_TrxName());
		MBPartner bpartner = new MBPartner(getCtx(), C_BPartner_ID, get_TrxName());
		
		int C_Tax_ID = DB.getSQLValue(get_TrxName()
				, "SELECT C_Tax_ID FROM C_Tax WHERE C_TaxCategory_ID = ?"
				 + " ORDER BY IsDefault DESC"
				, charge.getC_TaxCategory_ID());
		
		if (C_Tax_ID <= 0)
			throw new AdempiereException("@C_Tax_ID@ @NotFound@");
		
		StringBuilder sql = new StringBuilder("SELECT")
				.append(" vw.ITS_VoucherWithholding_ID")
				.append(", vw.DocumentNo")
				.append(", vw.C_Currency_ID")
				.append(", COALESCE(SUM(")
					.append("CASE")
						.append(" WHEN CHARAT(dt.DocBaseType, 3) = 'C' THEN iw.TaxAmt * -1")
						.append(" ELSE iw.TaxAmt")
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
					.append(" INNER JOIN C_Invoice ci ON ci.C_Invoice_ID = il.C_Invoice_ID")
					.append(" WHERE il.ITS_VoucherWithholding_ID = vw.ITS_VoucherWithholding_ID")
					.append(" AND ci.DocStatus NOT IN ('VO', 'RE') AND ci.AD_Org_ID = iw.AD_Org_ID")
				.append(")")
				.append(" GROUP BY vw.ITS_VoucherWithholding_ID");
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		int declared = 0;
		
		MDocType dtDeclare = new MDocType(getCtx(), C_DocTypeDeclare_ID, get_TrxName());
		
		MInvoice invoice = null;
		
		try {
			pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
			pstmt.setTimestamp(1, p_DateFrom);
			pstmt.setTimestamp(2, p_DateTo);
			pstmt.setInt(3, p_LCO_WithholdingType_ID);
			pstmt.setInt(4, p_AD_Org_ID);
			
			rs = pstmt.executeQuery();
			
			while (rs.next())
			{
				if (invoice == null)
				{
					invoice = new MInvoice(getCtx(), 0, get_TrxName());
					invoice.setAD_Org_ID(p_AD_Org_ID);
					invoice.setC_BPartner_ID(C_BPartner_ID);
					invoice.setC_BPartner_Location_ID(C_BPartner_Location_ID);
					invoice.setC_DocType_ID(C_DocTypeDeclare_ID);
					invoice.setC_DocTypeTarget_ID(C_DocTypeDeclare_ID);
					invoice.setDateInvoiced(p_DateDoc);
					invoice.setDateAcct(p_DateDoc);
					invoice.setIsSOTrx(dtDeclare.isSOTrx());
					invoice.setC_Currency_ID(p_C_Currency_ID);
					invoice.setC_ConversionType_ID(p_C_ConversionType_ID);
					invoice.setPaymentRule(bpartner.getPaymentRulePO());
					invoice.setC_PaymentTerm_ID(bpartner.getPO_PaymentTerm_ID());
					invoice.setAD_User_ID(getAD_User_ID());
					
					invoice.saveEx();
				}
				
				MInvoiceLine invLine = new MInvoiceLine(getCtx(), 0, get_TrxName());
				invLine.setC_Invoice_ID(invoice.get_ID());
				invLine.setAD_Org_ID(p_AD_Org_ID);
				invLine.setC_Charge_ID(C_Charge_ID);
				invLine.setQty(BigDecimal.ONE);
				
				BigDecimal price = ITSMConversionRate.convert(getCtx(), rs.getBigDecimal("TaxAmt")
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
		} catch (SQLException e) {
			throw new AdempiereException(e.getLocalizedMessage(), e);
		} finally {
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
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
