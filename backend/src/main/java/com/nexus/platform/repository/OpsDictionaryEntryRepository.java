package com.nexus.platform.repository;

import com.nexus.platform.entity.OpsDictionaryEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpsDictionaryEntryRepository extends JpaRepository<OpsDictionaryEntry, Long> {
    List<OpsDictionaryEntry> findAllByOrderByDictTypeAscSortOrderAscUpdatedAtDesc();

    Optional<OpsDictionaryEntry> findByDictTypeAndDictKey(String dictType, String dictKey);
}
