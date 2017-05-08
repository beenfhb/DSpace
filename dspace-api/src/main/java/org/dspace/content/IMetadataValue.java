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
