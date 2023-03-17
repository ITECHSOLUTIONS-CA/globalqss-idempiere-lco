package dev.itechsolutions.webui.factory;

import org.adempiere.base.ILookupFactory;
import org.compiere.model.GridFieldVO;
import org.compiere.model.InfoColumnVO;
import org.compiere.model.Lookup;

import dev.itechsolutions.model.MLocationLookupExt;
import static dev.itechsolutions.webui.factory.LocationExtendedDisplayFactory.LOCATION_EXTENDED_ID;

/**
 * 
 * @author Argenis Rodríguez
 *
 */
public class LocationExtendedLookupFactory implements ILookupFactory {
	
	@Override
	public Lookup getLookup(GridFieldVO gridFieldVO) {
		
		if (gridFieldVO.displayType == LOCATION_EXTENDED_ID)
			return new MLocationLookupExt(gridFieldVO.ctx, gridFieldVO.WindowNo);
		
		return null;
	}
	
	@Override
	public boolean isLookup(GridFieldVO gridFieldVO) {
		if (gridFieldVO.displayType == LOCATION_EXTENDED_ID)
			return true;
		
		return false;
	}
	
	@Override
	public boolean isLookup(InfoColumnVO infoColumnVO) {
		int displayType = infoColumnVO.getAD_Reference_ID();
		
		if (displayType == LOCATION_EXTENDED_ID)
			return true;
		
		return false;
	}
}
