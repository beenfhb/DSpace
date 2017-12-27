/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.service.EditItemService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the EditItem object.
 * This class is responsible for all business logic calls for the Item object and is autowired by spring.
 *
 * @author Pascarelli Luigi Andrea (luigiandrea.pascarelli at 4science dot it)
 */
public class EditItemServiceImpl implements EditItemService {
	
    @Autowired(required = true)
    private ItemService itemService;

	@Override
	public void deleteWrapper(Context context, EditItem inProgressSubmission) throws SQLException, AuthorizeException {
		try {
			getItemService().delete(context, inProgressSubmission.getItem());
		} catch (IOException e) {
			throw new SQLException(e);
		}		
	}

	@Override
	public void update(Context context, EditItem inProgressSubmission) throws SQLException, AuthorizeException {
		getItemService().update(context, inProgressSubmission.getItem());		
	}
	
    public ItemService getItemService()
    {
        return itemService;
    }


}
