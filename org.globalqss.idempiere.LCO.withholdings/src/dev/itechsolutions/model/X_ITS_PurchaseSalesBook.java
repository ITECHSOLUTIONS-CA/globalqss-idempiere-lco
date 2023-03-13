/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2012 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
/** Generated Model - DO NOT CHANGE */
package dev.itechsolutions.model;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for ITS_PurchaseSalesBook
 *  @author iDempiere (generated) 
 *  @version Release 10 - $Id$ */
@org.adempiere.base.Model(table="ITS_PurchaseSalesBook")
public class X_ITS_PurchaseSalesBook extends PO implements I_ITS_PurchaseSalesBook, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20230306L;

    /** Standard Constructor */
    public X_ITS_PurchaseSalesBook (Properties ctx, int ITS_PurchaseSalesBook_ID, String trxName)
    {
      super (ctx, ITS_PurchaseSalesBook_ID, trxName);
      /** if (ITS_PurchaseSalesBook_ID == 0)
        {
			setDateFrom (new Timestamp( System.currentTimeMillis() ));
			setDateTo (new Timestamp( System.currentTimeMillis() ));
        } */
    }

    /** Standard Constructor */
    public X_ITS_PurchaseSalesBook (Properties ctx, int ITS_PurchaseSalesBook_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ITS_PurchaseSalesBook_ID, trxName, virtualColumns);
      /** if (ITS_PurchaseSalesBook_ID == 0)
        {
			setDateFrom (new Timestamp( System.currentTimeMillis() ));
			setDateTo (new Timestamp( System.currentTimeMillis() ));
        } */
    }

    /** Load Constructor */
    public X_ITS_PurchaseSalesBook (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 3 - Client - Org 
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuilder sb = new StringBuilder ("X_ITS_PurchaseSalesBook[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Trx Organization.
		@param AD_OrgTrx_ID Performing or initiating organization
	*/
	public void setAD_OrgTrx_ID (int AD_OrgTrx_ID)
	{
		if (AD_OrgTrx_ID < 1)
			set_Value (COLUMNNAME_AD_OrgTrx_ID, null);
		else
			set_Value (COLUMNNAME_AD_OrgTrx_ID, Integer.valueOf(AD_OrgTrx_ID));
	}

	/** Get Trx Organization.
		@return Performing or initiating organization
	  */
	public int getAD_OrgTrx_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_OrgTrx_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Date From.
		@param DateFrom Starting date for a range
	*/
	public void setDateFrom (Timestamp DateFrom)
	{
		set_Value (COLUMNNAME_DateFrom, DateFrom);
	}

	/** Get Date From.
		@return Starting date for a range
	  */
	public Timestamp getDateFrom()
	{
		return (Timestamp)get_Value(COLUMNNAME_DateFrom);
	}

	/** Set Date To.
		@param DateTo End date of a date range
	*/
	public void setDateTo (Timestamp DateTo)
	{
		set_Value (COLUMNNAME_DateTo, DateTo);
	}

	/** Get Date To.
		@return End date of a date range
	  */
	public Timestamp getDateTo()
	{
		return (Timestamp)get_Value(COLUMNNAME_DateTo);
	}

	/** Set Description.
		@param Description Optional short description of the record
	*/
	public void setDescription (String Description)
	{
		set_Value (COLUMNNAME_Description, Description);
	}

	/** Get Description.
		@return Optional short description of the record
	  */
	public String getDescription()
	{
		return (String)get_Value(COLUMNNAME_Description);
	}

	/** Set Sales Transaction.
		@param IsSOTrx This is a Sales Transaction
	*/
	public void setIsSOTrx (boolean IsSOTrx)
	{
		set_ValueNoCheck (COLUMNNAME_IsSOTrx, Boolean.valueOf(IsSOTrx));
	}

	/** Get Sales Transaction.
		@return This is a Sales Transaction
	  */
	public boolean isSOTrx()
	{
		Object oo = get_Value(COLUMNNAME_IsSOTrx);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Valid.
		@param IsValid Element is valid
	*/
	public void setIsValid (boolean IsValid)
	{
		set_Value (COLUMNNAME_IsValid, Boolean.valueOf(IsValid));
	}

	/** Get Valid.
		@return Element is valid
	  */
	public boolean isValid()
	{
		Object oo = get_Value(COLUMNNAME_IsValid);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Purchase/Sales Book.
		@param ITS_PurchaseSalesBook_ID Purchase/Sales Book
	*/
	public void setITS_PurchaseSalesBook_ID (int ITS_PurchaseSalesBook_ID)
	{
		if (ITS_PurchaseSalesBook_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ITS_PurchaseSalesBook_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ITS_PurchaseSalesBook_ID, Integer.valueOf(ITS_PurchaseSalesBook_ID));
	}

	/** Get Purchase/Sales Book.
		@return Purchase/Sales Book	  */
	public int getITS_PurchaseSalesBook_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ITS_PurchaseSalesBook_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ITS_PurchaseSalesBook_UU.
		@param ITS_PurchaseSalesBook_UU ITS_PurchaseSalesBook_UU
	*/
	public void setITS_PurchaseSalesBook_UU (String ITS_PurchaseSalesBook_UU)
	{
		set_ValueNoCheck (COLUMNNAME_ITS_PurchaseSalesBook_UU, ITS_PurchaseSalesBook_UU);
	}

	/** Get ITS_PurchaseSalesBook_UU.
		@return ITS_PurchaseSalesBook_UU	  */
	public String getITS_PurchaseSalesBook_UU()
	{
		return (String)get_Value(COLUMNNAME_ITS_PurchaseSalesBook_UU);
	}

	/** Purchase = C */
	public static final String ITS_TYPEOPERATION_Purchase = "C";
	/** Sales = V */
	public static final String ITS_TYPEOPERATION_Sales = "V";
	/** Set Type Operation.
		@param ITS_TypeOperation Type Operation
	*/
	public void setITS_TypeOperation (String ITS_TypeOperation)
	{

		set_Value (COLUMNNAME_ITS_TypeOperation, ITS_TypeOperation);
	}

	/** Get Type Operation.
		@return Type Operation	  */
	public String getITS_TypeOperation()
	{
		return (String)get_Value(COLUMNNAME_ITS_TypeOperation);
	}

	/** Set Jasper Process Now.
		@param JasperProcessing Jasper Process Now
	*/
	public void setJasperProcessing (String JasperProcessing)
	{
		set_Value (COLUMNNAME_JasperProcessing, JasperProcessing);
	}

	/** Get Jasper Process Now.
		@return Jasper Process Now	  */
	public String getJasperProcessing()
	{
		return (String)get_Value(COLUMNNAME_JasperProcessing);
	}

	/** Set Online Processing.
		@param OProcessing This payment can be processed online
	*/
	public void setOProcessing (String OProcessing)
	{
		set_Value (COLUMNNAME_OProcessing, OProcessing);
	}

	/** Get Online Processing.
		@return This payment can be processed online
	  */
	public String getOProcessing()
	{
		return (String)get_Value(COLUMNNAME_OProcessing);
	}

	/** Set Parent Organization.
		@param Parent_Org_ID Parent (superior) Organization 
	*/
	public void setParent_Org_ID (int Parent_Org_ID)
	{
		if (Parent_Org_ID < 1)
			set_ValueNoCheck (COLUMNNAME_Parent_Org_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_Parent_Org_ID, Integer.valueOf(Parent_Org_ID));
	}

	/** Get Parent Organization.
		@return Parent (superior) Organization 
	  */
	public int getParent_Org_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Parent_Org_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Process Now.
		@param Processing Process Now
	*/
	public void setProcessing (boolean Processing)
	{
		set_Value (COLUMNNAME_Processing, Boolean.valueOf(Processing));
	}

	/** Get Process Now.
		@return Process Now	  */
	public boolean isProcessing()
	{
		Object oo = get_Value(COLUMNNAME_Processing);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}
}