package org.dspace.content.repository;

import org.dspace.handle.Handle;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@RepositoryRestResource(collectionResourceRel="handle", path="handle", exported=true)
public interface HandleRepository extends PagingAndSortingRepository<Handle, Integer>{
}
