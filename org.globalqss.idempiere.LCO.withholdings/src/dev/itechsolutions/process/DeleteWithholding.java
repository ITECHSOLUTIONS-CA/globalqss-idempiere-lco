package dev.itechsolutions.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.globalqss.model.LCO_MInvoice;

@Process(name = "DeleteWithholding")
public class DeleteWithholding extends SvrProcess {
	
	List<Integer> invs = new ArrayList<Integer>();
	
	@Override
	protected void prepare() {/****/}
	
	@Override
	protected String doIt() throws Exception
	{	
		if (getProcessInfo().getAD_InfoWindow_ID() <= 0)
			throw new AdempiereException("@NotSupported@");
		
		getInvoices();
		
		StringBuilder sql = new StringBuilder("DELETE FROM LCO_InvoiceWithholding")
				.append(" WHERE Processed = 'N' AND EXISTS(")
					.append(" SELECT 1 FROM T_Selection ts")
					.append(" WHERE ts.T_Selection_ID = LCO_InvoiceWithholding.LCO_InvoiceWithholding_ID")
					.append(" AND ts.AD_PInstance_ID = ?")
				.append(");").append(Env.NL);
		
		sql.append("UPDATE LCO_InvoiceWithholding SET ITS_VoucherWithholding_ID = NULL")
			.append(" WHERE Processed = 'Y' AND EXISTS(")
				.append(" SELECT 1 FROM T_Selection ts")
				.append(" WHERE ts.T_Selection_ID = LCO_InvoiceWithholding.LCO_InvoiceWithholding_ID")
				.append(" AND ts.AD_PInstance_ID = ?")
			.append(")");
		
		int no = DB.executeUpdate(sql.toString()
				, new Object[] {getAD_PInstance_ID(), getAD_PInstance_ID()}
		, true, get_TrxName(), 0);

		if(!invs.isEmpty())
		{
			for(Integer inv : invs)
			{
				LCO_MInvoice.updateHeaderWithholding(inv, get_TrxName());
			}
		}
		
		return "@deleted@ " + no;
	}
	
	public void getInvoices()
	{
		StringBuilder sql = new StringBuilder("SELECT DISTINCT C_Invoice_ID ")
				.append("FROM LCO_InvoiceWithholding ")
				.append("WHERE EXISTS(")
				.append("SELECT 1 FROM T_Selection ts ")
				.append("WHERE ts.T_Selection_ID = LCO_InvoiceWithholding.LCO_InvoiceWithholding_ID")
				.append(" AND ts.AD_PInstance_ID = ? )");
		
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		
		try
		{
			int index = 1;
			
			pstmt = DB.prepareStatement(sql.toString(),get_TrxName());
			pstmt.setInt(index++, getAD_PInstance_ID());
			
			rs = pstmt.executeQuery();
			
			while(rs.next())
			{
				invs.add(rs.getInt("C_Invoice_ID"));
			}	
		}
		catch (SQLException e)
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