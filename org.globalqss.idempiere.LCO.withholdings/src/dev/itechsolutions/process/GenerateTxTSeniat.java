package dev.itechsolutions.process;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.MTable;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Msg;
import org.compiere.util.Util;

import dev.itechsolutions.model.MITSGenerateTXT;

/**
 * 
 * @author José Castañeda
 *
 */
@Process
public class GenerateTxTSeniat extends SvrProcess {

	/**	Organization					*/
	private int			p_AD_Org_ID;
	/** Currency						*/
	private int 		p_C_Currency_ID;
	/** TypeOperation           		*/
	private String      p_OperationType;
	/** ValidFrom           	    	*/
	private Timestamp 	p_ValidFrom;
	/** ValidTo                 		*/
	private Timestamp   p_ValidTo;

	/** MITSGenerateTXT     			*/
	private MITSGenerateTXT generateTXT;
	
	@Override
	protected void prepare()
	{
		generateTXT = MITSGenerateTXT.get(getRecord_ID());
		
		p_AD_Org_ID 	= generateTXT.getAD_Org_ID();
		p_C_Currency_ID = generateTXT.getC_Currency_ID();
		p_OperationType = generateTXT.getITS_TypeOperation();
		p_ValidFrom 	= generateTXT.getValidFrom();
		p_ValidTo 		= generateTXT.getValidTo();
		
	}//	prepare

