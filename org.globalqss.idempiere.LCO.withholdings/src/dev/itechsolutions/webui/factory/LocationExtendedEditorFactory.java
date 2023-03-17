package dev.itechsolutions.webui.factory;

import org.adempiere.webui.editor.IEditorConfiguration;
import org.adempiere.webui.editor.WEditor;
import org.adempiere.webui.factory.IEditorFactory;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;

import dev.itechsolutions.webui.editor.WLocationEditorExt;
import static dev.itechsolutions.webui.factory.LocationExtendedDisplayFactory.LOCATION_EXTENDED_ID;

/**
 * 
 * @author Argenis Rodríguez
 *
 */
public class LocationExtendedEditorFactory implements IEditorFactory {
	
	@Override
	public WEditor getEditor(GridTab gridTab, GridField gridField, boolean tableEditor) {
		return getEditor(gridTab, gridField, tableEditor, null);
	}
	
	@Override
	public WEditor getEditor(GridTab gridTab, GridField gridField, boolean tableEditor,
			IEditorConfiguration editorConfiguration) {
		
		if (gridField == null)
			return null;
		
		int displayType = gridField.getDisplayType();
		
		if (gridField.isHeading())
			return null;
		
		if (displayType == LOCATION_EXTENDED_ID)
			return new WLocationEditorExt(gridField, tableEditor, editorConfiguration);
		
		return null;
	}
}
