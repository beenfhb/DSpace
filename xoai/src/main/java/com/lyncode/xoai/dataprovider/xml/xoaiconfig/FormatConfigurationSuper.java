package com.lyncode.xoai.dataprovider.xml.xoaiconfig;

import com.lyncode.xoai.dataprovider.exceptions.WritingXmlException;
import com.lyncode.xoai.dataprovider.xml.XMLWritable;
import com.lyncode.xoai.dataprovider.xml.XmlOutputContext;

public abstract class FormatConfigurationSuper {

	protected String prefix;
	protected String namespace;
	protected String schemaLocation;
	protected BundleReference filter;
	protected String id;

    public FormatConfigurationSuper(String id) {
        this.id = id;
    }

	public String getPrefix() {
	    return prefix;
	}

	public BundleReference getFilter() {
	    return filter;
	}

	public boolean hasFilter() {
	    return filter != null;
	}

	public String getId() {
	    return id;
	}

	public String getSchemaLocation() {
	    return schemaLocation;
	}

	public String getNamespace() {
	    return namespace;
	}

    public abstract void write(XmlOutputContext writer) throws WritingXmlException;    
 
}