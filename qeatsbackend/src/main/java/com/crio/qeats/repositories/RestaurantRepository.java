/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.repositories;

import com.crio.qeats.models.RestaurantEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

public interface RestaurantRepository extends MongoRepository<RestaurantEntity, String> {
    //List<RestaurantEntity> findByNameLike(String searchString);
    //List<RestaurantEntity> findByAttributesLike(String searchString);
    //List<RestaurantEntity> findByRestaurantId(String restaurantId);

    //@Query("{name: ?0}")
    //Optional<List<RestaurantEntity>> findRestaurantsByNameExact(String name);

    List<RestaurantEntity> findByAttributesIn(String attribute);



    @Query("{'name': {$regex: '^?0$', $options: 'i'}}")
    Optional<List<RestaurantEntity>> findRestaurantsByNameExact(String name);

    @Query("{'name': {$regex: '.*?0.*', $options: 'i'}}")
    Optional<List<RestaurantEntity>> findRestaurantsByName(String name);



    List<RestaurantEntity> findByRestaurantId(String restaurantId);



    Optional<List<RestaurantEntity>> findRestaurantsByRestaurantIdIn(List<String> restaurantIdList);

    

}

