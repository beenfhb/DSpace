package org.dspace.content.repository;

import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.content.repository.projection.ItemProjection;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@RepositoryRestResource(collectionResourceRel="item", path="item", exported=true)
public interface ItemRepository extends PagingAndSortingRepository<Item, UUID>{
}
