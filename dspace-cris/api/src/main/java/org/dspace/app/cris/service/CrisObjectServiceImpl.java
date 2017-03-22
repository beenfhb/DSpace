package org.dspace.app.cris.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import org.dspace.app.cris.model.ACrisObject;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObjectServiceImpl;
import org.dspace.core.Context;

public class CrisObjectServiceImpl extends DSpaceObjectServiceImpl<ACrisObject> implements CrisObjectService
{

    @Override
    public ACrisObject find(Context context, UUID id) throws SQLException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateLastModified(Context context, ACrisObject dso)
            throws SQLException, AuthorizeException
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void delete(Context context, ACrisObject dso)
            throws SQLException, AuthorizeException, IOException
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int getSupportsTypeConstant()
    {
        return 9;
    }


}
