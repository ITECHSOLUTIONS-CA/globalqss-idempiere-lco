/**********************************************************************
* This file is part of iDempiere ERP Open Source                      *
* http://www.idempiere.org                                            *
*                                                                     *
* Copyright (C) Contributors                                          *
*                                                                     *
* This program is free software; you can redistribute it and/or       *
* modify it under the terms of the GNU General Public License         *
* as published by the Free Software Foundation; either version 2      *
* of the License, or (at your option) any later version.              *
*                                                                     *
* This program is distributed in the hope that it will be useful,     *
* but WITHOUT ANY WARRANTY; without even the implied warranty of      *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the        *
* GNU General Public License for more details.                        *
*                                                                     *
* You should have received a copy of the GNU General Public License   *
* along with this program; if not, write to the Free Software         *
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,          *
* MA 02110-1301, USA.                                                 *
*                                                                     *
* Contributors:                                                       *
* - Carlos Ruiz - globalqss                                           *
**********************************************************************/

package org.globalqss.model;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAllocationHdr;
import org.compiere.model.MAllocationLine;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoicePaySchedule;
import org.compiere.model.MInvoiceTax;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.DocumentEngine;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.idempiere.cache.ImmutableIntPOCache;
import org.idempiere.cache.ImmutablePOSupport;

import dev.itechsolutions.model.MITSVoucherWithholding;

/**
 *	Invoice Withholding Model
 *
 *  @author Carlos Ruiz - globalqss
 */
public class MLCOInvoiceWithholding extends X_LCO_InvoiceWithholding implements ImmutablePOSupport
{
	/**
	 *
	 */
	private static final long serialVersionUID = -3086189821486687908L;
	/**	Static Logger	*/
	@SuppressWarnings("unused")
	private static CLogger	s_log	= CLogger.getCLogger (MLCOInvoiceWithholding.class);
	
	private static ImmutableIntPOCache<Integer, MLCOInvoiceWithholding> s_cache = new ImmutableIntPOCache<Integer, MLCOInvoiceWithholding>(Table_Name, 18, 0);
	
	private X_LCO_WithholdingRule rule = null;
	private X_LCO_WithholdingCalc calc = null;
	
	private MAllocationLine m_aLine = null;
	
	private MInvoice invoice = null;
	
