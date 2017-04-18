/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.solr.common.SolrDocument;

import com.lyncode.xoai.dataprovider.core.ItemMetadata;
import com.lyncode.xoai.dataprovider.core.ReferenceSet;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
public class DSpaceSolrItem extends DSpaceItem
{
    private static Logger log = LogManager
            .getLogger(DSpaceSolrItem.class);
    
    private String unparsedMD;
    private ItemMetadata metadata;
    private String handle;
    private Date lastMod;
    private List<ReferenceSet> sets;
    private boolean deleted;
    private String cerifEntity;
    private String oaiIdentifier;
    private boolean isCerif;
    
    public DSpaceSolrItem (SolrDocument doc) {
    	log.debug("Creating OAI Item from Solr source");
        unparsedMD = (String) doc.getFieldValue("item.compile");
        handle = (String) doc.getFieldValue("item.handle");
        lastMod = (Date) doc.getFieldValue("item.lastmodified");
        //VSTODO: aggiungere i set cerif
        sets = new ArrayList<ReferenceSet>();
        if(doc.getFieldValues("item.communities") != null)
        for (Object obj : doc.getFieldValues("item.communities"))
            sets.add(new ReferenceSet((String) obj));
        if(doc.getFieldValues("item.collections") != null)
        for (Object obj : doc.getFieldValues("item.collections"))
            sets.add(new ReferenceSet((String) obj));
        deleted = (Boolean) doc.getFieldValue("item.deleted");
        cerifEntity = (String)doc.getFieldValue("item.cerifEntity");
        oaiIdentifier = (String)doc.getFieldValue("item.identifier");
        isCerif = (Boolean)doc.getFieldValue("item.isCerif");
    }

    @Override
    public ItemMetadata getMetadata()
    {
        if (metadata == null) {
            metadata = new ItemMetadata(unparsedMD);
        }
        return metadata;
    }

    @Override
    public Date getDatestamp()
    {
        return lastMod;
    }

    @Override
    public List<ReferenceSet> getSets()
    {
        return sets;
    }

    @Override
    public boolean isDeleted()
    {
        return deleted;
    }

    @Override
    protected String getHandle()
    {
        return handle;
    }

	public String getCerifEntity() {
		return cerifEntity;
	}


	
	//VSTODO: implementare buildIdentifier e parseHandle per DspaceSolrItem per tener conto delle entità cerif (SERVE??)
	
    public static String parseHandle (String oaiIdentifier) {
    	String[] parts = oaiIdentifier.split(Pattern.quote(":"));
    	if (parts.length > 0) return parts[parts.length - 1];
    	else return null; // Contract
    }

    @Override
	public String getIdentifier() {
		return oaiIdentifier;
	}

	public boolean isCerif() {
		return isCerif;
	}

	public void setCerif(boolean isCerif) {
		this.isCerif = isCerif;
	}

}
