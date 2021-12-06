package dev.itechsolutions.model;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAllocationHdr;
import org.compiere.model.MAllocationLine;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MPeriod;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.process.DocOptions;
import org.compiere.process.DocumentEngine;
import org.compiere.util.DB;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.globalqss.model.MLCOInvoiceWithholding;

import dev.itechsolutions.exception.NoCurrencyConversionException;
import dev.itechsolutions.util.ColumnUtils;
import dev.itechsolutions.util.TimestampUtil;

/**
 * 
 * @author Argenis Rodríguez arodriguez@itechsolutions.dev
 *
 */
public class MITSVoucherWithholding extends X_ITS_VoucherWithholding implements DocAction, DocOptions {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**		Process Message		*/
	private String m_processMsg = null;
	
	/**		Just Prepared Flag	*/
	private boolean m_justPrepared = false;
	
	/**		Lines		*/
	private MLCOInvoiceWithholding[] m_lines = null;
	
	/**		Document Type			 */
	private MDocType docType = null;
	
	public MITSVoucherWithholding(Properties ctx, int ITS_VoucherWithholding_ID, String trxName) {
		super(ctx, ITS_VoucherWithholding_ID, trxName);
	}
	
	public MITSVoucherWithholding(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @return
	 */
	public MLCOInvoiceWithholding[] getLines() {
		return getLines(false);
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @param requery
	 * @return
	 */
	public MLCOInvoiceWithholding[] getLines(boolean requery) {
		if (!requery && m_lines != null)
			return m_lines;
		
		String where = "LCO_InvoiceWithholding.ITS_VoucherWithholding_ID = ?";
		List<MLCOInvoiceWithholding> lines = new Query(getCtx(), MLCOInvoiceWithholding.Table_Name, where, get_TrxName())
				.addJoinClause("INNER JOIN C_Invoice ci ON ci.C_Invoice_ID = LCO_InvoiceWithholding.C_Invoice_ID")
				.setOnlyActiveRecords(true)
				.setParameters(get_ID())
				.setOrderBy("ci." + MInvoice.COLUMNNAME_C_Currency_ID + ", ci." + MInvoice.COLUMNNAME_AD_Org_ID)
				.list();
		
		m_lines = lines.toArray(MLCOInvoiceWithholding[]::new);
		
		return m_lines;
	}
	
	@Override
	public boolean processIt(String action) throws Exception {
		m_processMsg = null;
		DocumentEngine engine = new DocumentEngine(this, getDocStatus());
		return engine.processIt(action, getDocAction());
	}
	
	@Override
	public boolean unlockIt() {
		log.info(toString());
		return true;
	}
	
	@Override
	public boolean invalidateIt() {
		log.info(toString());
		setDocAction(ACTION_Prepare);
		return true;
	}
	
	@Override
	public String prepareIt() {
		
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_PREPARE);
		
		if (!Util.isEmpty(m_processMsg, true))
			return STATUS_Invalid;
		
		MPeriod.testPeriodOpen(getCtx(), getDateAcct(), ColumnUtils.DOCBASETYPE_VoucherWithholding, getAD_Org_ID());
		getLines(true);
		
		if (m_lines.length == 0)
		{
			m_processMsg = "@NoLines@";
			return STATUS_Invalid;
		}
		
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_PREPARE);
		
		if (!Util.isEmpty(m_processMsg, true))
			return STATUS_Invalid;
		
		m_justPrepared = true;
		
		if (!DOCACTION_Complete.equals(getDocAction()))
			setDocAction(DOCACTION_Complete);
		
		return STATUS_InProgress;
	}
	
	@Override
	public boolean approveIt() {
		if (log.isLoggable(Level.INFO)) log.info(toString());
		setIsApproved(true);
		return true;
	}
	
	@Override
	public boolean rejectIt() {
		if (log.isLoggable(Level.INFO)) log.info(toString());
		setIsApproved(false);
		return true;
	}
	
	@Override
	public String completeIt() {
		
		if (!m_justPrepared)
		{
			String status = prepareIt();
			m_justPrepared = false;
			if (!STATUS_InProgress.equals(status))
				return status;
		}
		
		//Set Definite document number after completed (id need)
		setDefiniteDocumentNo();
		
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_COMPLETE);
		
