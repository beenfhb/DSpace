package org.dspace.repository;

import java.util.Optional;
import java.util.UUID;

import org.dspace.content.DSpaceObject;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DSORepository<T extends DSpaceObject, ID extends UUID> extends JpaRepository<T, ID> {
	Optional<T> findById(UUID uuid);
}
