package org.dspace.content.repository;

import java.util.UUID;

import org.dspace.content.Bundle;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@RepositoryRestResource(collectionResourceRel="bundle", path="bundle", exported=true)
public interface BundleRepository extends PagingAndSortingRepository<Bundle, UUID>{
}
