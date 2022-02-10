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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;

/** Generated Model for ITS_generateTXT
 *  @author iDempiere (generated) 
 *  @version Release 8.2 - $Id$ */
public class X_ITS_generateTXT extends PO implements I_ITS_generateTXT, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20220208L;

    /** Standard Constructor */
    public X_ITS_generateTXT (Properties ctx, int ITS_generateTXT_ID, String trxName)
    {
      super (ctx, ITS_generateTXT_ID, trxName);
      /** if (ITS_generateTXT_ID == 0)
        {
			setITS_TypeOperation (null);
// C
			setValidFrom (new Timestamp( System.currentTimeMillis() ));
			setValidTo (new Timestamp( System.currentTimeMillis() ));
        } */
    }

    /** Load Constructor */
    public X_ITS_generateTXT (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 6 - System - Client 
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
      StringBuilder sb = new StringBuilder ("X_ITS_generateTXT[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_C_Currency getC_Currency() throws RuntimeException
    {
		return (org.compiere.model.I_C_Currency)MTable.get(getCtx(), org.compiere.model.I_C_Currency.Table_Name)
			.getPO(getC_Currency_ID(), get_TrxName());	}

	/** Set Currency.
		@param C_Currency_ID 
		The Currency for this record
	  */
	public void setC_Currency_ID (int C_Currency_ID)
	{
		if (C_Currency_ID < 1) 
			set_Value (COLUMNNAME_C_Currency_ID, null);
		else 
			set_Value (COLUMNNAME_C_Currency_ID, Integer.valueOf(C_Currency_ID));
	}

	/** Get Currency.
		@return The Currency for this record
	  */
	public int getC_Currency_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Currency_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Generate TXT Seniat.
		@param GenerateTxtSeniat Generate TXT Seniat	  */
	public void setGenerateTxtSeniat (String GenerateTxtSeniat)
	{
		set_Value (COLUMNNAME_GenerateTxtSeniat, GenerateTxtSeniat);
	}

	/** Get Generate TXT Seniat.
		@return Generate TXT Seniat	  */
	public String getGenerateTxtSeniat () 
	{
		return (String)get_Value(COLUMNNAME_GenerateTxtSeniat);
	}

	/** Set Generate TXT Seniat.
		@param ITS_generateTXT_ID Generate TXT Seniat	  */
	public void setITS_generateTXT_ID (int ITS_generateTXT_ID)
	{
		if (ITS_generateTXT_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_ITS_generateTXT_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_ITS_generateTXT_ID, Integer.valueOf(ITS_generateTXT_ID));
	}

	/** Get Generate TXT Seniat.
		@return Generate TXT Seniat	  */
	public int getITS_generateTXT_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ITS_generateTXT_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ITS_generateTXT_UU.
		@param ITS_generateTXT_UU ITS_generateTXT_UU	  */
	public void setITS_generateTXT_UU (String ITS_generateTXT_UU)
	{
		set_Value (COLUMNNAME_ITS_generateTXT_UU, ITS_generateTXT_UU);
	}

	/** Get ITS_generateTXT_UU.
		@return ITS_generateTXT_UU	  */
	public String getITS_generateTXT_UU () 
	{
		return (String)get_Value(COLUMNNAME_ITS_generateTXT_UU);
	}

	/** Purchase = C */
	public static final String ITS_TYPEOPERATION_Purchase = "C";
	/** Sales = V */
	public static final String ITS_TYPEOPERATION_Sales = "V";
	/** Set Type Operation.
		@param ITS_TypeOperation Type Operation	  */
	public void setITS_TypeOperation (String ITS_TypeOperation)
	{

		set_Value (COLUMNNAME_ITS_TypeOperation, ITS_TypeOperation);
	}

	/** Get Type Operation.
		@return Type Operation	  */
	public String getITS_TypeOperation () 
	{
		return (String)get_Value(COLUMNNAME_ITS_TypeOperation);
	}

	/** Set Total Amount.
		@param TotalAmt 
		Total Amount
	  */
	public void setTotalAmt (BigDecimal TotalAmt)
	{
		set_Value (COLUMNNAME_TotalAmt, TotalAmt);
	}

	/** Get Total Amount.
		@return Total Amount
	  */
	public BigDecimal getTotalAmt () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_TotalAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Valid from.
		@param ValidFrom 
		Valid from including this date (first day)
	  */
	public void setValidFrom (Timestamp ValidFrom)
	{
		set_ValueNoCheck (COLUMNNAME_ValidFrom, ValidFrom);
	}

	/** Get Valid from.
		@return Valid from including this date (first day)
	  */
	public Timestamp getValidFrom () 
	{
		return (Timestamp)get_Value(COLUMNNAME_ValidFrom);
	}

	/** Set Valid to.
		@param ValidTo 
		Valid to including this date (last day)
	  */
	public void setValidTo (Timestamp ValidTo)
	{
		set_ValueNoCheck (COLUMNNAME_ValidTo, ValidTo);
	}

	/** Get Valid to.
		@return Valid to including this date (last day)
	  */
	public Timestamp getValidTo () 
	{
		return (Timestamp)get_Value(COLUMNNAME_ValidTo);
	}

    /** Get Record ID/ColumnName
        @return ID/ColumnName pair
      */
    public KeyNamePair getKeyNamePair() 
    {
        return new KeyNamePair(get_ID(), String.valueOf(getValidTo()));
    }
}