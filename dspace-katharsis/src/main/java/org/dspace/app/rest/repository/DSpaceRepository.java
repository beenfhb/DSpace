/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.Serializable;

import org.apache.log4j.Logger;
import org.dspace.app.rest.model.Item;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.dspace.utils.DSpace;

import org.dspace.app.rest.utils.ContextUtil;
import io.katharsis.queryspec.QuerySpec;
import io.katharsis.repository.ResourceRepositoryV2;
import io.katharsis.repository.annotations.JsonApiDelete;
import io.katharsis.repository.annotations.JsonApiFindAll;
import io.katharsis.repository.annotations.JsonApiFindAllWithIds;
import io.katharsis.repository.annotations.JsonApiFindOne;
import io.katharsis.repository.annotations.JsonApiSave;
import io.katharsis.resource.list.ResourceList;

public abstract class DSpaceRepository<R, K extends Serializable> {
	//implements ResourceRepositoryV2<R, K> {

	protected RequestService requestService = new DSpace().getRequestService();

	Logger log = Logger.getLogger(Item.class);

	// @Override
//	public <S extends R> S create(S arg0) {
//		Context context = null;
//		S result = null;
//		try {
//			context = obtainContext();
//			result = create(context, arg0);
//			context.complete();
//		} catch (Exception e) {
//			log.error(LogManager.getHeader(context, "rest_create", this.getResourceClass().getCanonicalName()), e);
//			// wrap the exception to simplify initial implementation
//			throw new RuntimeException(e);
//		} finally {
//			if (context.isValid()) {
//				context.abort();
//			}
//		}
//		return result;
//	}

//	protected abstract <S extends R> S create(Context context, S resource) throws Exception;

	// @Override
	@JsonApiDelete
	public void delete(K key) {
		Context context = null;
		try {
			context = obtainContext();
			delete(context, key);
			context.complete();
		} catch (Exception e) {
			log.error(LogManager.getHeader(context, "rest_delete", this.getResourceClass().getCanonicalName()), e);
			// wrap the exception to simplify initial implementation
			throw new RuntimeException(e);
		} finally {
			if (context.isValid()) {
				context.abort();
			}
		}

	}

	protected abstract void delete(Context context, K key) throws Exception;

	// @Override
	@JsonApiFindAllWithIds
	public ResourceList<R> findAll(Iterable<K> arg0, QuerySpec arg1) {
		Context context = null;
		ResourceList<R> result = null;
		try {
			context = obtainContext();
			result = findAll(context, arg0, arg1);
			context.complete();
		} catch (Exception e) {
			log.error(
					LogManager.getHeader(context, "rest_findAllMultiKeys", this.getResourceClass().getCanonicalName()),
					e);
			// wrap the exception to simplify initial implementation
			throw new RuntimeException(e);
		} finally {
			if (context.isValid()) {
				context.abort();
			}
		}
		return result;
	}

	protected abstract ResourceList<R> findAll(Context context, Iterable<K> keys, QuerySpec spec) throws Exception;

	// @Override
	@JsonApiFindAll
	public ResourceList<R> findAll(QuerySpec arg0) {
		Context context = null;
		ResourceList<R> result = null;
		try {
			context = obtainContext();
			result = findAll(context, arg0);
			context.complete();
		} catch (Exception e) {
			log.error(LogManager.getHeader(context, "rest_findAll", this.getResourceClass().getCanonicalName()), e);
			// wrap the exception to simplify initial implementation
			throw new RuntimeException(e);
		} finally {
			if (context.isValid()) {
				context.abort();
			}
		}
		return result;
	}

	protected abstract ResourceList<R> findAll(Context context, QuerySpec spec) throws Exception;

	// @Override
	@JsonApiFindOne
	public R findOne(K arg0, QuerySpec arg1) {
		Context context = null;
		R result = null;
		try {
			context = obtainContext();
			result = findOne(context, arg0, arg1);
			context.complete();
		} catch (Exception e) {
			log.error(LogManager.getHeader(context, "rest_findOne", this.getResourceClass().getCanonicalName()), e);
			// wrap the exception to simplify initial implementation
			throw new RuntimeException(e);
		} finally {
			if (context.isValid()) {
				context.abort();
			}
		}
		return result;
	}

	protected abstract R findOne(Context context, K key, QuerySpec spec) throws Exception;

	// @Override
	@JsonApiSave
	public <S extends R> S save(S arg0) {
		Context context = null;
		S result = null;
		try {
			context = obtainContext();
			result = save(context, arg0);
			context.complete();
		} catch (Exception e) {
			log.error(LogManager.getHeader(context, "rest_save", this.getResourceClass().getCanonicalName()), e);
			// wrap the exception to simplify initial implementation
			throw new RuntimeException(e);
		} finally {
			if (context.isValid()) {
				context.abort();
			}
		}
		return result;
	}

	protected abstract <S extends R> S save(Context context, S resource) throws Exception;

	private Context obtainContext() {
		Request currentRequest = requestService.getCurrentRequest();
		Context context = (Context) currentRequest.getAttribute(ContextUtil.DSPACE_CONTEXT);
		if (context != null && context.isValid()) {
			return context;
		}
		context = new Context();
		currentRequest.setAttribute(ContextUtil.DSPACE_CONTEXT, context);
		return context;
	}

	protected abstract <S extends R> Class<S> getResourceClass();
}
