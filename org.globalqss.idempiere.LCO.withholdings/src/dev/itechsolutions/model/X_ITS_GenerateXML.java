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

/** Generated Model for ITS_GenerateXML
 *  @author iDempiere (generated) 
 *  @version Release 10 - $Id$ */
@org.adempiere.base.Model(table="ITS_GenerateXML")
public class X_ITS_GenerateXML extends PO implements I_ITS_GenerateXML, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20230329L;

    /** Standard Constructor */
    public X_ITS_GenerateXML (Properties ctx, int ITS_GenerateXML_ID, String trxName)
    {
      super (ctx, ITS_GenerateXML_ID, trxName);
      /** if (ITS_GenerateXML_ID == 0)
        {
			setC_Currency_ID (0);
			setValidFrom (new Timestamp( System.currentTimeMillis() ));
			setValidTo (new Timestamp( System.currentTimeMillis() ));
        } */
    }

    /** Standard Constructor */
    public X_ITS_GenerateXML (Properties ctx, int ITS_GenerateXML_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ITS_GenerateXML_ID, trxName, virtualColumns);
      /** if (ITS_GenerateXML_ID == 0)
        {
			setC_Currency_ID (0);
			setValidFrom (new Timestamp( System.currentTimeMillis() ));
			setValidTo (new Timestamp( System.currentTimeMillis() ));
        } */
    }

    /** Load Constructor */
    public X_ITS_GenerateXML (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_ITS_GenerateXML[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_C_Currency getC_Currency() throws RuntimeException
	{
		return (org.compiere.model.I_C_Currency)MTable.get(getCtx(), org.compiere.model.I_C_Currency.Table_ID)
			.getPO(getC_Currency_ID(), get_TrxName());
	}

	/** Set Currency.
		@param C_Currency_ID The Currency for this record
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
	public int getC_Currency_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Currency_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Generate XML.
		@param ITS_GenerateXML Generate XML
	*/
	public void setITS_GenerateXML (String ITS_GenerateXML)
	{
		set_Value (COLUMNNAME_ITS_GenerateXML, ITS_GenerateXML);
	}

	/** Get Generate XML.
		@return Generate XML	  */
	public String getITS_GenerateXML()
	{
		return (String)get_Value(COLUMNNAME_ITS_GenerateXML);
	}

	/** Set Generate XML Seniat.
		@param ITS_GenerateXML_ID Generate XML Seniat
	*/
	public void setITS_GenerateXML_ID (int ITS_GenerateXML_ID)
	{
		if (ITS_GenerateXML_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ITS_GenerateXML_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ITS_GenerateXML_ID, Integer.valueOf(ITS_GenerateXML_ID));
	}

	/** Get Generate XML Seniat.
		@return Generate XML Seniat	  */
	public int getITS_GenerateXML_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ITS_GenerateXML_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ITS_GenerateXML_UU.
		@param ITS_GenerateXML_UU ITS_GenerateXML_UU
	*/
	public void setITS_GenerateXML_UU (String ITS_GenerateXML_UU)
	{
		set_ValueNoCheck (COLUMNNAME_ITS_GenerateXML_UU, ITS_GenerateXML_UU);
	}

	/** Get ITS_GenerateXML_UU.
		@return ITS_GenerateXML_UU	  */
	public String getITS_GenerateXML_UU()
	{
		return (String)get_Value(COLUMNNAME_ITS_GenerateXML_UU);
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

	/** Set Total Amount.
		@param TotalAmt Total Amount
	*/
	public void setTotalAmt (BigDecimal TotalAmt)
	{
		set_Value (COLUMNNAME_TotalAmt, TotalAmt);
	}

	/** Get Total Amount.
		@return Total Amount
	  */
	public BigDecimal getTotalAmt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_TotalAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Valid from.
		@param ValidFrom Valid from including this date (first day)
	*/
	public void setValidFrom (Timestamp ValidFrom)
	{
		set_ValueNoCheck (COLUMNNAME_ValidFrom, ValidFrom);
	}

	/** Get Valid from.
		@return Valid from including this date (first day)
	  */
	public Timestamp getValidFrom()
	{
		return (Timestamp)get_Value(COLUMNNAME_ValidFrom);
	}

	/** Set Valid to.
		@param ValidTo Valid to including this date (last day)
	*/
	public void setValidTo (Timestamp ValidTo)
	{
		set_ValueNoCheck (COLUMNNAME_ValidTo, ValidTo);
	}

	/** Get Valid to.
		@return Valid to including this date (last day)
	  */
	public Timestamp getValidTo()
	{
		return (Timestamp)get_Value(COLUMNNAME_ValidTo);
	}
}