	@Override
	protected String doIt() throws Exception
	{
		StringBuilder contentTXT= new StringBuilder();
		
		String day, month , year;
		
		Calendar CalendarDate = new GregorianCalendar();
		
		day = Integer.toString(CalendarDate.get(Calendar.DATE));
		month = Integer.toString(CalendarDate.get(Calendar.MONTH) + 1);
		year = Integer.toString(CalendarDate.get(Calendar.YEAR));
		
		String date = day + month + year;

		String fileName = "TXT_Seniat";
		String fileNameTXT = fileName + p_OperationType + date + ".txt";
		String operationType = p_OperationType.equals("V") ? "Ventas" : "Compras";
		
		File file = new File(fileNameTXT);
		
		log.log(Level.INFO, "Fecha: " + date);
		log.log(Level.INFO, "Nombre Archivo: " + fileNameTXT);

		//	Params
		List<Object>params = new ArrayList<Object>();
		
		//	Update Header Amt
		StringBuilder totalAmt = new StringBuilder("SELECT SUM((")
				.append("CASE ")
					.append("WHEN dt.DocbaseType IN ('APC','ARC') THEN -1 ")
					.append("ELSE 1 ")
				.append("END ) ")
				.append("* currencyConvert(iw.ConvertedTaxAmt, vw.C_Currency_ID, ? ")
					.append(", i.DateInvoiced, i.C_ConversionType_ID ")
					.append(", i.AD_Client_ID, i.AD_Org_ID)) ")
				.append("FROM LCO_InvoiceWithholding iw ")
				.append("JOIN C_Invoice i ON i.C_Invoice_ID = iw.C_Invoice_ID ")
				.append("JOIN C_DocType dt ON dt.C_DocType_ID = i.C_DocTypeTarget_ID ")
				.append("JOIN ITS_VoucherWithholding vw ON vw.ITS_VoucherWithholding_ID = iw.ITS_VoucherWithholding_ID")
				.append(" AND EXISTS ")
					.append("(")
						.append("SELECT 1 FROM C_InvoiceLine il ")
						.append("JOIN C_Invoice ii ON ii.C_Invoice_ID = il.C_Invoice_ID ")
						.append("WHERE il.ITS_VoucherWithholding_ID = vw.ITS_VoucherWithholding_ID")
						.append(" AND ii.DocStatus IN ('CO','DR','CL','IP')")
					.append(") ")
				.append("JOIN LCO_WithholdingType wt ON wt.LCO_WithholdingType_ID = iw.LCO_WithholdingType_ID")
				.append(" AND wt.WithholdingType = 'IVA' ")
				.append("WHERE vw.DocStatus IN ('CO','CL')")
				.append(" AND vw.DateAcct BETWEEN ? AND ?")
				.append(" AND (iw.AD_Org_ID IN")
					.append("(")
						.append("SELECT AD_Org_ID ")
						.append("FROM AD_OrgInfo ")
						.append("WHERE Parent_Org_ID = ?")
					.append(") OR vw.AD_Org_ID = ?)");
		
		params.add(p_C_Currency_ID);
		params.add(p_ValidFrom);
		params.add(p_ValidTo);
		params.add(p_AD_Org_ID);
		params.add(p_AD_Org_ID);
		
		BigDecimal amt = DB.getSQLValueBDEx(get_TrxName(), totalAmt.toString(), params);
		
		if(amt != null)
		{
			generateTXT.setTotalAmt(amt);
			generateTXT.saveEx(get_TrxName());
		}
		
		StringBuilder sql = new StringBuilder("SELECT REPLACE(ainf.TaxID, '-', '') AS rifempresa")
				.append(", BTRIM(TO_CHAR(DATE_PART('year', iw.DateAcct), '0000')) || BTRIM(TO_CHAR(DATE_PART('month', iw.DateAcct), '00')) AS periodo")
				.append(", (((TO_CHAR(DATE_PART('year', ci.DateInvoiced), '0000') || '-') ")
					.append("|| BTRIM(TO_CHAR(DATE_PART('month', ci.DateInvoiced), '00'))) || '-') ")
					.append("|| BTRIM(TO_CHAR(DATE_PART('day', ci.DateInvoiced), '00')) AS fechadocumento")
				.append(", CASE ")
					.append("WHEN dt.DocBaseType IN ('API','APC', 'APD') THEN 'C' ")
					.append("WHEN dt.DocBaseType IN ('ARI','ARC', 'ARD') THEN 'V' ")
					.append("ELSE '' ")
				.append("END AS tipooperacion ")
				.append(", CASE ")
					.append("WHEN dt.DocBaseType IN ('API','ARI') THEN '01' ")
					.append("WHEN dt.DocBaseType IN ('APD','ARD') THEN '02' ")
					.append("WHEN dt.DocBaseType IN ('APC','ARC') THEN '03' ")
					.append("ELSE '' ")
				.append("END AS tipodocumento ")
				.append(", COALESCE(REPLACE(bp.TaxID, '-',''),'') AS riftercero")
				.append(", CASE ")
					.append("WHEN dt.DocBaseType IN ('API', 'APC', 'APD','ARI', 'ARC', 'ARD') THEN ci.DocumentNo ")
					.append("ELSE '' ")
				.append("END AS numerodocumento")
				.append(", COALESCE(")
					.append("CASE ")
						.append("WHEN Length(\"substring\"(ci.ControlNumber, 1,\"position\"(ci.ControlNumber,'-'))) = 0 THEN '00-' ")
						.append("ELSE \"substring\"(ci.ControlNumber, 1, \"position\"(ci.ControlNumber, '-')) ")
					.append("END ")
					.append("|| \"substring\"(ci.ControlNumber, \"position\"(ci.ControlNumber, '-') + 1, 60), '00-00000000') AS control")
				.append(", TO_CHAR(Round(currencyConvert(ci.GrandTotal, ci.C_Currency_ID, ?")
					.append(", ci.DateInvoiced, ci.C_ConversionType_ID")
					.append(", ci.AD_Client_ID, ci.AD_Org_ID), 2)")
				.append(", '99999999999999999999.99') AS montodocumento")
				.append(", Round(currencyConvert(ITS_ImpBase ('IB', ci.C_Invoice_ID, ct.Rate), ci.C_Currency_ID, ?")
					.append(", ci.DateInvoiced, ci.C_ConversionType_ID")
					.append(", ci.AD_Client_ID, ci.AD_Org_ID), 2) AS baseimponible")
				.append(", TO_CHAR(ABS(Round(currencyConvert(iw.ConvertedTaxAmt, vw.C_Currency_ID, ?")
					.append(", ci.DateInvoiced, ci.C_ConversionType_ID")
					.append(", ci.AD_Client_ID, ci.AD_Org_ID), 2))")
					.append(", '99999999999999999999.99') AS montoiva")
				.append(", \"isnull\" ((( SELECT ")
					.append("CASE ")
						.append("WHEN dt2.DocBaseType IN ('API', 'APC', 'APD', 'ARI', 'ARC', 'ARD') THEN ci2.DocumentNo ")
						.append("ELSE '' ")
					.append("END AS documentoafectado ")
					.append("FROM C_Invoice ci2 ")
					.append("JOIN C_DocType dt2 ON dt2.C_DocType_ID = ci2.C_DocType_ID ")
					.append("WHERE ci2.C_Invoice_ID = ci.ITS_InvoiceAffected_ID")
					.append(" AND (BTRIM(TO_CHAR(DATE_PART('year', ci2.DateAcct),'0000')) ")
					.append("|| BTRIM(TO_CHAR(DATE_PART('month', ci2.DateAcct),'00'))) = ")
					.append("(BTRIM(TO_CHAR(DATE_PART('year', iw.DateAcct),'0000')) ")
					.append("|| BTRIM(TO_CHAR(DATE_PART('month', iw.DateAcct),'00')))")
					.append(" AND (DATE_PART('day', ci2.DateAcct) >= 1::Double Precision")
						.append(" AND DATE_PART('day', ci2.DateAcct) <= 15::Double Precision")
						.append(" AND DATE_PART('day', iw.DateAcct) >= 1::Double Precision")
						.append(" AND DATE_PART('day', iw.DateAcct) <= 15::Double Precision")
						.append(" OR DATE_PART('day', ci2.DateAcct) >= 16::Double Precision")
						.append(" AND DATE_PART('day', ci2.DateAcct) <= 31::Double Precision")
						.append(" AND DATE_PART('day', iw.DateAcct) >= 16::Double Precision")
						.append(" AND DATE_PART('day', iw.DateAcct) <= 31::Double Precision)))")
						.append(", '0') AS documentoafectado")
				.append(", vw.DocumentNo AS numerocomprobante")
				.append(", Round(currencyConvert(ITS_CalcExempt(ci.C_Invoice_ID)")
					.append(", ci.C_Currency_ID, ?")
					.append(", ci.DateInvoiced, ci.C_ConversionType_ID")
					.append(", ci.AD_Client_ID, ci.AD_Org_ID), 2) AS exento")
				.append(", TO_CHAR(ct.Rate, '99.99') AS alic")
				.append(", '0' AS expediente")
				.append(", iw.DateTrx AS fechareten")
				.append(", iw.AD_Org_ID")
				.append(", iw.AD_Client_ID")
				.append(", 'Y' AS isactive")
				.append(", ainf.Parent_Org_ID ")
				.append("FROM C_Invoice ci ")
				.append("JOIN LCO_InvoiceWithholding iw ON iw.C_Invoice_ID = ci.C_Invoice_ID ")
				.append("JOIN ITS_VoucherWithholding vw ON vw.ITS_VoucherWithholding_ID = iw.ITS_VoucherWithholding_ID")
				.append(" AND vw.DocStatus IN ('CO','CL')")
				.append(" AND EXISTS (SELECT 1 ") //
					.append("FROM C_InvoiceLine il ")
					.append("JOIN C_Invoice i ON i.C_Invoice_ID = il.C_Invoice_ID ")
					.append("WHERE il.ITS_VoucherWithholding_ID = vw.ITS_VoucherWithholding_ID")
					.append(" AND i.DocStatus IN ('CO', 'DR', 'CL', 'IP' )) ") // 
				.append("JOIN LCO_WithholdingType wt ON wt.LCO_WithholdingType_ID = iw.LCO_WithholdingType_ID")
				.append(" AND wt.WithholdingType = 'IVA' ")
				.append("JOIN LCO_WithholdingRule wr ON wr.LCO_WithholdingRule_ID = iw.LCO_WithholdingRule_ID ")
				.append("JOIN LCO_WithholdingCalc wc ON wc.LCO_WithholdingCalc_ID = wr.LCO_WithholdingCalc_ID ")
				.append("JOIN C_Tax ct ON ct.C_Tax_ID = wc.C_BaseTax_ID ")
				.append("JOIN C_DocType dt ON dt.C_DocType_ID = ci.C_DocType_ID")
				.append(" AND dt.DocBaseType IN ('API', 'APC', 'APD', 'ARI', 'APD', 'ARC') ")
				.append("JOIN C_BPartner bp ON bp.C_BPartner_ID = ci.C_BPartner_ID ")
				.append("JOIN AD_Org ao ON ao.AD_Org_ID = ci.AD_Org_ID ")
				.append("JOIN AD_OrgInfo ainf ON ainf.AD_Org_ID = ci.AD_Org_ID ")
				.append("LEFT JOIN LCO_TaxIDType tit ON bp.LCO_TaxIdType_ID = tit.LCO_TaxIDType_ID ")
				.append("WHERE (iw.AD_Org_ID = ? OR ainf.Parent_Org_ID = ?)")
				.append(" AND ci.IsSOTrx = ?")
				.append(" AND iw.DateAcct BETWEEN ? AND ? ");
		
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		
		log.log(Level.INFO, "SQL: " + sql);
		int cont = 0;
		
		try
		{
			int index = 1;
			
			pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
			
			pstmt.setInt(index++, p_C_Currency_ID);
			pstmt.setInt(index++, p_C_Currency_ID);
			pstmt.setInt(index++, p_C_Currency_ID);
			pstmt.setInt(index++, p_C_Currency_ID);
			pstmt.setInt(index++, p_AD_Org_ID);
			pstmt.setInt(index++, p_AD_Org_ID);
			pstmt.setString(index++, p_OperationType.equals("V") ? "Y" : "N");
			pstmt.setTimestamp(index++, p_ValidFrom);
			pstmt.setTimestamp(index++, p_ValidTo);
			
			rs = pstmt.executeQuery();
			
			while (rs.next())
			{
				contentTXT.append(Optional.ofNullable(rs.getString(1)).orElse("null").trim()+"	")
								.append(Optional.ofNullable(rs.getString(2)).orElse("null").trim()+"	")
								.append(Optional.ofNullable(rs.getString(3)).orElse("null").trim()+"	")
								.append(Optional.ofNullable(rs.getString(4)).orElse("null").trim()+"	")
								.append(Optional.ofNullable(rs.getString(5)).orElse("null").trim()+"	")
								.append(Optional.ofNullable(rs.getString(6)).orElse("null").trim()+"	")
								.append(Optional.ofNullable(rs.getString(7)).orElse("null").trim()+"	")
								.append(Optional.ofNullable(rs.getString(8)).orElse("null").trim()+"	")
								.append(Optional.ofNullable(rs.getString(9)).orElse("null").trim()+"	")
								.append(Optional.ofNullable(rs.getString(10)).orElse("null").trim()+"	")
								.append(Optional.ofNullable(rs.getString(11)).orElse("null").trim()+"	")
								.append(Optional.ofNullable(rs.getString(12)).orElse("null").trim()+"	")
								.append(Optional.ofNullable(rs.getString(13)).orElse("null").trim()+"	")
								.append(Optional.ofNullable(rs.getString(14)).orElse("null").trim()+"	")
								.append(Optional.ofNullable(rs.getString(15)).orElse("null").trim()+"	")
								.append(Optional.ofNullable(rs.getString(16)).orElse("null").trim());
				
				contentTXT.append("\n");
				cont ++;
			}
			
		} catch (Exception e)
		{
			throw new AdempiereException(e.getLocalizedMessage());
		}
		finally
		{
			DB.close(rs,pstmt);
			rs = null;
			pstmt = null;
		}
		
		log.info("Contenido: " + contentTXT);
		
		try 
		{
			FileWriter fileW = new java.io.FileWriter(file);
			BufferedWriter bw = new java.io.BufferedWriter(fileW);
			PrintWriter pw = new java.io.PrintWriter(bw); 
			
			pw.write(contentTXT.toString());
			pw.close();
			bw.close();
		}
		catch (IOException ioe)
		{
            throw new AdempiereException("IOException: " + ioe.getLocalizedMessage());
		}
		
		if (contentTXT != null && !Util.isEmpty(contentTXT.toString(), true))
		{
			int AD_Table_ID = MTable.getTable_ID(MITSGenerateTXT.Table_Name);
			
			MAttachment attach =  MAttachment.get(getCtx(), AD_Table_ID, getRecord_ID());
			
			if (attach == null )
			{
				attach = new  MAttachment(getCtx(), AD_Table_ID , getRecord_ID(), get_TrxName());
				
				attach.addEntry(file);
				attach.save();
				
				log.info("attach.save");
			}
			else
			{
				int index = (attach.getEntryCount()-1);
				
				MAttachmentEntry entry = attach.getEntry(index) ;
				String renamed = fileName + p_OperationType + date + "_OLD" + ".txt";
				
				entry.setName(renamed);
				attach.save();
				
				//	The new file is added as the old one has been renamed
				attach.addEntry(file);
				attach.save();	
			}
			
			return Msg.getMsg(generateTXT.getCtx(), "FileGenerated",new Object[] {fileNameTXT, cont});
			
		} else
			return Msg.getMsg(generateTXT.getCtx(), "FileNoGenerated",new Object[] {operationType, p_ValidFrom, p_ValidTo});
		
	}//	doIt
	
}//	GenerateTxTSeniat