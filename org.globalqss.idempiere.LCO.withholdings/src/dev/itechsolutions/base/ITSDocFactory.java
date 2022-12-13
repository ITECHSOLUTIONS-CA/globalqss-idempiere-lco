package dev.itechsolutions.base;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.base.IDocFactory;
import org.compiere.acct.Doc;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MTable;
import org.compiere.model.POInfo;
import org.compiere.util.CLogger;
import org.compiere.util.DB;

/**
 * 
 * @author Argenis Rodríguez
 *
 */
public abstract class ITSDocFactory implements IDocFactory {
	
	private CLogger logger = CLogger.getCLogger(getClass());
	
	public ITSDocFactory() {
		initialize();
	}
	
	@Override
	public Doc getDocument(MAcctSchema as, int AD_Table_ID, int Record_ID, String trxName) {
		
		String tableName = MTable.getTableName(as.getCtx(), AD_Table_ID);
		POInfo poInfo = POInfo.getPOInfo(as.getCtx(), AD_Table_ID);
		
		StringBuilder sql = poInfo.buildSelect(true, false)
				.append(" WHERE ").append(tableName).append(".").append(tableName).append("_ID = ?")
				.append(" AND ").append(tableName).append(".Processed = 'Y'");
		
		Doc doc = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			pstmt = DB.prepareStatement(sql.toString(), trxName);
			DB.setParameter(pstmt, 1, Record_ID);
			rs = pstmt.executeQuery();
			
			if (rs.next())
				doc = getDocument(as, AD_Table_ID, rs, trxName);
			else
				logger.log(Level.WARNING, "Document not found for tablename " + tableName);
		} catch (SQLException e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		} finally {
			DB.close(rs, pstmt);
		}
		
		return doc;
	}
	
	@Override
	public Doc getDocument(MAcctSchema as, int AD_Table_ID, ResultSet rs, String trxName) {
		
		Doc doc = null;
		String tableName = MTable.getTableName(as.getCtx(), AD_Table_ID);
		
		if (!documents.containsKey(tableName))
			return null;
		
		Class<? extends Doc> clazz = documents.get(tableName);
		
		try {
			Constructor<? extends Doc> constructor = clazz.getDeclaredConstructor(MAcctSchema.class, ResultSet.class, String.class);
			doc = constructor.newInstance(as, rs, trxName);
		} catch (NoSuchMethodException e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		} catch (SecurityException e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		} catch (InstantiationException e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		} catch (IllegalAccessException e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		} catch (IllegalArgumentException e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		} catch (InvocationTargetException e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
		
		return doc;
	}
	
	/**
	 * 
	 * @author Argenis Rodríguez
	 * @param tableName
	 * @param clazz
	 */
	public void addDocument(String tableName, Class<? extends Doc> clazz) {
		documents.put(tableName, clazz);
	}
	
	public abstract void initialize();
	
	private Map<String, Class<? extends Doc>> documents = new HashMap<String, Class<? extends Doc>>(); 
}
