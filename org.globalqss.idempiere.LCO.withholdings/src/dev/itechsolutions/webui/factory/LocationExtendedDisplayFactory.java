package dev.itechsolutions.webui.factory;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import org.adempiere.base.IDisplayTypeFactory;
import org.compiere.util.Language;

import dev.itechsolutions.util.ColumnUtils;

public class LocationExtendedDisplayFactory implements IDisplayTypeFactory {
	
	public static final int LOCATION_EXTENDED_ID = ColumnUtils.getLocationExtendedReferenceId();
	
	@Override
	public boolean isID(int displayType) {
		if (LOCATION_EXTENDED_ID == displayType)
			return true;
		
		return false;
	}
	
	@Override
	public boolean isNumeric(int displayType) {
		return false;
	}
	
	@Override
	public Integer getDefaultPrecision(int displayType) {
		return null;
	}
	
	@Override
	public boolean isText(int displayType) {
		return false;
	}
	
	@Override
	public boolean isDate(int displayType) {
		return false;
	}
	
	@Override
	public boolean isLookup(int displayType) {
		return false;
	}
	
	@Override
	public boolean isLOB(int displayType) {
		return false;
	}
	
	@Override
	public DecimalFormat getNumberFormat(int displayType, Language language, String pattern) {
		return null;
	}
	
	@Override
	public SimpleDateFormat getDateFormat(int displayType, Language language, String pattern) {
		return null;
	}
	
	@Override
	public Class<?> getClass(int displayType, boolean yesNoAsBoolean) {
		return null;
	}
	
	@Override
	public String getSQLDataType(int displayType, String columnName, int fieldLength) {
		return null;
	}
	
	@Override
	public String getDescription(int displayType) {
		if (LOCATION_EXTENDED_ID == displayType)
			return "Location";
		
		return null;
	}
}
