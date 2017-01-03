package org.dspace.app.rest;

import java.util.List;

import org.dspace.content.BitstreamFormat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@RepositoryRestResource(collectionResourceRel="bitstreamformat", path="bitstreamformat", exported=true)
public interface BitstreamFormatRepository extends PagingAndSortingRepository<BitstreamFormat, Integer>{
	BitstreamFormat findByShortDescription(@Param("q") String shortDescription);
	
	@Query("select bf from BitstreamFormat bf where bf.mimetype = ?1 AND bf.internal = ?2")
    List<BitstreamFormat> findByMIMEType(@Param("q") String mimeType, @Param("includeInternal") boolean includeInternal);

    //int updateRemovedBitstreamFormat(BitstreamFormat deletedBitstreamFormat, BitstreamFormat newBitstreamFormat) throws SQLException;

    @Query("select bf from BitstreamFormat bf where bf.internal = false")
    Page<BitstreamFormat> findNonInternal(Pageable pageable);
    
    @Query("select bf from BitstreamFormat bf where bf.internal = false")
    @RestResource(exported=false)
    List<BitstreamFormat> findNonInternal();

    @EntityGraph(attributePaths = { "fileExtensions" })
    @Query("select bf from BitstreamFormat bf where bf.fileExtensions = ?1")
    List<BitstreamFormat> findByFileExtension(@Param("q") String extension);

}
