package dev.itechsolutions.process;

import org.adempiere.base.annotation.Process;
import org.compiere.process.SvrProcess;

import dev.itechsolutions.model.ITSMInvoice;
import dev.itechsolutions.model.MITSVoucherWithholding;

/**
 * 
 * @author Argenis Rodríguez
 *
 */
@Process
public class GenerateInvoiceWithholding extends SvrProcess {
	
	@Override
	protected void prepare() {/****/}
	
	@Override
	protected String doIt() throws Exception
	{	
		MITSVoucherWithholding voucher = new MITSVoucherWithholding(getCtx(), getRecord_ID(), get_TrxName());
		int nInsert = 0;
		ITSMInvoice[] invoices = ITSMInvoice.getOfVoucherWithholding(voucher);
		
		for (ITSMInvoice invoice: invoices)
			nInsert += ITSMInvoice.recalcWithholdings(invoice, voucher, getProcessInfo());
		
		return "@inserted@ " + nInsert;
	}
}
