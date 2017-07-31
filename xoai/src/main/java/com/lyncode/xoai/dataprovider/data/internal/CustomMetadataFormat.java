package com.lyncode.xoai.dataprovider.data.internal;

import com.lyncode.xoai.dataprovider.data.ICustomFormatter;
import com.lyncode.xoai.dataprovider.xml.xoaiconfig.parameters.ParameterMap;

public class CustomMetadataFormat extends MetadataFormatSuper {
	private Class<? extends ICustomFormatter > customClass; 
	private ParameterMap configuration;

	public CustomMetadataFormat(String prefix, Class<? extends ICustomFormatter>  customClass, ParameterMap configuration, String namespace, String schemaLocation) {
        this.prefix = prefix;
        this.customClass = customClass;
        this.namespace = namespace;
        this.schemaLocation = schemaLocation;
        this.configuration = configuration;
    }	
	
    public Class<? extends ICustomFormatter> getCustomClass() {
		return customClass;
	}

	public ParameterMap getConfiguration() {
		return configuration;
	}
	
	
}
