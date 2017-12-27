/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

public interface IMetadataValue {
		
	String getSchema();
	String getElement();
	String getQualifier();
	String getLanguage();
	String getValue();
	String getAuthority();
	int getPlace();
	int getConfidence();
	
	void setAuthority(String authority);
	void setValue(String value);
	void setConfidence(int confidence);
	void setLanguage(String lang);
	void setPlace(int place);
	
	MetadataField getMetadataField();
}
