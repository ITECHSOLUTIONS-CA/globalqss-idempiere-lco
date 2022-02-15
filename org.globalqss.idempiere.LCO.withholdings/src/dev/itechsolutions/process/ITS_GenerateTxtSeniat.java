package dev.itechsolutions.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

import java.io.*;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Optional;
import java.util.logging.Level;

import dev.itechsolutions.model.X_ITS_generateTXT;

public class ITS_GenerateTxtSeniat extends SvrProcess {
	
	/**	Organization					*/
	private int	p_AD_Org_ID = 0;	
	/** ValidFrom           	    	*/
	private Timestamp 	p_ValidFrom=null;
	/** ValidTo                 		*/
	private Timestamp   p_ValidTo=null;
	/** TypeOperation           		*/
	private String      p_TypeOperation=null;
	/** Record_ID               		*/
	private int p_Record_ID=0;
	/** X_ITS_generateTXT     			*/
	private int p_ITS_generateTXT_ID = 0;
	
	private int p_C_Currency_ID = 0; 
	
	boolean IsFixedConversion = false;

	private X_ITS_generateTXT generateTXT;
	
	@Override
	protected void prepare() {
	
		p_Record_ID = getRecord_ID();
		p_ITS_generateTXT_ID = p_Record_ID;
		generateTXT = new X_ITS_generateTXT(getCtx(), p_ITS_generateTXT_ID, get_TrxName());
		
		p_AD_Org_ID = generateTXT.getAD_Org_ID();
		p_ValidFrom = generateTXT.getValidFrom();
		p_ValidTo = generateTXT.getValidTo();
		p_TypeOperation = generateTXT.getITS_TypeOperation();
		p_C_Currency_ID = generateTXT.get_ValueAsInt("C_Currency_ID");
		
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;	
			else if (name.equals("IsFixedConversion"))
				IsFixedConversion = para[i].getParameterAsBoolean();
		}
		
		
		log.log(Level.INFO, "*********  Prepare  **********");
		log.log(Level.INFO, "Parameters: " + "AD_Org_ID: " + p_AD_Org_ID + ", ValidFrom: " + p_ValidFrom + ", ValidTo: " + p_ValidTo + ", TypeOperation: " + p_TypeOperation + ", Record_ID: " + p_Record_ID);
		
