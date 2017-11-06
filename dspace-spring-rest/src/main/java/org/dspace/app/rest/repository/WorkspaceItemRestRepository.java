/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.converter.WorkspaceItemConverter;
import org.dspace.app.rest.model.WorkspaceItemRest;
import org.dspace.app.rest.model.hateoas.WorkspaceItemResource;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage WorkspaceItem Rest object
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */

@Component(WorkspaceItemRest.CATEGORY + "." + WorkspaceItemRest.NAME)
public class WorkspaceItemRestRepository extends DSpaceRestRepository<WorkspaceItemRest, Integer> {

	@Autowired
	WorkspaceItemService wis;
	
	@Autowired
	WorkspaceItemConverter converter;
	
	
	public WorkspaceItemRestRepository() {
	}

	@Override
	public WorkspaceItemRest findOne(Context context, Integer id) {
		WorkspaceItem witem = null;
		try {
			witem = wis.find(context, id);
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		if (witem == null) {
			return null;
		}
		return converter.fromModel(witem);
	}

	@Override
	public Page<WorkspaceItemRest> findAll(Context context, Pageable pageable) {
		List<WorkspaceItem> witems = null;
		int total = 0;
		try {
			total = wis.countTotal(context);
			witems = wis.findAll(context, pageable.getPageSize(), pageable.getOffset());
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		Page<WorkspaceItemRest> page = new PageImpl<WorkspaceItem>(witems, pageable, total).map(converter);
		return page;
	}
	
	@SearchRestMethod(name = "findBySubmitter")
	public Page<WorkspaceItemRest> findBySubmitter(Context context,@Param(value="uuid") UUID submitterID, Pageable pageable) {
		//TODO
		return null;
	}
	
	@Override
	public Class<WorkspaceItemRest> getDomainClass() {
		return WorkspaceItemRest.class;
	}
	
	@Override
	public WorkspaceItemResource wrapResource(WorkspaceItemRest witem, String... rels) {
		return new WorkspaceItemResource(witem, utils, rels);
	}

}