package dev.itechsolutions.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MDocType;
import org.compiere.model.MSysConfig;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Msg;

import dev.itechsolutions.model.ITSMInvoice;
import dev.itechsolutions.model.MITSVoucherWithholding;
import dev.itechsolutions.util.ColumnUtils;

/**
 * 
 * @author José Castañeda
 *
 */
public class GenerateWithholdingVouchers extends SvrProcess {
	
	/****			Parameters			****/
	private int count 				  = 0;
	private int p_DaysBack 			  = 0;
	private int p_AD_Org_ID 		  = 0;
	private int p_C_Invoice_ID 		  = 0;
	private int p_C_BPartner_ID		  = 0;
	private int p_C_Currency_ID 	  = 0;
	private int p_WithholdingType_ID  = 0;
	private int p_C_ConversionType_ID = 0;
	
	private String p_DocAction 		= "DR";
	
	private Timestamp p_DateDocFrom;
	private Timestamp p_DateDocTo;
	private Timestamp p_DateTrx;
	/*-------------------------------------*/
	
	@Override
	protected void prepare()
	{
		ProcessInfoParameter[] params = getParameter();
		
		for(ProcessInfoParameter para : params)
		{
			String name = para.getParameterName();
			
			if (para.getParameter() == null)
				continue ;
			else if(name.equals("DaysBack"))
				p_DaysBack = para.getParameterAsInt();
			else if (name.equals("AD_Org_ID"))
				p_AD_Org_ID = para.getParameterAsInt();
			else if (name.equals("C_Invoice_ID"))
				p_C_Invoice_ID = para.getParameterAsInt();
			else if (name.equals("C_BPartner_ID"))
				p_C_BPartner_ID = para.getParameterAsInt();
			else if (name.equals("C_Currency_ID"))
				p_C_Currency_ID = para.getParameterAsInt();
			else if (name.equals("LCO_WithholdingType_ID"))
				p_WithholdingType_ID = para.getParameterAsInt();
			else if (name.equals("C_ConversionType_ID"))
				p_C_ConversionType_ID = para.getParameterAsInt();
			else if (name.equals("DocAction"))
				p_DocAction = para.getParameterAsString();
			else if (name.equals("DateTrx"))
				p_DateTrx = para.getParameterAsTimestamp();
			else if (name.equals("DateDoc"))
			{
				p_DateDocFrom = para.getParameterAsTimestamp();
				p_DateDocTo = para.getParameter_ToAsTimestamp();
			}
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
		
	}//	prepare
	
	@Override
	protected String doIt() throws Exception
	{
		String msg = Msg.parseTranslation(getCtx(), "@ITS_VoucherWithholding_ID@");
		
		StringBuilder sqlwt = new StringBuilder("SELECT wt.IsSOTrx")
				.append(", wt.LCO_WithholdingType_ID")
				.append(", wt.WithholdingType ")
				.append("FROM LCO_WithholdingType wt ")
				.append("WHERE wt.IsActive = 'Y'");
		
		if (p_WithholdingType_ID > 0)
			sqlwt.append(" AND wt.LCO_WithholdingType_ID = ?");
		
		ResultSet rswt = null;
		PreparedStatement pstmtwt = null;
		
		try
		{
			pstmtwt = DB.prepareStatement(sqlwt.toString(), get_TrxName());
			
			if (p_WithholdingType_ID > 0)
				pstmtwt.setInt(1, p_WithholdingType_ID);
			
			rswt = pstmtwt.executeQuery();
			
			while (rswt.next())
			{
				String type = rswt.getString("WithholdingType");
				boolean IsSOTrx = rswt.getBoolean("IsSOTrx");
				int LCO_WithholdingType_ID = rswt.getInt("LCO_WithholdingType_ID");			
				
				MDocType DocType = new Query(getCtx(), MDocType.Table_Name, "LCO_WithholdingType_ID = ?", get_TrxName())
						.setOnlyActiveRecords(true)
						.setParameters(LCO_WithholdingType_ID)
						.first();
				
				if (DocType == null)
					continue ;
				
				StringBuilder sql = new StringBuilder("");
				
				StringBuilder where = new StringBuilder("(i.AD_Org_ID IN (SELECT DISTINCT Node_ID ")
							.append("FROM getnodes")
								.append("(").append(p_AD_Org_ID)
									.append(", (SELECT AD_TREE_ID ")
									.append("FROM AD_TREE ")
									.append("WHERE TreeType ='OO'")
									.append(" AND AD_Client_ID = ").append(getAD_Client_ID())
								.append("),").append(getAD_Client_ID())
								.append(") AS N (Parent_ID numeric,Node_ID numeric) ")
							.append("WHERE Parent_ID = ").append(p_AD_Org_ID)
						.append(") OR i.AD_Org_ID = ").append(p_AD_Org_ID)
						.append(" OR 0 = ").append(p_AD_Org_ID)
						.append(")");
				
				if(type.equalsIgnoreCase("IVA"))
				{
					sql.append("SELECT i.C_BPartner_ID")
					   .append(", i.AD_Org_ID")
					   .append(", DATE_TRUNC('day', i.DateAcct) AS DateAcct ")
					   .append("FROM C_Invoice i ")
					   .append("JOIN C_DocType cd ON cd.C_DocType_ID = i.C_DocType_ID ")
					   .append("WHERE NOT EXISTS ")
					   		.append("(SELECT 1 FROM LCO_InvoiceWithholding iw ")
					   		.append("JOIN ITS_VoucherWithholding vo ON vo.ITS_VoucherWithholding_ID = iw.ITS_VoucherWithholding_ID ")
					   		.append("WHERE iw.C_Invoice_ID = i.C_Invoice_ID")
					   		.append(" AND vo.DocStatus IN ('CO','CL')")
					   		.append(" AND iw.LCO_WithholdingType_ID = ?)")
					  .append(" AND cd.GenerateWithholding = 'Y' ")
					  .append(" AND ")
					  .append(where);
				}
				else if (type.equalsIgnoreCase("ISLR"))
				{
					sql.append("SELECT i.C_BPartner_ID")
					   .append(", i.AD_Org_ID")
					   .append(", DATE_TRUNC('day', i.DateAcct) AS DateAcct ")
					   .append("FROM C_Invoice i ")
					   .append("JOIN C_DocType cd ON cd.C_DocType_ID = i.C_DocType_ID ")
					   .append("INNER JOIN C_InvoiceLine il ON il.C_Invoice_ID = i.C_Invoice_ID ")
					   .append("LEFT JOIN C_Charge c ON c.C_Charge_ID = il.C_Charge_ID ")
					   .append("LEFT JOIN M_Product p ON p.M_Product_ID = il.M_Product_ID ")
					   .append("WHERE NOT EXISTS ")
					   		.append("(SELECT 1 FROM LCO_InvoiceWithholding iw ")
					   		.append("JOIN ITS_VoucherWithholding vo ON vo.ITS_VoucherWithholding_ID = iw.ITS_VoucherWithholding_ID ")
					   		.append("WHERE iw.C_Invoice_ID = i.C_Invoice_ID")
					   		.append(" AND vo.DocStatus IN ('CO','CL')")
					   		.append(" AND iw.LCO_WithholdingType_ID = ?)")
					  .append(" AND cd.GenerateWithholding = 'Y' ")
					  .append(" AND ")
					  .append(where)
					  .append(" AND (p.ProductType IS NULL OR p.ProductType = 'S')")
					  .append(" AND (c.LCO_WithholdingCategory_ID > 0 OR p.LCO_WithholdingCategory_ID > 0)");
				}
				else if (type.equalsIgnoreCase("IAE"))
				{
					sql.append("SELECT i.C_BPartner_ID")
					   .append(", i.AD_Org_ID")
					   .append(", DATE_TRUNC('day', i.DateAcct) AS DateAcct ")
					   .append("FROM C_Invoice i ")
					   .append("JOIN C_DocType cd ON cd.C_DocType_ID = i.C_DocType_ID ")
					   .append("INNER JOIN C_BPartner bp ON bp.C_BPartner_ID = i.C_BPartner_ID ")
					   .append("JOIN C_InvoiceLine il ON il.C_Invoice_ID = i.C_Invoice_ID ")
					   .append("LEFT JOIN M_Product p ON p.M_Product_ID = il.M_Product_ID ")
					   .append("WHERE NOT EXISTS ")
					   		.append("(SELECT 1 FROM LCO_InvoiceWithholding iw ")
					   		.append("JOIN ITS_VoucherWithholding vo ON vo.ITS_VoucherWithholding_ID = iw.ITS_VoucherWithholding_ID ")
					   		.append("WHERE iw.C_Invoice_ID = i.C_Invoice_ID")
					   		.append(" AND vo.DocStatus IN ('CO','CL')")
					   		.append(" AND iw.LCO_WithholdingType_ID = ?)")
					  .append(" AND cd.GenerateWithholding = 'Y' ")
					  .append(" AND ")
					  .append(" AND (p.ProductType IS NULL OR p.ProductType = 'S')")
					  .append(" AND bp.LCO_ISIC_ID > 0")
					  .append(where);
				}
				
				if(p_DaysBack > 0)
					sql.append(" AND DATE_TRUNC('day', i.DateAcct) BETWEEN (?::timestamp-Interval '").append(p_DaysBack).append(" Day') AND ?");
				else
					sql.append(" AND DATE_TRUNC('day', i.DateAcct) BETWEEN ? AND ?");
				
				if(p_C_BPartner_ID > 0)
					sql.append(" AND i.C_BPartner_ID = ?");
				
				if(p_C_Invoice_ID > 0)
					sql.append(" AND i.C_Invoice_ID = ?");
	
				sql.append(" AND cd.GenerateWithholding = 'Y'")
				   .append(" AND i.IsSOTrx = ?")
				   .append(" AND i.DocStatus IN ('CO','CL')")
				   .append(" AND i.AD_Client_ID = ?");
				
				if(!MSysConfig.getBooleanValue(ColumnUtils.SYSCONFIG_LVE_GenerateInvoiceWithholdingIsPaid, false
						, getAD_Client_ID()))
					sql.append(" AND i.ISPaid = 'N'");
				
				sql.append(" GROUP BY i.C_BPartner_ID, i.AD_Org_ID, DATE_TRUNC('day', i.DateAcct)");
				
				ResultSet rs = null;
				PreparedStatement pstmt = null;
				
				try
				{
					int index = 1;
					
					pstmt= DB.prepareStatement(sql.toString(), get_TrxName());
					
					pstmt.setInt(index++, LCO_WithholdingType_ID);
					
					pstmt.setTimestamp(index++, p_DateDocFrom);
					pstmt.setTimestamp(index++, p_DateDocTo);
				
					if (p_C_BPartner_ID > 0)
						pstmt.setInt(index++, p_C_BPartner_ID);
					
					if (p_C_Invoice_ID > 0)
						pstmt.setInt(index++, p_C_Invoice_ID);
					
					pstmt.setString(index++, IsSOTrx ? "Y" : "N");
					pstmt.setInt(index++, getAD_Client_ID());
					
					rs = pstmt.executeQuery();
					
					int ins = 0;
					int vins = 0;
					
					while (rs.next())
					{
						vins = 0;
						
						int C_BPartner_ID = rs.getInt("C_BPartner_ID");
						int AD_Org_ID = rs.getInt("AD_Org_ID");
						Timestamp Date = rs.getTimestamp("DateAcct");
						
						MITSVoucherWithholding voucher = new MITSVoucherWithholding(getCtx(), 0, get_TrxName());
						
						voucher.setAD_Org_ID(AD_Org_ID);
						voucher.setC_BPartner_ID(C_BPartner_ID);
						
						if(p_DaysBack > 0)
						{
							voucher.setDateTrx(Date);
							voucher.set_ValueOfColumn("DateAcct",Date);								
						}
						else
						{
							voucher.setDateTrx(p_DateTrx);
							voucher.set_ValueOfColumn("DateAcct", p_DateTrx);							
						}
						
						voucher.setLCO_WithholdingType_ID(LCO_WithholdingType_ID);
						voucher.setIsSOTrx(IsSOTrx);
						voucher.setC_Currency_ID(p_C_Currency_ID);
						voucher.setC_ConversionType_ID(p_C_ConversionType_ID);
						voucher.setC_DocType_ID(DocType.get_ID());		
						
						if(p_C_Invoice_ID > 0)
							voucher.setC_Invoice_ID(p_C_Invoice_ID);
						
						voucher.saveEx(get_TrxName());
						
						ITSMInvoice[] invoices = ITSMInvoice.getOfVoucherWithholding(voucher);
						
						for (ITSMInvoice invoice : invoices)
						{								
							ins = invoice.recalcWithholdings(voucher);
							vins +=  ins;
							count += ins;
						}
						
						if(vins>0)
						{
							if(p_DocAction.equals("CO"))
							{
								try
								{
									if(!voucher.processIt(p_DocAction))
										throw new AdempiereException(voucher.getProcessMsg());
									
								} catch (Exception e)
								{
									throw new AdempiereException(e.getLocalizedMessage());
								}
							}
							
							voucher.saveEx(get_TrxName());
							
							addBufferLog(voucher.get_ID(), new Timestamp(System.currentTimeMillis())
									, null
									, msg+": "+voucher.getDocumentNo()
									, voucher.get_Table_ID()
									, voucher.get_ID());
						}
						else
							voucher.deleteEx(true, get_TrxName());
					}
				}
				catch (Exception e)
				{
					throw new AdempiereException(e.getLocalizedMessage());
				}
				finally
				{
					DB.close(rs, pstmt);
					rs = null;
					pstmt = null; 
				}
			}
		}
		catch (Exception e)
		{
			throw new AdempiereException(e.getLocalizedMessage());
		}
		finally
		{
			DB.close(rswt, pstmtwt);
			rswt = null;
			pstmtwt = null;
		}
		
		return "@ITS_VoucherWithholding_ID@ @Generate@: " + count;
		
	}//	doIt

}//	GenerateWithholdingVouchers