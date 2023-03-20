package dev.itechsolutions.process;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import org.adempiere.base.annotation.Parameter;
import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.FillMandatoryException;
import org.compiere.model.MAllocationHdr;
import org.compiere.model.MAllocationLine;
import org.compiere.model.MBPartner;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoicePaySchedule;
import org.compiere.model.MInvoiceTax;
import org.compiere.process.DocumentEngine;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.globalqss.model.MLCOInvoiceWithholding;

import dev.itechsolutions.model.ITSMInvoice;
import dev.itechsolutions.util.ColumnUtils;

@Process(name = "RecalculateInvoiceWithholding")
public class RecalculateInvoiceWithholding extends SvrProcess {
	
	@Parameter
	private int p_LCO_InvoiceWithholding_ID;
	
	@Override
	protected void prepare() {
		if (p_LCO_InvoiceWithholding_ID <= 0)
			p_LCO_InvoiceWithholding_ID = getRecord_ID();
	}
	
	@Override
	protected String doIt() throws Exception {
		
		if (p_LCO_InvoiceWithholding_ID <= 0)
			throw new FillMandatoryException("LCO_InvoiceWithholding_ID");
		
		MLCOInvoiceWithholding invWh = MLCOInvoiceWithholding.getCopy(getCtx(), p_LCO_InvoiceWithholding_ID, get_TrxName());
		
		if (!invWh.isProcessed())
			throw new AdempiereException("@InvoiceWithholdingUnprocessed@");
		
		if (invWh.getITS_VoucherWithholding_ID() > 0)
			throw new AdempiereException("@InvoiceWithholdingInVoucher@");
		
		MInvoice invoice = invWh.getC_Invoice();
		
		if (MInvoice.STATUS_Reversed.equals(invoice.getDocStatus())
				|| MInvoice.STATUS_Voided.equals(invoice.getDocStatus()))
			throw new AdempiereException("@C_Invoice_ID@ @Voided@");
		
		invWh = ITSMInvoice.recalculateInvoiceWithholding(invWh, getProcessInfo());
		
		BigDecimal difference = invWh.getTaxAmtDifference();
		
		if (BigDecimal.ZERO.compareTo(difference) != 0)
		{
			reprocessInvoiceWithholding(invWh);
			
			if (BigDecimal.ZERO.compareTo(invWh.getTaxAmt()) == 0)
			{
				invWh.deleteEx(true);
				return "@LCO_InvoiceWithholding_ID@ @Deleted@ @TaxAmt@ = 0";
			}
		}
		
		return "@Difference@ " + difference;
	}
	
	private void reprocessInvoiceWithholding(MLCOInvoiceWithholding invWh) {
		if (invWh.isCalcOnInvoice())
			recalculateForInvoiceTax(invWh);
		else if (invWh.isCalcOnAllocation())
			recalculateForAllocation(invWh);
	}
	
