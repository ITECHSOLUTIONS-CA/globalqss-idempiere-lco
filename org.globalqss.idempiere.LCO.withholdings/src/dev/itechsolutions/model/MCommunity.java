package dev.itechsolutions.model;

import java.sql.ResultSet;
import java.util.Properties;

public class MCommunity extends X_C_Community {
	
	private static final long serialVersionUID = 7210211986548686915L;
	
	public MCommunity(Properties ctx, int C_Community_ID, String trxName) {
		super(ctx, C_Community_ID, trxName);
	}
	
	public MCommunity(Properties ctx, int C_Community_ID, String trxName, String... virtualColumns) {
		super(ctx, C_Community_ID, trxName, virtualColumns);
	}
	
	public MCommunity(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
}
