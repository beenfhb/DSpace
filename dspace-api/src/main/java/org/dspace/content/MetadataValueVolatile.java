package org.dspace.content;

public class MetadataValueVolatile implements IMetadataValue {

	public String schema;
	public String element;
	public String qualifier;
	String authority;
	String language;
	String value;
	int confidence;
	int place;
	
	public MetadataValueVolatile() {
	}
	
	public MetadataValueVolatile(String schema, String element, String qualifier, String authority, String language) {
		this.schema = schema;
		this.element = element;
		this.qualifier = qualifier;
		this.authority = authority;
		this.language = language;
	}

	@Override
	public String getSchema() {
		return schema;
	}

	@Override
	public String getElement() {
		return element;
	}

	@Override
	public String getQualifier() {
		return qualifier;
	}

	@Override
	public String getLanguage() {
		return language;
	}

	public void setLanguage(String lang) {
		language = lang;
	}
	
	@Override
	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public void setConfidence(int confidence) {
		this.confidence = confidence;
	}

	@Override
	public void setAuthority(String authority) {
		this.authority = authority;
	}

	@Override
	public String getAuthority() {
		return this.authority;
	}

	@Override
	public int getConfidence() {
		return this.confidence;
	}

	@Override
	public String getValue() {
		return this.value;
	}

	@Override
	public MetadataField getMetadataField() {
		//TODO
		return null;
	}

	@Override
	public int getPlace() {		
		return this.place;
	}

	@Override
	public void setPlace(int place) {
		this.place = place;		
	}
}
