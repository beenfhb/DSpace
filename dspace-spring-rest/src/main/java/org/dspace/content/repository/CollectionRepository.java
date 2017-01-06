package org.dspace.content.repository;

import java.util.UUID;

import org.dspace.content.Collection;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@RepositoryRestResource(collectionResourceRel="collection", path="collection", exported=true)
public interface CollectionRepository extends PagingAndSortingRepository<Collection, UUID>{
}
