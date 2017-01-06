package org.dspace.content.repository;

import java.util.UUID;

import org.dspace.eperson.Group;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@RepositoryRestResource(collectionResourceRel="group", path="group", exported=true)
public interface EPersonGroupRepository extends PagingAndSortingRepository<Group, UUID>{
}
