/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package com.archimatetool.editor.ui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.archimatetool.editor.utils.StringUtils;
import com.archimatetool.model.IArchimateModelObject;
import com.archimatetool.model.IConnectable;
import com.archimatetool.model.IDiagramModelArchimateComponent;
import com.archimatetool.model.IDiagramModelNote;
import com.archimatetool.model.IDocumentable;
import com.archimatetool.model.INameable;
import com.archimatetool.model.IProperties;
import com.archimatetool.model.IProperty;
import com.archimatetool.model.ITextContent;

/**
 * Render Text for display in Text controls in diagrams
 * 
 * @author Phillip Beauvoir
 */
@SuppressWarnings("nls")
public class TextControlRenderer {
    
    public static final boolean MULTI_LINE = true;
    
    public static final String FEATURE_NAME = "labelExpression";

    public static final String NAME = "${name}";
    public static final String DOCUMENTATION = "${documentation}";
    public static final String CONTENT = "${content}";
    public static final String PROPERTIES = "${properties}";
    public static final String PROPERTIES_VALUES = "${propertiesvalues}";
    
    public static final String LINKED_DOCUMENTATION = "${linkeddoc}";
    
    Pattern PROPERTY_VALUE = Pattern.compile("\\$\\{property:([^\\}]+)\\}");
    
    private static TextControlRenderer defaultTextRenderer = new TextControlRenderer();
    
    public static TextControlRenderer getDefault() {
        return defaultTextRenderer;
    }
    
    private TextControlRenderer() {
    }
    
    /**
     * @param object The object that has the format expression string and will be rendered 
     * @return The converted text, or the empty string "" if no rendering is performed
     */
    public String render(IArchimateModelObject object) {
        // Get the format string from the object's feature
        String result = object.getFeatures().getString(FEATURE_NAME, "");
        
        if(!StringUtils.isSet(result)) {
            return "";
        }
        
        // Remove escapement of newline chars
        result = renderNewLines(result);
        
        // Get the underlying concept if there is one
        if(object instanceof IDiagramModelArchimateComponent) {
            object = ((IDiagramModelArchimateComponent)object).getArchimateConcept();
        }

        // Check for Name
        result = renderName(object, result);

        // Check for Documentation
        if(object instanceof IDocumentable) {
            result = renderDocumentation((IDocumentable)object, result);
        }
        
        // Check for Content
        if(object instanceof ITextContent) {
            result = renderTextContent((ITextContent)object, result);
        }

        // Check for Properties
        if(object instanceof IProperties) {
            result = renderProperties((IProperties)object, result);
        }
        
        // Check for linked notes
        if(object instanceof IDiagramModelNote) {
            result = renderNoteLinkedDocumentation((IDiagramModelNote)object, result);
        }
        
        return result;
    }
    
    private String renderNewLines(String result) {
        if(MULTI_LINE) {
            result = result.replace("\\n", "");
        }
        else {
            result = result.replace("\\n", "\n");
        }
        
        return result;
    }
    
    private String renderName(INameable object, String result) {
        return result.replace(NAME, object.getName());
    }
    
    private String renderDocumentation(IDocumentable object, String result) {
        return result.replace(DOCUMENTATION, object.getDocumentation());
    }

    private String renderTextContent(ITextContent object, String result) {
        return result.replace(CONTENT, object.getContent());
    }

    private String renderProperties(IProperties object, String result) {
        // Get Property Value from its key
        Matcher matcher = PROPERTY_VALUE.matcher(result);
        while(matcher.find()) {
            String key = matcher.group(1);
            String propertyValue = getPropertyValue(object, key);
            result = result.replace(matcher.group(), propertyValue);
        }
        
        // List all properties like key: value
        result = result.replace(PROPERTIES, getAllProperties(object, true));
        
        // List all properties' values
        result = result.replace(PROPERTIES_VALUES, getAllProperties(object, false));
        
        return result;
    }
    
    /**
     * If the note is connected with a connection to an ArchiMate object display its Documentation
     */
    private String renderNoteLinkedDocumentation(IDiagramModelNote note, String result) {
        if(result.contains(LINKED_DOCUMENTATION)) {
            String replacement = "";
            IConnectable other = null;
            
            // Note has at least one source connection...
            if(!note.getSourceConnections().isEmpty()) {
                other = note.getSourceConnections().get(0).getTarget();
            }
            // Or Note has at least one target connection...
            else if(!note.getTargetConnections().isEmpty()) {
                other = note.getTargetConnections().get(0).getSource();
            }
            
            // Use the other's documentation
            if(other instanceof IDiagramModelArchimateComponent) {
                replacement = ((IDiagramModelArchimateComponent)other).getArchimateConcept().getDocumentation();
            }

            result = result.replace(LINKED_DOCUMENTATION, replacement);
        }
        
        return result;
    }
    
    private String getAllProperties(IProperties object, boolean full) {
        String s = "";
        
        for(int i = 0; i < object.getProperties().size(); i++) {
            IProperty property = object.getProperties().get(i);
            
            if(full) {
                s += property.getKey() + ": ";
            }

            s += property.getValue();
            
            if(i < object.getProperties().size() - 1) {
                s += "\n";
            }
        }
        
        return s;
    }
    
    private String getPropertyValue(IProperties object, String key) {
        for(IProperty property : object.getProperties()) {
            if(property.getKey().equals(key)) {
                return property.getValue();
            }
        }
        
        return "";
    }
}
