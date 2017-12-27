/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna.value;

import java.sql.SQLException;
import java.util.UUID;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;

import it.cilea.osd.jdyna.model.AValue;

@Entity
@DiscriminatorValue(value="group")
public class GroupValue extends AValue<UUID>
{

    @Basic
    @Column(name="customPointer")
    private UUID real;

    @Override
    public UUID getObject()
    {
        return real;
    }

    @Override
    protected void setReal(UUID oggetto)
    {
        this.real = oggetto;
        if(oggetto != null) {
            Context context = null;
            try {
                context = new Context();
                String displayValue = EPersonServiceFactory.getInstance().getGroupService().find(context, oggetto).getName().toLowerCase();
                sortValue = displayValue.substring(0,(displayValue.length()<200?displayValue.length():200));
            }
            catch(Exception ex) {
                log.error(ex.getMessage(), ex);
            }
            finally {
                if(context!=null) {
                    context.abort();
                }
            }
        }   
    }

    @Override
    public UUID getDefaultValue()
    {
        Context context = null;
        Group group = null;
        try {
            context = new Context();
            context.turnOffAuthorisationSystem();
            group = EPersonServiceFactory.getInstance().getGroupService().create(context);
            context.restoreAuthSystemState();
        }
        catch (SQLException | AuthorizeException e)
        {
           //NONE
        }
        finally {            
            if(context!=null && context.isValid()) {
                context.abort();
            }
        }
        return group.getID();
    }

    @Override
    public String[] getUntokenizedValue()
    {
        return getObject() != null?
                new String[]{String.valueOf((getObject()))}:null;
    }

    @Override
    public String[] getTokenizedValue()
    {
        return null;
    }
    
 
}
