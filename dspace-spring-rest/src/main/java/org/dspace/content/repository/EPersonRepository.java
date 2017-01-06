package org.dspace.content.repository;

import java.util.UUID;

import org.dspace.eperson.EPerson;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@RepositoryRestResource(collectionResourceRel="eperson", path="eperson", exported=true)
public interface EPersonRepository extends PagingAndSortingRepository<EPerson, UUID>{
	EPerson findByEmail(@Param("q") String email);
}
