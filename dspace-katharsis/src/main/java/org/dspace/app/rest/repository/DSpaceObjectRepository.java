/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.dspace.app.rest.converter.DSpaceObjectConverter;
import org.dspace.content.DSpaceObject;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

import io.katharsis.queryspec.QuerySpec;
import io.katharsis.resource.exception.init.ResourceIdNotFoundException;
import io.katharsis.resource.list.ResourceList;

public abstract class DSpaceObjectRepository <DSO extends DSpaceObject, DSOR extends org.dspace.app.rest.model.DSpaceObject> extends DSpaceRepository<DSOR, String> {

	@Autowired
	private  DSpaceObjectConverter<DSO, DSOR> converter;

	Logger log = Logger.getLogger(DSpaceObjectRepository.class);

	protected abstract DSpaceObjectService<DSO> getContentService();
	
//	@Override
//	public <R extends DSOR> R create(Context context, R arg0) throws Exception {
//		DSO item = converter.toModel(arg0);
//		getContentService().update(context, item);
//		DSOR result = converter.fromModel(item);
//		return (R) result;
//	}

	@Override
	protected void delete(Context context, String arg0) throws Exception {
		DSO item = item = getContentService().find(context, UUID.fromString(arg0));

		if (item == null) {
			throw new ResourceIdNotFoundException("item");
		}
		getContentService().delete(context, item);
	}

	@Override
	protected ResourceList<DSOR> findAll(Context context, Iterable<String> keys, QuerySpec spec) throws Exception {
		//TODO we should prevent users to ask for too much keys
		//DefaultResourceList<Item> items = new De
		List<DSOR> items = new ArrayList<DSOR>();
		for (String key : keys) {
			DSOR item = findOne(context, key, spec);
			items.add(item);
		}
		return spec.apply(items);
	}
	
	@Override
	protected DSOR findOne(Context context, String key, QuerySpec spec) throws Exception {
		DSO item = getContentService().find(context, UUID.fromString(key));

		if (item == null) {
			throw new ResourceIdNotFoundException("item");
		}
		
		return converter.fromModel(item);
	}
	
	@Override
	protected <S extends DSOR> S save(Context context, S resource) throws Exception {
		DSO item = converter.toModel(resource);
		getContentService().update(context, item);
		return (S) converter.fromModel(item);
	}
}
