/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.app.rest.converter.BitstreamConverter;
import org.dspace.app.rest.model.Bitstream;
import org.dspace.app.rest.model.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.katharsis.queryspec.QuerySpec;
import io.katharsis.repository.annotations.JsonApiResourceRepository;
import io.katharsis.resource.list.ResourceList;

@JsonApiResourceRepository(value = Bitstream.class)
@Component
public class BitstreamRepository extends DSpaceObjectRepository<org.dspace.content.Bitstream, Bitstream> {

	@Autowired
	private BitstreamConverter converter;

	protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();

	Logger log = Logger.getLogger(Item.class);

	@Override
	protected DSpaceObjectService<org.dspace.content.Bitstream> getContentService() {
		return bitstreamService;
	}

	@Override
	protected ResourceList<Bitstream> findAll(Context context, QuerySpec spec) throws Exception {
		// FIXME oooooh... are we really going to perform pagination and
		// filtering in memory?!!!
		Iterator<org.dspace.content.Bitstream> items = bitstreamService.findAll(context).iterator();
		List<Bitstream> itemResources = new ArrayList<Bitstream>();
		org.dspace.content.Bitstream obj = null;
		while (items.hasNext()) {
			obj = items.next();
			Bitstream item = converter.fromModel(obj);
			itemResources.add(item);
		}
		return spec.apply(itemResources);
	}

	public Class<Bitstream> getResourceClass() {
		return Bitstream.class;
	}

}
