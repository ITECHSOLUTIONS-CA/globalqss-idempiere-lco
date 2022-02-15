package dev.itechsolutions.process;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.Level;

import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.MTable;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import dev.itechsolutions.model.X_ITS_generateXML;

public class ITS_GenerateXmlSeniat extends SvrProcess {

	/**Organization                  */
	private int	p_AD_Org_ID = 0;	
	/** ValidFrom               	*/
	private Timestamp 	p_ValidFrom=null;
	/** ValidTo                 	*/
	private Timestamp   p_ValidTo=null;
	/** Record_ID               */
	private int p_Record_ID = 0;
	/** X_LVE_generateXML       */
	private int p_LVE_generateXML_ID = 0;
	
	private int p_C_Currency_ID = 0;
	
	private X_ITS_generateXML generateXML;
	/* (non-Javadoc)
	 * @see org.compiere.process.SvrProcess#prepare()
	 */
	@Override
	protected void prepare() {
	
		p_Record_ID = getRecord_ID();
		p_LVE_generateXML_ID = p_Record_ID;
		generateXML = new X_ITS_generateXML(getCtx(), p_LVE_generateXML_ID, get_TrxName());
		
		p_AD_Org_ID = generateXML.getAD_Org_ID();
		p_ValidFrom = generateXML.getValidFrom();
		p_ValidTo = generateXML.getValidTo();
		p_C_Currency_ID = generateXML.get_ValueAsInt("C_Currency_ID");
		
		
		log.log(Level.INFO, "*********  Prepare  **********");
		log.log(Level.INFO, "Parameters: " + "AD_Org_ID: " + p_AD_Org_ID + ", ValidFrom: " + p_ValidFrom + ", ValidTo: " + p_ValidTo + ", Record_ID: " + p_Record_ID);
		
	}
	
