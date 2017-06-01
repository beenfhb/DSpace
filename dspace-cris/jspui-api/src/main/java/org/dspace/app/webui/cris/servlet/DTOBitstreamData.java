package org.dspace.app.webui.cris.servlet;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dspace.app.webui.cris.servlet.DuplicateCheckerServlet.DTOResourcePolicy;
import org.dspace.content.BitstreamFormat;

public class DTOBitstreamData {
	private UUID id;
	private UUID itemID;
	private UUID bundleID;
	private String name;
	private String source;
	private String description;
	private BitstreamFormat format;
	private String userFormatDescription;
	private List<DTOResourcePolicy> rps;
	private InputStream is;

	public DTOBitstreamData() {
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public UUID getItemID() {
		return itemID;
	}

	public void setItemID(UUID itemID) {
		this.itemID = itemID;
	}

	public UUID getBundleID() {
		return bundleID;
	}

	public void setBundleID(UUID bundleID) {
		this.bundleID = bundleID;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public BitstreamFormat getFormat() {
		return format;
	}

	public void setFormat(BitstreamFormat format) {
		this.format = format;
	}

	public String getUserFormatDescription() {
		return userFormatDescription;
	}

	public void setUserFormatDescription(String userFormatDescription) {
		this.userFormatDescription = userFormatDescription;
	}

	public List<DTOResourcePolicy> getRps() {
		if(this.rps==null) {
			this.rps = new ArrayList<>();
		}
		return rps;
	}

	public void setRps(List<DTOResourcePolicy> rps) {
		this.rps = rps;
	}

	public InputStream getIs() {
		return is;
	}

	public void setIs(InputStream is) {
		this.is = is;
	}
}