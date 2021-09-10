package dev.itechsolutions.process;

import org.compiere.process.SvrProcess;

import dev.itechsolutions.model.ITSMInvoice;
import dev.itechsolutions.model.MITSVoucherWithholding;

/**
 * 
 * @author Argenis Rodríguez
 *
 */
public class GenerateInvoiceWithholding extends SvrProcess {
	
	@Override
	protected void prepare() {
		
	}
	
	@Override
	protected String doIt() throws Exception {
		
		MITSVoucherWithholding voucher = new MITSVoucherWithholding(getCtx(), getRecord_ID(), get_TrxName());
		int nInsert = 0;
		ITSMInvoice[] invoices = ITSMInvoice.getOfVoucherWithholding(voucher);
		
		for (ITSMInvoice invoice: invoices)
			nInsert += invoice.recalcWithholdings(voucher);
		
		return "@inserted@ " + nInsert;
	}
}
