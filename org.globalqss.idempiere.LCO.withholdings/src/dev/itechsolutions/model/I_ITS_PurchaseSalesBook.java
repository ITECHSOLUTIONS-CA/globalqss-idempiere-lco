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
package dev.itechsolutions.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.compiere.model.*;
import org.compiere.util.KeyNamePair;

/** Generated Interface for ITS_PurchaseSalesBook
 *  @author iDempiere (generated) 
 *  @version Release 10
 */
@SuppressWarnings("all")
public interface I_ITS_PurchaseSalesBook 
{

    /** TableName=ITS_PurchaseSalesBook */
    public static final String Table_Name = "ITS_PurchaseSalesBook";

    /** AD_Table_ID=1000020 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 3 - Client - Org 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(3);

    /** Load Meta Data */

    /** Column name AD_Client_ID */
    public static final String COLUMNNAME_AD_Client_ID = "AD_Client_ID";

	/** Get Tenant.
	  * Tenant for this installation.
	  */
	public int getAD_Client_ID();

    /** Column name AD_Org_ID */
    public static final String COLUMNNAME_AD_Org_ID = "AD_Org_ID";

	/** Set Organization.
	  * Organizational entity within tenant
	  */
	public void setAD_Org_ID (int AD_Org_ID);

	/** Get Organization.
	  * Organizational entity within tenant
	  */
	public int getAD_Org_ID();

    /** Column name AD_OrgTrx_ID */
    public static final String COLUMNNAME_AD_OrgTrx_ID = "AD_OrgTrx_ID";

	/** Set Trx Organization.
	  * Performing or initiating organization
	  */
	public void setAD_OrgTrx_ID (int AD_OrgTrx_ID);

	/** Get Trx Organization.
	  * Performing or initiating organization
	  */
	public int getAD_OrgTrx_ID();

    /** Column name Created */
    public static final String COLUMNNAME_Created = "Created";

	/** Get Created.
	  * Date this record was created
	  */
	public Timestamp getCreated();

    /** Column name CreatedBy */
    public static final String COLUMNNAME_CreatedBy = "CreatedBy";

	/** Get Created By.
	  * User who created this records
	  */
	public int getCreatedBy();

    /** Column name DateFrom */
    public static final String COLUMNNAME_DateFrom = "DateFrom";

	/** Set Date From.
	  * Starting date for a range
	  */
	public void setDateFrom (Timestamp DateFrom);

	/** Get Date From.
	  * Starting date for a range
	  */
	public Timestamp getDateFrom();

    /** Column name DateTo */
    public static final String COLUMNNAME_DateTo = "DateTo";

	/** Set Date To.
	  * End date of a date range
	  */
	public void setDateTo (Timestamp DateTo);

	/** Get Date To.
	  * End date of a date range
	  */
	public Timestamp getDateTo();

    /** Column name Description */
    public static final String COLUMNNAME_Description = "Description";

	/** Set Description.
	  * Optional short description of the record
	  */
	public void setDescription (String Description);

	/** Get Description.
	  * Optional short description of the record
	  */
	public String getDescription();

    /** Column name IsActive */
    public static final String COLUMNNAME_IsActive = "IsActive";

	/** Set Active.
	  * The record is active in the system
	  */
	public void setIsActive (boolean IsActive);

	/** Get Active.
	  * The record is active in the system
	  */
	public boolean isActive();

    /** Column name IsSOTrx */
    public static final String COLUMNNAME_IsSOTrx = "IsSOTrx";

	/** Set Sales Transaction.
	  * This is a Sales Transaction
	  */
	public void setIsSOTrx (boolean IsSOTrx);

	/** Get Sales Transaction.
	  * This is a Sales Transaction
	  */
	public boolean isSOTrx();

    /** Column name IsValid */
    public static final String COLUMNNAME_IsValid = "IsValid";

	/** Set Valid.
	  * Element is valid
	  */
	public void setIsValid (boolean IsValid);

	/** Get Valid.
	  * Element is valid
	  */
	public boolean isValid();

    /** Column name ITS_PurchaseSalesBook_ID */
    public static final String COLUMNNAME_ITS_PurchaseSalesBook_ID = "ITS_PurchaseSalesBook_ID";

	/** Set Purchase/Sales Book	  */
	public void setITS_PurchaseSalesBook_ID (int ITS_PurchaseSalesBook_ID);

	/** Get Purchase/Sales Book	  */
	public int getITS_PurchaseSalesBook_ID();

    /** Column name ITS_PurchaseSalesBook_UU */
    public static final String COLUMNNAME_ITS_PurchaseSalesBook_UU = "ITS_PurchaseSalesBook_UU";

	/** Set ITS_PurchaseSalesBook_UU	  */
	public void setITS_PurchaseSalesBook_UU (String ITS_PurchaseSalesBook_UU);

	/** Get ITS_PurchaseSalesBook_UU	  */
	public String getITS_PurchaseSalesBook_UU();

    /** Column name ITS_TypeOperation */
    public static final String COLUMNNAME_ITS_TypeOperation = "ITS_TypeOperation";

	/** Set Type Operation	  */
	public void setITS_TypeOperation (String ITS_TypeOperation);

	/** Get Type Operation	  */
	public String getITS_TypeOperation();

    /** Column name JasperProcessing */
    public static final String COLUMNNAME_JasperProcessing = "JasperProcessing";

	/** Set Jasper Process Now	  */
	public void setJasperProcessing (String JasperProcessing);

	/** Get Jasper Process Now	  */
	public String getJasperProcessing();

    /** Column name OProcessing */
    public static final String COLUMNNAME_OProcessing = "OProcessing";

	/** Set Online Processing.
	  * This payment can be processed online
	  */
	public void setOProcessing (String OProcessing);

	/** Get Online Processing.
	  * This payment can be processed online
	  */
	public String getOProcessing();

    /** Column name Parent_Org_ID */
    public static final String COLUMNNAME_Parent_Org_ID = "Parent_Org_ID";

	/** Set Parent Organization.
	  * Parent (superior) Organization 
	  */
	public void setParent_Org_ID (int Parent_Org_ID);

	/** Get Parent Organization.
	  * Parent (superior) Organization 
	  */
	public int getParent_Org_ID();

    /** Column name Processing */
    public static final String COLUMNNAME_Processing = "Processing";

	/** Set Process Now	  */
	public void setProcessing (boolean Processing);

	/** Get Process Now	  */
	public boolean isProcessing();

    /** Column name Updated */
    public static final String COLUMNNAME_Updated = "Updated";

	/** Get Updated.
	  * Date this record was updated
	  */
	public Timestamp getUpdated();

    /** Column name UpdatedBy */
    public static final String COLUMNNAME_UpdatedBy = "UpdatedBy";

	/** Get Updated By.
	  * User who updated this records
	  */
	public int getUpdatedBy();
}
