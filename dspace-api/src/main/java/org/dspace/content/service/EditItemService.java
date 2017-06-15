/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import org.dspace.content.EditItem;

/**
 * Service interface class for the Item object.
 * The implementation of this class is responsible for all business logic calls for the Item object and is autowired by spring
 * 
 * @author Pascarelli Luigi Andrea (luigiandrea.pascarelli at 4science dot it)
 *
 */
public interface EditItemService extends InProgressSubmissionService<EditItem>
{
    
}
