package dev.itechsolutions.model;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MOrg;
import org.compiere.util.DB;

/**
 * 
 * @author José Castañeda
 *
 */
public class MITSPurchaseSalesBook extends X_ITS_PurchaseSalesBook {
	
	private static final long serialVersionUID = -4928163056723080752L;
	
	public MITSPurchaseSalesBook(Properties ctx, int ITS_PurchaseSalesBook_ID, String trxName) 
	{
		super(ctx, ITS_PurchaseSalesBook_ID, trxName);
	}
	
	public MITSPurchaseSalesBook(Properties ctx, int ITS_PurchaseSalesBook_ID, String trxName,
			String... virtualColumns) 
	{
		super(ctx, ITS_PurchaseSalesBook_ID, trxName, virtualColumns);
	}
	
	public MITSPurchaseSalesBook(Properties ctx, ResultSet rs, String trxName) 
	{
		super(ctx, rs, trxName);
	}
	
	@Override
	public boolean beforeSave(boolean newRecord) 
	{
		if(getDateFrom().after(getDateTo()))
			throw new AdempiereException("@DateFrom@>@DateTo@");
		
		int bookSameDate = lookForBooksWithSameDate(this, false);
		if(bookSameDate == 1)
			throw new AdempiereException("@AlreadyExists@ @From@ @Date@:"+this.getDateFrom());
		
		else if(bookSameDate == 2)
			throw new AdempiereException("@AlreadyExists@ @To@ @Date@:"+this.getDateTo());
		
		if(getITS_TypeOperation().equalsIgnoreCase("C"))
			setIsSOTrx(false);
		
		if(getITS_TypeOperation().equalsIgnoreCase("V"))
			setIsSOTrx(true);
		
		MOrg org = new MOrg (getCtx(),getAD_Org_ID(),get_TrxName());
		
		if(org.isSummary())
			this.setParent_Org_ID(getAD_Org_ID());
		else
			this.setAD_OrgTrx_ID(getAD_Org_ID());
		
		return true;
	}
	
	public int lookForBooksWithSameDate(MITSPurchaseSalesBook book, boolean valid) 
	{
		return lookForBooksWithDate(book.getAD_Org_ID(),book.getITS_TypeOperation()
				,book.getDateFrom(),book.getDateTo()
				,valid, book.get_ID(),book.get_TrxName());
	}
	
	public static int lookForBooksWithDate(int AD_Org_ID, String typeOperation
			,Timestamp dateFrom, Timestamp dateTo
			, boolean valid, int ITS_PurchaseSalesBook_ID
			, String trxName) 
	{
		if(!typeOperation.equalsIgnoreCase("C")
				&& !typeOperation.equalsIgnoreCase("V"))
			throw new AdempiereException("Invalid TypeOperation");
		
		List<Object> params = new ArrayList<Object>();
		
		params.add(dateFrom); 		// 1 - DateFrom
		params.add(dateTo); 		// 2 - DateTo
		params.add(typeOperation);	// 3 - TypeOperation
		params.add(dateFrom);		// 4 - DateFrom
		params.add(dateTo);			// 5 - DateTo
		params.add(AD_Org_ID);		// 6 - AD_Org_ID
		params.add(AD_Org_ID);		// 7 - AD_Org_ID

		StringBuilder sql = new StringBuilder("SELECT ")
				.append("CASE ")
					.append("WHEN DATE_TRUNC('Day',?::DATE)::TimeStamp BETWEEN bk.DateFrom AND bk.DateTo ")
					.append("THEN 1 ")
					.append("WHEN DATE_TRUNC('Day',?::DATE)::TimeStamp BETWEEN bk.DateFrom AND bk.DateTo ")
					.append("THEN 2 ")
					.append("ELSE 0 ")
				.append("END AS Value ")
				.append("FROM ITS_PurchaseSalesBook bk ")
				.append("JOIN AD_OrgInfo oi ON oi.AD_Org_ID = bk.AD_Org_ID ")
				.append("WHERE bk.IsActive = 'Y'")
				.append(" AND bk.ITS_TypeOperation = ?")
				.append(" AND (DATE_TRUNC('Day',?::DATE)::TimeStamp BETWEEN bk.DateFrom AND bk.DateTo ")
					.append("OR DATE_TRUNC('Day',?::DATE)::TimeStamp BETWEEN bk.DateFrom AND bk.DateTo)")
				.append(" AND (bk.AD_Org_ID = ? ")
					.append("OR ? IN (SELECT DISTINCT Node_ID FROM getnodes(0,(SELECT AD_Tree_ID ")
					.append("FROM AD_Tree tree ")
					.append("WHERE TreeType = 'OO'")
					.append(" AND tree.AD_Client_ID = bk.AD_Client_ID)")
					.append(", bk.AD_Client_ID) AS N(Parent_ID NUMERIC, Node_ID NUMERIC) ")
				.append("WHERE Parent_ID = bk.AD_Org_ID) OR (bk.AD_Org_ID = 0))");
		
		if(ITS_PurchaseSalesBook_ID>0)
		{
			sql.append(" AND bk.ITS_PurchaseSalesBook_ID<>?");
			params.add(ITS_PurchaseSalesBook_ID);
		}
		
		if(valid)
			sql.append(" AND bk.isvalid = 'Y' ");
		
		int value = DB.getSQLValueEx(trxName, sql.toString(), params);
		
		return value;
	}
	
	@Override
	public boolean beforeDelete() 
	{
		if(get_ValueAsBoolean("IsValid"))
			throw new AdempiereException("@IsValidBook@");
		
		return true;
	}
	
	@Override
	public String toString() {
		return getITS_PurchaseSalesBook_ID()+"";
	}
}//	ITSMPurchaseSalesBook