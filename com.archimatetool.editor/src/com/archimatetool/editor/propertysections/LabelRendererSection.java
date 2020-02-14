/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package com.archimatetool.editor.propertysections;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import com.archimatetool.editor.model.commands.FeatureCommand;
import com.archimatetool.editor.ui.TextControlRenderer;
import com.archimatetool.editor.ui.components.StyledTextControl;
import com.archimatetool.model.IArchimatePackage;
import com.archimatetool.model.IDiagramModelArchimateComponent;
import com.archimatetool.model.IDiagramModelComponent;
import com.archimatetool.model.IDiagramModelGroup;
import com.archimatetool.model.IDiagramModelNote;
import com.archimatetool.model.IFeatures;



/**
 * Property Section for a Label Renderer
 * 
 * @author Phillip Beauvoir
 */
public class LabelRendererSection extends AbstractECorePropertySection {
    
    private static final String HELP_ID = "com.archimatetool.help.elementPropertySection"; //$NON-NLS-1$
    
    /**
     * Filter to show or reject this section depending on input value
     */
    public static class Filter extends ObjectFilter {
        @Override
        public boolean isRequiredType(Object object) {
            return object instanceof IDiagramModelArchimateComponent 
                    || object instanceof IDiagramModelNote
                    || object instanceof IDiagramModelGroup;
        }

        @Override
        public Class<?> getAdaptableType() {
            return IDiagramModelComponent.class;
        }
    }

    @Override
    protected void notifyChanged(Notification msg) {
        if(msg.getNotifier() == getFirstSelectedObject()) {
            Object feature = msg.getFeature();
            if(feature == IArchimatePackage.Literals.LOCKABLE__LOCKED) {
                update();
            }
        }
        
        if(isFeatureNotification(msg, TextControlRenderer.FEATURE_NAME)) {
            update();
        }
    }
    
    private PropertySectionTextControl fTextRender;

    @Override
    protected void createControls(Composite parent) {
        createLabel(parent, "Text Expression:", ITabbedLayoutConstants.STANDARD_LABEL_WIDTH, TextControlRenderer.MULTI_LINE ? SWT.NONE : SWT.CENTER);
        
        Control textControl;
        
        if(TextControlRenderer.MULTI_LINE) {
            StyledTextControl styledTextControl = createStyledTextControl(parent, SWT.NONE);
            styledTextControl.setMessage("Add an expression here");
            textControl = styledTextControl.getControl();
        }
        else {
            textControl = createSingleTextControl(parent, SWT.NONE);
            ((Text)textControl).setMessage("Add an expression here");
        }
        
        fTextRender = new PropertySectionTextControl(textControl, TextControlRenderer.FEATURE_NAME) {
            @Override
            protected void textChanged(String oldText, String newText) {
                CompoundCommand result = new CompoundCommand();
                
                for(EObject eObject : getEObjects()) {
                    if(isAlive(eObject)) {
                        Command cmd = new FeatureCommand("Set expression", (IFeatures)eObject, TextControlRenderer.FEATURE_NAME, newText, ""); //$NON-NLS-2$
                        if(cmd.canExecute()) {
                            result.add(cmd);
                        }
                    }
                }

                executeCommand(result.unwrap());
            }
        };
        
        // Help ID
        PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, HELP_ID);
    }

    @Override
    protected void update() {
        if(fIsExecutingCommand) {
            return; 
        }
        
        fTextRender.refresh(getFirstSelectedObject());
        fTextRender.setEditable(!isLocked(getFirstSelectedObject()));
    }
    
    @Override
    protected IObjectFilter getFilter() {
        return new Filter();
    }
    
    @Override
    public boolean shouldUseExtraSpace() {
        return true;
    }
}
