package com.lyncode.xoai.dataprovider.data.internal;

import com.lyncode.xoai.dataprovider.data.ItemIdentifier;
import com.lyncode.xoai.dataprovider.filter.conditions.Condition;

public class MetadataFormatSuper {

	protected String prefix;
	protected String namespace;
	protected String schemaLocation;
	private Condition filter;

	public MetadataFormatSuper() {
		super();
	}

	public String getPrefix() {
	    return prefix;
	}

	public String getNamespace() {
	    return namespace;
	}

	public String getSchemaLocation() {
	    return schemaLocation;
	}

	public boolean isApplicable(ItemIdentifier item) {
	    if (item.isDeleted()) return true;
	    if (hasCondition()) return getCondition().getFilter().isItemShown(item);
	    return true;
	}

	public Condition getCondition() {
	    return filter;
	}

	public boolean hasCondition() {
	    return filter != null;
	}

	public void setFilter(Condition filter) {
	    this.filter = filter;
	}

}