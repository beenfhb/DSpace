/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.core.Context;

public interface IDisseminateUniqueNumber {

    public void disseminate(Context context, BrowsableDSpaceObject dso, OutputStream out, Integer index)
            throws CrosswalkException, IOException, SQLException, AuthorizeException;
    
}
