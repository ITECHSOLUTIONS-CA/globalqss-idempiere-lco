package dev.itechsolutions.util;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

/**
 * 
 * @author Argenis Rodríguez
 *
 */
public class TimestampUtil {
	
	/**
	 * @author Argenis Rodríguez
	 * @return
	 */
	public static Timestamp now() {
		return Timestamp.valueOf(LocalDateTime.now());
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @param date
	 * @param unit
	 * @return
	 */
	public static Timestamp trunc(Timestamp date, TemporalUnit unit) {
		LocalDateTime dateTime = date != null ? date.toLocalDateTime()
				: LocalDateTime.now();
		
		return Timestamp.valueOf(dateTime.truncatedTo(unit));
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @param date
	 * @return
	 */
	public static Timestamp getDay(Timestamp date) {
		return trunc(date, ChronoUnit.DAYS);
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @return
	 */
	public static Timestamp getDay() {
		return getDay(null);
	}
}
