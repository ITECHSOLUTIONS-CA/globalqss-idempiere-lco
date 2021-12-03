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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.FactsEventData;
import org.adempiere.base.event.IEventManager;
import org.adempiere.base.event.IEventTopics;
import org.adempiere.base.event.LoginEventData;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.acct.Doc;
import org.compiere.acct.DocLine_Allocation;
import org.compiere.acct.DocTax;
import org.compiere.acct.Fact;
import org.compiere.acct.FactLine;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MAllocationHdr;
import org.compiere.model.MAllocationLine;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MInvoicePaySchedule;
import org.compiere.model.MInvoiceTax;
import org.compiere.model.MMovement;
import org.compiere.model.MPayment;
import org.compiere.model.MPaymentAllocate;
import org.compiere.model.MSequence;
import org.compiere.model.MSysConfig;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.osgi.service.event.Event;

import dev.itechsolutions.util.ColumnUtils;

/**
 *	Validator or Localization Colombia (Withholdings)
 *
 *  @author Carlos Ruiz - globalqss - Quality Systems & Solutions - http://globalqss.com
 *  @author Argenis Rodríguez - iTechSolutions
 */
public class LCO_ValidatorWH extends AbstractEventHandler
{
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(LCO_ValidatorWH.class);
	
	/**
	 *	Initialize Validation
	 */
	@Override
	protected void initialize() {
		log.warning("");
		
		//	Tables to be monitored
		registerTableEvent(IEventTopics.PO_BEFORE_CHANGE, MInvoice.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_NEW, MInvoiceLine.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_CHANGE, MInvoiceLine.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_DELETE, MInvoiceLine.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_NEW, X_LCO_WithholdingCalc.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_CHANGE, X_LCO_WithholdingCalc.Table_Name);
		
		//	Documents to be monitored
		registerTableEvent(IEventTopics.DOC_BEFORE_PREPARE, MInvoice.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_COMPLETE, MInvoice.Table_Name);
		registerTableEvent(IEventTopics.DOC_AFTER_COMPLETE, MInvoice.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_REVERSEACCRUAL, MInvoice.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_REVERSECORRECT, MInvoice.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_VOID, MInvoice.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_REVERSEACCRUAL, MMovement.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_REVERSECORRECT, MMovement.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_VOID, MMovement.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_REVERSEACCRUAL, MInOut.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_REVERSECORRECT, MInOut.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_VOID, MInOut.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_COMPLETE, MPayment.Table_Name);
		registerTableEvent(IEventTopics.DOC_AFTER_COMPLETE, MAllocationHdr.Table_Name);
		registerTableEvent(IEventTopics.ACCT_FACTS_VALIDATE, MAllocationHdr.Table_Name);
		registerTableEvent(IEventTopics.DOC_AFTER_VOID, MAllocationHdr.Table_Name);
		registerTableEvent(IEventTopics.DOC_AFTER_REVERSECORRECT, MAllocationHdr.Table_Name);
		registerTableEvent(IEventTopics.DOC_AFTER_REVERSEACCRUAL, MAllocationHdr.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_COMPLETE, MMovement.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_COMPLETE, MInOut.Table_Name);
		
		//Added By Argenis Rodríguez
		registerTableEvent(IEventTopics.DOC_BEFORE_VOID, MAllocationHdr.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_REVERSEACCRUAL, MAllocationHdr.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_REVERSECORRECT, MAllocationHdr.Table_Name);
		
		registerEvent(IEventTopics.AFTER_LOGIN);
	}	//	initialize
	
