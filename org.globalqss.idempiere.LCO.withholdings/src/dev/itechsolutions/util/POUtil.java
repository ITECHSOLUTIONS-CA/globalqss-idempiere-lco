package dev.itechsolutions.util;

import java.util.function.Supplier;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.PO;

/**
 * 
 * @author Argenis Rodríguez
 *
 */
public class POUtil {
	
	/**
	 * 
	 * @author Argenis Rodríguez
	 * @param sql
	 * @param columns
	 * @return
	 */
	public static String addColumnsToSql(String sql, String ...columns) {
		int index = sql.lastIndexOf(" FROM");
		
		if (index == -1)
			index = sql.lastIndexOf(" from");
		
		if (index == -1)
			throw new AdempiereException("SQL Can't not be without from");
		
		StringBuilder sqlBefore = new StringBuilder(sql.substring(0, index));
		
		for (String column: columns)
			sqlBefore.append(", ").append(column);
		
		return sqlBefore.append(sql.substring(index)).toString();
	}
	
	/**
	 * 
	 * @author Argenis Rodríguez
	 * @param <T>
	 * @param po
	 * @param attName
	 * @param supplier
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getAttribute(PO po, String attName, Supplier<? extends T> supplier) {
		T retVal = (T) po.get_Attribute(attName);
		
		if (retVal == null)
		{
			retVal = supplier.get();
			po.set_Attribute(attName, retVal);
		}
		
		return retVal;
	}
	
	/**
	 * 
	 * @author Argenis Rodríguez
	 * @param <T>
	 * @param po
	 * @param attName
	 * @param defaultVal
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getAttribute(PO po, String attName, T defaultVal) {
		T retVal = (T) po.get_Attribute(attName);
		
		if (retVal == null)
		{
			retVal = defaultVal;
			po.set_Attribute(attName, retVal);
		}
		
		return retVal;
	}
}
