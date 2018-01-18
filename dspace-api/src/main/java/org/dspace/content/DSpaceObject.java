/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.dspace.authorize.AuthorizableEntity;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;
import org.dspace.discovery.IGlobalSearchResult;
import org.dspace.handle.Handle;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;

/**
 * Abstract base class for DSpace objects
 */
@Entity
@Inheritance(strategy= InheritanceType.JOINED)
@Table(name = "dspaceobject")
public abstract class DSpaceObject implements Serializable, ReloadableEntity<java.util.UUID>, IGlobalSearchResult, UsageEventEntity, AuthorizableEntity, RootObject
{
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid2")
    @Column(name = "uuid", unique = true, nullable = false, insertable = true, updatable = false)    
    protected java.util.UUID id;
    
    // accumulate information to add to "detail" element of content Event,
    // e.g. to document metadata fields touched, etc.
    @Transient
    private StringBuffer eventDetails = null;
    
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "dSpaceObject", cascade = CascadeType.ALL, orphanRemoval = true, targetEntity = MetadataValue.class)
    @OrderBy("metadataField, place")
    private List<IMetadataValue> metadata = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "dso")
    // Order by is here to ensure that the oldest handle is retrieved first,
    // multiple handles are assigned to the latest version of an item the original handle will have the lowest identifier
    // This handle is the preferred handle.
    @OrderBy("id ASC")
    private List<Handle> handles = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "dSpaceObject", cascade = CascadeType.ALL)
    private List<ResourcePolicy> resourcePolicies = new ArrayList<>();

    /**
     * True if anything else was changed since last update()
     * (to drive event mechanism)
     */
    @Transient
    private boolean modifiedMetadata = false;

    /** Flag set when data is modified, for events */
    @Transient
    private boolean modified = false;

    protected DSpaceObject()
    {

    }

    /**
     * Reset the cache of event details.
     */
    public void clearDetails()
    {
        eventDetails = null;
    }

    /**
     * Add a string to the cache of event details.  Automatically
     * separates entries with a comma.
     * Subclass can just start calling addDetails, since it creates
     * the cache if it needs to.
     * @param d detail string to add.
     */
    protected void addDetails(String d)
    {
        if (eventDetails == null)
        {
            eventDetails = new StringBuffer(d);
        }
        else
        {
            eventDetails.append(", ").append(d);
        }
    }

    /**
     * @return summary of event details, or null if there are none.
     */
    public String getDetails()
    {
        return (eventDetails == null ? null : eventDetails.toString());
    }

    /**
     * Get the type of this object, found in Constants
     * 
     * @return type of the object
     */
    public abstract int getType();

    /**
     * Get the internal ID (database primary key) of this object
     * 
     * @return internal ID of object
     */
    public UUID getID(){
        return id;
    }

    public abstract String getName();

    /**
     * Get the Handle of the object. This may return <code>null</code>
     * 
     * @return Handle of the object, or <code>null</code> if it doesn't have
     *         one
     */
    public String getHandle()
    {
        return (CollectionUtils.isNotEmpty(handles) ? handles.get(0).getHandle() : null);
    }

    void setHandle(List<Handle> handle)
    {
        this.handles = handle;
    }

    public void addHandle(Handle handle)
    {
        this.handles.add(handle);
    }

    public List<Handle> getHandles() {
        return handles;
    }

    public List<IMetadataValue> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<IMetadataValue> metadata) {
        this.metadata = metadata;
    }

    protected void removeMetadata(IMetadataValue metadataValue)
    {
        setMetadataModified();
        getMetadata().remove(metadataValue);
    }

    protected void removeMetadata(List<IMetadataValue> metadataValues)
    {
        setMetadataModified();
        getMetadata().removeAll(metadataValues);
    }

    public List<IMetadataValue> getMetadataWithoutPlaceholder(String schema, String element, String qualifier,
            String lang) {
    	return getDSpaceObjectService().getMetadataWithoutPlaceholder(this, schema, element, qualifier, lang);
    }

    /**
     * Retrieve first metadata field value
     */
    protected String getMetadataFirstValue(String schema, String element, String qualifier, String language){
        return getDSpaceObjectService().getMetadataFirstValue(this, schema, element, qualifier, language);
    }

    protected void addMetadata(MetadataValue metadataValue) {
        setMetadataModified();
        getMetadata().add(metadataValue);
        addDetails(metadataValue.getMetadataField().toString());
    }

    public List<ResourcePolicy> getResourcePolicies() {
        return resourcePolicies;
    }

    public boolean isMetadataModified() {
        return modifiedMetadata;
    }

    protected void setMetadataModified() {
        this.modifiedMetadata = true;
    }

    public boolean isModified() {
        return modified;
    }
    public void clearModified() {
        this.modified = false;
    }
    protected void setModified() {
        this.modified = true;
    }
    
	@Override
	public boolean isWithdrawn() {	
		return false;
	}

	@Override
	public List<String> getMetadataValue(String mdString) {
		return getDSpaceObjectService().getAllMetadata(this, mdString);
	}

	@Override
	public List<IMetadataValue> getMetadataValueInDCFormat(String mdString) {
		return getDSpaceObjectService().getMetadataByMetadataString(this, mdString);
	}
	
	public void clearMetadata(Context context, String schema, String element, String qualifier, String lang) throws SQLException {
		getDSpaceObjectService().clearMetadata(context, this, schema, element, qualifier, lang);
	}

	public static DSpaceObject find(Context context, int resourceTypeID, UUID internalID) throws SQLException {
		RootObject dso = ContentServiceFactory.getInstance().getDSpaceObjectService(resourceTypeID).find(context, internalID);
		return (DSpaceObject)dso;
	}

	public DSpaceObjectService<DSpaceObject> getDSpaceObjectService() {
		return ContentServiceFactory.getInstance().getDSpaceObjectService(this);
	}
	
	public static final Object unwrapProxy(Object bean) throws Exception {
		
		/*
		 * If the given object is a proxy, set the return value as the object
		 * being proxied, otherwise return the given object.
		 */
		if (AopUtils.isAopProxy(bean) && bean instanceof Advised) {
			
			Advised advised = (Advised) bean;
			
			bean = advised.getTargetSource().getTarget();
		}
		
		return bean;
	}
}