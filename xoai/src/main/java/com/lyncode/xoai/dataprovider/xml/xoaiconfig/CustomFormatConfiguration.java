package com.lyncode.xoai.dataprovider.xml.xoaiconfig;

import com.lyncode.xoai.dataprovider.exceptions.WritingXmlException;
import com.lyncode.xoai.dataprovider.xml.XmlOutputContext;

public class CustomFormatConfiguration extends FormatConfigurationSuper {

	private String clazz;
	
    public String getClazz() {
		return clazz;
	}

	public CustomFormatConfiguration(String id) {
    	super(id);
    }

    public CustomFormatConfiguration withPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }


    public CustomFormatConfiguration withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public CustomFormatConfiguration withSchemaLocation(String schemaLocation) {
        this.schemaLocation = schemaLocation;
        return this;
    }

    public CustomFormatConfiguration withFilter(String filterId) {
        this.filter = new BundleReference(filterId);
        return this;
    }
    
    public  void write(XmlOutputContext writer) throws WritingXmlException{
    	validate();
    	//VSTODO: implementare
    };

    private void validate() throws WritingXmlException {

    }

	public CustomFormatConfiguration withClazz(String clazz) {
		this.clazz = clazz;
		return this;
	}    
}
