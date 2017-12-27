package org.dspace.app.webui.cris.servlet;

import java.util.Date;

import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

public class DTOResourcePolicyData {
	private Group group;
	private EPerson eperson;
	private int action;
	private String resourcePolicyType;
	private Date startDate;
	private Date endDate;

	public DTOResourcePolicyData() {
	}

	public Group getGroup() {
		return group;
	}

	public void setGroup(Group group) {
		this.group = group;
	}

	public EPerson getEperson() {
		return eperson;
	}

	public void setEperson(EPerson eperson) {
		this.eperson = eperson;
	}

	public int getAction() {
		return action;
	}

	public void setAction(int action) {
		this.action = action;
	}

	public String getResourcePolicyType() {
		return resourcePolicyType;
	}

	public void setResourcePolicyType(String resourcePolicyType) {
		this.resourcePolicyType = resourcePolicyType;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
}