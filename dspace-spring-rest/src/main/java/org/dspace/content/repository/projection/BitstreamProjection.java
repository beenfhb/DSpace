package org.dspace.content.repository.projection;

import javax.persistence.Column;

import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.springframework.data.rest.core.config.Projection;

@Projection(name = "format", types = Bitstream.class)
public interface BitstreamProjection {
	Integer getSequenceId();

	String getChecksum();

	String getChecksumAlgorithm();

	long getSizeBytes();

	boolean isDeleted();

	String getInternalId();

	int getStoreNumber();

	BitstreamFormat getBitstreamFormat();

	String getUserFormatDescription();
}