		log.log(Level.INFO, "Contexto: " + getCtx().toString());
		
	}

	@Override
	protected String doIt() throws Exception {
		
		StringBuilder sql= new StringBuilder();
		
		String dia, mes , anio;
		Calendar date = new GregorianCalendar();
		dia = Integer.toString(date.get(Calendar.DATE));
		mes = Integer.toString(date.get(Calendar.MONTH) + 1);
		anio = Integer.toString(date.get(Calendar.YEAR));
		String fecha = dia + mes + anio;
		
		String nombreArch="TXT_Seniat";
		String fileNameTXT=nombreArch + p_TypeOperation + fecha + ".txt";
		File archivo=new File(fileNameTXT);
		StringBuilder contentTXT= new StringBuilder();
		
		String tipoOperacion = "";
		
		log.log(Level.INFO, "Fecha: " + fecha);
		log.log(Level.INFO, "Nombre Archivo: " + fileNameTXT);
		
		if (p_TypeOperation.equals("V"))
			tipoOperacion = "VENTAS";
		else tipoOperacion = "COMPRAS";
		
		
		String totalAmtSql = "((SELECT SUM((CASE\n" + 
		"            WHEN dt.docbasetype = ANY (ARRAY['APC'::bpchar, 'ARC'::bpchar]) THEN -1\n" + 
		"            ELSE 1 end) * currencyconvert(iw.taxamt, vw.c_currency_id, "+p_C_Currency_ID+", i.dateinvoiced::timestamp with time zone, i.c_conversiontype_id, i.ad_client_id, i.ad_org_id)\n" + 
		"            ) \n" + 
		"     FROM lco_invoicewithholding iw \n" + 
		"    JOIN c_invoice i ON iw.c_invoice_id=i.c_invoice_id\n" + 
		"    JOIN c_doctype dt ON i.c_doctypetarget_id=dt.c_doctype_id\n" + 
		"    INNER JOIN its_voucherwithholding vw ON iw.its_voucherwithholding_id = vw.its_voucherwithholding_id AND\n" + 
		"     EXISTS (SELECT 1 FROM c_invoiceline JOIN c_invoice ON c_invoiceline.c_invoice_id =c_invoice.c_invoice_id \n" + 
		"     WHERE c_invoiceline.its_voucherwithholding_id = vw.its_voucherwithholding_id AND c_invoice.docstatus IN ('CO','DR','CL','IP'))\n" + 
		"     INNER JOIN lco_withholdingtype ON lco_withholdingtype.lco_withholdingtype_id = iw.lco_withholdingtype_id \n" + 
		"     AND lco_withholdingtype.withholdingtype = 'IVA'\n" + 
		"     WHERE vw.docstatus IN ('CO','CL') AND vw.dateacct BETWEEN '"+p_ValidFrom+"' AND '"+p_ValidTo+"'\n" + 
		"     AND (vw.AD_Org_ID IN (SELECT AD_Org_ID FROM AD_OrgInfo where Parent_Org_ID="+p_AD_Org_ID+") \n" + 
		"     OR vw.AD_Org_ID="+p_AD_Org_ID+")))";
		
		BigDecimal totalAmt = DB.getSQLValueBDEx(get_TrxName(), totalAmtSql);
		if(totalAmt!=null) {
			generateTXT.set_ValueOfColumn("TotalAmt", totalAmt);
			generateTXT.saveEx(get_TrxName());
		}
		
		sql.append("SELECT replace(ainf.taxid::text, '-'::text, ''::text) AS rifempresa,\n" + 
				"    btrim(to_char(date_part('year'::text, iw.dateacct), '0000'::text)) || btrim(to_char(date_part('month'::text, iw.dateacct), '00'::text)) AS periodo,\n" + 
				"    (((to_char(date_part('year'::text, ci.dateinvoiced), '0000'::text) || '-'::text) || btrim(to_char(date_part('month'::text, ci.dateinvoiced), '00'::text))) || '-'::text) || btrim(to_char(date_part('day'::text, ci.dateinvoiced), '00'::text)) AS fechadocumento,\n" + 
				"        CASE\n" + 
				"            WHEN dt.docbasetype = ANY (ARRAY['API'::bpchar, 'APC'::bpchar, 'APD'::bpchar]) THEN 'C'::text\n" + 
				"            WHEN dt.docbasetype = ANY (ARRAY['ARI'::bpchar, 'ARC'::bpchar, 'ARD'::bpchar]) THEN 'V'::text\n" + 
				"            ELSE ''::text\n" + 
				"        END AS tipooperacion,\n" + 
				"        CASE\n" + 
				"            WHEN dt.docbasetype = ANY (ARRAY['API'::bpchar, 'ARI'::bpchar]) THEN '01'::text\n" + 
				"            WHEN dt.docbasetype = ANY (ARRAY['APD'::bpchar, 'ARD'::bpchar]) THEN '02'::text\n" + 
				"            WHEN dt.docbasetype = ANY (ARRAY['APC'::bpchar, 'ARC'::bpchar]) THEN '03'::text\n" + 
				"            ELSE ''::text\n" + 
				"        END AS tipodocumento,\n" + 
				"    COALESCE(replace(bp.taxid::text, '-'::text, ''::text), ''::text) AS riftercero,\n" + 
				"        CASE\n" + 
				"            WHEN dt.docbasetype = ANY (ARRAY['API'::bpchar, 'APC'::bpchar, 'APD'::bpchar]) THEN COALESCE(ci.its_poinvoiceno, ci.documentno)\n" + 
				"            WHEN dt.docbasetype = ANY (ARRAY['ARI'::bpchar, 'ARC'::bpchar, 'ARD'::bpchar]) THEN ci.documentno\n" + 
				"            ELSE ''::character varying\n" + 
				"        END::text AS numerodocumento,\n" + 
				"    COALESCE(\n" + 
				"        CASE\n" + 
				"            WHEN length(\"substring\"(ci.its_controlnumber::text, 1,\"position\"(ci.its_controlnumber::text, '-'::text))) = 0 THEN '00-'::text\n" + 
				"            ELSE \"substring\"(ci.its_controlnumber::text, 1, \"position\"(ci.its_controlnumber::text, '-'::text))\n" + 
				"        END || \"substring\"(ci.its_controlnumber::text, \"position\"(ci.its_controlnumber::text, '-'::text) + 1, 60), '00-00000000'::text) AS control,\n" + 
				"    to_char(round(currencyconvert(ci.grandtotal, ci.c_currency_id, "+p_C_Currency_ID+", ci.dateinvoiced::timestamp with time zone, ci.c_conversiontype_id, ci.ad_client_id, ci.ad_org_id), 2)\n" + 
				"       , '99999999999999999999.99'::text) AS montodocumento,\n" +  
				"    round(currencyconvert(its_impbase('IB'::bpchar, ci.c_invoice_id, ct.rate), ci.c_currency_id, "+p_C_Currency_ID+", ci.dateinvoiced::timestamp with time zone, ci.c_conversiontype_id, ci.ad_client_id, ci.ad_org_id)\n" + 
				"    	, 2)::text AS baseimponible,\n" + 
				"    to_char(abs(round(currencyconvert(iw.taxamt, vw.c_currency_id, "+p_C_Currency_ID+", ci.dateinvoiced::timestamp with time zone, ci.c_conversiontype_id, ci.ad_client_id, ci.ad_org_id), 2)), '99999999999999999999.99'::text) AS montoiva,\n" + 
				"    \"isnull\"((( SELECT\n" + 
				"                CASE\n" + 
				"                    WHEN dt2.docbasetype = ANY (ARRAY['API'::bpchar, 'APC'::bpchar, 'APD'::bpchar]) THEN COALESCE(ci2.its_poinvoiceno, ci2.documentno)\n" + 
				"                    WHEN dt.docbasetype = ANY (ARRAY['ARI'::bpchar, 'ARC'::bpchar, 'ARD'::bpchar]) THEN ci2.documentno\n" + 
				"                    ELSE ''::character varying\n" + 
				"                END AS documentoafectado\n" + 
				"           FROM c_invoice ci2\n" + 
				"             JOIN c_doctype dt2 ON dt2.c_doctype_id = ci2.c_doctype_id\n" + 
				"          WHERE ci2.c_invoice_id = ci.its_invoiceaffected_id AND (btrim(to_char(date_part('year'::text, ci2.dateacct), '0000'::text)) || btrim(to_char(date_part('month'::text, ci2.dateacct), '00'::text))) = (btrim(to_char(date_part('year'::text, iw.dateacct), '0000'::text)) || btrim(to_char(date_part('month'::text, iw.dateacct), '00'::text))) AND (date_part('day'::text, ci2.dateacct) >= 1::double precision AND date_part('day'::text, ci2.dateacct) <= 15::double precision AND date_part('day'::text, iw.dateacct) >= 1::double precision AND date_part('day'::text, iw.dateacct) <= 15::double precision OR date_part('day'::text, ci2.dateacct) >= 16::double precision AND date_part('day'::text, ci2.dateacct) <= 31::double precision AND date_part('day'::text, iw.dateacct) >= 16::double precision AND date_part('day'::text, iw.dateacct) <= 31::double precision)))::text, '0'::text) AS documentoafectado,\n" + 
				"    vw.documentno AS numerocomprobante,\n" + 
				"        round(currencyconvert(its_calcexempt(ci.c_invoice_id), ci.c_currency_id, " + p_C_Currency_ID + 
				"        , ci.dateinvoiced::timestamp with time zone, ci.c_conversiontype_id, ci.ad_client_id, ci.ad_org_id), 2)\n" + 
				"        AS exento,\n" + 
				"    to_char(ct.rate, '99.99'::text) AS alic,\n" + 
				"    '0'::text AS expediente,\n" + 
				"    iw.datetrx AS fechareten,\n" + 
				"    iw.ad_org_id AS org,\n" + 
				"    iw.ad_client_id AS client,\n" + 
				"    iw.ad_org_id,\n" + 
				"    iw.ad_client_id,\n" + 
				"    'Y'::text AS isactive,\n" + 
				"    ainf.parent_org_id\n" + 
				"   FROM c_invoice ci\n" + 
				"     JOIN lco_invoicewithholding iw ON ci.c_invoice_id = iw.c_invoice_id\n" + 
				"     JOIN its_voucherwithholding vw ON vw.its_voucherwithholding_id = iw.its_voucherwithholding_id AND (vw.docstatus::text = ANY (ARRAY['CO'::character varying::text, 'CL'::character varying::text])) AND (EXISTS ( SELECT 1\n" + 
				"           FROM c_invoiceline il\n" + 
				"             JOIN c_invoice i ON il.c_invoice_id = i.c_invoice_id\n" + 
				"          WHERE il.its_voucherwithholding_id = vw.its_voucherwithholding_id AND (i.docstatus = ANY (ARRAY['CO'::bpchar, 'DR'::bpchar, 'CL'::bpchar, 'IP'::bpchar]))))\n" + 
				"     JOIN lco_withholdingtype wt ON iw.lco_withholdingtype_id = wt.lco_withholdingtype_id AND wt.withholdingtype::bpchar = 'IVA'::bpchar\n" + 
				"     JOIN lco_withholdingrule wr ON iw.lco_withholdingrule_id = wr.lco_withholdingrule_id\n" + 
				"     JOIN lco_withholdingcalc wc ON wc.lco_withholdingcalc_id = wr.lco_withholdingcalc_id\n" + 
				"     JOIN c_tax ct ON ct.c_tax_id = wc.c_basetax_id\n" + 
				"     JOIN c_doctype dt ON ci.c_doctype_id = dt.c_doctype_id AND (dt.docbasetype = ANY (ARRAY['API'::bpchar, 'APC'::bpchar, 'APD'::bpchar, 'ARI'::bpchar, 'ARD'::bpchar, 'ARC'::bpchar]))\n" + 
				"     JOIN c_bpartner bp ON bp.c_bpartner_id = ci.c_bpartner_id\n" + 
				"     JOIN ad_org ao ON ao.ad_org_id = ci.ad_org_id\n" + 
				"     JOIN ad_orginfo ainf ON ainf.ad_org_id = ci.ad_org_id\n" + 
				"     LEFT JOIN lco_taxidtype tit ON bp.lco_taxidtype_id = tit.lco_taxidtype_id");
		
		sql.append(" WHERE (iw.ad_org_id=").append(p_AD_Org_ID).append(" OR ainf.parent_org_id= ").append(p_AD_Org_ID).append(")")
		.append(" AND ci.issotrx = "+(p_TypeOperation.equals("V")?"'Y'":"'N'"))
		.append(" AND iw.datetrx between '").append(p_ValidFrom).append("' AND '").append(p_ValidTo).append("'");
	
		//
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		log.log(Level.INFO, "SQL: " + sql);
		int cont = 0;
		
		try {
			pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
			rs = pstmt.executeQuery();
			while (rs.next())
			{
			contentTXT.append(Optional.ofNullable(rs.getString(1)).orElse("null").trim()+"	")
							.append(Optional.ofNullable(rs.getString(2)).orElse("null").trim()+"	")//+rs.getString(2).trim()+"	"
							.append(Optional.ofNullable(rs.getString(3)).orElse("null").trim()+"	")//+rs.getString(3).trim()+"	"
							.append(Optional.ofNullable(rs.getString(4)).orElse("null").trim()+"	")//+rs.getString(4).trim()+"	"
							.append(Optional.ofNullable(rs.getString(5)).orElse("null").trim()+"	")//+rs.getString(5).trim()+"	"
							.append(Optional.ofNullable(rs.getString(6)).orElse("null").trim()+"	")//+rs.getString(6).trim()+"	"
							.append(Optional.ofNullable(rs.getString(7)).orElse("null").trim()+"	")//+rs.getString(7).trim()+"	"
							.append(Optional.ofNullable(rs.getString(8)).orElse("null").trim()+"	")//+rs.getString(8).trim()+"	"
							.append(Optional.ofNullable(rs.getString(9)).orElse("null").trim()+"	")//+rs.getString(9).trim()+"	"
							.append(Optional.ofNullable(rs.getString(10)).orElse("null").trim()+"	")//+rs.getString(10).trim()+"	"
							.append(Optional.ofNullable(rs.getString(11)).orElse("null").trim()+"	")//+rs.getString(11).trim()+"	"
							.append(Optional.ofNullable(rs.getString(12)).orElse("null").trim()+"	")//+rs.getString(12).trim()+"	"
							.append(Optional.ofNullable(rs.getString(13)).orElse("null").trim()+"	")//+rs.getString(13).trim()+"	"
							.append(Optional.ofNullable(rs.getString(14)).orElse("null").trim()+"	")//+rs.getString(14).trim()+"	"
							.append(Optional.ofNullable(rs.getString(15)).orElse("null").trim()+"	")//+rs.getString(15).trim()+"	"
							.append(Optional.ofNullable(rs.getString(16)).orElse("null").trim());//+rs.getString(16).trim())*/
			
			contentTXT.append("\n");
			cont ++;
			}
			
		}
		catch ( Exception e )
        {
            throw new AdempiereException(e);
        }finally {
        	DB.close(rs,pstmt);
        }
		
		log.info("Contenido: " + contentTXT);
		
		try {
			java.io.FileWriter file = new java.io.FileWriter(archivo);
			java.io.BufferedWriter bw = new java.io.BufferedWriter(file);
			java.io.PrintWriter pw = new java.io.PrintWriter(bw); 
			pw.write(contentTXT.toString());
			pw.close();
			bw.close();	
		}catch (IOException ioe) {
			System.out.println("IOException: " + ioe.getMessage());
            throw new AdempiereException("IOException: "+ioe);
		}
		
		if (contentTXT !=null && !contentTXT.toString().equals(""))
		{
			int  AD_Table_ID = MTable.getTable_ID(X_ITS_generateTXT.Table_Name);
			log.log(Level.INFO, "AD_Table_ID: " + AD_Table_ID + " - Record_ID: " + p_Record_ID);
			MAttachment attach =  MAttachment.get(getCtx(),AD_Table_ID,p_Record_ID);
			log.log(Level.INFO, "Contexto: " + getCtx().toString());
		
			if (attach == null ) {
				log.info("attach == null: ");
				log.log(Level.INFO, "AD_Table_ID: " + AD_Table_ID + " - Record_ID: " + p_Record_ID);
				attach = new  MAttachment(getCtx(),AD_Table_ID ,p_Record_ID,get_TrxName());
				attach.addEntry(archivo);
				attach.save();
				log.info("attach.save");
			} else {
				log.info("attach != null: ");
				log.log(Level.INFO, "AD_Table_ID: " + AD_Table_ID + " - Record_ID: " + p_Record_ID);
				int index = (attach.getEntryCount()-1);
				MAttachmentEntry entry = attach.getEntry(index) ;
				String renamed = nombreArch + p_TypeOperation + fecha + "_old" + ".txt";
				entry.setName(renamed);
				attach.save();
				//agrega el nuevo archivo ya q el anterior ha sido renombrado
				attach.addEntry(archivo);
				attach.save();
				}
			return "Archivo Generado y Anexado:  -> " + fileNameTXT + ", "+cont+" Retenciónes Procesadas, Refrescar Ventana y revisar en Anexos.	";
			
		} else
			return "El Archivo no pudo ser Generado porque no hay retenciones de " + tipoOperacion + " para este periodo, desde: " + p_ValidFrom + ", hasta: " + p_ValidTo + ".";
	}
}