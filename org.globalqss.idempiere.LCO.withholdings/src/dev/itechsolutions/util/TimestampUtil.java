package dev.itechsolutions.util;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * 
 * @author Argenis Rodríguez
 *
 */
public class TimestampUtil {
	
	/**
	 * 
	 * @author Argenis Rodríguez
	 * @param date
	 * @return
	 */
	public static Timestamp endOfDay(Timestamp date) {
		if (date == null)
			return null;
		
		LocalDateTime localDate = date.toLocalDateTime()
				.toLocalDate()
				.atTime(23, 59, 59);
		
		return Timestamp.valueOf(localDate);
	}
	
	/**
	 * 
	 * @author Argenis Rodríguez
	 * @param date
	 * @return
	 */
	public static Timestamp startOfDay(Timestamp date) {
		if (date == null)
			return null;
		
		LocalDateTime localDate = date.toLocalDateTime()
				.toLocalDate()
				.atStartOfDay();
		
		return Timestamp.valueOf(localDate);
	}
	
	/**
	 * 
	 * @author Argenis Rodríguez
	 * @return
	 */
	public static Timestamp now() {
		return Timestamp.valueOf(LocalDateTime.now());
	}
	
	public static Timestamp today() {
		return Timestamp.valueOf(LocalDateTime.now()
				.toLocalDate()
				.atStartOfDay());
	}
}
