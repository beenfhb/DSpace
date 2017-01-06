package org.dspace.content.repository;

import org.dspace.authorize.ResourcePolicy;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@RepositoryRestResource(collectionResourceRel="resourcePolicy", path="resourcePolicy", exported=true)
public interface ResourcePolicyRepository extends PagingAndSortingRepository<ResourcePolicy, Integer>{
}
