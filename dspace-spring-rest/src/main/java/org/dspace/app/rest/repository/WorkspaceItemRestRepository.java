/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.converter.WorkspaceItemConverter;
import org.dspace.app.rest.exception.PatchBadRequestException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.ErrorRest;
import org.dspace.app.rest.model.WorkspaceItemRest;
import org.dspace.app.rest.model.hateoas.WorkspaceItemResource;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.submit.AbstractRestProcessingStep;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.rest.submit.UploadableStep;
import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.EPersonServiceImpl;
import org.dspace.services.ConfigurationService;
import org.dspace.services.model.Request;
import org.dspace.submit.AbstractProcessingStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * This is the repository responsible to manage WorkspaceItem Rest object
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */

@Component(WorkspaceItemRest.CATEGORY + "." + WorkspaceItemRest.NAME)
public class WorkspaceItemRestRepository extends DSpaceRestRepository<WorkspaceItemRest, Integer> {

	public static final String OPERATION_PATH_SECTIONS = "sections";
	
	private static final Logger log = Logger.getLogger(WorkspaceItemRestRepository.class);
	
	@Autowired
	WorkspaceItemService wis;
	@Autowired
	ItemService itemService;
	@Autowired
	BitstreamService bitstreamService;
	@Autowired
	BitstreamFormatService bitstreamFormatService;
	@Autowired
	ConfigurationService configurationService;
	
	@Autowired
	WorkspaceItemConverter converter;
	
	@Autowired
	SubmissionService submissionService;
	@Autowired
	EPersonServiceImpl epersonService;
	
	private SubmissionConfigReader submissionConfigReader;
	
