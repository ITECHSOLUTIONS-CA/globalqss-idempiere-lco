package dev.itechsolutions.model;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MDocType;
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

import dev.itechsolutions.util.TimestampUtil;

/**
 * 
 * @author Argenis Rodríguez
 *
 */
public class MITSVoucherWithholding extends X_ITS_VoucherWithholding implements DocAction, DocOptions {
	
	private static final long serialVersionUID = 1L;
	
	private String m_processMsg = null;
	
	private boolean m_justPrepared = false;
	
	private MLCOInvoiceWithholding[] m_lines;
	
	private MDocType m_docType = null;
	
	public MITSVoucherWithholding(Properties ctx, int ITS_VoucherWithholding_ID, String trxName) {
		super(ctx, ITS_VoucherWithholding_ID, trxName);
	}
	
	public MITSVoucherWithholding(Properties ctx, int ITS_VoucherWithholding_ID, String trxName,
			String... virtualColumns) {
		super(ctx, ITS_VoucherWithholding_ID, trxName, virtualColumns);
	}
	
	public MITSVoucherWithholding(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
	
	@Override
	public boolean processIt(String processAction) throws Exception {
		m_processMsg = null;
		DocumentEngine engine = new DocumentEngine(this, getDocStatus());
		return engine.processIt(processAction, getDocAction());
	}
	
	public MLCOInvoiceWithholding[] getLines() {
		return getLines(false);
	}
	
	public MLCOInvoiceWithholding[] getLines(boolean requery) {
		if (!requery && m_lines != null)
		{
			set_TrxName(m_lines, get_TrxName());
			return m_lines;
		}
		
		return m_lines = new Query(getCtx(), MLCOInvoiceWithholding.Table_Name, "ITS_VoucherWithholding_ID = ?", get_TrxName())
				.setOnlyActiveRecords(true)
				.setParameters(get_ID())
				.list()
				.toArray(MLCOInvoiceWithholding[]::new);
	}
	
	@Override
	public boolean unlockIt() {
		if (log.isLoggable(Level.INFO)) log.info(toString());
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
		
		if (getLines().length == 0)
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
		
		// Set the definite document number after completed (if needed)
		setDefiniteDocumentNo();
		
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_COMPLETE);
		
		if (!Util.isEmpty(m_processMsg, true))
			return STATUS_Invalid;
		
		if (!isApproved())
			approveIt();
		
		m_processMsg = allocateUnprocessedLines();
		
		if (!Util.isEmpty(m_processMsg, true))
			return STATUS_Invalid;
		
		m_processMsg = translateWithholdingsToTax();
		
		if (!Util.isEmpty(m_processMsg, true))
			return STATUS_Invalid;
		
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_COMPLETE);
		
		if (!Util.isEmpty(m_processMsg, true))
			return STATUS_Invalid;
		