		if (!Util.isEmpty(m_processMsg, true))
			return STATUS_Invalid;
		
		if (!isApproved())
			approveIt();
		
		MDocType dt = getC_DocType();
		int C_DocTypeAllocation_ID = dt.get_ValueAsInt(ColumnUtils.COLUMNNAME_C_DocTypeAllocation_ID);
		
		if (C_DocTypeAllocation_ID <= 0)
		{
			m_processMsg = "@DocTypeAllocationNotConfigured@";
			return STATUS_Invalid;
		}
		
		getLines(true);
		int lastC_Currency_ID = -1;
		int lastAD_Org_ID = -1;
		MAllocationHdr allocation = null;
		
		for (MLCOInvoiceWithholding line: m_lines)
		{
			int C_Currency_ID = line.getC_Invoice().getC_Currency_ID();
			int AD_Org_ID = line.getC_Invoice().getAD_Org_ID();
			
			line.setDocumentNo(getDocumentNo());
			line.setDateTrx(getDateTrx());
			line.setDateAcct(getDateAcct());
			
			if (!line.isCalcOnInvoice() && !line.isCalcOnPayment())
			{
				if (allocation == null
						|| C_Currency_ID != lastC_Currency_ID
						|| AD_Org_ID != lastAD_Org_ID)
				{
					lastC_Currency_ID = C_Currency_ID;
					lastAD_Org_ID = AD_Org_ID;
					
					if (allocation != null
							&& !allocation.processIt(MAllocationHdr.ACTION_Complete))
					{
						m_processMsg = allocation.getProcessMsg();
						return STATUS_Invalid;
					}
					
					allocation = createAllocation(C_DocTypeAllocation_ID
							, C_Currency_ID, AD_Org_ID);
				}
				
				MInvoice invoice = line.getC_Invoice();
				
				BigDecimal invoiceOpen = invoice.getOpenAmt();
				
				BigDecimal writeOffAmt = line.getTaxAmt();
				
				writeOffAmt = ITSMConversionRate.convert(getCtx(), writeOffAmt
						, getC_Currency_ID(), C_Currency_ID
						, invoice.getDateInvoiced(), getC_ConversionType_ID()
						, getAD_Client_ID(), getAD_Org_ID());
				
				if (writeOffAmt == null)
				{
					m_processMsg = NoCurrencyConversionException.buildMessage(
							getC_Currency_ID(), C_Currency_ID
							, invoice.getDateInvoiced(), getC_ConversionType_ID()
							, getAD_Client_ID(), getAD_Org_ID());
					
					return STATUS_Invalid;
				}
				
				BigDecimal overUnderAmt = invoiceOpen.subtract(writeOffAmt);
				
				if (!isSOTrx())
				{
					writeOffAmt = writeOffAmt.negate();
					overUnderAmt = overUnderAmt.negate();
				}
				
				if (invoice.isCreditMemo())
				{
					writeOffAmt = writeOffAmt.negate();
					overUnderAmt = overUnderAmt.negate();
				}
				
				MAllocationLine aLine = new MAllocationLine(allocation
						, BigDecimal.ZERO, BigDecimal.ZERO
						, writeOffAmt, overUnderAmt);
				
				aLine.setDocInfo(invoice.getC_BPartner_ID(), 0, invoice.get_ID());
				aLine.saveEx();
				
				line.setC_AllocationLine_ID(aLine.get_ID());
			}
			
			line.saveEx();
		}
		