	/**************************************************************************
	 * 	Default Constructor
	 *	@param ctx context
	 *	@param MLCOInvoiceWithholding_ID id
	 *	@param trxName transaction
	 */
	public MLCOInvoiceWithholding (Properties ctx, int MLCOInvoiceWithholding_ID, String trxName)
	{
		super(ctx, MLCOInvoiceWithholding_ID, trxName);
	}	//	MLCOInvoiceWithholding

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MLCOInvoiceWithholding(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MLCOInvoiceWithholding
	
	/**
	 * 
	 * @param ctx context
	 * @param copy invoice withholding to copy
	 * @param trxName
	 */
	public MLCOInvoiceWithholding(Properties ctx, MLCOInvoiceWithholding copy, String trxName) {
		this(ctx, 0, trxName);
		copyPO(copy);
	}
	
	public MLCOInvoiceWithholding(Properties ctx, MLCOInvoiceWithholding copy) {
		this(ctx, copy, null);
	}
	
	public MLCOInvoiceWithholding(MLCOInvoiceWithholding copy) {
		this(Env.getCtx(), copy);
	}
	
	public static MLCOInvoiceWithholding get(int LCO_InvoiceWithholding_ID, String trxName) {
		return get(Env.getCtx(), LCO_InvoiceWithholding_ID, trxName);
	}
	
	public static MLCOInvoiceWithholding get(Properties ctx, int LCO_InvoiceWithholding_ID, String trxName) {
		//New
		if (LCO_InvoiceWithholding_ID == 0)
			return new MLCOInvoiceWithholding(Env.getCtx(), LCO_InvoiceWithholding_ID, trxName);
		
		MLCOInvoiceWithholding iwh = s_cache.get(ctx, LCO_InvoiceWithholding_ID, e -> new MLCOInvoiceWithholding(ctx, e));
		if (iwh != null)
			return iwh;
		
		iwh = new MLCOInvoiceWithholding(ctx, LCO_InvoiceWithholding_ID, trxName);
		
		if (iwh.get_ID() == LCO_InvoiceWithholding_ID)
		{
			s_cache.put(LCO_InvoiceWithholding_ID, iwh, e -> new MLCOInvoiceWithholding(Env.getCtx(), e));
			return iwh;
		}
		return null;
	}
	
	public static MLCOInvoiceWithholding getCopy(Properties ctx, int LCO_InvoiceWithholding_ID, String trxName) {
		MLCOInvoiceWithholding iwh = get(LCO_InvoiceWithholding_ID, trxName);
		
		if (iwh != null && iwh.getLCO_InvoiceWithholding_ID() > 0)
			return new MLCOInvoiceWithholding(ctx, iwh, trxName);
		return iwh;
	}
	
	/**************************************************************************
	 * 	Before Save
	 *	@param newRecord
	 *	@return true if save
	 */
	protected boolean beforeSave (boolean newRecord)
	{
		log.fine("New=" + newRecord);
		MInvoice inv = getC_Invoice();
		if (inv.getReversal_ID() <= 0) {
			if (getLCO_WithholdingRule_ID() > 0) {
				// Fill isCalcOnPayment according to rule
				X_LCO_WithholdingRule rule = getLCO_WithholdingRule();
				X_LCO_WithholdingCalc wc = getCalc();
				
				if (wc == null)
				{
					log.saveError("Error", new AdempiereException("@LCO_WithholdingCalc_ID@ @NotFound@"
							+ " @LCO_WithholdingRule_ID@ " + rule.getName()));
					return false;
				}
				
				setIsCalcOnPayment(wc.isCalcOnPayment());
				setIsCalcOnInvoice(wc.isCalcOnInvoice());
				setIsCalcOnAllocation(wc.isCalcOnAllocation());
			} else {
				if (inv.isProcessed()) {
					setIsCalcOnPayment(true);
				}
			}
			
			// Fill DateTrx and DateAcct for isCalcOnInvoice according to the invoice
			if (getC_AllocationLine_ID() <= 0) {
				setDateAcct(inv.getDateAcct());
				setDateTrx(inv.getDateInvoiced());
			}
		}
		
		return true;
	}	//	beforeSave
	
	@Override
	public MInvoice getC_Invoice() throws RuntimeException {
		if (invoice != null && getC_Invoice_ID() != invoice.get_ID())
			invoice = null;
		
		if (invoice == null && getC_Invoice_ID() > 0)
			invoice = (MInvoice) super.getC_Invoice();
		
		return invoice;
	}
	
	public void setInvoice(MInvoice invoice) {
		setC_Invoice_ID(invoice.get_ID());
		this.invoice = invoice;
	}
	
	/**
	 * 	After Save
	 *	@param newRecord new
	 *	@param success success
	 *	@return saved
	 */
	protected boolean afterSave (boolean newRecord, boolean success)
	{
		if (!success)
			return success;
		
		return LCO_MInvoice.updateHeaderWithholding(getC_Invoice_ID(), get_TrxName());
	}	//	afterSave

	/**
	 * 	After Delete
	 *	@param success success
	 *	@return deleted
	 */
	protected boolean afterDelete (boolean success)
	{
		if (!success)
			return success;
		return LCO_MInvoice.updateHeaderWithholding(getC_Invoice_ID(), get_TrxName());
	}	//	afterDelete
	
	@Override
	public PO markImmutable() {
		if (is_Immutable())
			return this;
		
		makeImmutable();
		return this;
	}
	
	/**
	 * 
	 * @author Argenis Rodríguez
	 * @param ctx
	 * @param C_Invoice_ID
	 * @param trxName
	 * @return
	 */
	public static List<MLCOInvoiceWithholding> getFromInvoice(Properties ctx, int C_Invoice_ID, String trxName) {
		return new Query(ctx, Table_Name, "C_Invoice_ID = ?", trxName)
				.setParameters(C_Invoice_ID)
				.setOnlyActiveRecords(true)
				.list();
	}
	
	public void setWithholdingRule(X_LCO_WithholdingRule rule) {
		setLCO_WithholdingRule_ID(rule.get_ID());
		this.rule = rule;
	}
	
	public void setWithholdingCalc(X_LCO_WithholdingCalc calc) {
		this.calc = calc;
	}
	
	public X_LCO_WithholdingCalc getCalc() {
		X_LCO_WithholdingRule rule = getLCO_WithholdingRule();
		
		if (rule == null
				|| (calc != null && calc.get_ID() != rule.getLCO_WithholdingCalc_ID()))
			calc = null;
		
		if (calc == null && rule != null && rule.getLCO_WithholdingCalc_ID() > 0)
			calc = (X_LCO_WithholdingCalc) rule.getLCO_WithholdingCalc();
		
		return calc;
	}
	
	@Override
	public X_LCO_WithholdingRule getLCO_WithholdingRule() throws RuntimeException {
		if (rule != null && rule.get_ID() != getLCO_WithholdingRule_ID())
			rule = null;
		
		if (rule == null && getLCO_WithholdingRule_ID() > 0)
			rule = (X_LCO_WithholdingRule) super.getLCO_WithholdingRule();
		
		return rule;
	}
	
	public static int sortByTaxId(MLCOInvoiceWithholding wh1, MLCOInvoiceWithholding wh2) {
		return wh1.getC_Tax_ID() < wh2.getC_Tax_ID()
				? -1 : wh1.getC_Tax_ID() == wh2.getC_Tax_ID() ? 0 : 1;
	}
	
	public static int sortByInvoiceIdAndTaxId(MLCOInvoiceWithholding wh1, MLCOInvoiceWithholding wh2) {
		if (wh1.getC_Invoice_ID() < wh2.getC_Invoice_ID())
			return -1;
		else if (wh1.getC_Invoice_ID() > wh2.getC_Invoice_ID())
			return 1;
		return wh1.getC_Tax_ID() < wh2.getC_Tax_ID() ? -1
				: wh1.getC_Tax_ID() == wh2.getC_Tax_ID() ? 0 : 1;
	}
	
	public static int sortByCurrencyAndOrg(MLCOInvoiceWithholding wh1, MLCOInvoiceWithholding wh2) {
		if (wh1.getC_Invoice().getC_Currency_ID() < wh2.getC_Invoice().getC_Currency_ID())
			return -1;
		else if (wh1.getC_Invoice().getC_Currency_ID() > wh2.getC_Invoice().getC_Currency_ID())
			return 1;
		return wh1.getAD_Org_ID() < wh2.getAD_Org_ID() ? -1
				: wh1.getAD_Org_ID() == wh2.getAD_Org_ID() ? 0 : 1;
	}
	
	public static int sortByInvoice(MLCOInvoiceWithholding wh1, MLCOInvoiceWithholding wh2) {
		return wh1.getC_Invoice_ID() < wh2.getC_Invoice_ID() ? -1
				: (wh1.getC_Invoice_ID() == wh2.getC_Invoice_ID() ? 0: 1);
	}
	
	public static final String translateToInvoiceTax(MITSVoucherWithholding voucher
			, MLCOInvoiceWithholding[] lines, boolean resetWhAmt, boolean postIt) {
		
		if (lines.length == 0)
			return null;
		
		int lastC_Invoice_ID = -1;
		int lastC_Tax_ID = -1;
		MInvoiceTax it = null;
		BigDecimal sumIt = BigDecimal.ZERO;
		MInvoice invoice = null;
		String errorMsg = null;
		
		for (MLCOInvoiceWithholding line: lines)
		{
			if (it == null
					|| lastC_Invoice_ID != line.getC_Invoice_ID()
					|| lastC_Tax_ID != line.getC_Tax_ID())
			{
				if (it != null)
					it.saveEx();
				
				if (lastC_Invoice_ID != line.getC_Invoice_ID())
				{
					if (invoice != null)
					{
						errorMsg = processInvoice(invoice, sumIt, resetWhAmt, postIt);
						if (!Util.isEmpty(errorMsg, true))
							return errorMsg;
					}
					lastC_Invoice_ID = line.getC_Invoice_ID();
					invoice = line.getC_Invoice();
					sumIt = BigDecimal.ZERO;
				}
				
				lastC_Tax_ID = line.getC_Tax_ID();
				it = line.getOrCreateInvoiceTax(true);
			}
			
			it.setTaxBaseAmt(it.getTaxBaseAmt().add(line.getTaxBaseAmt()));
			it.setTaxAmt(it.getTaxAmt().add(line.getTaxAmt().negate()));
			sumIt = sumIt.add(line.getTaxAmt());
			
			if (voucher != null)
			{
				line.setDocumentNo(voucher.getDocumentNo());
				line.setDateAcct(voucher.getDateAcct());
			}
			line.setProcessed(true);
			line.saveEx();
		}
		
		//Process Last Invoice
		if (it != null)
		{
			it.saveEx();
			errorMsg = processInvoice(invoice, sumIt, resetWhAmt, postIt);
			
			if (!Util.isEmpty(errorMsg, true))
				return errorMsg;
		}
		
		return null;
	}
	
	public static String allocateLines(MLCOInvoiceWithholding[] lines, MITSVoucherWithholding voucher, MInvoice invoice) {
		if (lines.length == 0)
			return null;
		
		Properties ctx = lines[0].getCtx();
		String trxName = lines[0].get_TrxName();
		boolean isSOTrx = voucher == null ? invoice.isSOTrx() : voucher.isSOTrx();
		
		Timestamp dateAcct = voucher == null ? invoice.getDateAcct() : voucher.getDateAcct();
		
		MDocType[] docTypes = MDocType.getOfDocBaseType(ctx, MDocType.DOCBASETYPE_PaymentAllocation);
		MDocType docType = Arrays.stream(docTypes)
				.filter(dt -> dt.isSOTrx() == isSOTrx)
				.findFirst()
				.orElse(docTypes.length > 0 ? docTypes[0]: null);
		
		if (docType == null)
			return "@AllocationDocumentTypeNotFound@"
					+ " @IsSOTrx@ = " + (isSOTrx ? "@yes@" : "@no@");
		
		int lastC_Invoice_ID = -1;
		MAllocationHdr allocation = null;
		
		for (MLCOInvoiceWithholding line: lines)
		{
			MInvoice inv = line.getC_Invoice();
			
			if (allocation == null
					|| line.getC_Invoice_ID() != lastC_Invoice_ID)
			{
				lastC_Invoice_ID = line.getC_Invoice_ID();
				
				if (allocation != null && invoice != null)
					allocation.set_Attribute(DocumentEngine.DOCUMENT_POST_IMMEDIATE_AFTER_COMPLETE, Boolean.FALSE);
				
				if (allocation != null
						&& !allocation.processIt(MAllocationHdr.ACTION_Complete))
					return allocation.getProcessMsg();
				else if (allocation != null)
					allocation.saveEx();
				
				if (allocation != null && invoice != null)
					invoice.getDocsPostProcess().add(allocation);
				
				allocation = createAllocation(ctx, docType.get_ID()
						, line.getC_Invoice().getC_Currency_ID()
						, line.getAD_Org_ID(), inv.getDateInvoiced()
						, dateAcct, trxName);
			}
			
			BigDecimal writeOffAmt = line.getTaxAmt();
			
			BigDecimal overUnder = inv.getOpenAmt (true, null, true)
					.subtract(writeOffAmt);
			
			if (!isSOTrx)
			{
				writeOffAmt = writeOffAmt.negate();
				overUnder = overUnder.negate();
			}
			
			if (inv.isCreditMemo())
			{
				writeOffAmt = writeOffAmt.negate();
				overUnder = overUnder.negate();
			}
			
			MAllocationLine aLine = new MAllocationLine(allocation, BigDecimal.ZERO
					, BigDecimal.ZERO, writeOffAmt, overUnder);
			
			aLine.setDocInfo(inv.getC_BPartner_ID(), 0, inv.get_ID());
			aLine.saveEx();
			
			if (voucher != null)
			{
				line.setDocumentNo(voucher.getDocumentNo());
				line.setDateAcct(voucher.getDateAcct());
			}
			
			line.setC_AllocationLine_ID(aLine.get_ID());
			line.setProcessed(true);
			line.saveEx();
		}
		
		if (invoice != null)
			allocation.set_Attribute(DocumentEngine.DOCUMENT_POST_IMMEDIATE_AFTER_COMPLETE, Boolean.FALSE);
		
		if (allocation != null
				&& !allocation.processIt(MAllocationHdr.ACTION_Complete))
			return allocation.getProcessMsg();
		else if (allocation != null)
			allocation.saveEx();
		
		if (invoice != null)
			invoice.getDocsPostProcess().add(allocation);
		
		if (invoice != null
				&& invoice.testAllocation(true))
			invoice.saveEx();
		
		return null;
	}
	
	private static String processInvoice(MInvoice inv, BigDecimal sumit
			, boolean resetWhAmt, boolean postIt) 
	{	
		inv.setGrandTotal(inv.getGrandTotal().subtract(sumit));
		inv.saveEx();
		
		if (sumit.signum() != 0)
		{
			// GrandTotal changed!  If there are payment schedule records they need to be recalculated
			// subtract withholdings from the first installment
			BigDecimal toSubtract = sumit;
			for (MInvoicePaySchedule ips : MInvoicePaySchedule.getInvoicePaySchedule(inv.getCtx(), inv.getC_Invoice_ID(), 0, inv.get_TrxName())) {
				if (ips.getDueAmt().compareTo(toSubtract) >= 0) {
					ips.setDueAmt(ips.getDueAmt().subtract(toSubtract));
					toSubtract = Env.ZERO;
				} else {
					toSubtract = toSubtract.subtract(ips.getDueAmt());
					ips.setDueAmt(Env.ZERO);
				}
				if (!ips.save()) {
					return "Error saving Invoice Pay Schedule subtracting withholdings";
				}
				if (toSubtract.signum() <= 0)
					break;
			}
		}
		
		String error = null;
		
		if (postIt)
			error = DocumentEngine.postImmediate(inv.getCtx(), inv.getAD_Client_ID()
					, MInvoice.Table_ID, inv.get_ID()
					, true, inv.get_TrxName());
		
		return error;
	}
	
	private static MAllocationHdr createAllocation(Properties ctx, int C_DocType_ID
			, int C_Currency_ID, int AD_Org_ID
			, Timestamp dateTrx, Timestamp dateAcct, String trxName) {
		MAllocationHdr allocation = new MAllocationHdr(ctx, 0, trxName);
		allocation.setAD_Org_ID(AD_Org_ID);
		allocation.setDateTrx(dateTrx);
		allocation.setDateAcct(dateAcct);
		allocation.setC_DocType_ID(C_DocType_ID);
		allocation.setC_Currency_ID(C_Currency_ID);
		
		allocation.saveEx();
		
		return allocation;
	}
	
	public MInvoiceTax getOrCreateInvoiceTax(boolean saveNew) {
		MInvoiceTax it = new Query(getCtx(), MInvoiceTax.Table_Name, "C_Invoice_ID = ? AND C_Tax_ID = ?", get_TrxName())
				.setParameters(getC_Invoice_ID(), getC_Tax_ID())
				.first();
		
		if (it == null)
		{
			it = new MInvoiceTax(getCtx(), 0, get_TrxName());
			it.setAD_Org_ID(getAD_Org_ID());
			it.setC_Invoice_ID(getC_Invoice_ID());
			it.setC_Tax_ID(getC_Tax_ID());
			it.setTaxBaseAmt(BigDecimal.ZERO);
			it.setTaxAmt(BigDecimal.ZERO);
			
			if (saveNew)
				it.saveEx();
		}
		
		return it;
	}
	
	public BigDecimal getOldTaxAmt() {
		return Optional.ofNullable((BigDecimal) get_ValueOld(COLUMNNAME_TaxAmt))
				.orElse(BigDecimal.ZERO);
	}
	
	public BigDecimal getTaxAmtDifference() {
		return getTaxAmt().subtract(getOldTaxAmt());
	}
	
	public BigDecimal getOldTaxBaseAmt() {
		return Optional.ofNullable((BigDecimal) get_ValueOld(COLUMNNAME_TaxBaseAmt))
				.orElse(BigDecimal.ZERO);
	}
	
	public BigDecimal getTaxBaseAmtDifference() {
		return getTaxBaseAmt().subtract(getOldTaxBaseAmt());
	}
	
	public void setAllocationLine(MAllocationLine aLine) {
		setC_AllocationLine_ID(aLine.get_ID());
		m_aLine = aLine;
	}
	
	@Override
	public MAllocationLine getC_AllocationLine() throws RuntimeException
	{
		if (m_aLine != null && m_aLine.get_ID() != getC_AllocationLine_ID())
			m_aLine = null;
		
		if (m_aLine == null && getC_AllocationLine_ID() > 0)
			m_aLine = (MAllocationLine) super.getC_AllocationLine();
		
		return m_aLine;
	}
}	//	MLCOInvoiceWithholding