	@SuppressWarnings("resource")
	@Override
	protected String doIt() throws Exception {
		
		String sqlTotalAmt = "(select sum(currencyconvert(iw.taxamt, vw.c_currency_id, "+p_C_Currency_ID+", i.dateinvoiced,i.c_conversiontype_id,i.ad_client_id,i.ad_org_id)) \n" + 
				"from lco_invoicewithholding iw\n" + 
				"join its_voucherwithholding vw on iw.its_voucherwithholding_id = vw.its_voucherwithholding_id\n" + 
				"and exists (select 1 from c_invoiceline iil join c_invoice ii on iil.c_invoice_id = ii.c_invoice_id\n" + 
				"where iil.its_voucherwithholding_id = vw.its_voucherwithholding_id and ii.docstatus in ('CO','DR','CL','IP'))\n" + 
				"join lco_withholdingtype on lco_withholdingtype.lco_withholdingtype_id = iw.lco_withholdingtype_id \n" + 
				"join c_invoice i on iw.c_invoice_id = i.c_invoice_id \n" + 
				"where vw.docstatus in ('CO','CL') and lco_withholdingtype.\"withholdingtype\" = 'ISLR' \n" + 
				"and vw.dateacct between '"+p_ValidFrom+"' and '"+p_ValidTo+"'\n" + 
				"and (vw.AD_Org_ID in (select AD_Org_ID from AD_OrgInfo \n" + 
				"	where Parent_Org_ID = "+p_AD_Org_ID+")\n" + 
				"	or vw.AD_Org_ID = "+p_AD_Org_ID+"))";
		BigDecimal totalAmt = DB.getSQLValueBDEx(get_TrxName(), sqlTotalAmt);
		if(totalAmt!=null) {
			generateXML.set_ValueOfColumn("TotalAmt", totalAmt);
			generateXML.saveEx(get_TrxName());
		}
		
		
		String sql="";
		Element root= null;
		
		String dia, mes , anio;
		Calendar date = new GregorianCalendar();
		dia = Integer.toString(date.get(Calendar.DATE));
		mes = Integer.toString(date.get(Calendar.MONTH) + 1);
		anio = Integer.toString(date.get(Calendar.YEAR));
		String fecha = dia + mes + anio;
		
		String nombreArch="XML_Seniat";
		String fileNameXML=nombreArch + fecha + ".xml";
		
		FileOutputStream file=new FileOutputStream(fileNameXML);
	   
		sql=("SELECT *, to_char(fecha,'DD/MM/YYYY') as fechaoperacion "
				+ " FROM its_xmlislr " 
				+ " WHERE (" 
					+ " its_xmlislr.org IN  (SELECT DISTINCT Node_ID FROM getnodes("+p_AD_Org_ID+",(SELECT AD_Tree_ID FROM AD_Tree WHERE TreeType ='OO' "
					+ "AND AD_Client_ID="+getAD_Client_ID()+"),"+getAD_Client_ID()+") AS N (Parent_ID numeric,Node_ID numeric) " 
					+ " WHERE Parent_ID = "+p_AD_Org_ID+") OR its_xmlislr.org="+p_AD_Org_ID+")"		
				+ " AND (its_xmlislr.fecha BETWEEN '" + p_ValidFrom + "' AND '"+ p_ValidTo +"')" 
				);
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
	try {
		pstmt = DB.prepareStatement(sql, null);
		rs = pstmt.executeQuery();
		
		while (rs.next())
		{
				
				root = new Element("RelacionRetencionesISLR");
			
				root.setAttribute("RifAgente", rs.getString(8).trim());
				root.setAttribute("Periodo", rs.getString(9).trim());
			    		    
		//  Creamos un hijo para el root
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
			    
			    root.addContent(detalleRetencion);
		    			      
			//   //Agregamos al root
			}
		}	
		catch ( Exception e )
        {
            System.out.println(e.getMessage());
        }finally {
        	DB.close(rs,pstmt);
        }
	
		if(root!=null)
		{
			
			//Creamos el documento
			Document doc=new Document(root);
			
			try{	
			      Format format = Format.getPrettyFormat();
			      format.setEncoding("ISO-8859-1");
			      log.log(Level.INFO, "Format XML");
			      XMLOutputter out=new XMLOutputter();
			      out.setFormat(format);
			      out.output(doc,file);
			      file.flush();
			      file.close();
				
			    }catch(Exception e){e.printStackTrace();}
			
			File archivoXML=new File(fileNameXML);
				
			
				int  AD_Table_ID = MTable.getTable_ID(X_ITS_generateXML.Table_Name);
				log.log(Level.INFO, "AD_Table_ID: " + AD_Table_ID + " - Record_ID: " + p_Record_ID);
				MAttachment attach =  MAttachment.get(getCtx(),AD_Table_ID,p_Record_ID);
			
				if (attach == null ) {
					log.info("attach == null: ");
					log.log(Level.INFO, "AD_Table_ID: " + AD_Table_ID + " - Record_ID: " + p_Record_ID);
					attach = new  MAttachment(getCtx(),AD_Table_ID ,p_Record_ID,get_TrxName());
					attach.addEntry(archivoXML);
					attach.save();
					log.info("attach.save");
				} else {
					log.info("attach != null: ");
					log.log(Level.INFO, "AD_Table_ID: " + AD_Table_ID + " - Record_ID: " + p_Record_ID);
					int index = (attach.getEntryCount()-1);
					MAttachmentEntry entry = attach.getEntry(index) ;
					String renamed = nombreArch + fecha + "_old" + ".xml";
					entry.setName(renamed);
					attach.save();
					//agrega el nuevo archivo ya q el anterior ha sido renombrado
					attach.addEntry(archivoXML);
					attach.save();
					}
				
				return "Archivo Generado y Anexado:  -> " + fileNameXML + ", Refrescar Ventana y revisar en Anexos.	";	
		}else {
			return "El Archivo no pudo ser Generado porque no hay retenciones ISLR para este periodo, desde: " + p_ValidFrom + ", hasta: " + p_ValidTo + ".";
		}
			
	}
}