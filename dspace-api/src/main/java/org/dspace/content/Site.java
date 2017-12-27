/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.SiteService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Represents the root of the DSpace Archive.
 * By default, the handle suffix "0" represents the Site, e.g. "1721.1/0"
 */
@Entity
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Table(name = "site")
public class Site extends DSpaceObject implements BrowsableDSpaceObject
{

    @Transient
    private transient SiteService siteService;

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.content.service.SiteService#createSite(Context)}
     *
     */
    protected Site()
    {

    }

    /**
     * Get the type of this object, found in Constants
     *
     * @return type of the object
     */
    @Override
    public int getType()
    {
        return Constants.SITE;
    }

    @Override
    public String getName()
    {
        return getSiteService().getName(this);
    }

    public String getURL()
    {
        return ConfigurationManager.getProperty("dspace.url");
    }

    private SiteService getSiteService() {
        if(siteService == null)
        {
            siteService = ContentServiceFactory.getInstance().getSiteService();
        }
        return siteService;
    }

	@Override
	public List<String> getMetadataValue(String mdString) {
		return siteService.getAllMetadata(this, mdString);
	}

	@Override
	public List<IMetadataValue> getMetadataValueInDCFormat(String mdString) {
		return siteService.getMetadataByMetadataString(this, mdString);
	}

	@Override
	public String getTypeText() {
		// TODO Auto-generated method stub
		return Constants.typeText[getType()];
	}

	@Override
	public UUID getID() {
		return id;
	}

	@Override
	public boolean haveHierarchy() {
		return false;
	}

	@Override
	public Integer getLegacyId() {		
		return -1;
	}

	@Override
	public Map<String, Object> getExtraInfo() {
		return new HashMap<String, Object>();
	}

	@Override
	public boolean isArchived() {
		return false;
	}

	@Override
	public List<IMetadataValue> getMetadata(String schema, String element, String qualifier, String lang) {
		return siteService.getMetadata(this, schema, element, qualifier, lang);
	}

	@Override
	public String getMetadata(String field) {
		return siteService.getMetadata(this, field); 
	}

	@Override
	public boolean isDiscoverable() {
		return false;
	}

	@Override
	public String findHandle(Context context) throws SQLException {		
		return HandleServiceFactory.getInstance().getHandleService().findHandle(context, this);
	}

	@Override
	public BrowsableDSpaceObject getParentObject() {
		return null;
	}

	@Override
	public String getMetadataFirstValue(String schema, String element, String qualifier, String language) {
		return siteService.getMetadataFirstValue(this, schema, element, qualifier, language);
	}

	@Override
	public Date getLastModified() {
		return new Date();
	}
}