		if (allocation != null
				&& !allocation.processIt(MAllocationHdr.ACTION_Complete))
		{
			m_processMsg = allocation.getProcessMsg();
			return STATUS_Invalid;
		}
		
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_COMPLETE);
		
		if (!Util.isEmpty(m_processMsg, true))
			return STATUS_Invalid;
		
		setProcessed(true);
		setDocAction(DOCACTION_Close);
		return STATUS_Completed;
	}
	
	
	
	@Override
	public void setProcessed(boolean Processed) {
		super.setProcessed(Processed);
		
		StringBuilder sqlUpdate = new StringBuilder("UPDATE LCO_InvoiceWithholding SET Processed = ?")
				.append(" WHERE ITS_VoucherWithholding_ID = ?");
		
		DB.executeUpdateEx(sqlUpdate.toString()
				, new Object[] {Processed, get_ID()}
				, get_TrxName());
		
		getLines(true);
	}

	/**
	 * 
	 * @author Argenis Rodríguez
	 * @param C_DocType_ID
	 * @param C_Currency_ID
	 * @param AD_Org_ID
	 * @return
	 */
	private MAllocationHdr createAllocation(int C_DocType_ID
			, int C_Currency_ID, int AD_Org_ID) {
		
		MAllocationHdr allocation = new MAllocationHdr(getCtx(), 0, get_TrxName());
		allocation.setAD_Org_ID(AD_Org_ID);
		allocation.setDateTrx(getDateTrx());
		allocation.setDateAcct(getDateAcct());
		allocation.setC_DocType_ID(C_DocType_ID);
		allocation.setC_Currency_ID(C_Currency_ID);
		
		allocation.saveEx();
		
		return allocation;
	}
	
	private void setDefiniteDocumentNo() {
		
		MDocType dt = getC_DocType();
		
		if (dt.isOverwriteDateOnComplete())
		{
			setDateTrx(TimestampUtil.now());
			
			if (getDateAcct().before(getDateTrx()))
			{
				setDateAcct(getDateTrx());
				MPeriod.testPeriodOpen(getCtx(), getDateAcct(), getC_DocType_ID(), getAD_Org_ID());
			}
		}
		
		if (dt.isOverwriteSeqOnComplete())
		{
			String docNo = DB.getDocumentNo(getC_DocType_ID(), get_TrxName(), true, this);
			if (!Util.isEmpty(docNo, true))
				setDocumentNo(docNo);
		}
	}
	
	@Override
	public boolean voidIt() {
		
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_VOID);
		
		if (!Util.isEmpty(m_processMsg, true))
			return false;
		
		setDocumentNo(getDocumentNo() + "^");
		
		m_processMsg = reverseAllocations();
		
		if (!Util.isEmpty(m_processMsg, true))
			return false;
		
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_VOID);
		
		if (!Util.isEmpty(m_processMsg, true))
			return false;
		
		setProcessed(true);
		setDocAction(DOCACTION_None);
		return true;
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @return
	 */
	private String reverseAllocations() {
		
		int lastC_Currency_ID = -1;
		int lastAD_Org_ID = -1;
		MAllocationHdr alloc = null;
		
		getLines(true);
		
		for (MLCOInvoiceWithholding line: m_lines)
		{
			
			if (!line.isCalcOnPayment() && !line.isCalcOnInvoice())
			{
				int C_Currency_ID = line.getC_Invoice().getC_Currency_ID();
				int AD_Org_ID = line.getC_Invoice().getAD_Org_ID();
				
				if (line.getC_AllocationLine_ID() <= 0)
					continue;
				
				if (alloc == null
						||lastC_Currency_ID != C_Currency_ID
						|| lastAD_Org_ID != AD_Org_ID)
				{
					if (alloc != null && !alloc.processIt(MAllocationHdr.DOCACTION_Reverse_Correct))
						return alloc.getProcessMsg();
					
					alloc = (MAllocationHdr) line.getC_AllocationLine().getC_AllocationHdr();
					
					lastC_Currency_ID = C_Currency_ID;
					lastAD_Org_ID = AD_Org_ID;
				}
				
				line.setC_AllocationLine_ID(0);
				line.saveEx();
			} else if (line.isCalcOnPayment() && line.getC_AllocationLine_ID() > 0)
			{
				MAllocationLine aLine = new MAllocationLine(getCtx(), line.getC_AllocationLine_ID(), get_TrxName());
				MAllocationHdr ah = aLine.getParent();
				
				return "@VoucherWithAllocation@ " + ah.getDocumentNo();
			}
		}
		
		if (alloc != null && !alloc.processIt(MAllocationHdr.DOCACTION_Reverse_Correct))
			return alloc.getProcessMsg();
		
		return null;
	}
	
	@Override
	public boolean closeIt() {
		
		//Before Close
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_CLOSE);
		
		if (!Util.isEmpty(m_processMsg, true))
			return false;
		
		setProcessed(true);
		setDocAction(DOCACTION_None);
		
		//After Close
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_CLOSE);
		
		if (!Util.isEmpty(m_processMsg, true))
			return false;
		
		return true;
	}
	
	@Override
	public boolean reverseCorrectIt() {
		
		//Before reverseCorrect
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_REVERSECORRECT);
		
		if (!Util.isEmpty(m_processMsg, true))
			return false;
		
		setDocumentNo(getDocumentNo() + "^");
		
		m_processMsg = reverseAllocations();
		
		if (!Util.isEmpty(m_processMsg, true))
			return false;
		
		//After reverseCorrect
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_REVERSECORRECT);
		
		if (!Util.isEmpty(m_processMsg, true))
			return false;
		
		setProcessed(true);
		setDocAction(DOCACTION_None);
		return true;
	}
	
	@Override
	public boolean reverseAccrualIt() {
		
		//Before reverseAccrual
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_REVERSEACCRUAL);
		
		if (!Util.isEmpty(m_processMsg, true))
			return false;
		
		setDocumentNo(getDocumentNo() + "^");
		
		m_processMsg = reverseAllocations();
		
		if (!Util.isEmpty(m_processMsg, true))
			return false;
		
		//After reverseAccrual
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_REVERSEACCRUAL);
		
		if (!Util.isEmpty(m_processMsg, true))
			return false;
		
		setProcessed(true);
		setDocAction(DOCACTION_None);
		return true;
	}
	
	@Override
	public boolean reActivateIt() {
		
		//Before reActivate
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_REACTIVATE);
		
		if (!Util.isEmpty(m_processMsg, true))
			return false;
		
		m_processMsg = validateAllocations();
		
		if (!Util.isEmpty(m_processMsg, true))
			return false;
		
		setProcessed(false);
		setIsApproved(false);
		setDocAction(DOCACTION_Complete);
		
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_REACTIVATE);
		
		//After reActivate
		if (!Util.isEmpty(m_processMsg, true))
			return false;
		
		return true;
	}
	
	/**
	 * 
	 * @author Argenis Rodríguez
	 * @return
	 */
	private String validateAllocations() {
		
		int lastAD_Org_ID = -1;
		int lastC_Currency_ID = -1;
		MAllocationHdr alloc = null;
		
		for (MLCOInvoiceWithholding iwh: getLines(true))
		{
			if (iwh.isCalcOnPayment() && iwh.getC_AllocationLine_ID() > 0)
			{
				MAllocationLine aLine = new MAllocationLine(getCtx(), iwh.getC_AllocationLine_ID(), get_TrxName());
				MAllocationHdr allocation = aLine.getParent();
				
				return "@VoucherWithAllocation@ " + allocation.getDocumentNo();
			} else if (!iwh.isCalcOnPayment() && !iwh.isCalcOnInvoice())
			{
				if (iwh.getC_AllocationLine_ID() <= 0)
					continue;
				
				int AD_Org_ID = iwh.getAD_Org_ID();
				int C_Currency_ID = iwh.getC_Invoice().getC_Currency_ID();
				
				if (alloc == null
						|| lastAD_Org_ID != AD_Org_ID
						|| lastC_Currency_ID != C_Currency_ID)
				{
					if (alloc != null)
						alloc.deleteEx(true);
					
					MAllocationLine aLine = new MAllocationLine(getCtx(), iwh.getC_AllocationLine_ID(), iwh.get_TrxName());
					alloc = aLine.getParent();
					
					lastAD_Org_ID = AD_Org_ID;
					lastC_Currency_ID = C_Currency_ID;
				}
				
				iwh.setC_AllocationLine_ID(0);
				iwh.saveEx();
			}
		}
		
		if (alloc != null)
			alloc.deleteEx(true);
		
		return null;
	}
	
	@Override
	public String getSummary() {
		
		StringBuilder info = new StringBuilder();
		
		info.append(getDocumentNo())
		.append(": ")
		.append("(#").append(getLines().length).append(")");
		
		return info.toString();
	}
	
	@Override
	public String getDocumentInfo() {
		
		MDocType docType = getC_DocType();
		StringBuilder documentInfo = new StringBuilder();
		
		documentInfo.append(docType.getNameTrl())
		.append(" ")
		.append(getDocumentNo());
		
		return documentInfo.toString();
	}
	
	@Override
	public File createPDF() {
		
		try {
			File temp = File.createTempFile(get_TableName() + get_ID() + "_", ".pdf");
			return createPDF(temp);
		} catch (IOException e) {
			log.severe("Could not create PDF - " + e.getLocalizedMessage());
		}
		
		return null;
	}
	
	public File createPDF(File file) {
		return null;
	}
	
	@Override
	public String getProcessMsg() {
		return m_processMsg;
	}
	
	@Override
	public int getDoc_User_ID() {
		return getCreatedBy();
	}
	
	@Override
	public BigDecimal getApprovalAmt() {
		return null;
	}
	
	@Override
	public int customizeValidActions(String docStatus, Object processing, String orderType, String isSOTrx,
			int AD_Table_ID, String[] docAction, String[] options, int index) {
		
		if (Table_ID == AD_Table_ID)
		{
			if (STATUS_Drafted.equals(docStatus) || STATUS_Invalid.equals(docStatus))
				options[index++] = ACTION_Prepare;
			else if (STATUS_InProgress.equals(docStatus))
				options[index++] = ACTION_Approve;
			else if (STATUS_Approved.equals(docStatus))
				options[index++] = ACTION_Complete;
			else if (STATUS_Completed.equals(docStatus))
			{
				options[index++] = ACTION_ReActivate;
				options[index++] = ACTION_Void;
				options[index++] = ACTION_Close;
			}
		}
		
		return index;
	}
	
	@Override
	public MDocType getC_DocType() throws RuntimeException {
		
		if (docType == null)
			docType = (MDocType) super.getC_DocType();
		
		return docType;
	}
	
	@Override
	protected boolean beforeSave(boolean newRecord) {
		
		//Validate Invoice
		if ((newRecord
				|| is_ValueChanged(COLUMNNAME_C_Invoice_ID))
				 && getC_Invoice_ID() > 0)
		{
			StringBuilder sql = new StringBuilder("SELECT vw.DocumentNo")
					.append(" FROM ITS_VoucherWithholding vw")
					.append(" INNER JOIN LCO_InvoiceWithholding iw ON iw.ITS_VoucherWithholding_ID = vw.ITS_VoucherWithholding_ID")
					.append(" WHERE iw.C_Invoice_ID = ?")
					.append(" AND vw.DocStatus NOT IN ('VO', 'RE')")
					.append(" AND vw.LCO_WithholdingType_ID = ?");
			
			String voucher = DB.getSQLValueString(get_TrxName(), sql.toString()
					, getC_Invoice_ID(), getLCO_WithholdingType_ID());
			
			if (!Util.isEmpty(voucher, true))
			{
				log.saveError("Error", Msg.getMsg(getCtx(), "InvoiceInVoucher"
						, new Object[] {voucher}));
				return false;
			}
		}
		
		if (!newRecord
				&& is_ValueChanged(COLUMNNAME_C_BPartner_ID))
		{
			if (getLines().length> 0)
			{
				log.saveError("Error", new AdempiereException("@CanNotChangeBPartner@"));
				return false;
			}
		}
		
		if (!newRecord
				&& is_ValueChanged(COLUMNNAME_C_Currency_ID))
		{
			if (getLines().length> 0)
			{
				log.saveError("Error", new AdempiereException("@CanNotChangeCurrency@"));
				return false;
			}
		}
		
		if (!newRecord
				&& is_ValueChanged(COLUMNNAME_C_ConversionType_ID))
		{
			if (getLines().length> 0)
			{
				log.saveError("Error", new AdempiereException("@CanNotChangeConversionType@"));
				return false;
			}
		}
		
		return true;
	}
}
