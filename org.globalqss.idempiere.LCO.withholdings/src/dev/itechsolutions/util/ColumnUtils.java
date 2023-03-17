package dev.itechsolutions.util;

import org.compiere.util.CCache;
import org.compiere.util.DB;

/**
 * 
 * @author Argenis Rodríguez
 *
 */
public class ColumnUtils {
	
	public static int getLocationExtendedReferenceId() {
		return getReferenceIdFromUUID(REFERENCE_LOCATION_EXTENDED);
	}
	
	public static int getReferenceIdFromUUID(String uuid) {
		if (s_referencesCache.containsKey(uuid))
			return s_referencesCache.get(uuid);
		
		int AD_Reference_ID = DB.getSQLValue(null
				, "SELECT AD_Reference_ID FROM AD_Reference WHERE AD_Reference_UU = ?"
				, uuid);
		
		if (AD_Reference_ID > 0)
			s_referencesCache.put(uuid, AD_Reference_ID);
		
		return AD_Reference_ID;
	}
	
	//-------------------------Cache----------------------------
	/**
	 * Reference Cache expired after 2 hours
	 */
	private static final CCache<String, Integer> s_referencesCache = new CCache<String, Integer>("References", 25, 120);
	//----------------------End of Cache------------------------
	
	//-----------------------Sys Config-------------------------
	public static final String SYSCONFIG_LVE_GenerateInvoiceWithholdingIsPaid = "LVE_GenerateInvoiceWithholdingIsPaid";
	public static final String SYSCONFIG_LCO_USE_WITHHOLDINGS = "LCO_USE_WITHHOLDINGS";
	//---------------------End Sys Config-----------------------
	
	//------------------------Attributes-------------------------
	public static final String ATTRIBUTE_LCO_InvoiceWithholding_ID = "LCO_InvoiceWithholding_ID";
	public static final String ATTRIBUTE_LCO_InvoiceWithholdings = "LCO_InvoiceWithholdings";
	//---------------------End of Attributes---------------------
	
	//-------------------------Column Names----------------------
	public static final String COLUMNNAME_GenerateWithholding = "GenerateWithholding";
	public static final String COLUMNNAME_LCO_ISIC_ID = "LCO_ISIC_ID";
	public static final String COLUMNNAME_LCO_TaxPayerType_ID = "LCO_TaxPayerType_ID";
	public static final String COLUMNNAME_LCO_BP_ISIC_ID = "LCO_BP_ISIC_ID";
	public static final String COLUMNNAME_LCO_Org_ISIC_ID = "LCO_Org_ISIC_ID";
	public static final String COLUMNNAME_LCO_BP_TaxPayerType_ID = "LCO_BP_TaxPayerType_ID";
	public static final String COLUMNNAME_LCO_Org_TaxPayerType_ID = "LCO_Org_TaxPayerType_ID";
	public static final String COLUMNNAME_LCO_BP_City_ID = "LCO_BP_City_ID";
	public static final String COLUMNNAME_LCO_Org_City_ID = "LCO_Org_City_ID";
	public static final String COLUMNNAME_LCO_WithholdingType_ID = "LCO_WithholdingType_ID";
	public static final String COLUMNNAME_HasMunicipality = "HasMunicipality";
	public static final String COLUMNNAME_HasParish = "HasParish";
	public static final String COLUMNNAME_ValidMunicipality = "ValidMunicipality";
	//----------------------End of Column Names------------------
	
	//----------------------List Column Values-------------------
	//Values for Generate Withholding Columns
	public static final String GENERATEWITHHOLDING_Yes = "Y";
	public static final String GENERATEWITHHOLDING_No = "N";
	public static final String GENERATEWITHHOLDING_Automatic = "A";
	
	//Value for Validate Municipality
	public static final String VALIDATEMUNICIPALITY_None = "N";
	public static final String VALIDATEMUNICIPALITY_Both = "B";
	public static final String VALIDATEMUNICIPALITY_OutSiders = "O";
	public static final String VALIDATEMUNICIPALITY_Local = "L";
	//-------------------End of List Column Values---------------
	
	//-------------------------References------------------------
	public static final String REFERENCE_LOCATION_EXTENDED = "91f20a6b-f1ac-410d-ad78-4dd03036f1f7";
	//----------------------End of References--------------------
}
