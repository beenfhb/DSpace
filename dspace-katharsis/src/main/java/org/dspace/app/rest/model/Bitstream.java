/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import io.katharsis.resource.annotations.JsonApiIncludeByDefault;
import io.katharsis.resource.annotations.JsonApiResource;
import io.katharsis.resource.annotations.JsonApiToOne;

@JsonApiResource(type = "bitstreams")
public class Bitstream extends DSpaceObject {

	private String bundleName;
	@JsonApiToOne
	private BitstreamFormat format;
	private Long sizeBytes;
	@JsonApiToOne
	private Item parentObject;
	private String retrieveLink;
	private CheckSum checkSum;
	private Integer sequenceId;

	public String getBundleName() {
		return bundleName;
	}

	public void setBundleName(String bundleName) {
		this.bundleName = bundleName;
	}

	public BitstreamFormat getFormat() {
		return format;
	}

	public void setFormat(BitstreamFormat format) {
		this.format = format;
	}

	public Long getSizeBytes() {
		return sizeBytes;
	}

	public void setSizeBytes(Long sizeBytes) {
		this.sizeBytes = sizeBytes;
	}

	public Item getParentObject() {
		return parentObject;
	}

	public void setParentObject(Item parentObject) {
		this.parentObject = parentObject;
	}

	public String getRetrieveLink() {
		return retrieveLink;
	}

	public void setRetrieveLink(String retrieveLink) {
		this.retrieveLink = retrieveLink;
	}

	public CheckSum getCheckSum() {
		return checkSum;
	}

	public void setCheckSum(CheckSum checkSum) {
		this.checkSum = checkSum;
	}

	public Integer getSequenceId() {
		return sequenceId;
	}

	public void setSequenceId(Integer sequenceId) {
		this.sequenceId = sequenceId;
	}

}