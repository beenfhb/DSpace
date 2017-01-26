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

import org.apache.log4j.Logger;
import org.dspace.app.rest.converter.BitstreamFormatConverter;
import org.dspace.app.rest.model.BitstreamFormat;
import org.dspace.content.factory.ContentServiceFactoryImpl;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.katharsis.queryspec.QuerySpec;
import io.katharsis.repository.annotations.JsonApiResourceRepository;
import io.katharsis.resource.exception.init.ResourceIdNotFoundException;
import io.katharsis.resource.list.ResourceList;

@JsonApiResourceRepository(value=BitstreamFormat.class)
@Component
public class BitstreamFormatRepository extends DSpaceRepository<BitstreamFormat, Integer> {

	@Autowired
	private  BitstreamFormatConverter converter;

	Logger log = Logger.getLogger(BitstreamFormatRepository.class);

	private BitstreamFormatService bitstreamFormatService = ContentServiceFactoryImpl.getInstance().getBitstreamFormatService();
	
//	@Override
//	public <R extends BitstreamFormat> R create(Context context, R arg0) throws Exception {
//		org.dspace.content.BitstreamFormat item = converter.toModel(arg0);
//		bitstreamFormatService.update(context, item);
//		BitstreamFormat result = converter.fromModel(item);
//		return (R) result;
//	}

	@Override
	protected void delete(Context context, Integer arg0) throws Exception {
		org.dspace.content.BitstreamFormat item = bitstreamFormatService.find(context, arg0);

		if (item == null) {
			throw new ResourceIdNotFoundException("bitstreamFormat");
		}
		bitstreamFormatService.delete(context, item);
	}

	@Override
	protected ResourceList<BitstreamFormat> findAll(Context context, Iterable<Integer> keys, QuerySpec spec) throws Exception {
		//TODO we should prevent users to ask for too much keys
		//DefaultResourceList<Item> items = new De
		List<BitstreamFormat> items = new ArrayList<BitstreamFormat>();
		for (Integer key : keys) {
			BitstreamFormat item = findOne(context, key, spec);
			items.add(item);
		}
		return spec.apply(items);
	}
	
	@Override
	protected BitstreamFormat findOne(Context context, Integer key, QuerySpec spec) throws Exception {
		org.dspace.content.BitstreamFormat item = bitstreamFormatService.find(context, key);

		if (item == null) {
			throw new ResourceIdNotFoundException("item");
		}
		
		return converter.fromModel(item);
	}
	
	@Override
	protected <S extends BitstreamFormat> S save(Context context, S resource) throws Exception {
		org.dspace.content.BitstreamFormat item = converter.toModel(resource);
		bitstreamFormatService.update(context, item);
		return (S) converter.fromModel(item);
	}

@Override
protected ResourceList<BitstreamFormat> findAll(Context context, QuerySpec spec) throws Exception {
	List<BitstreamFormat> items = new ArrayList<BitstreamFormat>();
	for (org.dspace.content.BitstreamFormat bf : bitstreamFormatService.findAll(context)) {
		BitstreamFormat item = converter.fromModel(bf);
		items.add(item);
	}
	return spec.apply(items);
}

	@Override
	protected <S extends BitstreamFormat> Class<S> getResourceClass() {
		return (Class<S>) BitstreamFormat.class;
	}
}
