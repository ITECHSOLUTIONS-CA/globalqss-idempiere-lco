package dev.itechsolutions.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import org.adempiere.base.annotation.Parameter;
import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MInvoice;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

import dev.itechsolutions.model.MITSPurchaseSalesBook;


/**
 * 
 * @author José Castañeda
 *
 */
@Process
public class ValidateBook extends SvrProcess {
	
	@Parameter(name = "DocAction")
	private String 		DocAction = "";
	
	@Parameter(name = "IsConfirmed")
	private boolean 	IsConfirmed;
	
	private String msg = "";
	private MITSPurchaseSalesBook Book;
	
	@Override
	protected void prepare()
	{
		Book = new MITSPurchaseSalesBook(getCtx(), getRecord_ID(), get_TrxName());
	}//	prepare
	
	@Override
	protected String doIt() throws Exception 
	{
		if(DocAction.equals("CO")) 
			return complete();
		
		if(DocAction.equals("RE"))
			return reactivate();
		
		return "";
	}//	doIt
	
	public String reactivate()
	{
		Book.setIsValid(false);
		Book.saveEx(get_TrxName());
		
		return "";
	}//	reactivate
	
	public String complete()
	{
		String TypeOperation = Book.getITS_TypeOperation().equals("V") ? "Y" : "N";
		
		StringBuilder orgWhereClause = new StringBuilder(" AND (org.AD_Org_ID IN ")
				.append("(SELECT DISTINCT Node_ID FROM getnodes(0,(SELECT AD_Tree_ID ")
				.append("FROM AD_Tree tree ")
				.append("WHERE TreeType ='OO'")
				.append(" AND tree.AD_Client_ID = o.AD_Client_ID)")
				.append(", o.AD_Client_ID) ")
				.append("AS N(Parent_ID numeric, Node_ID numeric) ")
				.append("WHERE Parent_ID = " + Book.getAD_Org_ID())
				.append(" OR 0 = " + Book.getAD_Org_ID() + ")) ");
		
		boolean valid = true;
		
		if(TypeOperation.equals("N"))
		{
			Book.setIsSOTrx(TypeOperation.equals("N"));
			
			if(!ValidateUnProcessedInvoice(TypeOperation, orgWhereClause))
				valid = false;
			
			if(!ValidateInvoiceHaveWithHoldingVoucher(TypeOperation, orgWhereClause))
				valid = false;
		}
		
		if(TypeOperation.equals("Y")) 
		{
			Book.setIsSOTrx(TypeOperation.equals("Y"));
			
			if(!ValidateUnProcessedInvoice(TypeOperation, orgWhereClause))
				valid = false;
		}
		
		Book.setIsValid(valid);
		Book.saveEx();
		
		return msg;
	}//	complete
	
	public boolean ValidateUnProcessedInvoice(String TypeOperation, StringBuilder orgWhereClause) 
	{
		msg = "@UnProcessedByStatus@ \n";
		
		StringBuilder sql = new StringBuilder("SELECT DISTINCT count(i.C_Invoice_ID)")
				.append(", trl.name")
				.append(", org.name as OrgName ")
				.append("FROM C_Invoice i ")
				.append("JOIN AD_Org org ON org.AD_Org_ID = i.AD_Org_ID ")
				.append("JOIN AD_OrgInfo o ON o.AD_Org_ID = i.AD_Org_ID ")
				.append("JOIN AD_Reference aref ON AD_Reference_ID = 131 ")
				.append("JOIN AD_Ref_List refl ON refl.AD_Reference_ID = aref.AD_Reference_ID")
				.append(" AND refl.Value = i.DocStatus ")
				.append("JOIN AD_Ref_List_Trl trl ON trl.AD_Ref_List_ID = refl.AD_Ref_List_ID")
				.append(" AND trl.AD_Language = 'es_CO' ")
				.append("JOIN C_DocType cd ON cd.C_DocType_ID = i.C_DocTypeTarget_ID")
				.append(" AND IsAffectedBook = 'Y' ")
				.append("WHERE i.IsActive = 'Y'")
				.append(" AND i.DocStatus NOT IN ('CO','RE','VO')")
				.append(" AND DATE_TRUNC('Day', i.DateAcct)::TimesTamp")
				.append(" BETWEEN ? AND ? ")
				.append(" AND i.IsSOTrx = ? ")
				.append(orgWhereClause)
				.append("GROUP BY trl.Name, org.Name ")
				.append("ORDER BY org.Name ");
		
		int count = 0;
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try
		{
			int index = 1;
			
			pstmt = DB.prepareStatement(sql.toString(),get_TrxName());
			
			pstmt.setTimestamp(index++, Book.getDateFrom());
			pstmt.setTimestamp(index++, Book.getDateTo());
			pstmt.setString(index++, TypeOperation);
			
			rs = pstmt.executeQuery();
			
			while(rs.next())
			{
				if(count == 0
						&& !IsConfirmed)
					this.addLog(msg);
				
				if(!IsConfirmed)
					this.addLog(rs.getString("OrgName")+" "+rs.getString(2)+":"+rs.getInt(1)+"\n");
				
				count++;
			}
		}
		catch (SQLException e) 
		{
			log.log(Level.SEVERE, sql.toString(), e);
			throw new AdempiereException(e.getLocalizedMessage());
		}
		finally 
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		
		if(count > 0)
		{
			if(!IsConfirmed)
			{
				msg = "@CheckValid@";
				return false;
			}
			else
				msg = "";
		}
		else
			msg = "";
		
		return true;
	}//	ValidateUnProcessedInvoice

