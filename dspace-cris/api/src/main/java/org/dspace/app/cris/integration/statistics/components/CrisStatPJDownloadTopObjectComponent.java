/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration.statistics.components;

import org.dspace.app.cris.model.jdyna.ProjectNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.ProjectPropertiesDefinition;

import it.cilea.osd.jdyna.model.PropertiesDefinition;

public class CrisStatPJDownloadTopObjectComponent extends
        CrisStatDownloadTopObjectComponent
{

    @Override
    protected PropertiesDefinition innerCall(Integer pkey)
    {

        PropertiesDefinition def = getApplicationService().get(
                ProjectPropertiesDefinition.class, pkey);
        if (def == null)
        {
            def = getApplicationService().get(ProjectNestedPropertiesDefinition.class,
                    pkey);
        }

        return def;
    }

}
