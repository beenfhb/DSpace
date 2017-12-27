/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.content.Bitstream;
import org.dspace.content.IMetadataValue;
import org.dspace.content.Item;
import org.dspace.content.Thumbnail;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.IGlobalSearchResult;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

public class ThumbDisplayStrategy implements IDisplayMetadataValueStrategy
{

    private ConfigurationService configurationService;

    private final transient ItemService itemService = ContentServiceFactory
            .getInstance().getItemService();

    private final transient BitstreamService bitstreamService = ContentServiceFactory
            .getInstance().getBitstreamService();

    private final transient AuthorizeService authorizeService = AuthorizeServiceFactory
            .getInstance().getAuthorizeService();

    /** Config value of thumbnail view toggle */
    private boolean showThumbs;

    /** Config browse/search width and height */
    private int thumbItemListMaxWidth;

    private int thumbItemListMaxHeight;

    /** Config browse/search thumbnail link behaviour */
    private boolean linkToBitstream = false;

    public ThumbDisplayStrategy()
    {
        configurationService = DSpaceServicesFactory.getInstance()
                .getConfigurationService();

        showThumbs = configurationService
                .getBooleanProperty("webui.browse.thumbnail.show");

        if (showThumbs)
        {
            thumbItemListMaxHeight = configurationService
                    .getIntProperty("webui.browse.thumbnail.maxheight");

            if (thumbItemListMaxHeight == 0)
            {
                thumbItemListMaxHeight = configurationService
                        .getIntProperty("thumbnail.maxheight");
            }

            thumbItemListMaxWidth = configurationService
                    .getIntProperty("webui.browse.thumbnail.maxwidth");

            if (thumbItemListMaxWidth == 0)
            {
                thumbItemListMaxWidth = configurationService
                        .getIntProperty("thumbnail.maxwidth");
            }
        }

        String linkBehaviour = configurationService
                .getProperty("webui.browse.thumbnail.linkbehaviour");

        if ("bitstream".equals(linkBehaviour))
        {
            linkToBitstream = true;
        }
    }

    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, UUID colIdx, String field,
            List<IMetadataValue> metadataArray, BrowsableDSpaceObject item,
            boolean disableCrossLinks, boolean emph) throws JspException
    {
        try
        {
            return getThumbMarkup(hrq, itemService.find(UIUtil.obtainContext(hrq), item.getID()), item.getHandle());
        }
        catch (SQLException e)
        {
            throw new JspException(e);
        }
    }

    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, UUID colIdx, String field,
            List<IMetadataValue> metadataArray, Item item, boolean disableCrossLinks,
            boolean emph) throws JspException
    {
        return getThumbMarkup(hrq, item, item.getHandle());
    }

    public String getExtraCssDisplay(HttpServletRequest hrq, int limit,
            boolean b, String string, UUID colIdx, String field,
            List<IMetadataValue> metadataArray, BrowsableDSpaceObject browseItem,
            boolean disableCrossLinks, boolean emph) throws JspException
    {
        return null;
    }

    public String getExtraCssDisplay(HttpServletRequest hrq, int limit,
            boolean b, String browseType, UUID colIdx, String field,
            List<IMetadataValue> metadataArray, Item item, boolean disableCrossLinks,
            boolean emph) throws JspException
    {
        return null;
    }

    /*
     * Get the (X)HTML width and height attributes. As the browser is being used
     * for scaling, we only scale down otherwise we'll get hideously chunky
     * images. This means the media filter should be run with the maxheight and
     * maxwidth set greater than or equal to the size of the images required in
     * the search/browse
     */
    private String getScalingAttr(HttpServletRequest hrq, Bitstream bitstream)
            throws JspException
    {
        BufferedImage buf;

        try
        {
            Context c = UIUtil.obtainContext(hrq);

            InputStream is = bitstreamService.retrieve(c, bitstream);

            // AuthorizeManager.authorizeAction(bContext, this, Constants.READ);
            // read in bitstream's image
            buf = ImageIO.read(is);
            is.close();
        }
        catch (IOException | AuthorizeException | SQLException ex)
        {
            throw new JspException(ex.getMessage(), ex);
        }

        // now get the image dimensions
        float xsize = (float) buf.getWidth(null);
        float ysize = (float) buf.getHeight(null);

        // scale by x first if needed
        if (xsize > (float) thumbItemListMaxWidth)
        {
            // calculate scaling factor so that xsize * scale = new size (max)
            float scale_factor = (float) thumbItemListMaxWidth / xsize;

            // now reduce x size and y size
            xsize = xsize * scale_factor;
            ysize = ysize * scale_factor;
        }

        // scale by y if needed
        if (ysize > (float) thumbItemListMaxHeight)
        {
            float scale_factor = (float) thumbItemListMaxHeight / ysize;

            // now reduce x size
            // and y size
            xsize = xsize * scale_factor;
            ysize = ysize * scale_factor;
        }

        StringBuffer sb = new StringBuffer("width=\"").append(xsize)
                .append("\" height=\"").append(ysize).append("\"");

        return sb.toString();
    }

    /* generate the (X)HTML required to show the thumbnail */
    private String getThumbMarkup(HttpServletRequest hrq, Item item)
            throws JspException
    {
        try
        {
            Context c = UIUtil.obtainContext(hrq);
            Thumbnail thumbnail = itemService.getThumbnail(c, item,
                    linkToBitstream);

            if (thumbnail == null || !authorizeService.authorizeActionBoolean(c,
                    thumbnail.getThumb(), Constants.READ))
            {
                return "";
            }
            StringBuffer thumbFrag = new StringBuffer();

            if (linkToBitstream)
            {
                Bitstream original = thumbnail.getOriginal();
                String link = hrq.getContextPath() + "/bitstream/"
                        + item.getHandle() + "/" + original.getSequenceID()
                        + "/" + UIUtil.encodeBitstreamName(original.getName(),
                                Constants.DEFAULT_ENCODING);
                thumbFrag.append(
                        "<a target=\"_blank\" href=\"" + link + "\" />");
            }
            else
            {
                String link = hrq.getContextPath() + "/handle/"
                        + item.getHandle();
                thumbFrag.append("<a href=\"" + link + "\" />");
            }

            Bitstream thumb = thumbnail.getThumb();
            String img = hrq.getContextPath() + "/retrieve/" + thumb.getID()
                    + "/" + UIUtil.encodeBitstreamName(thumb.getName(),
                            Constants.DEFAULT_ENCODING);
            String alt = thumb.getName();
            String scAttr = getScalingAttr(hrq, thumb);
            thumbFrag.append("<img src=\"").append(img).append("\" alt=\"")
                    .append(alt).append("\" ").append(scAttr)
                    .append("/ border=\"0\"></a>");

            return thumbFrag.toString();
        }
        catch (SQLException sqle)
        {
            throw new JspException(sqle.getMessage(), sqle);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new JspException(
                    "Server does not support DSpace's default encoding. ", e);
        }
    }

    /* generate the (X)HTML required to show the thumbnail */
    private String getThumbMarkup(HttpServletRequest hrq, Item item,
            String handle) throws JspException
    {
        try
        {

            Context c = UIUtil.obtainContext(hrq);
            Thumbnail thumbnail = itemService.getThumbnail(c, item,
                    linkToBitstream);

            if (thumbnail == null || !authorizeService.authorizeActionBoolean(c,
                    thumbnail.getThumb(), Constants.READ))
            {
                return "";
            }

            StringBuffer thumbFrag = new StringBuffer();

            if (linkToBitstream)
            {
                Bitstream original = thumbnail.getOriginal();
                String link = hrq.getContextPath() + "/bitstream/" + handle
                        + "/" + original.getSequenceID() + "/"
                        + UIUtil.encodeBitstreamName(original.getName(),
                                Constants.DEFAULT_ENCODING);
                thumbFrag.append(
                        "<a target=\"_blank\" href=\"" + link + "\" />");
            }
            else
            {
                String link = hrq.getContextPath() + "/handle/" + handle;
                thumbFrag.append("<a href=\"" + link + "\" />");
            }

            Bitstream thumb = thumbnail.getThumb();
            String img = hrq.getContextPath() + "/retrieve/" + thumb.getID()
                    + "/" + UIUtil.encodeBitstreamName(thumb.getName(),
                            Constants.DEFAULT_ENCODING);
            String alt = thumb.getName();
            String scAttr = getScalingAttr(hrq, thumb);
            thumbFrag.append("<img src=\"").append(img).append("\" alt=\"")
                    .append(alt + "\" ").append(scAttr)
                    .append("/ border=\"0\"></a>");

            return thumbFrag.toString();
        }
        catch (SQLException sqle)
        {
            throw new JspException(sqle.getMessage());
        }
        catch (UnsupportedEncodingException e)
        {
            throw new JspException(
                    "Server does not support DSpace's default encoding. ", e);
        }
    }

    @Override
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, UUID colIdx, String field,
            List<IMetadataValue> metadataArray, IGlobalSearchResult item,
            boolean disableCrossLinks, boolean emph) throws JspException
    {
        try
        {
            return getThumbMarkup(hrq, itemService.find(UIUtil.obtainContext(hrq), item.getID()), item.getHandle());
        }
        catch (SQLException e)
        {
            throw new JspException(e);
        }
    }
}
