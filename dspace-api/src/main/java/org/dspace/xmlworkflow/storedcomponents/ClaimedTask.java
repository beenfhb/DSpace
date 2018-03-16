/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents;

import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.content.IMetadataValue;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;
import org.dspace.eperson.EPerson;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.*;

/**
 * Claimed task representing the database representation of an action claimed by an eperson
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
@Entity
@Table(name="cwf_claimtask")
public class ClaimedTask implements ReloadableEntity<Integer>, BrowsableDSpaceObject<Integer> {

    @Transient
    public transient Map<String, Object> extraInfo = new HashMap<String, Object>();
    
    @Id
    @Column(name="claimtask_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE ,generator="cwf_claimtask_seq")
    @SequenceGenerator(name="cwf_claimtask_seq", sequenceName="cwf_claimtask_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflowitem_id")
    private XmlWorkflowItem workflowItem;

//    @Column(name = "workflow_id")
//    @Lob
    @Column(name="workflow_id", columnDefinition = "text")
    private String workflowId;

//    @Column(name = "step_id")
//    @Lob
    @Column(name="step_id", columnDefinition = "text")
    private String stepId;

//    @Column(name = "action_id")
//    @Lob
    @Column(name="action_id", columnDefinition = "text")
    private String actionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private EPerson owner;

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService#create(Context)}
     *
     */
    protected ClaimedTask()
    {

    }

    public Integer getID() {
        return id;
    }

    public void setOwner(EPerson owner){
        this.owner = owner;
    }

    public EPerson getOwner(){
        return owner;
    }

    public void setWorkflowItem(XmlWorkflowItem workflowItem){
        this.workflowItem = workflowItem;
    }

    public XmlWorkflowItem getWorkflowItem(){
        return workflowItem;
    }

    public void setActionID(String actionID){
        this.actionId = actionID;
    }

    public String getActionID(){
        return actionId;
    }

    public void setStepID(String stepID){
        this.stepId = stepID;
    }

    public String getStepID(){
        return stepId;
    }

    public void setWorkflowID(String workflowID){
        this.workflowId = workflowID;
    }

    public String getWorkflowID(){
        return workflowId;
    }
    
	@Override
	public String getHandle() {
		return null;
	}

	@Override
	public List<String> getMetadataValue(String mdString) {
		return workflowItem.getItem().getMetadataValue(mdString);
	}

	@Override
	public List<IMetadataValue> getMetadataValueInDCFormat(String mdString) {
		return workflowItem.getItem().getMetadataValueInDCFormat(mdString);
	}

	@Override
	public String getTypeText() {
		return "claimedtask";
	}

	@Override
	public int getType() {
		return Constants.WORKFLOW_CLAIMED;
	}

	@Override
	public boolean isWithdrawn() {
		return false;
	}

	@Override
	public Map<String, Object> getExtraInfo() {
		return extraInfo;
	}

	@Override
	public boolean isArchived() {
		return false;
	}

	@Override
	public List<IMetadataValue> getMetadata(String schema, String element, String qualifier, String lang) {
		return workflowItem.getItem().getMetadata(schema, element, qualifier, lang);
	}

	@Override
	public List<IMetadataValue> getMetadata() {
		return workflowItem.getItem().getMetadata();
	}

	@Override
	public String getMetadata(String field) {
		return workflowItem.getItem().getMetadata(field);
	}

	@Override
	public boolean isDiscoverable() {
		return false;
	}

	@Override
	public String getName() {
		return workflowItem.getItem().getName();
	}

	@Override
	public String findHandle(Context context) throws SQLException {
		return null;
	}

	@Override
	public boolean haveHierarchy() {
		return false;
	}

	@Override
	public BrowsableDSpaceObject getParentObject() {
		return getWorkflowItem();
	}

	@Override
	public String getMetadataFirstValue(String schema, String element, String qualifier, String language) {
		return workflowItem.getItem().getMetadataFirstValue(schema, element, qualifier, language);
	}

	@Override
	public Date getLastModified() {
		return workflowItem.getItem().getLastModified();
	}
}