    /**
     *	Model Change of a monitored Table or Document
     *  @param event
     *	@exception Exception if the recipient wishes the change to be not accept.
     */
	@Override
	protected void doHandleEvent(Event event) {
		String type = event.getTopic();
		
		if (type.equals(IEventTopics.AFTER_LOGIN)) {
			log.info("Type: " + type);
			// on login set context variable #LCO_USE_WITHHOLDINGS
			LoginEventData loginData = (LoginEventData) event.getProperty(IEventManager.EVENT_DATA);
			boolean useWH = MSysConfig.getBooleanValue("LCO_USE_WITHHOLDINGS", true, loginData.getAD_Client_ID());
			Env.setContext(Env.getCtx(), "#LCO_USE_WITHHOLDINGS", useWH);
			return;
		}
		
		if (! MSysConfig.getBooleanValue("LCO_USE_WITHHOLDINGS", true, Env.getAD_Client_ID(Env.getCtx())))
			return;
		
		PO po = null;
		if (type.equals(IEventTopics.ACCT_FACTS_VALIDATE)) {
			FactsEventData fed = getEventData(event);
			po = fed.getPo();
		} else {
			po = getPO(event);
		}
		log.info(po.get_TableName() + " Type: "+type);
		String msg;
		
		// Model Events
		if (po instanceof MInvoice && type.equals(IEventTopics.PO_BEFORE_CHANGE)) {
			msg = clearInvoiceWithholdingAmtFromInvoice((MInvoice) po);
			if (!Util.isEmpty(msg, true))
				throw new RuntimeException(msg);
		}
		
		// when invoiceline is changed clear the withholding amount on invoice
		// in order to force a regeneration
		if (po instanceof MInvoiceLine &&
				(type.equals(IEventTopics.PO_BEFORE_NEW) ||
				 type.equals(IEventTopics.PO_BEFORE_CHANGE) ||
				 type.equals(IEventTopics.PO_BEFORE_DELETE)
				)
			)
		{
			MInvoiceLine invLine = (MInvoiceLine) po;
			msg = clearInvoiceWithholdingAmtFromInvoiceLine(invLine, type);
			if (!Util.isEmpty(msg, true))
				throw new RuntimeException(msg);
			
			if (type.equals(IEventTopics.PO_BEFORE_NEW))
			{
				MInvoice inv = invLine.getParent();
				
				if (invLine.get_ValueAsInt(ColumnUtils.COLUMNNAME_ITS_InvoiceAffected_ID) <= 0
						&& inv.get_ValueAsInt(ColumnUtils.COLUMNNAME_ITS_InvoiceAffected_ID) > 0)
					invLine.set_ValueOfColumn(ColumnUtils.COLUMNNAME_ITS_InvoiceAffected_ID
							, inv.get_ValueAsInt(ColumnUtils.COLUMNNAME_ITS_InvoiceAffected_ID));
			}
		}
		
		if (po instanceof X_LCO_WithholdingCalc
				&& (type.equals(IEventTopics.PO_BEFORE_CHANGE) || type.equals(IEventTopics.PO_BEFORE_NEW))) {
			X_LCO_WithholdingCalc lwc = (X_LCO_WithholdingCalc) po;
			if (lwc.isCalcOnInvoice() && lwc.isCalcOnPayment())
				lwc.setIsCalcOnPayment(false);
		}
		
		// Document Events
		// before preparing a reversal invoice add the invoice withholding taxes
		if (po instanceof MInvoice
				&& type.equals(IEventTopics.DOC_BEFORE_PREPARE)) {
			MInvoice inv = (MInvoice) po;
			if (inv.isReversal()) {
				int invid = inv.getReversal_ID();
				
				if (invid > 0) {
					MInvoice invreverted = new MInvoice(inv.getCtx(), invid, inv.get_TrxName());
					String sql =
						  "SELECT LCO_InvoiceWithholding_ID "
						 + " FROM LCO_InvoiceWithholding "
						+ " WHERE C_Invoice_ID = ? "
						+ " ORDER BY LCO_InvoiceWithholding_ID";
					try (PreparedStatement pstmt = DB.prepareStatement(sql, inv.get_TrxName()))
					{
						pstmt.setInt(1, invreverted.getC_Invoice_ID());
						ResultSet rs = pstmt.executeQuery();
						while (rs.next()) {
							MLCOInvoiceWithholding iwh = new MLCOInvoiceWithholding(inv.getCtx(), rs.getInt(1), inv.get_TrxName());
							MLCOInvoiceWithholding newiwh = new MLCOInvoiceWithholding(inv.getCtx(), 0, inv.get_TrxName());
							newiwh.setAD_Org_ID(iwh.getAD_Org_ID());
							newiwh.setC_Invoice_ID(inv.getC_Invoice_ID());
							newiwh.setLCO_WithholdingType_ID(iwh.getLCO_WithholdingType_ID());
							newiwh.setPercent(iwh.getPercent());
							newiwh.setTaxAmt(iwh.getTaxAmt().negate());
							newiwh.setTaxBaseAmt(iwh.getTaxBaseAmt().negate());
							newiwh.setC_Tax_ID(iwh.getC_Tax_ID());
							newiwh.setIsCalcOnPayment(iwh.isCalcOnPayment());
							newiwh.setIsCalcOnInvoice(iwh.isCalcOnInvoice());
							newiwh.setIsActive(iwh.isActive());
							newiwh.setDateAcct(inv.getDateAcct());
							newiwh.setDateTrx(inv.getDateInvoiced());
							newiwh.setITS_VoucherWithholding_ID(iwh.getITS_VoucherWithholding_ID());
							if (!newiwh.save())
								throw new RuntimeException("Error saving LCO_InvoiceWithholding docValidate");
						}
					} catch (Exception e) {
						log.log(Level.SEVERE, sql, e);
						throw new RuntimeException("Error creating LCO_InvoiceWithholding for reversal invoice");
					}
				} else {
					throw new RuntimeException("Can't get the number of the invoice reversed");
				}
			}
		}
		
		// before preparing invoice validate if withholdings has been generated
		if (po instanceof MInvoice
				&& type.equals(IEventTopics.DOC_BEFORE_PREPARE)) {
			MInvoice inv = (MInvoice) po;
			if (inv.isReversal()) {
				// don't validate this for autogenerated reversal invoices
			} else {
				if (inv.get_Value("WithholdingAmt") == null) {
					MDocType dt = new MDocType(inv.getCtx(), inv.getC_DocTypeTarget_ID(), inv.get_TrxName());
					String genwh = dt.get_ValueAsString("GenerateWithholding");
					if (genwh != null) {
						
						/*if (genwh.equals("Y")) {
							// document type configured to compel generation of withholdings
							throw new RuntimeException(Msg.getMsg(inv.getCtx(), "LCO_WithholdingNotGenerated"));
						}*/
						
						if (genwh.equals("A")) {
							// document type configured to generate withholdings automatically
							LCO_MInvoice lcoinv = new LCO_MInvoice(inv.getCtx(), inv.getC_Invoice_ID(), inv.get_TrxName());
							lcoinv.recalcWithholdings();
						}
					}
				}
			}
		}
		
		// after preparing invoice move invoice withholdings to taxes and recalc grandtotal of invoice
		if (po instanceof MInvoice && type.equals(IEventTopics.DOC_BEFORE_COMPLETE)) {
			msg = translateWithholdingToTaxes((MInvoice) po);
			if (!Util.isEmpty(msg, true))
				throw new RuntimeException(msg);
			
			msg = validateInvoiceControl((MInvoice) po);
			
			if (!Util.isEmpty(msg, true))
				throw new AdempiereException(msg);
		}
		
		// after completing the invoice fix the dates on withholdings and mark the invoice withholdings as processed
		if (po instanceof MInvoice && type.equals(IEventTopics.DOC_AFTER_COMPLETE)) {
			msg = completeInvoiceWithholding((MInvoice) po);
			if (!Util.isEmpty(msg, true))
				throw new RuntimeException(msg);
			
			msg = automaticAllocation((MInvoice) po);
			
			if (!Util.isEmpty(msg, true))
				throw new AdempiereException(msg);
		}
		
		// before completing the payment - validate that writeoff amount must be greater than sum of payment withholdings
		if (po instanceof MPayment && type.equals(IEventTopics.DOC_BEFORE_COMPLETE)) {
			msg = validateWriteOffVsPaymentWithholdings((MPayment) po);
			if (!Util.isEmpty(msg, true))
				throw new RuntimeException(msg);
		}
		
		// after completing the allocation - complete the payment withholdings
		if (po instanceof MAllocationHdr && type.equals(IEventTopics.DOC_AFTER_COMPLETE)) {
			msg = completePaymentWithholdings((MAllocationHdr) po);
			if (!Util.isEmpty(msg, true))
				throw new RuntimeException(msg);
		}
		
		// before posting the allocation - post the payment withholdings vs writeoff amount
		if (po instanceof MAllocationHdr && type.equals(IEventTopics.ACCT_FACTS_VALIDATE)) {
			msg = accountingForInvoiceWithholdingOnPayment((MAllocationHdr) po, event);
			if (!Util.isEmpty(msg, true))
				throw new RuntimeException(msg);
		}
		
		// after completing the allocation - complete the payment withholdings
		if (po instanceof MAllocationHdr
				&& (type.equals(IEventTopics.DOC_AFTER_VOID) ||
					type.equals(IEventTopics.DOC_AFTER_REVERSECORRECT) ||
					type.equals(IEventTopics.DOC_AFTER_REVERSEACCRUAL))) {
			msg = reversePaymentWithholdings((MAllocationHdr) po);
			if (!Util.isEmpty(msg, true))
				throw new RuntimeException(msg);
		}
		
		//Added By Argenis Rodríguez
		if (MAllocationHdr.Table_Name.equals(po.get_TableName())
				&& (IEventTopics.DOC_BEFORE_VOID.equals(type)
					|| IEventTopics.DOC_BEFORE_REVERSEACCRUAL.equals(type)
					|| IEventTopics.DOC_BEFORE_REVERSECORRECT.equals(type)))
			validateAllocationBeforeVoid((MAllocationHdr) po);
		
		//Added By Argenis Rodríguez 30-11-2021
		if (MMovement.Table_Name.equals(po.get_TableName())
				&& IEventTopics.DOC_BEFORE_COMPLETE.equals(type))
		{
			msg = validateMovementControl((MMovement) po);
			
			if (!Util.isEmpty(msg, true))
				throw new AdempiereException(msg);
		}
		
		//Added By Argenis Rodríguez 30-11-2021
		if (MInOut.Table_Name.equals(po.get_TableName())
				&& IEventTopics.DOC_BEFORE_COMPLETE.equals(type))
		{
			msg = validateShipmentControl((MInOut) po);
			
			if (!Util.isEmpty(msg, true))
				throw new AdempiereException(msg);
		}
		
		//Added By Argenis Rodríguez 02-12-2021
		if (MInvoice.Table_Name.equals(po.get_TableName())
				&& (IEventTopics.DOC_BEFORE_REVERSEACCRUAL.equals(type)
						|| IEventTopics.DOC_BEFORE_REVERSECORRECT.equals(type)
						|| IEventTopics.DOC_BEFORE_VOID.equals(type)))
			overwriteDocNo((MInvoice) po);
		
		//Added By Argenis Rodríguez 02-12-2021
		if (MMovement.Table_Name.equals(po.get_TableName())
				&& (IEventTopics.DOC_BEFORE_REVERSEACCRUAL.equals(type)
						|| IEventTopics.DOC_BEFORE_REVERSECORRECT.equals(type)
						|| IEventTopics.DOC_BEFORE_VOID.equals(type)))
			overwriteDocNo((MMovement) po);
		
		//Added By Argenis Rodríguez 02-12-2021
		if (MInOut.Table_Name.equals(po.get_TableName())
				&& (IEventTopics.DOC_BEFORE_REVERSEACCRUAL.equals(type)
						|| IEventTopics.DOC_BEFORE_REVERSECORRECT.equals(type)
						|| IEventTopics.DOC_BEFORE_VOID.equals(type)))
			overwriteDocNo((MInOut) po);
	}	//	doHandleEvent
	
