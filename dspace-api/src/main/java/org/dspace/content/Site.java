/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.SiteService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.hibernate.annotations.GenericGenerator;

import java.util.List;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * Represents the root of the DSpace Archive.
 * By default, the handle suffix "0" represents the Site, e.g. "1721.1/0"
 */
@Entity
@Table(name = "site")
public class Site extends DSpaceObject
{

    @Transient
    private transient SiteService siteService;

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid2")
    @Column(name = "uuid", unique = true, nullable = false, insertable = true, updatable = false)
    protected java.util.UUID id;
    
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
	public List<MetadataValue> getMetadataValueInDCFormat(String mdString) {
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

}
