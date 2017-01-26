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
import org.dspace.app.rest.converter.ItemConverter;
import org.dspace.app.rest.model.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.katharsis.queryspec.QuerySpec;
import io.katharsis.repository.annotations.JsonApiResourceRepository;
import io.katharsis.resource.list.ResourceList;

@JsonApiResourceRepository(value = Item.class)
@Component
public class ItemRepository extends DSpaceObjectRepository<org.dspace.content.Item, Item> {

	@Autowired
	private ItemConverter converter;

	protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();

	Logger log = Logger.getLogger(Item.class);

	@Override
	protected DSpaceObjectService<org.dspace.content.Item> getContentService() {
		return itemService;
	}

	@Override
	protected ResourceList<Item> findAll(Context context, QuerySpec spec) throws Exception {
		// FIXME oooooh... are we really going to perform pagination and
		// filtering in memory?!!!
		Iterator<org.dspace.content.Item> items = itemService.findAllUnfiltered(context);
		List<Item> itemResources = new ArrayList<Item>();
		org.dspace.content.Item obj = null;
		while (items.hasNext()) {
			obj = items.next();
			Item item = converter.fromModel(obj);
			itemResources.add(item);
		}
		return spec.apply(itemResources);
	}

	public Class<Item> getResourceClass() {
		return Item.class;
	}

}
