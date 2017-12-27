/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.bulkedit.DSpaceCSV;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.SelfNamedPlugin;

public class CSVDisseminationCrosswalk extends SelfNamedPlugin
        implements StreamGenericDisseminationCrosswalk, FileNameDisseminator
{
    private static Logger log = Logger
            .getLogger(CSVDisseminationCrosswalk.class);
    
    public static final String FILE_NAME_EXPORT_CSV = "csv.export.filename";
    
    @Override
    public boolean canDisseminate(Context context, DSpaceObject dso)
    {
        return (dso.getType() == Constants.ITEM);
    }

    @Override
    public void disseminate(Context context, List<BrowsableDSpaceObject> dso,
            OutputStream out) throws CrosswalkException, IOException,
                    SQLException, AuthorizeException
    {

        // Process each item
        DSpaceCSV csv = new DSpaceCSV(false);
        for (BrowsableDSpaceObject toExport : dso)
        {
            try
            {
                csv.addItem((Item) toExport);
            }
            catch (Exception e)
            {
                log.error(e.getMessage(), e);
            }
        }
        
        // Return the csv file
        BufferedWriter writer = new BufferedWriter( new OutputStreamWriter(out, "UTF-8") );
        writer.write(csv.toString());
        writer.flush();        
        writer.close();
    }

    @Override
    public String getMIMEType()
    {
        return "text/csv";
    }

    @Override
    public void disseminate(Context context, BrowsableDSpaceObject dso, OutputStream out)
            throws CrosswalkException, IOException, SQLException,
            AuthorizeException
    {
        // Process each item
        DSpaceCSV csv = new DSpaceCSV(false);
        try
        {
            csv.addItem((Item) dso);
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }

        // Return the csv file
        BufferedWriter writer = new BufferedWriter( new OutputStreamWriter(out, "UTF-8") );
        writer.write(csv.toString());
        writer.flush();        
        writer.close();        
    }

    @Override
    public String getFileName()
    {
        String result = ConfigurationManager
                .getProperty(FILE_NAME_EXPORT_CSV);
        if (StringUtils.isNotEmpty(result))
            return result;
        return "export.csv";
    }

	@Override
	public boolean assignUniqueNumber() {
		return false;
	}

}