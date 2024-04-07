
package com.crio.qeats.repositories;

import com.crio.qeats.models.ItemEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

public interface ItemRepository extends MongoRepository<ItemEntity, String> {

    List<ItemEntity> findByNameLike(String searchString);
    List<ItemEntity> findByAttributesLike(String searchString);
    List<ItemEntity> findItemsByAttributesIn(String searchString);

}

