package org.dspace.content.repository;

import java.util.UUID;

import org.dspace.content.Bitstream;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@RepositoryRestResource(collectionResourceRel="bitstream", path="bitstream", exported=true)
public interface BitstreamRepository extends PagingAndSortingRepository<Bitstream, UUID>{
}
