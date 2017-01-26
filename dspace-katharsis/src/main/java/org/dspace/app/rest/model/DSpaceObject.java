/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.io.Serializable;
import java.util.List;

import io.katharsis.resource.annotations.JsonApiId;

public class DSpaceObject implements Serializable {
	@JsonApiId
    private String uuid;

    private String name;
    private String handle;
    private String type;

    List<MetadataEntry> metadata;

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getHandle() {
		return handle;
	}

	public void setHandle(String handle) {
		this.handle = handle;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List<MetadataEntry> getMetadata() {
		return metadata;
	}

	public void setMetadata(List<MetadataEntry> metadata) {
		this.metadata = metadata;
	}
    
}