	public boolean ValidateInvoiceHaveWithHoldingVoucher(String TypeOperation, StringBuilder orgWhereClause)
	{
		msg = "@NoHaveWithholdingGenerate@ \n";
		
		StringBuilder sql = new StringBuilder("SELECT DISTINCT i.C_Invoice_ID")
				.append(", org.Name AS OrgName ")
				.append("FROM C_Invoice i ")
				.append("JOIN AD_Org org ON org.AD_Org_ID = i.AD_Org_ID ")
				.append("JOIN AD_OrgInfo o ON o.AD_Org_ID = org.AD_Org_ID ")
				.append("JOIN LCO_WithholdingType lwt ON lwt.WithholdingType::TEXT = 'IVA'")
				.append(" AND lwt.IsSOTrx = i.IsSOTrx")
				.append(" AND lwt.AD_Client_ID = i.AD_Client_ID ")
				.append("LEFT JOIN LCO_InvoiceWIthholding li ON li.C_Invoice_ID = i.C_Invoice_ID")
				.append(" AND li.LCO_WithholdingType_ID = lwt.LCO_WithholdingType_ID ")
				.append("JOIN C_DocType cd ON cd.C_DocType_ID = i.C_DocTypeTarget_ID")
				.append(" AND cd.IsAffectedBook = 'Y' ")
				.append("WHERE NOT EXISTS (SELECT 1 FROM ITS_VoucherWithholding vwh ")
					.append("JOIN LCO_InvoiceWithholding iwh ON iwh.ITS_VoucherWithholding_ID = vwh.ITS_VoucherWithholding_ID ")
					.append("WHERE vwh.DocStatus IN ('CO', 'CL')")
					.append(" AND iwh.LCO_WithholdingType_ID = lwt.LCO_WithholdingType_ID")
					.append(" AND iwh.C_Invoice_ID = i.C_Invoice_ID)")
				.append(" AND i.GrandTotal <> i.TotalLines")
				.append(" AND i.DocStatus IN ('CO', 'CL')")
				.append(" AND i.IsSOTrx = 'N'")
				.append(" AND DATE_TRUNC('Day',i.DateAcct)::TimesTamp")
				.append(" BETWEEN ? AND ? ")
				.append(orgWhereClause);
		
		int count = 0;
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try 
		{
			int index = 1;
			
			pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
			
			pstmt.setTimestamp(index++, Book.getDateFrom());
			pstmt.setTimestamp(index++, Book.getDateTo());
			
			rs = pstmt.executeQuery();
			
			while(rs.next())
			{
				int C_Invoice_ID = rs.getInt("C_Invoice_ID");
				String orgName = rs.getString("OrgName");
				
				if(count == 0
						&& !IsConfirmed)
					this.addLog(msg);
				
				if(!IsConfirmed)
				{
					MInvoice invoice = new MInvoice(getCtx(), C_Invoice_ID, get_TrxName());
					
					addBufferLog(invoice.get_ID(), null
							, null, "@C_Invoice_ID@: "+orgName+" "+invoice.getDocumentNo()
							, invoice.get_Table_ID(), invoice.get_ID());
				}
				
				count++;
			}
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, sql.toString(), e);
			throw new AdempiereException(e.getLocalizedMessage());
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		
		if(count > 0)
		{
			if(!IsConfirmed)
			{
				msg = "@CheckValid@";
				return false;
			}
			else
				msg = "";
		}
		else
			msg = "";
		
		return true;
	}//	ValidateInvoiceHaveWithHoldingVoucher

}//	ValidateBook