package org.dspace.content.repository;

import java.util.UUID;

import org.dspace.content.Community;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@RepositoryRestResource(collectionResourceRel="community", path="community", exported=true)
public interface CommunityRepository extends PagingAndSortingRepository<Community, UUID>{
}