	/**
	 * 
	 * @author Argenis Rodríguez
	 * @param creditMemo
	 * @return
	 */
	private String automaticAllocation(MInvoice creditMemo) {
		
		MDocType dt = (MDocType) creditMemo.getC_DocType();
		
		if (!dt.get_ValueAsBoolean(ColumnUtils.COLUMNNAME_IsAutoAllocation)
				||creditMemo.isPaid()
				|| creditMemo.getReversal_ID() != 0
				|| !creditMemo.isCreditMemo())
			return null;
		
		int C_DocTypeAllocation_ID = dt.get_ValueAsInt(ColumnUtils.COLUMNNAME_C_DocTypeAllocation_ID);
		
		if (C_DocTypeAllocation_ID <= 0)
			return "@C_DocTypeAllocation_ID@ @NotFound@";
		
		BigDecimal appliedAmt = BigDecimal.ZERO;
		MInvoiceLine[] lines = getLines(creditMemo.getCtx(), creditMemo.get_ID()
				, "ITS_InvoiceAffected_ID IS NOT NULL", creditMemo.get_TrxName());
		MAllocationHdr alloc = null;
		BigDecimal creditMemoOpenAmt = creditMemo.getOpenAmt();
		
		for (MInvoiceLine line: lines)
		{
			int ITS_InvoiceAffected_ID = line.get_ValueAsInt(ColumnUtils.COLUMNNAME_ITS_InvoiceAffected_ID);
			
			MInvoice invoiceAffected = new MInvoice(creditMemo.getCtx()
					, ITS_InvoiceAffected_ID
					, creditMemo.get_TrxName());
			
			if (invoiceAffected.isCreditMemo()
					|| invoiceAffected.isPaid())
				continue;
			
			BigDecimal invAffectedOpenAmt = invoiceAffected.getOpenAmt();
			BigDecimal amtApply = line.getLineNetAmt();
			
			if (amtApply.compareTo(invAffectedOpenAmt) > 0)
				amtApply = invAffectedOpenAmt;
			
			appliedAmt = appliedAmt.add(amtApply);
			
			if (alloc == null)
				alloc = createAllocation(creditMemo.getCtx(), C_DocTypeAllocation_ID
						, creditMemo.getDateInvoiced(), creditMemo.getDateAcct()
						, creditMemo.getC_Currency_ID(), creditMemo.getAD_Org_ID()
						, creditMemo.get_TrxName());
			
			createAllocationLine(invoiceAffected, alloc
					, amtApply, invAffectedOpenAmt.subtract(amtApply));
		}
		
		createAllocationLine(creditMemo, alloc
				, appliedAmt, creditMemoOpenAmt.subtract(appliedAmt));
		
		String msg = completeAllocation(alloc);
		
		if (creditMemo.testAllocation(true))
			creditMemo.saveEx();
		
		return msg;
	}
	
	/**
	 * 
	 * @author Argenis Rodríguez
	 * @param alloc
	 * @return
	 */
	private static String completeAllocation(MAllocationHdr alloc) {
		if (alloc != null && !alloc.processIt(MAllocationHdr.ACTION_Complete))
			return alloc.getProcessMsg();
		else if (alloc != null)
			alloc.saveEx();
		
		return null;
	}
	
	/**
	 * 
	 * @author Argenis Rodríguez
	 * @param invoice
	 * @param alloc
	 * @param amtToApply
	 */
	private static void createAllocationLine(MInvoice invoice
			, MAllocationHdr alloc, BigDecimal amtToApply, BigDecimal openAmt) {
		
		if (alloc == null)
			return ;
		
		if (!invoice.isSOTrx())
		{
			amtToApply = amtToApply.negate();
			openAmt = openAmt.negate();
		}
		
		if (invoice.isCreditMemo())
		{
			amtToApply = amtToApply.negate();
			openAmt = openAmt.negate();
		}
		
		MAllocationLine aLine = new MAllocationLine(alloc, amtToApply
				, BigDecimal.ZERO, BigDecimal.ZERO
				, openAmt);
		
		aLine.setDocInfo(invoice.getC_BPartner_ID(), 0, invoice.get_ID());
		aLine.saveEx();
	}
	
	/**
	 * 
	 * @author Argenis Rodríguez
	 * @param ctx
	 * @param C_DocType_ID
	 * @param date
	 * @param C_Currency_ID
	 * @param AD_Org_ID
	 * @param trxName
	 * @return
	 */
	private static MAllocationHdr createAllocation(Properties ctx, int C_DocType_ID
			, Timestamp date, Timestamp dateAcct
			, int C_Currency_ID, int AD_Org_ID, String trxName) {
		
		MAllocationHdr alloc = new MAllocationHdr(ctx, false
				, date, C_Currency_ID
				, Env.getContext(ctx, "#AD_User_Name") + " " + Msg.translate(ctx, "IsAutoAllocation")
				, trxName);
		
		alloc.setDateAcct(dateAcct);
		alloc.setAD_Org_ID(AD_Org_ID);
		alloc.setC_DocType_ID(C_DocType_ID);
		alloc.saveEx();
		
		return alloc;
	}
	
