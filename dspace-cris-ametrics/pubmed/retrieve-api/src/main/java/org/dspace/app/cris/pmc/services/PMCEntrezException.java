/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.cris.pmc.services;

public class PMCEntrezException extends Exception
{
    public PMCEntrezException()
    {
        super();
    }
    
    public PMCEntrezException(String message)
    {
        super(message);
    }
    
    public PMCEntrezException(String message, Throwable rootCause)
    {
        super(message, rootCause);
    }
}
