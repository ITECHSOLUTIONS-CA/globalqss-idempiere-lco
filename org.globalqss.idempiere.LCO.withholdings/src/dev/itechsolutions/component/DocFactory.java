package dev.itechsolutions.component;

import org.compiere.acct.ITSDoc_AllocationHdr;
import org.compiere.model.MAllocationHdr;

import dev.itechsolutions.base.ITSDocFactory;

/**
 * 
 * @author Argenis Rodríguez
 *
 */
public class DocFactory extends ITSDocFactory {
	
	@Override
	public void initialize() {
		addDocument(MAllocationHdr.Table_Name, ITSDoc_AllocationHdr.class);
	}
}
