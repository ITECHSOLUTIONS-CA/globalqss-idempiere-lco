package dev.itechsolutions.util;

import java.math.BigDecimal;
import java.sql.Timestamp;

import org.compiere.process.ProcessInfo;

/**
 * 
 * @author Argenis Rodríguez
 *
 */
public class ProcessInfoUtil {
	
	public static void addLog (ProcessInfo m_pi, int id, Timestamp date, BigDecimal number, String msg, int tableId ,int recordId)
	{
		if (m_pi != null)
			m_pi.addLog(id, date, number, msg,tableId,recordId);
	}	//	addLog
}
