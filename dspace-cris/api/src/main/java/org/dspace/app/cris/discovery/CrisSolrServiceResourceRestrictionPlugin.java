/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.cris.discovery;


import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.core.Context;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;

import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;


/**
 * 
 * @author Luigi Andrea Pascarelli
 *
 */
public class CrisSolrServiceResourceRestrictionPlugin implements CrisServiceIndexPlugin{

    private static final Logger log = Logger.getLogger(CrisSolrServiceResourceRestrictionPlugin.class);

	@Override
	public <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void additionalIndex(
			ACrisObject<P, TP, NP, NTP, ACNO, ATNO> crisObject, SolrInputDocument sorlDoc, Map<String, List<DiscoverySearchFilter>> searchFilters) {
		
		Context context = new Context();
		try {
			sorlDoc.addField("read", "g" + EPersonServiceFactory.getInstance().getGroupService().findByName(context, Group.ANONYMOUS).getID().toString());
		} catch (SQLException e) {
			log.error(e.getMessage(),e);
		}
		
	}

	@Override
	public <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void additionalIndex(
			ACNO dso, SolrInputDocument sorlDoc, Map<String, List<DiscoverySearchFilter>> searchFilters) {
		// TODO Auto-generated method stub
		
	}

}