	/**
	 * 
	 * @author Argenis Rodríguez
	 * @param ctx
	 * @param C_Invoice_ID
	 * @param where
	 * @param trxName
	 * @return
	 */
	private static MInvoiceLine[] getLines(Properties ctx, int C_Invoice_ID
			, String where, String trxName) {
		
		StringBuilder whereClause = new StringBuilder("C_Invoice_ID = ?");
		
		if (!Util.isEmpty(where, true))
			whereClause.append(" AND ").append(where);
		
		List<MInvoiceLine> lines = new Query(ctx, MInvoiceLine.Table_Name, whereClause.toString(), trxName)
				.setParameters(C_Invoice_ID)
				.setOnlyActiveRecords(true)
				.list();
		
		return lines.toArray(MInvoiceLine[]::new);
	}
	
	/**
	 * 
	 * @author Argenis Rodríguez
	 * @param inout
	 */
	private void overwriteDocNo(MInOut inout) {
		
		MDocType dt = (MDocType) inout.getC_DocType();
		
		if (!dt.get_ValueAsBoolean(ColumnUtils.COLUMNNAME_IsOverWriteDocNoWhenReversing))
			return ;
		
		int ITS_ReverseSequence_ID = dt.get_ValueAsInt(ColumnUtils.COLUMNNAME_ITS_ReverseSequence_ID);
		String newDocNo = null;
		
		if (ITS_ReverseSequence_ID > 0)
		{
			MSequence seq = new MSequence(inout.getCtx(), ITS_ReverseSequence_ID, inout.get_TrxName());
			newDocNo = MSequence.getDocumentNoFromSeq(seq, inout.get_TrxName(), inout);
		}
		
		if (!Util.isEmpty(newDocNo, true))
			inout.setDocumentNo(newDocNo);
		else
			inout.setDocumentNo(String.valueOf(inout.get_ID()));
		
		inout.saveEx();
	}
	
	/**
	 * 
	 * @author Argenis Rodríguez
	 * @param movement
	 */
	private void overwriteDocNo(MMovement movement) {
		
		MDocType dt = (MDocType) movement.getC_DocType();
		
		if (!dt.get_ValueAsBoolean(ColumnUtils.COLUMNNAME_IsOverWriteDocNoWhenReversing))
			return ;
		
		int ITS_ReverseSequence_ID = dt.get_ValueAsInt(ColumnUtils.COLUMNNAME_ITS_ReverseSequence_ID);
		String newDocNo = null;
		
		if (ITS_ReverseSequence_ID > 0)
		{
			MSequence seq = new MSequence(movement.getCtx(), ITS_ReverseSequence_ID, movement.get_TrxName());
			newDocNo = MSequence.getDocumentNoFromSeq(seq, movement.get_TrxName(), movement);
		}
		
		if (!Util.isEmpty(newDocNo, true))
			movement.setDocumentNo(newDocNo);
		else
			movement.setDocumentNo(String.valueOf(movement.get_ID()));
		
		movement.saveEx();
	}
	
	/**
	 * 
	 * @author Argenis Rodríguez
	 * @param inv
	 */
	private void overwriteDocNo(MInvoice inv) {
		
		MDocType dt = Optional.ofNullable((MDocType) inv.getC_DocType())
				.filter(dct -> dct.get_ID() > 0)
				.orElseGet(() -> (MDocType) inv.getC_DocTypeTarget());
		
		if (!dt.get_ValueAsBoolean(ColumnUtils.COLUMNNAME_IsOverWriteDocNoWhenReversing))
			return ;
		
		int ITS_ReverseSequence_ID = dt.get_ValueAsInt(ColumnUtils.COLUMNNAME_ITS_ReverseSequence_ID);
		String newDocNo = null;
		
		if (ITS_ReverseSequence_ID > 0)
		{
			MSequence seq = new MSequence(inv.getCtx(), ITS_ReverseSequence_ID, inv.get_TrxName());
			newDocNo = MSequence.getDocumentNoFromSeq(seq, inv.get_TrxName(), inv);
		}
		
		if (!Util.isEmpty(newDocNo, true))
			inv.setDocumentNo(newDocNo);
		else
			inv.setDocumentNo(String.valueOf(inv.get_ID()));
		
		inv.saveEx();
	}
	
	/**
	 * 
	 * @author Argenis Rodríguez
	 * @param shipment
	 * @return
	 */
	private String validateShipmentControl(MInOut shipment) {
		
		MDocType docType = (MDocType) shipment.getC_DocType();
		
		if (shipment.getReversal_ID() == 0
				&& docType.get_ValueAsBoolean(ColumnUtils.COLUMNNAME_IsControlNoDocument))
		{
			if (MDocType.DOCBASETYPE_MaterialDelivery.equals(docType.getDocBaseType())
				&& shipment.isSOTrx())
			{
				if (!Util.isEmpty(shipment.get_ValueAsString(ColumnUtils.COLUMNNAME_ITS_ControlNumber), true))
					return null;
				
				int ITS_ControlNoSequence_ID = docType.get_ValueAsInt(ColumnUtils.COLUMNNAME_ITS_ControlNoSequence_ID);
				
				if (ITS_ControlNoSequence_ID <= 0)
					return "@ITS_ControlNoSequence_ID@ @NotFound@ @C_DocType_ID@ " + docType.getNameTrl();
				
				MSequence seq = new MSequence(shipment.getCtx()
						, ITS_ControlNoSequence_ID
						, shipment.get_TrxName());
				
				String controlNumber = MSequence.getDocumentNoFromSeq(seq, shipment.get_TrxName(), shipment);
				
				shipment.set_ValueOfColumn(ColumnUtils.COLUMNNAME_ITS_ControlNumber, controlNumber);
				shipment.saveEx();
			}else if (MDocType.DOCBASETYPE_MaterialReceipt.equals(docType.getDocBaseType())
					&& !shipment.isSOTrx())
			{
				if (Util.isEmpty(shipment.get_ValueAsString(ColumnUtils.COLUMNNAME_ITS_ControlNumber), true))
					return "@FillMandatory@ @ITS_ControlNumber@";
			}
		}
		
		return null;
	}
	
	private String validateMovementControl(MMovement movement) {
		
		MDocType docType = (MDocType) movement.getC_DocType();
		
		if (movement.getReversal_ID() == 0
				&& docType.get_ValueAsBoolean(ColumnUtils.COLUMNNAME_IsControlNoDocument))
		{
			if (!Util.isEmpty(movement.get_ValueAsString(ColumnUtils.COLUMNNAME_ITS_ControlNumber), true))
				return null;
			
			int ITS_ControlNoSequence_ID = docType.get_ValueAsInt(ColumnUtils.COLUMNNAME_ITS_ControlNoSequence_ID);
			
			if (ITS_ControlNoSequence_ID <= 0)
				return "@ITS_ControlNoSequence_ID@ @NotFound@ @C_DocType@ " + docType.getNameTrl();
			
			MSequence seq = new MSequence(movement.getCtx()
					, ITS_ControlNoSequence_ID
					, movement.get_TrxName());
			
			String controlNumber = MSequence.getDocumentNoFromSeq(seq, movement.get_TrxName(), movement);
			
			movement.set_ValueOfColumn(ColumnUtils.COLUMNNAME_ITS_ControlNumber, controlNumber);
			movement.saveEx();
		}
		
		return null;
	}
	