		setProcessed(true);
		setDocAction(ACTION_Close);
		return STATUS_Completed;
	}
	
	private MLCOInvoiceWithholding[] getUnprocessedLinesForAllocation() {
		return Arrays.stream(getLines())
				.filter(line -> !line.isProcessed() && line.isCalcOnAllocation())
				.sorted(MLCOInvoiceWithholding::sortByInvoice)
				.toArray(MLCOInvoiceWithholding[]::new);
	}
	
	private MLCOInvoiceWithholding[] getUnprocessedLinesForTax() {
		return Arrays.stream(getLines())
				.filter(line -> !line.isProcessed() && line.isCalcOnInvoice())
				.sorted(MLCOInvoiceWithholding::sortByInvoiceIdAndTaxId)
				.toArray(MLCOInvoiceWithholding[]::new);
	}
	
	private String translateWithholdingsToTax() {
		MLCOInvoiceWithholding[] lines = getUnprocessedLinesForTax();
		return MLCOInvoiceWithholding.translateToInvoiceTax(this, lines, false, true);
	}
	
	private String allocateUnprocessedLines() {
		
		MLCOInvoiceWithholding[] lines = getUnprocessedLinesForAllocation();
		
		return MLCOInvoiceWithholding.allocateLines(lines, this, null);
	}
	
	private void setDefiniteDocumentNo() {
		MDocType dt = MDocType.get(getC_DocType_ID());
		if (dt.isOverwriteDateOnComplete()) {
			setDateTrx(TimestampUtil.today());
		}
		if (dt.isOverwriteSeqOnComplete()) {
			String value = DB.getDocumentNo(getC_DocType_ID(), get_TrxName(), true, this);
			if (value != null)
				setDocumentNo(value);
		}
	}
	
	@Override
	public boolean voidIt() {
		
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_VOID);
		
		if (!Util.isEmpty(m_processMsg, true))
			return false;
		
		setDocumentNo(getDocumentNo() + "^");
		
		m_processMsg = updateInvoiceWithholding();
		
		if (!Util.isEmpty(m_processMsg, true))
			return false;
		
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_VOID);
		
		if (!Util.isEmpty(m_processMsg, true))
			return false;
		
		setProcessed(true);
		setDocAction(ACTION_None);
		
		return true;
	}
	
	private String updateInvoiceWithholding() {
		
		StringBuilder sql = new StringBuilder("UPDATE LCO_InvoiceWithholding SET ITS_VoucherWithholding_ID = NULL")
				.append(" WHERE ITS_VoucherWithholding_ID = ?");
		
		DB.executeUpdate(sql.toString(), new Object[] {get_ID()}, false, get_TrxName());
		
		return null;
	}
	
	@Override
	public boolean closeIt() {
		
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_CLOSE);
		
		if (!Util.isEmpty(m_processMsg, true))
			return false;
		
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_CLOSE);
		
		if (!Util.isEmpty(m_processMsg, true))
			return false;
		
		setProcessed(true);
		setDocAction(ACTION_None);
		
		return true;
	}
	
	@Override
	public boolean reverseCorrectIt() {
		
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_REVERSECORRECT);
		
		if (!Util.isEmpty(m_processMsg, true))
			return false;
		
		setDocumentNo(getDocumentNo() + "^");
		
		m_processMsg = updateInvoiceWithholding();
		
		if (!Util.isEmpty(m_processMsg, true))
			return false;
		
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_REVERSECORRECT);
		
		if (!Util.isEmpty(m_processMsg, true))
			return false;
		
		return true;
	}
	
	@Override
	public boolean reverseAccrualIt() {
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_REVERSEACCRUAL);
		
		if (!Util.isEmpty(m_processMsg, true))
			return false;
		
		setDocumentNo(getDocumentNo() + "^");
		
		m_processMsg = updateInvoiceWithholding();
		
		if (!Util.isEmpty(m_processMsg, true))
			return false;
		
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_REVERSEACCRUAL);
		
		if (!Util.isEmpty(m_processMsg, true))
			return false;
		
		return true;
	}
	
	@Override
	public boolean reActivateIt() {
		
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_REACTIVATE);
		
		if (!Util.isEmpty(m_processMsg, true))
			return false;
		
		setProcessed(false);
		setIsApproved(false);
		setDocAction(ACTION_Complete);
		
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_REACTIVATE);
		
		if (!Util.isEmpty(m_processMsg, true))
			return false;
		
		return true;
	}
	
	@Override
	public String getSummary() {
		StringBuilder info = new StringBuilder(getDocumentNo())
				.append(":")
				.append("#(").append(getLines().length).append(")");
		
		return info.toString();
	}
	
	@Override
	public String getDocumentInfo() {
		
		MDocType dt = getC_DocType();
		StringBuilder info = new StringBuilder(dt.getNameTrl())
				.append(" ")
				.append(getDocumentNo());
		
		return info.toString();
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
	public MDocType getC_DocType() throws RuntimeException {
		if (m_docType == null
				|| m_docType.get_ID() != getC_DocType_ID())
			m_docType = (MDocType) super.getC_DocType();
		return m_docType;
	}
	
	public void setDocType(MDocType docType) {
		m_docType = docType;
		setC_DocType_ID(m_docType.get_ID());
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
	protected boolean beforeSave(boolean newRecord) {
		
		if ((newRecord
				|| is_ValueChanged(COLUMNNAME_C_Invoice_ID))
				&& getC_Invoice_ID() > 0)
		{
			StringBuilder sql = new StringBuilder("SELECT vw.DocumentNo")
					.append(" FROM ITS_VoucherWithholding vw")
					.append(" INNER JOIN LCO_InvoiceWithholding iw ON iw.ITS_VoucherWithholding_ID = vw.ITS_VoucherWithholding_ID")
					.append(" WHERE vw.DocStatus NOT IN ('VO', 'RE')")
					.append(" AND iw.C_Invoice_ID = ?")
					.append(" AND vw.LCO_WithholdingType_ID = ?");
			
			String docNo = DB.getSQLValueString(get_TrxName()
					, sql.toString()
					, getC_Invoice_ID()
					, getLCO_WithholdingType_ID());
			
			if (!Util.isEmpty(docNo, true))
			{
				log.saveError("Error", Msg.getMsg(getCtx(), "InvoiceInVoucher"
						, new Object[] {docNo}));
				return false;
			}
		}
		
		if (!newRecord
				&& is_ValueChanged(COLUMNNAME_C_BPartner_ID)
				&& getLines().length > 0)
		{
			log.saveError("Error", new AdempiereException("@CanNotChangeBPartner@"));
			return false;
		}
		
		if (!newRecord
				&& is_ValueChanged(COLUMNNAME_LCO_WithholdingType_ID)
				&& getLines().length > 0)
		{
			log.saveError("Error", new AdempiereException("@CanNotChangeWithholdingType@"));
			return false;
		}
		
		if (!newRecord
				&& is_ValueChanged(COLUMNNAME_C_Currency_ID)
				&& getLines().length > 0)
		{
			log.saveError("Error", new AdempiereException("@CanNotChangeCurrency@"));
			return false;
		}
		
		return true;
	}
}
