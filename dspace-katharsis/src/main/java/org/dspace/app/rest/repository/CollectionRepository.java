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
import org.dspace.app.rest.converter.CollectionConverter;
import org.dspace.app.rest.model.Collection;
import org.dspace.app.rest.model.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.katharsis.queryspec.QuerySpec;
import io.katharsis.repository.annotations.JsonApiFindAll;
import io.katharsis.repository.annotations.JsonApiResourceRepository;
import io.katharsis.resource.list.ResourceList;

@JsonApiResourceRepository(value = Collection.class)
@Component
public class CollectionRepository extends DSpaceObjectRepository<org.dspace.content.Collection, Collection> {

	@Autowired
	private CollectionConverter converter;

	protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();

	Logger log = Logger.getLogger(Item.class);

	@Override
	protected DSpaceObjectService<org.dspace.content.Collection> getContentService() {
		return collectionService;
	}

	@Override
	protected ResourceList<Collection> findAll(Context context, QuerySpec spec) throws Exception {
		// FIXME oooooh... are we really going to perform pagination and
		// filtering in memory?!!!
		Iterator<org.dspace.content.Collection> items = collectionService.findAll(context).iterator();
		List<Collection> itemResources = new ArrayList<Collection>();
		org.dspace.content.Collection obj = null;
		while (items.hasNext()) {
			obj = items.next();
			Collection item = converter.fromModel(obj);
			itemResources.add(item);
		}
		return spec.apply(itemResources);
	}

	public Class<Collection> getResourceClass() {
		return Collection.class;
	}

}