	public WorkspaceItemRestRepository() throws SubmissionConfigReaderException {
		submissionConfigReader = new SubmissionConfigReader();
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
	public Page<WorkspaceItemRest> findBySubmitter(@Param(value="uuid") UUID submitterID, Pageable pageable) {
		List<WorkspaceItem> witems = null;
		int total = 0;
		try {		
			Context context = obtainContext();
			EPerson ep = epersonService.find(context, submitterID); 
			witems = wis.findByEPerson(context, ep);
			total = witems.size();
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		Page<WorkspaceItemRest> page = new PageImpl<WorkspaceItem>(witems, pageable, total).map(converter);
		return page;
	}
	
	@Override
	protected WorkspaceItemRest createAndReturn(Context context) {
		WorkspaceItem source = submissionService.createWorkspaceItem(context, getRequestService().getCurrentRequest());		
		return converter.convert(source);		
	}

	@Override
	protected WorkspaceItemRest save(Context context, WorkspaceItemRest wsi) {
		SubmissionConfig submissionConfig = submissionConfigReader.getSubmissionConfigByName(submissionConfigReader.getDefaultSubmissionConfigName());
		WorkspaceItem source = converter.toModel(wsi);
		for(int stepNum = 0; stepNum<submissionConfig.getNumberOfSteps(); stepNum++) {
			
			SubmissionStepConfig stepConfig = submissionConfig.getStep(stepNum);
			/*
			 * First, load the step processing class (using the current
			 * class loader)
			 */
			ClassLoader loader = this.getClass().getClassLoader();
			Class stepClass;
			try {
				stepClass = loader.loadClass(stepConfig.getProcessingClassName());

				Object stepInstance = stepClass.newInstance();

				if (stepInstance instanceof AbstractProcessingStep) {
					// load the JSPStep interface for this step
					AbstractProcessingStep stepProcessing = (AbstractProcessingStep) stepClass
							.newInstance();
					stepProcessing.doPreProcessing(context, source);
				} else {
					throw new Exception("The submission step class specified by '"
							+ stepConfig.getProcessingClassName()
							+ "' does not extend the class org.dspace.submit.AbstractProcessingStep!"
							+ " Therefore it cannot be used by the Configurable Submission as the <processing-class>!");
				}

			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		submissionService.saveWorkspaceItem(context, source);
		return wsi;		
	}
	
	@Override
	public Class<WorkspaceItemRest> getDomainClass() {
		return WorkspaceItemRest.class;
	}
	
	@Override
	public WorkspaceItemResource wrapResource(WorkspaceItemRest witem, String... rels) {
		return new WorkspaceItemResource(witem, utils, rels);
	}
	
	@Override
	public WorkspaceItemRest upload(HttpServletRequest request, String apiCategory, String model, Integer id,
			String extraField, MultipartFile file) throws Exception {
		
		Context context = obtainContext();
		WorkspaceItemRest wsi = findOne(id);
		WorkspaceItem source = wis.find(context, id);
		List<ErrorRest> errors = new ArrayList<ErrorRest>();
			SubmissionConfig submissionConfig = submissionConfigReader.getSubmissionConfigByName(wsi.getSubmissionDefinition().getName());			
			for (int i = 0; i<submissionConfig.getNumberOfSteps(); i++) {
				SubmissionStepConfig stepConfig = submissionConfig.getStep(i);

				/*
				 * First, load the step processing class (using the current
				 * class loader)
				 */
				ClassLoader loader = this.getClass().getClassLoader();
				Class stepClass;
				try {
					stepClass = loader.loadClass(stepConfig.getProcessingClassName());

					Object stepInstance = stepClass.newInstance();
					if (UploadableStep.class.isAssignableFrom(stepClass)) {
						UploadableStep uploadableStep = (UploadableStep) stepInstance;
						uploadableStep.doPreProcessing(context, source);
						ErrorRest err = uploadableStep.upload(context, submissionService, stepConfig, source, file, extraField);
						uploadableStep.doPostProcessing(context, source);
						if(err!=null) {
							errors.add(err);
						}
					} 
					
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}

			}
		wsi = converter.convert(source);
		
		if(errors.isEmpty()) {
			wsi.setStatus(true);	
		}
		else {
			wsi.setStatus(false);
			wsi.getErrors().addAll(errors);
		}
		
		context.commit();
		return wsi;
	}

	@Override
	public void patch(Context context, HttpServletRequest request, String apiCategory, String model, Integer id, Patch patch) throws SQLException, AuthorizeException {
		List<Operation> operations = patch.getOperations();
		WorkspaceItemRest wsi = findOne(id);
		WorkspaceItem source = wis.find(context, id);
		for(Operation op : operations) {
			//the value in the position 0 is a null value
			String[] path = op.getPath().substring(1).split("/",3);
			if(OPERATION_PATH_SECTIONS.equals(path[0])) {
				String section = path[1];
				evaluatePatch(context, request, source, wsi, section, op);
			}
			else {
				throw new PatchBadRequestException("Patch path operation need to starts with '" + OPERATION_PATH_SECTIONS + "'");
			}
		}
		wis.update(context, source);
	}

	private void evaluatePatch(Context context, HttpServletRequest request, WorkspaceItem source, WorkspaceItemRest wsi, String section, Operation op) {
		SubmissionConfig submissionConfig = submissionConfigReader.getSubmissionConfigByName(wsi.getSubmissionDefinition().getName());
		for(int stepNum = 0; stepNum<submissionConfig.getNumberOfSteps(); stepNum++) {
			
			SubmissionStepConfig stepConfig = submissionConfig.getStep(stepNum);
			
			if (section.equals(stepConfig.getId())) {
				/*
				 * First, load the step processing class (using the current
				 * class loader)
				 */
				ClassLoader loader = this.getClass().getClassLoader();
				Class stepClass;
				try {
					stepClass = loader.loadClass(stepConfig.getProcessingClassName());

					Object stepInstance = stepClass.newInstance();

					if (stepInstance instanceof AbstractRestProcessingStep) {
						// load the JSPStep interface for this step
						AbstractRestProcessingStep stepProcessing = (AbstractRestProcessingStep) stepClass
								.newInstance();
						stepProcessing.doPreProcessing(context, source);
						stepProcessing.doPatchProcessing(context, getRequestService().getCurrentRequest(), source, op);
						stepProcessing.doPostProcessing(context, source);
					} else {
						throw new PatchBadRequestException("The submission step class specified by '"
								+ stepConfig.getProcessingClassName()
								+ "' does not extend the class org.dspace.submit.AbstractProcessingStep!"
								+ " Therefore it cannot be used by the Configurable Submission as the <processing-class>!");
					}

				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		}
	}

	@Override
	protected void delete(Context context, Integer id) throws RepositoryMethodNotImplementedException {
		WorkspaceItem witem = null;
		try {
			witem = wis.find(context, id);
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		try {
			wis.deleteAll(context, witem);
		} catch (SQLException | AuthorizeException | IOException e) {
			log.error(e.getMessage(), e);
		}
	}
}