	/**
	 * 
	 * @author Argenis Rodríguez
	 * @param inv
	 * @return
	 */
	private String validateInvoiceControl(MInvoice inv) {
		
		MDocType docType = (MDocType) inv.getC_DocType();
		
		if (inv.getReversal_ID() == 0
				&& docType.get_ValueAsBoolean(ColumnUtils.COLUMNNAME_IsControlNoDocument))
		{
			if (inv.isSOTrx())
			{
				if (!Util.isEmpty(inv.get_ValueAsString(ColumnUtils.COLUMNNAME_ITS_ControlNumber), true))
					return null;
				
				int ITS_ControlNoSequence_ID = docType.get_ValueAsInt(ColumnUtils.COLUMNNAME_ITS_ControlNoSequence_ID);
				
				if (ITS_ControlNoSequence_ID <= 0)
					return "@ITS_ControlNoSequence_ID@ @NotFound@ @C_DocType@ " + docType.getNameTrl();
				
				MSequence seq = new MSequence(inv.getCtx(), ITS_ControlNoSequence_ID, inv.get_TrxName());
				
				String controlNumber = MSequence.getDocumentNoFromSeq(seq, inv.get_TrxName(), inv);
				
				inv.set_ValueOfColumn(ColumnUtils.COLUMNNAME_ITS_ControlNumber, controlNumber);
				inv.saveEx();
			}
			else
			{
				if (Util.isEmpty(inv.get_ValueAsString(ColumnUtils.COLUMNNAME_ITS_ControlNumber), true))
					return "@FillMandatory@ @ITS_ControlNumber@";
				else if (Util.isEmpty(inv.get_ValueAsString(ColumnUtils.COLUMNNAME_ITS_POInvoiceNo), true))
					return "@FillMandatory@ @ITS_POInvoiceNo@";
			}
		}
		
		return null;
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @param alloc
	 */
	private void validateAllocationBeforeVoid(MAllocationHdr alloc) {
		
		String where = "aline.C_AllocationHdr_ID = ? AND vw.DocStatus IN ('CO', 'CL')"
				+ " AND LCO_InvoiceWithholding.IsCalcOnInvoice = 'N' AND IsCalcOnPayment = 'N'";
		
		boolean match = new Query(alloc.getCtx(), MLCOInvoiceWithholding.Table_Name, where, alloc.get_TrxName())
				.addJoinClause("INNER JOIN ITS_VoucherWithholding vw"
						+ " ON vw.ITS_VoucherWithholding_ID = LCO_InvoiceWithholding.ITS_VoucherWithholding_ID")
				.addJoinClause("INNER JOIN C_AllocationLine aline"
						+ " ON aline.C_AllocationLine_ID = LCO_InvoiceWithholding.C_AllocationLine_ID")
				.setOnlyActiveRecords(true)
				.setParameters(alloc.get_ID())
				.match();
		
		if (match)
			throw new AdempiereException("@AllocationInVoucher@");
	}

	private String clearInvoiceWithholdingAmtFromInvoice(MInvoice inv) {
		// Clear invoice withholding amount

		if (inv.is_ValueChanged("AD_Org_ID")
				|| inv.is_ValueChanged(MInvoice.COLUMNNAME_C_BPartner_ID)
				|| inv.is_ValueChanged(MInvoice.COLUMNNAME_C_DocTypeTarget_ID)) {

			boolean thereAreCalc;
			try {
				thereAreCalc = thereAreCalc(inv);
			} catch (SQLException e) {
				log.log(Level.SEVERE, "Error looking for calc on invoice rules", e);
				return "Error looking for calc on invoice rules";
			}

			BigDecimal curWithholdingAmt = (BigDecimal) inv.get_Value("WithholdingAmt");
			if (thereAreCalc) {
				if (curWithholdingAmt != null) {
					inv.set_CustomColumn("WithholdingAmt", null);
				}
			} else {
				if (curWithholdingAmt == null) {
					inv.set_CustomColumn("WithholdingAmt", Env.ZERO);
				}
			}

		}

		return null;
	}

	private String clearInvoiceWithholdingAmtFromInvoiceLine(MInvoiceLine invline, String type) {

		if (   type.equals(IEventTopics.PO_BEFORE_NEW)
			|| type.equals(IEventTopics.PO_BEFORE_DELETE)
			|| (   type.equals(IEventTopics.PO_BEFORE_CHANGE)
				&& (   invline.is_ValueChanged("LineNetAmt")
					|| invline.is_ValueChanged("M_Product_ID")
					|| invline.is_ValueChanged("C_Charge_ID")
					|| invline.is_ValueChanged("IsActive")
					|| invline.is_ValueChanged("C_Tax_ID")
					)
				)
			)
		{
			// Clear invoice withholding amount
			MInvoice inv = invline.getParent();

			boolean thereAreCalc;
			try {
				thereAreCalc = thereAreCalc(inv);
			} catch (SQLException e) {
				log.log(Level.SEVERE, "Error looking for calc on invoice rules", e);
				return "Error looking for calc on invoice rules";
			}

			BigDecimal curWithholdingAmt = (BigDecimal) inv.get_Value("WithholdingAmt");
			if (thereAreCalc) {
				if (curWithholdingAmt != null) {
					if (!LCO_MInvoice.setWithholdingAmtWithoutLogging(inv, null))
						return "Error saving C_Invoice clearInvoiceWithholdingAmtFromInvoiceLine";
				}
			} else {
				if (curWithholdingAmt == null) {
					if (!LCO_MInvoice.setWithholdingAmtWithoutLogging(inv, Env.ZERO))
						return "Error saving C_Invoice clearInvoiceWithholdingAmtFromInvoiceLine";
				}
			}
		}

		return null;
	}

	private boolean thereAreCalc(MInvoice inv) throws SQLException {
		boolean thereAreCalc = false;
		String sqlwccoi =
			"SELECT 1 "
			+ "  FROM LCO_WithholdingType wt, LCO_WithholdingCalc wc "
			+ " WHERE wt.LCO_WithholdingType_ID = wc.LCO_WithholdingType_ID";
		PreparedStatement pstmtwccoi = DB.prepareStatement(sqlwccoi, inv.get_TrxName());
		ResultSet rswccoi = null;
		try {
			rswccoi = pstmtwccoi.executeQuery();
			if (rswccoi.next())
				thereAreCalc = true;
		} catch (SQLException e) {
			throw e;
		} finally {
			DB.close(rswccoi, pstmtwccoi);
			rswccoi = null; pstmtwccoi = null;
		}
		return thereAreCalc;
	}

	private String validateWriteOffVsPaymentWithholdings(MPayment pay) {
		if (pay.getC_Invoice_ID() > 0) {
			// validate vs invoice of payment
			BigDecimal wo = pay.getWriteOffAmt();
			BigDecimal sumwhamt = Env.ZERO;
			
			StringBuilder sql = new StringBuilder("SELECT COALESCE(SUM(")
						.append("CASE")
							.append(" WHEN vw.ITS_VoucherWithholding_ID IS NOT NULL THEN")
								.append(" currencyConvert(iw.TaxAmt, vw.C_Currency_ID")
								.append(", ci.C_Currency_ID, ci.DateInvoiced")
								.append(", vw.C_ConversionType_ID, vw.AD_Client_ID, vw.AD_Org_ID)")
							.append(" ELSE")
								.append(" iw.TaxAmt")
						.append(" END")
					.append("), 0)")
					.append(" FROM LCO_InvoiceWithholding iw")
					.append(" INNER JOIN C_Invoice ci ON ci.C_Invoice_ID = iw.C_Invoice_ID")
					.append(" LEFT JOIN ITS_VoucherWithholding vw ON vw.ITS_VoucherWithholding_ID = iw.ITS_VoucherWithholding_ID")
					.append(" WHERE iw.C_Invoice_ID = ? AND iw.IsActive = 'Y'")
					.append(" AND iw.IsCalcOnPayment = 'Y'")
					.append(" AND CASE WHEN iw.ITS_VoucherWithholding_ID IS NOT NULL THEN vw.DocStatus = 'CO' ELSE iw.Processed = 'N' END")
					.append(" AND C_AllocationLine_ID IS NULL");
			
			sumwhamt = DB.getSQLValueBD(pay.get_TrxName(), sql.toString(), pay.getC_Invoice_ID());
			
			if (sumwhamt == null)
				sumwhamt = Env.ZERO;
			MInvoice invoice = new MInvoice(pay.getCtx(), pay.getC_Invoice_ID(), pay.get_TrxName());
			if (invoice.isCreditMemo())
				sumwhamt = sumwhamt.negate();
			if (wo.compareTo(sumwhamt) < 0 && sumwhamt.compareTo(Env.ZERO) != 0)
				return Msg.getMsg(pay.getCtx(), "LCO_WriteOffLowerThanWithholdings");
		} else {
			// validate every C_PaymentAllocate
			String sql =
				"SELECT C_PaymentAllocate_ID " +
				"FROM C_PaymentAllocate " +
				"WHERE C_Payment_ID = ?";
			PreparedStatement pstmt = DB.prepareStatement(sql, pay.get_TrxName());
			ResultSet rs = null;
			try {
				pstmt.setInt(1, pay.getC_Payment_ID());
				rs = pstmt.executeQuery();
				while (rs.next()) {
					int palid = rs.getInt(1);
					MPaymentAllocate pal = new MPaymentAllocate(pay.getCtx(), palid, pay.get_TrxName());
					BigDecimal wo = pal.getWriteOffAmt();
					BigDecimal sumwhamt = Env.ZERO;
					
					StringBuilder sqlWh = new StringBuilder("SELECT COALESCE(SUM(")
							.append("CASE")
								.append(" WHEN vw.ITS_VoucherWithholding_ID IS NOT NULL THEN")
									.append(" currencyConvert(iw.TaxAmt, vw.C_Currency_ID")
									.append(", ci.C_Currency_ID, ci.DateInvoiced")
									.append(", vw.C_ConversionType_ID, vw.AD_Client_ID, vw.AD_Org_ID)")
								.append(" ELSE")
									.append(" iw.TaxAmt")
							.append(" END")
						.append("), 0)")
						.append(" FROM LCO_InvoiceWithholding iw")
						.append(" INNER JOIN C_Invoice ci ON ci.C_Invoice_ID = iw.C_Invoice_ID")
						.append(" LEFT JOIN ITS_VoucherWithholding vw ON vw.ITS_VoucherWithholding_ID = iw.ITS_VoucherWithholding_ID")
						.append(" WHERE iw.C_Invoice_ID = ? AND iw.IsActive = 'Y'")
						.append(" AND iw.IsCalcOnPayment = 'Y'")
						.append(" AND CASE WHEN iw.ITS_VoucherWithholding_ID IS NOT NULL THEN vw.DocStatus = 'CO' ELSE iw.Processed = 'N' END")
						.append(" AND C_AllocationLine_ID IS NULL");
					
					sumwhamt = DB.getSQLValueBD(pay.get_TrxName(), sqlWh.toString(), pal.getC_Invoice_ID());
					
					if (sumwhamt == null)
						sumwhamt = Env.ZERO;
					MInvoice invoice = new MInvoice(pay.getCtx(), pal.getC_Invoice_ID(), pay.get_TrxName());
					if (invoice.isCreditMemo())
						sumwhamt = sumwhamt.negate();
					if (wo.compareTo(sumwhamt) < 0 && sumwhamt.compareTo(Env.ZERO) != 0)
						return Msg.getMsg(pay.getCtx(), "LCO_WriteOffLowerThanWithholdings");
				}
			} catch (SQLException e) {
				e.printStackTrace();
				return e.getLocalizedMessage();
			} finally {
				DB.close(rs, pstmt);
				rs = null; pstmt = null;
			}
		}

		return null;
	}

	private String completePaymentWithholdings(MAllocationHdr ah) {
		MAllocationLine[] als = ah.getLines(true);
		for (int i = 0; i < als.length; i++) {
			MAllocationLine al = als[i];
			if (al.getC_Invoice_ID() > 0) {
				String sql =
					"SELECT iw.LCO_InvoiceWithholding_ID " +
					"FROM LCO_InvoiceWithholding iw " +
					"LEFT JOIN ITS_VoucherWithholding vw ON vw.ITS_VoucherWithholding_ID = iw.ITS_VoucherWithholding_ID " +
					"WHERE iw.C_Invoice_ID = ? AND " +
					"iw.IsActive = 'Y' AND " +
					"iw.IsCalcOnPayment = 'Y' AND " +
					"iw.C_AllocationLine_ID IS NULL AND " +
					"CASE WHEN iw.ITS_VoucherWithholding_ID IS NOT NULL THEN " +
					"vw.DocStatus = 'CO' " +
					"ELSE iw.Processed = 'N' END ";
				PreparedStatement pstmt = DB.prepareStatement(sql, ah.get_TrxName());
				ResultSet rs = null;
				try {
					pstmt.setInt(1, al.getC_Invoice_ID());
					rs = pstmt.executeQuery();
					while (rs.next()) {
						int iwhid = rs.getInt(1);
						MLCOInvoiceWithholding iwh = new MLCOInvoiceWithholding(
								ah.getCtx(), iwhid, ah.get_TrxName());
						iwh.setC_AllocationLine_ID(al.getC_AllocationLine_ID());
						iwh.setDateAcct(ah.getDateAcct());
						iwh.setDateTrx(ah.getDateTrx());
						
						if (iwh.getITS_VoucherWithholding_ID() <= 0)
							iwh.setProcessed(true);
						
						if (!iwh.save())
							return "Error saving LCO_InvoiceWithholding completePaymentWithholdings";
					}
				} catch (SQLException e) {
					e.printStackTrace();
					return e.getLocalizedMessage();
				} finally {
					DB.close(rs, pstmt);
					rs = null; pstmt = null;
				}
			}
		}
		return null;
	}

	private String reversePaymentWithholdings(MAllocationHdr ah) {
		MAllocationLine[] als = ah.getLines(true);
		for (int i = 0; i < als.length; i++) {
			MAllocationLine al = als[i];
			if (al.getC_Invoice_ID() > 0) {
				String sql =
					"SELECT LCO_InvoiceWithholding_ID " +
					"FROM LCO_InvoiceWithholding " +
					"WHERE C_Invoice_ID = ? AND " +
					"IsActive = 'Y' AND " +
					"IsCalcOnPayment = 'Y' AND " +
					"Processed = 'Y' AND " +
					"C_AllocationLine_ID = ?";
				PreparedStatement pstmt = DB.prepareStatement(sql, ah.get_TrxName());
				ResultSet rs = null;
				try {
					pstmt.setInt(1, al.getC_Invoice_ID());
					pstmt.setInt(2, al.getC_AllocationLine_ID());
					rs = pstmt.executeQuery();
					while (rs.next()) {
						int iwhid = rs.getInt(1);
						MLCOInvoiceWithholding iwh = new MLCOInvoiceWithholding(
								ah.getCtx(), iwhid, ah.get_TrxName());
						iwh.setC_AllocationLine_ID(0);
						if (iwh.getITS_VoucherWithholding_ID() <= 0)
							iwh.setProcessed(false);
						if (!iwh.save())
							return "Error saving LCO_InvoiceWithholding reversePaymentWithholdings";
					}
				} catch (SQLException e) {
					e.printStackTrace();
					return e.getLocalizedMessage();
				} finally {
					DB.close(rs, pstmt);
					rs = null; pstmt = null;
				}
			}
		}
		return null;
	}

	private String accountingForInvoiceWithholdingOnPayment(MAllocationHdr ah, Event event) {
		// Accounting like Doc_Allocation
		// (Write off) vs (invoice withholding where iscalconpayment=Y)
		// 20070807 - globalqss - instead of adding a new WriteOff post, find the
		//  current WriteOff and subtract from the posting
		
		Doc doc = ah.getDoc();
		FactsEventData fed = getEventData(event);
		List<Fact> facts = fed.getFacts();
		
		// one fact per acctschema
		for (int i = 0; i < facts.size(); i++)
		{
			Fact fact = facts.get(i);
			MAcctSchema as = fact.getAcctSchema();
			
			MAllocationLine[] alloc_lines = ah.getLines(false);
			for (int j = 0; j < alloc_lines.length; j++) {
				BigDecimal tottax = BigDecimal.ZERO;
				
				MAllocationLine alloc_line = alloc_lines[j];
				DocLine_Allocation docLine = new DocLine_Allocation(alloc_line, doc);
				doc.setC_BPartner_ID(alloc_line.getC_BPartner_ID());
				
				int inv_id = alloc_line.getC_Invoice_ID();
				if (inv_id <= 0)
					continue;
				MInvoice invoice = null;
				invoice = new MInvoice (ah.getCtx(), alloc_line.getC_Invoice_ID(), ah.get_TrxName());
				if (invoice == null || invoice.getC_Invoice_ID() == 0)
					continue;
				String sql =
					  "SELECT i.C_Tax_ID"
					  + ", NVL(SUM(CASE"
					  + " WHEN vw.ITS_VoucherWithholding_ID IS NOT NULL THEN"
					  	+ " currencyConvert(i.TaxBaseAmt, vw.C_Currency_ID"
					  		+ ", ci.C_Currency_ID, ci.DateInvoiced, vw.C_ConversionType_ID"
					  		+ ", vw.AD_Client_ID, vw.AD_Org_ID)"
					  + " ELSE"
					  	+ " i.TaxBaseAmt"
					  + " END),0) AS TaxBaseAmt"
					  + ", NVL(SUM(CASE"
					  + " WHEN vw.ITS_VoucherWithholding_ID IS NOT NULL THEN"
					  	+ " currencyConvert(i.TaxAmt, vw.C_Currency_ID"
					  		+ ", ci.C_Currency_ID, ci.DateInvoiced, vw.C_ConversionType_ID"
					  		+ ", vw.AD_Client_ID, vw.AD_Org_ID)"
					  + " ELSE"
					  	+ " i.TaxAmt"
					  + " END),0) AS TaxAmt"
					  + ", t.Name, t.Rate, t.IsSalesTax "
					 + " FROM LCO_InvoiceWithholding i"
					 + " INNER JOIN C_Invoice ci ON ci.C_Invoice_ID = i.C_Invoice_ID"
					 + " INNER JOIN C_Tax t ON t.C_Tax_ID = i.C_Tax_ID"
					 + " LEFT JOIN ITS_VoucherWithholding vw ON vw.ITS_VoucherWithholding_ID = i.ITS_VoucherWithholding_ID"
					 + " WHERE i.C_Invoice_ID = ? AND " +
							 "(i.IsCalcOnPayment = 'Y' OR vw.ITS_VoucherWithholding_ID IS NOT NULL) AND " +
							 "i.IsActive = 'Y' AND " +
							 "i.Processed = 'Y' AND " +
							 "i.C_AllocationLine_ID = ? "
					+ "GROUP BY i.C_Tax_ID, t.Name, t.Rate, t.IsSalesTax";
				PreparedStatement pstmt = null;
				ResultSet rs = null;
				try
				{
					pstmt = DB.prepareStatement(sql, ah.get_TrxName());
					pstmt.setInt(1, invoice.getC_Invoice_ID());
					pstmt.setInt(2, alloc_line.getC_AllocationLine_ID());
					rs = pstmt.executeQuery();
					while (rs.next()) {
						int tax_ID = rs.getInt(1);
						BigDecimal taxBaseAmt = rs.getBigDecimal(2);
						BigDecimal amount = rs.getBigDecimal(3);
						if (invoice.isCreditMemo()) {
							taxBaseAmt = taxBaseAmt.negate();
							amount = amount.negate();
						}
						String name = rs.getString(4);
						BigDecimal rate = rs.getBigDecimal(5);
						boolean salesTax = rs.getString(6).equals("Y") ? true : false;

						DocTax taxLine = new DocTax(tax_ID, name, rate,
								taxBaseAmt, amount, salesTax);

						if (amount != null && amount.signum() != 0)
						{
							FactLine tl = null;
							if (invoice.isSOTrx()) {
								tl = fact.createLine(docLine, taxLine.getAccount(DocTax.ACCTTYPE_TaxDue, as),
										ah.getC_Currency_ID(), amount, null);
							} else {
								tl = fact.createLine(docLine, taxLine.getAccount(taxLine.getAPTaxType(), as),
										ah.getC_Currency_ID(), null, amount);
							}
							if (tl != null)
								tl.setC_Tax_ID(taxLine.getC_Tax_ID());
							tottax = tottax.add(amount);
						}
					}
				} catch (Exception e) {
					log.log(Level.SEVERE, sql, e);
					return "Error posting C_InvoiceTax from LCO_InvoiceWithholding";
				} finally {
					DB.close(rs, pstmt);
					rs = null; pstmt = null;
				}

				//	Write off		DR
				if (Env.ZERO.compareTo(tottax) != 0)
				{
					// First try to find the WriteOff posting record
					FactLine[] factlines = fact.getLines();
					boolean foundflwriteoff = false;
					for (int ifl = 0; ifl < factlines.length; ifl++) {
						FactLine fl = factlines[ifl];
						if (fl.getAccount().equals(doc.getAccount(Doc.ACCTTYPE_WriteOff, as))) {
							foundflwriteoff = true;
							// old balance = DB - CR
							BigDecimal balamt = fl.getAmtSourceDr().subtract(fl.getAmtSourceCr());
							// new balance = old balance +/- tottax
							BigDecimal newbalamt = Env.ZERO;
							if (invoice.isSOTrx())
								newbalamt = balamt.subtract(tottax);
							else
								newbalamt = balamt.add(tottax);
							if (Env.ZERO.compareTo(newbalamt) == 0) {
								// both zeros, remove the line
								fact.remove(fl);
							} else if (Env.ZERO.compareTo(newbalamt) > 0) {
								fl.setAmtSource(fl.getC_Currency_ID(), Env.ZERO, newbalamt);
								fl.convert();
							} else {
								fl.setAmtSource(fl.getC_Currency_ID(), newbalamt, Env.ZERO);
								fl.convert();
							}
							break;
						}
					}

					if (! foundflwriteoff) {
						// Create a new line - never expected to arrive here as it must always be a write-off line
						DocLine_Allocation line = new DocLine_Allocation(alloc_line, doc);
						FactLine fl = null;
						if (invoice.isSOTrx()) {
							fl = fact.createLine (line, doc.getAccount(Doc.ACCTTYPE_WriteOff, as),
									ah.getC_Currency_ID(), null, tottax);
						} else {
							fl = fact.createLine (line, doc.getAccount(Doc.ACCTTYPE_WriteOff, as),
									ah.getC_Currency_ID(), tottax, null);
						}
						if (fl != null)
							fl.setAD_Org_ID(ah.getAD_Org_ID());
					}

				}

			}

		}

		return null;
	}

	private String completeInvoiceWithholding(MInvoice inv) {

		// Fill DateAcct and DateTrx with final dates from Invoice
		String upd_dates =
			"UPDATE LCO_InvoiceWithholding "
			 + "   SET DateAcct = "
			 + "          (SELECT DateAcct "
			 + "             FROM C_Invoice "
			 + "            WHERE C_Invoice.C_Invoice_ID = LCO_InvoiceWithholding.C_Invoice_ID), "
			 + "       DateTrx = "
			 + "          (SELECT DateInvoiced "
			 + "             FROM C_Invoice "
			 + "            WHERE C_Invoice.C_Invoice_ID = LCO_InvoiceWithholding.C_Invoice_ID) "
			 + " WHERE C_Invoice_ID = ? ";
		int noupddates = DB.executeUpdate(upd_dates, inv.getC_Invoice_ID(), inv.get_TrxName());
		if (noupddates == -1)
			return "Error updating dates on invoice withholding";

		// Set processed for isCalcOnInvoice records
		String upd_proc =
			"UPDATE LCO_InvoiceWithholding "
			 + "   SET Processed = 'Y' "
			 + " WHERE C_Invoice_ID = ? AND IsCalcOnInvoice = 'Y'";
		int noupdproc = DB.executeUpdate(upd_proc, inv.getC_Invoice_ID(), inv.get_TrxName());
		if (noupdproc == -1)
			return "Error updating processed on invoice withholding";

		return null;
	}

	private String translateWithholdingToTaxes(MInvoice inv) {
		BigDecimal sumit = new BigDecimal(0);

		MDocType dt = new MDocType(inv.getCtx(), inv.getC_DocTypeTarget_ID(), inv.get_TrxName());
		String genwh = dt.get_ValueAsString("GenerateWithholding");
		if (genwh == null || genwh.equals("N")) {
			// document configured to not manage withholdings - delete any
			String sqldel = "DELETE FROM LCO_InvoiceWithholding "
				+ " WHERE C_Invoice_ID = ?";
			PreparedStatement pstmtdel = null;
			try
			{
				// Delete previous records generated
				pstmtdel = DB.prepareStatement(sqldel,
						ResultSet.TYPE_FORWARD_ONLY,
						ResultSet.CONCUR_UPDATABLE, inv.get_TrxName());
				pstmtdel.setInt(1, inv.getC_Invoice_ID());
				int nodel = pstmtdel.executeUpdate();
				log.config("LCO_InvoiceWithholding deleted="+nodel);
			} catch (Exception e) {
				log.log(Level.SEVERE, sqldel, e);
				return "Error creating C_InvoiceTax from LCO_InvoiceWithholding -delete";
			} finally {
				DB.close(pstmtdel);
				pstmtdel = null;
			}
			inv.set_CustomColumn("WithholdingAmt", Env.ZERO);

		} else {
			// translate withholding to taxes
			String sql =
				  "SELECT C_Tax_ID, NVL(SUM(TaxBaseAmt),0) AS TaxBaseAmt, NVL(SUM(TaxAmt),0) AS TaxAmt "
				 + " FROM LCO_InvoiceWithholding "
				+ " WHERE C_Invoice_ID = ? AND IsCalcOnInvoice = 'Y' AND IsActive = 'Y' "
				+ "GROUP BY C_Tax_ID";
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try
			{
				pstmt = DB.prepareStatement(sql, inv.get_TrxName());
				pstmt.setInt(1, inv.getC_Invoice_ID());
				rs = pstmt.executeQuery();
				while (rs.next()) {
					MInvoiceTax it = new MInvoiceTax(inv.getCtx(), 0, inv.get_TrxName());
					it.setAD_Org_ID(inv.getAD_Org_ID());
					it.setC_Invoice_ID(inv.getC_Invoice_ID());
					it.setC_Tax_ID(rs.getInt(1));
					it.setTaxBaseAmt(rs.getBigDecimal(2));
					it.setTaxAmt(rs.getBigDecimal(3).negate());
					sumit = sumit.add(rs.getBigDecimal(3));
					if (!it.save())
						return "Error creating C_InvoiceTax from LCO_InvoiceWithholding - save InvoiceTax";
				}
				BigDecimal actualamt = (BigDecimal) inv.get_Value("WithholdingAmt");
				if (actualamt == null)
					actualamt = new BigDecimal(0);
				if (actualamt.compareTo(sumit) != 0 || sumit.signum() != 0) {
					inv.set_CustomColumn("WithholdingAmt", sumit);
					// Subtract to invoice grand total the value of withholdings
					BigDecimal gt = inv.getGrandTotal();
					inv.setGrandTotal(gt.subtract(sumit));
					inv.saveEx();  // need to save here in order to let apply get the right total
				}

				if (sumit.signum() != 0) {
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
			} catch (Exception e) {
				log.log(Level.SEVERE, sql, e);
				return "Error creating C_InvoiceTax from LCO_InvoiceWithholding - select InvoiceTax";
			} finally {
				DB.close(rs, pstmt);
				rs = null; pstmt = null;
			}
		}

		return null;
	}

}	//	LCO_ValidatorWH