	private void recalculateForAllocation(MLCOInvoiceWithholding invWh) {
		MAllocationLine aLine = invWh.getC_AllocationLine();
		MInvoice invoice = invWh.getC_Invoice();
		BigDecimal taxAmtDifference = invWh.getTaxAmtDifference();
		int C_AllocationHdr_ID = aLine.getC_AllocationHdr_ID();
		MBPartner bpartner = (MBPartner) aLine.getC_BPartner();
		BigDecimal overUnderAmt = invoice.getOpenAmt(true, null, true);
		
		if (!invoice.isSOTrx())
		{
			taxAmtDifference = taxAmtDifference.negate();
			overUnderAmt = overUnderAmt.negate();
		}
		
		if (invoice.isCreditMemo())
		{
			taxAmtDifference = taxAmtDifference.negate();
			overUnderAmt = overUnderAmt.negate();
		}
		
		aLine.setWriteOffAmt(aLine.getWriteOffAmt().add(taxAmtDifference));
		aLine.setOverUnderAmt(overUnderAmt.subtract(taxAmtDifference));
		
		if (BigDecimal.ZERO.compareTo(aLine.getWriteOffAmt()) == 0)
		{
			aLine.deleteEx(true);
			invoice.load(invoice.get_TrxName());
		}
		else
		{
			aLine.saveEx();
			
			if (invoice.testAllocation())
				invoice.saveEx();
		}
		
		if (BigDecimal.ZERO.compareTo(invWh.getTaxAmt()) != 0)
		{
			invWh.setProcessed(true);
			invWh.saveEx();
		}
		
		bpartner.setTotalOpenBalance();
		bpartner.saveEx();
		
		MAllocationHdr allocation = new MAllocationHdr(getCtx(), C_AllocationHdr_ID, get_TrxName());
		
		if (allocation.getLines(false).length > 0)
		{
			BigDecimal approvalAmt = Arrays.stream(allocation.getLines(false))
					.map(line -> line.getWriteOffAmt().add(line.getDiscountAmt()))
					.reduce(BigDecimal.ZERO, (acum, amt) -> acum.add(amt));
			allocation.setApprovalAmt(approvalAmt);
			allocation.saveEx();
			
			String error = DocumentEngine.postImmediate(getCtx(), getAD_Client_ID()
					, MAllocationHdr.Table_ID, C_AllocationHdr_ID
					, true, get_TrxName());
			
			if (!Util.isEmpty(error, true))
				throw new AdempiereException(error);
		}
		else
			allocation.deleteEx(true);
	}
	
	private void recalculateForInvoiceTax(MLCOInvoiceWithholding invWh) {
		MInvoiceTax it = invWh.getOrCreateInvoiceTax(false);
		BigDecimal taxAmtDifference = invWh.getTaxAmtDifference();
		BigDecimal baseTaxAmtDifference = invWh.getTaxBaseAmtDifference();
		
		it.setTaxBaseAmt(it.getTaxBaseAmt().add(baseTaxAmtDifference));
		it.setTaxAmt(it.getTaxAmt().add(taxAmtDifference.negate()));
		
		if (BigDecimal.ZERO.compareTo(it.getTaxAmt()) != 0)
			it.saveEx();
		else
			it.deleteEx(true);
		
		if (BigDecimal.ZERO.compareTo(invWh.getTaxAmt()) != 0)
		{
			invWh.setProcessed(true);
			invWh.saveEx();
		}
		
		MInvoice invoice = invWh.getC_Invoice();
		BigDecimal actualamt = Optional.ofNullable((BigDecimal) invoice.get_Value(ColumnUtils.COLUMNNAME_WithholdingAmt))
				.orElse(BigDecimal.ZERO);
		
		actualamt = actualamt.add(taxAmtDifference);
		invoice.setGrandTotal(invoice.getGrandTotal().subtract(taxAmtDifference));
		invoice.set_ValueOfColumn(ColumnUtils.COLUMNNAME_WithholdingAmt, actualamt);
		invoice.saveEx();
		
		BigDecimal toSubtract = taxAmtDifference;
		
		for (MInvoicePaySchedule ips : MInvoicePaySchedule.getInvoicePaySchedule(invoice.getCtx(), invoice.getC_Invoice_ID(), 0, invoice.get_TrxName())) {
			if (ips.getDueAmt().compareTo(toSubtract) >= 0) {
				ips.setDueAmt(ips.getDueAmt().subtract(toSubtract));
				toSubtract = Env.ZERO;
			} else {
				toSubtract = toSubtract.subtract(ips.getDueAmt());
				ips.setDueAmt(Env.ZERO);
			}
			if (!ips.save()) {
				throw new AdempiereException("Error saving Invoice Pay Schedule subtracting withholdings");
			}
			if (toSubtract.signum() <= 0)
				break;
		}
		
		String error = DocumentEngine.postImmediate(getCtx(), getAD_Client_ID()
				, MInvoice.Table_ID, invoice.get_ID()
				, true, get_TrxName());
		
		if (!Util.isEmpty(error, true))
			throw new AdempiereException(error);
	}
}
