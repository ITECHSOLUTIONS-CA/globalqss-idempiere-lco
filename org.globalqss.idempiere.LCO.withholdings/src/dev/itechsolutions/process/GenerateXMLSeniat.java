package dev.itechsolutions.process;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.MTable;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Msg;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import dev.itechsolutions.model.MITSGenerateXML;

/**
 * 
 * @author José Castañeda
 *
 */
@Process
public class GenerateXMLSeniat extends SvrProcess {

	/**	Organization					*/
	private int			p_AD_Org_ID;
	/** Currency						*/
	private int 		p_C_Currency_ID;
	/** ValidFrom           	    	*/
	private Timestamp 	p_ValidFrom;
	/** ValidTo                 		*/
	private Timestamp   p_ValidTo;
	
	/** MITSgenerateXML     			*/
	private MITSGenerateXML generateXML;
	
	@Override
	protected void prepare()
	{
		generateXML = MITSGenerateXML.get(getRecord_ID());
		
		p_AD_Org_ID 	= generateXML.getAD_Org_ID();
		p_C_Currency_ID = generateXML.getC_Currency_ID();
		p_ValidFrom 	= generateXML.getValidFrom();
		p_ValidTo 		= generateXML.getValidTo();	
	
	}//	prepare

	@SuppressWarnings("resource")
	protected String doIt() throws Exception
	{
		//	Params
		List<Object>params = new ArrayList<Object>();
		
		//	Update Header Amt
		StringBuilder totalAmt = new StringBuilder("SELECT ")
				.append("SUM (currencyConvert(iw.ConvertedTaxAmt, vw.C_Currency_ID, ?")
				.append(", i.DateInvoiced, i.C_ConversionType_ID")
				.append(", i.AD_Client_ID, i.AD_Org_ID)) ")
				.append("FROM LCO_InvoiceWithholding iw ")
				.append("JOIN ITS_VoucherWithholding vw ON vw.ITS_VoucherWithholding_ID = iw.ITS_VoucherWithholding_ID ")
				.append(" AND EXISTS (SELECT 1  ")
					.append("FROM C_InvoiceLine iil ")
					.append("JOIN C_Invoice ii ON ii.C_Invoice_ID = iil.C_Invoice_ID ")
					.append("WHERE iil.ITS_VoucherWithholding_ID = vw.ITS_VoucherWithholding_ID")
					.append(" AND ii.DocStatus IN ('CO','DR','CL','IP')) ")
				.append("JOIN LCO_WithholdingType wt ON wt.LCO_WithholdingType_ID = iw.LCO_WithholdingType_ID ")
				.append("JOIN C_Invoice i ON i.C_Invoice_ID = iw.C_Invoice_ID ")
				.append("WHERE vw.DocStatus IN ('CO','CL')")
				.append(" AND wt.WithholdingType = 'ISLR'")
				.append(" AND vw.DateAcct BETWEEN ? AND ?")
				.append(" AND (iw.AD_Org_ID IN (")
					.append("SELECT AD_Org_ID ")
					.append("FROM AD_OrgInfo ")
					.append("WHERE Parent_Org_ID = ? ")
					.append(" OR vw.AD_Org_ID = ?))");
		
		params.add(p_C_Currency_ID);
		params.add(p_ValidFrom);
		params.add(p_ValidTo);
		params.add(p_AD_Org_ID);
		params.add(p_AD_Org_ID);
		
		BigDecimal amt = DB.getSQLValueBDEx(get_TrxName(), totalAmt.toString(), params);
		
		if(amt !=null)
		{
			generateXML.setTotalAmt(amt);
			generateXML.saveEx(get_TrxName());
		}
		
		Element root = null;
		
		String day, month , year;
		
		Calendar CalendarDate = new GregorianCalendar();
		
		day = Integer.toString(CalendarDate.get(Calendar.DATE));
		month = Integer.toString(CalendarDate.get(Calendar.MONTH) + 1);
		year = Integer.toString(CalendarDate.get(Calendar.YEAR));
		
		String date = day + month + year;

		String fileName = "XML_Seniat";
		String fileNameXML = fileName + date + ".xml";
		
		FileOutputStream file = new FileOutputStream(fileNameXML);
		
		StringBuilder sql = new StringBuilder("SELECT TaxID")
				.append(", DocumentNo")
				.append(", ControlNumber")
				.append(", Code")
				.append(", TaxBaseAmt")
				.append(", Percent")
				.append(", DateAcct")
				.append(", OrgTaxID")
				.append(", Period")
				.append(", Org_ID ")
				.append("FROM ITS_XMLISLR xml ")
				.append("WHERE (xml.Org_ID IN")
					.append("(SELECT DISTINCT Node_ID ")
					.append("FROM getnodes(?, (SELECT AD_Tree_ID FROM AD_Tree ")
					.append("WHERE TreeType = 'OO'")
					.append(" AND AD_Client_ID = ?), ?) AS N (Parent_ID numeric,Node_ID numeric) ")
					.append("WHERE Parent_ID = ?) OR xml.Org_ID = ?)")
				.append(" AND (xml.DateAcct BETWEEN ? AND ?)");
		
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		
		int cont = 0;
		
		try
		{
			int index = 1;
			
			pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
			
			pstmt.setInt(index++, p_AD_Org_ID);
			pstmt.setInt(index++, getAD_Client_ID());
			pstmt.setInt(index++, getAD_Client_ID());
			pstmt.setInt(index++, p_AD_Org_ID);
			pstmt.setInt(index++, p_AD_Org_ID);
			pstmt.setTimestamp(index++, p_ValidFrom);
			pstmt.setTimestamp(index++, p_ValidTo);

			rs = pstmt.executeQuery();
			
			while(rs.next())
			{
				root = new Element("RelacionRetencionesISLR");
				
				root.setAttribute("RifAgente", rs.getString(8).trim());
				root.setAttribute("Periodo", rs.getString(9).trim());
			    		    
				//  Create a Child For The Root 
				Element detalleRetencion=new Element("DetalleRetencion");
				 
				if (rs.getString(1) != null)
					detalleRetencion.addContent(new Element("RifRetenido").setText(rs.getString(1).trim()));
				else
					detalleRetencion.addContent(new Element("RifRetenido").setText("Vacio"));
				 
				if (rs.getString(2) != null)
					detalleRetencion.addContent(new Element("NumeroFactura").setText(rs.getString(2).trim()));
				else
					detalleRetencion.addContent(new Element("NumeroFactura").setText("Vacio"));
				 
				if (rs.getString(3) != null)
					detalleRetencion.addContent(new Element("NumeroControl").setText(rs.getString(3).trim()));
				else
					detalleRetencion.addContent(new Element("NumeroControl").setText("Vacio"));
				 
				if (rs.getString(4) != null)
					detalleRetencion.addContent(new Element("CodigoConcepto").setText(rs.getString(4).trim()));
				else
					detalleRetencion.addContent(new Element("CodigoConcepto").setText("Vacio"));
				
			    detalleRetencion.addContent(new Element("MontoOperacion").setText(rs.getString(5).trim()));
			    detalleRetencion.addContent(new Element("PorcentajeRetencion").setText(rs.getString(6).trim()));
			    
			    //	Add Root
			    root.addContent(detalleRetencion);
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
	
		if(root!=null)
		{
			//	Create Document
			Document doc=new Document(root);
			
			try
			{
				Format format = Format.getPrettyFormat();
				
				format.setEncoding("ISO-8859-1");
				
				log.log(Level.INFO, "Format XML");
				
				XMLOutputter out=new XMLOutputter();
				
				out.setFormat(format);
				out.output(doc,file);
				
				file.flush();
				file.close();
				
			}
			catch(Exception e)
			{
				e.printStackTrace();
				throw new AdempiereException(e.getLocalizedMessage());
			}
			
			File archivoXML=new File(fileNameXML);
			
			int  AD_Table_ID = MTable.getTable_ID(MITSGenerateXML.Table_Name);
			
			MAttachment attach =  MAttachment.get(getCtx(), AD_Table_ID, getRecord_ID());
		
			if (attach == null )
			{
				attach = new  MAttachment(getCtx(), AD_Table_ID, getRecord_ID(), get_TrxName());
				
				attach.addEntry(archivoXML);
				attach.save();
				
				log.info("attach.save");
			}
			else
			{
				int index = (attach.getEntryCount()-1);
				
				MAttachmentEntry entry = attach.getEntry(index) ;
				
				String renamed = fileName + date + "_OLD" + ".xml";
				
				entry.setName(renamed);
				attach.save();
				
				//	The new file is added as the old one has been renamed
				attach.addEntry(archivoXML);
				attach.save();
			}
			
			return Msg.getMsg(generateXML.getCtx(), "FileGenerated",new Object[] {fileNameXML, cont});	
		} 
		else
			return Msg.getMsg(generateXML.getCtx(), "FileNoGenerated",new Object[] {"ISLR", p_ValidFrom, p_ValidTo});
	}//	doIt

}// GenerateXMLSeniat