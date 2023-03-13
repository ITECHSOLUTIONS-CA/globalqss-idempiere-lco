package dev.itechsolutions.model;

import java.sql.ResultSet;
import java.util.Properties;

public class MMunicipality extends X_C_Municipality {
	
	private static final long serialVersionUID = -947266622352105510L;
	
	public MMunicipality(Properties ctx, int C_Municipality_ID, String trxName) {
		super(ctx, C_Municipality_ID, trxName);
	}
	
	public MMunicipality(Properties ctx, int C_Municipality_ID, String trxName, String... virtualColumns) {
		super(ctx, C_Municipality_ID, trxName, virtualColumns);
	}
	
	public MMunicipality(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
}
