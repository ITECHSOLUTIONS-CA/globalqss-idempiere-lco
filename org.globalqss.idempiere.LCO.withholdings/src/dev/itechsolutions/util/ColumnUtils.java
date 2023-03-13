package dev.itechsolutions.util;

public class ColumnUtils {
	
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
	//----------------------End of Column Names------------------
	
	//----------------------List Column Values-------------------
	//Values for Generate Withholding Columns
	public static final String GENERATEWITHHOLDING_Yes = "Y";
	public static final String GENERATEWITHHOLDING_No = "N";
	public static final String GENERATEWITHHOLDING_Automatic = "A";
	//-------------------End of List Column Values---------------
}
