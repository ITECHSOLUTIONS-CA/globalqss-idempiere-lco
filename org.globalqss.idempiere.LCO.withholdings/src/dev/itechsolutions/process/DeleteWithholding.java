package dev.itechsolutions.process;

import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

@Process(name = "DeleteWithholding")
public class DeleteWithholding extends SvrProcess {
	
	@Override
	protected void prepare() {
	}
	
	@Override
	protected String doIt() throws Exception {
		
		if (getProcessInfo().getAD_InfoWindow_ID() <= 0)
			throw new AdempiereException("@NotSupported@");
		
		StringBuilder sql = new StringBuilder("DELETE FROM LCO_InvoiceWithholding")
				.append(" WHERE Processed = 'N' AND EXISTS(")
					.append(" SELECT 1 FROM T_Selection ts")
					.append(" WHERE ts.T_Selection_ID = LCO_InvoiceWithholding.LCO_InvoiceWithholding_ID")
					.append(" AND ts.AD_PInstance_ID = ?")
				.append(");").append(Env.NL);
		
		sql.append("UPDATE LCO_InvoiceWithholding SET ITS_VoucherWithholding_ID = NULL")
			.append(" WHERE Processed = 'Y' AND EXISTS(")
				.append(" SELECT 1 FROM T_Selection ts")
				.append(" WHERE ts.T_Selection_ID = LCO_InvoiceWithholding.LCO_InvoiceWithholding_ID")
				.append(" AND ts.AD_PInstance_ID = ?")
			.append(")");
		
		int no = DB.executeUpdate(sql.toString()
				, new Object[] {getAD_PInstance_ID(), getAD_PInstance_ID()}
		, true, get_TrxName(), 0);
		
		return "@deleted@ " + no;
	}

}
