/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Date;
import java.util.List;

import io.katharsis.resource.annotations.JsonApiResource;
import io.katharsis.resource.annotations.JsonApiToMany;
import io.katharsis.resource.annotations.JsonApiToOne;

@JsonApiResource(type = "items")
public class Item extends DSpaceObject {
	boolean isWithdrawn;
	Date lastModified;
	@JsonApiToOne(opposite="true")
	Collection isTemplateOfCollection;
	
	@JsonApiToOne(opposite="true")
	Collection owningCollection;

	@JsonApiToMany(opposite="true")
	List<Collection> mappedCollections;
	
	@JsonApiToMany
	List<Bitstream> bitstreams;

	public boolean isWithdrawn() {
		return isWithdrawn;
	}

	public void setWithdrawn(boolean isWithdrawn) {
		this.isWithdrawn = isWithdrawn;
	}

	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	public Collection getOwningCollection() {
		return owningCollection;
	}

	public void setOwningCollection(Collection owningCollection) {
		this.owningCollection = owningCollection;
	}

	public List<Collection> getMappedCollections() {
		return mappedCollections;
	}

	public void setMappedCollections(List<Collection> mappedCollections) {
		this.mappedCollections = mappedCollections;
	}

	public List<Bitstream> getBitstreams() {
		return bitstreams;
	}

	public void setBitstreams(List<Bitstream> bitstreams) {
		this.bitstreams = bitstreams;
	}
	
	public Collection getIsTemplateOfCollection() {
		return isTemplateOfCollection;
	}
	
	public void setIsTemplateOfCollection(Collection isTemplateOfCollection) {
		this.isTemplateOfCollection = isTemplateOfCollection;
	}
}
