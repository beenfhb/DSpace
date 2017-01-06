package org.dspace.content.repository.projection;

import java.util.List;

import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.eperson.EPerson;
import org.springframework.data.rest.core.config.Projection;

@Projection(name="test", types=Item.class)
public interface ItemProjection {
	public boolean isArchived();
    public boolean isWithdrawn();
	public EPerson getSubmitter();
//	public List<Bundle> getBundles();
}
