package dev.itechsolutions.util;

/**
 * 
 * @author Argenis Rodríguez
 *
 */
public class ColumnUtils {
	//-----------------------------Doc Base Type--------------------------------------
	public static final String DOCBASETYPE_VoucherWithholding = "VWH";
	//---------------------------End Doc Base Type------------------------------------
	
	//--------------------------------Sys Config-----------------------------------------
	public static final String SYSCONFIG_LVE_GenerateInvoiceWithholdingIsPaid = "LVE_GenerateInvoiceWithholdingIsPaid";
	public static final String SYSCONFIG_LCO_USE_WITHHOLDINGS = "LCO_USE_WITHHOLDINGS";
	//-------------------------------End Sys Config--------------------------------------
	
	//--------------------------------Column Names---------------------------------------
	public static final String COLUMNNAME_C_DocTypeAllocation_ID = "C_DocTypeAllocation_ID";
	public static final String COLUMNNAME_HasCommunity = "HasCommunity";
	public static final String COLUMNNAME_HasMunicipality = "HasMunicipality";
	public static final String COLUMNNAME_HasParish = "HasParish";
	public static final String COLUMNNAME_GenerateWithholding = "GenerateWithholding";
	public static final String COLUMNNAME_LCO_ISIC_ID = "LCO_ISIC_ID";
	public static final String COLUMNNAME_LCO_TaxPayerType_ID = "LCO_TaxPayerType_ID";
	public static final String COLUMNNAME_ValidMunicipality = "ValidMunicipality";
	//-----------------------------End of Column Names-----------------------------------
	
	//-----------------------------List Column Values------------------------------------
	//Values for GenerateWithholding Column
	public static final String GenerateWithholding_Yes = "Y";
	public static final String GenerateWithholding_No = "N";
	public static final String GenerateWithholding_Automatic = "A";
	//Values for ValidMunicipality Column
	public static final String ValidMunicipality_Both = "B";
	public static final String ValidMunicipality_Outsiders = "F";
	public static final String ValidMunicipality_Local = "L";
	public static final String ValidMunicipality_None = "N";
	//----------------------------End of Column Values-----------------------------------
}
