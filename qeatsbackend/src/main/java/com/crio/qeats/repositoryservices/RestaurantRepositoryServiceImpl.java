/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.repositoryservices;

import ch.hsr.geohash.GeoHash;
import com.crio.qeats.configs.RedisConfiguration;
import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.globals.GlobalConstants;
import com.crio.qeats.models.RestaurantEntity;
import com.crio.qeats.models.ItemEntity;
import com.crio.qeats.models.MenuEntity;
import com.crio.qeats.models.RestaurantEntity;
import com.crio.qeats.repositories.ItemRepository;
import com.crio.qeats.repositories.MenuRepository;
import com.crio.qeats.repositories.RestaurantRepository;
import com.crio.qeats.utils.GeoLocation;
import com.crio.qeats.utils.GeoUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.lang.StackWalker.Option;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.StringOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;


@Service
public class RestaurantRepositoryServiceImpl implements RestaurantRepositoryService {



  @Autowired
  private RedisConfiguration redisConfiguration;

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private Provider<ModelMapper> modelMapperProvider;

  @Autowired
  private RestaurantRepository restaurantRepository;

  @Autowired
  private ItemRepository itemRepository;
  
  @Autowired
  private MenuRepository menuRepository;

  private boolean isOpenNow(LocalTime time, RestaurantEntity res) {
    LocalTime openingTime = LocalTime.parse(res.getOpensAt());
    LocalTime closingTime = LocalTime.parse(res.getClosesAt());

    return time.isAfter(openingTime) && time.isBefore(closingTime);
  }

  // TODO: CRIO_TASK_MODULE_NOSQL
  // Objectives:
  // 1. Implement findAllRestaurantsCloseby.
  // 2. Remember to keep the precision of GeoHash in mind while using it as a key.
  // Check RestaurantRepositoryService.java file for the interface contract.
  public List<Restaurant> findAllRestaurantsCloseBy(Double latitude,
      Double longitude, LocalTime currentTime, Double servingRadiusInKms) {

    List<Restaurant> restaurants = null;
    // TODO: CRIO_TASK_MODULE_REDIS
    // We want to use cache to speed things up. Write methods that perform the same functionality,
    // but using the cache if it is present and reachable.
    // Remember, you must ensure that if cache is not present, the queries are directed at the
    // database instead.

    if (redisConfiguration.isCacheAvailable()) {
      restaurants =
          findAllRestaurantsCloseByFromCache(latitude, longitude, currentTime, servingRadiusInKms);
          System.out.println("Called From Cache ");
    } else {
      restaurants =
          findAllRestaurantsCloseFromDb(latitude, longitude, currentTime, servingRadiusInKms);
          System.out.println("Called From Database ");
    }

      //CHECKSTYLE:OFF
      //CHECKSTYLE:ON
    return restaurants;
  }

  // TODO: CRIO_TASK_MODULE_NOSQL
  // Objective:
  // 1. Check if a restaurant is nearby and open. If so, it is a candidate to be returned.
  // NOTE: How far exactly is "nearby"?

  private List<Restaurant> findAllRestaurantsCloseFromDb(Double latitude, Double longitude,
  LocalTime currentTime, Double servingRadiusInKms) {
ModelMapper modelMapper = modelMapperProvider.get();
List<RestaurantEntity> restaurantEntities = restaurantRepository.findAll();
List<Restaurant> restaurants = new ArrayList<Restaurant>();
for (RestaurantEntity restaurantEntity : restaurantEntities) {
  if (isRestaurantCloseByAndOpen(restaurantEntity, currentTime, latitude, longitude,
      servingRadiusInKms)) {
    restaurants.add(modelMapper.map(restaurantEntity, Restaurant.class));
  }
}
return restaurants;
}

  private List<Restaurant> findAllRestaurantsCloseByFromCache(Double latitude, Double longitude,
      LocalTime currentTime, Double servingRadiusInKms) {
    List<Restaurant> restaurantList = new ArrayList<>();
    GeoLocation geoLocation = new GeoLocation(latitude, longitude);
    GeoHash geoHash =
        GeoHash.withCharacterPrecision(geoLocation.getLatitude(), geoLocation.getLongitude(), 7);
 
    try (Jedis jedis = redisConfiguration.getJedisPool().getResource()) {
      String jsonStringFromCache = jedis.get(geoHash.toBase32());
 
      if (jsonStringFromCache == null) {
        // Cache needs to be updated.
        String createdJsonString = "";
        try {
          restaurantList = findAllRestaurantsCloseFromDb(geoLocation.getLatitude(),
              geoLocation.getLongitude(), currentTime, servingRadiusInKms);
          createdJsonString = new ObjectMapper().writeValueAsString(restaurantList);
        } catch (JsonProcessingException e) {
          e.printStackTrace();
        }
 
        // Do operations with jedis resource
        jedis.setex(geoHash.toBase32(), GlobalConstants.REDIS_ENTRY_EXPIRY_IN_SECONDS,
            createdJsonString);
      } else {
        try {
          restaurantList = new ObjectMapper().readValue(jsonStringFromCache,
              new TypeReference<List<Restaurant>>() {});
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
 
    return restaurantList;
  }

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants whose names have an exact or partial match with the search query.
  @Override
  public List<Restaurant> findRestaurantsByName(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {

        // AggregationOperation project = Aggregation.project().and(StringOperators.Concat.valueOf("Firstname").concatValueOf("Lastname")).as("newField");
        ModelMapper modelMapper = modelMapperProvider.get();

        List<Restaurant> restaurantList = new ArrayList<>();
        Optional<List<RestaurantEntity>> opt =  restaurantRepository.findRestaurantsByNameExact(searchString);
        if(!opt.isEmpty())
        {
          List<RestaurantEntity> restaurantEntities = opt.get();
          for (RestaurantEntity restaurantEntity : restaurantEntities) {
            if (isRestaurantCloseByAndOpen(restaurantEntity, currentTime, latitude, longitude,
                servingRadiusInKms)) {
                  restaurantList.add(modelMapper.map(restaurantEntity, Restaurant.class));
            }
          }
        }
 
       Optional<List<RestaurantEntity>> opt2 = restaurantRepository.findRestaurantsByName(searchString);
        if(!opt2.isEmpty())
        {
          List<RestaurantEntity> restaurantEntities2 = opt2.get();

         for(RestaurantEntity restaurantEntity : restaurantEntities2)
          {
          if (isRestaurantCloseByAndOpen(restaurantEntity, currentTime, latitude, longitude,
          servingRadiusInKms)) {
            restaurantList.add(modelMapper.map(restaurantEntity, Restaurant.class));
          } 
          }
        }
       

       

        return restaurantList;
  }


  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants whose attributes (cuisines) intersect with the search query.
  @Override
  public List<Restaurant> findRestaurantsByAttributes(
      Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {


        List<Restaurant> restaurantList = new ArrayList<>();
        List<RestaurantEntity> restaurantEntities =  restaurantRepository.findByAttributesIn(searchString);
        //System.out.println("Debug "+ restaurantEntities.toString());
        ModelMapper modelMapper = modelMapperProvider.get();

        for (RestaurantEntity restaurantEntity : restaurantEntities) {
          if (isRestaurantCloseByAndOpen(restaurantEntity, currentTime, latitude, longitude,
              servingRadiusInKms)) {
                 restaurantList.add(modelMapper.map(restaurantEntity, Restaurant.class));
           }
         }
         return restaurantList;
  }



  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants which serve food items whose names form a complete or partial match
  // with the search query.

  @Override
  public List<Restaurant> findRestaurantsByItemName(
      Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {
         List<Restaurant> restaurantList = new ArrayList<>();
         List<ItemEntity> itemEntities = itemRepository.findByNameLike(searchString);
         List<String> itemIdList = new ArrayList<String>();

          for(ItemEntity ie : itemEntities)
          {
          itemIdList.add(ie.getItemId());
          }
          
        Optional<List<MenuEntity>> optionalMenuEntityList   = menuRepository.findMenusByItemsItemIdIn(itemIdList);
        Optional<List<RestaurantEntity>> optionalRestaurantEntityList = Optional.empty();

        if (optionalMenuEntityList.isPresent()) {
          List<MenuEntity> menuEntityList = optionalMenuEntityList.get();
          List<String> restaurantIdList = menuEntityList
              .stream()
              .map(MenuEntity::getRestaurantId)
              .collect(Collectors.toList());
          optionalRestaurantEntityList = restaurantRepository
              .findRestaurantsByRestaurantIdIn(restaurantIdList);
         }
         
         List<RestaurantEntity> masterRestaurantEntities = new ArrayList<>();
         if(optionalRestaurantEntityList.isPresent())
         {
          masterRestaurantEntities = optionalRestaurantEntityList.get();
          ModelMapper modelMapper = modelMapperProvider.get();

          for (RestaurantEntity restaurantEntity : masterRestaurantEntities) {
            if (isRestaurantCloseByAndOpen(restaurantEntity, currentTime, latitude, longitude,
                servingRadiusInKms)) {
                  restaurantList.add(modelMapper.map(restaurantEntity, Restaurant.class));
            }
          }
         }

        return restaurantList;
  }

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants which serve food items whose attributes intersect with the search query.
  @Override
  public List<Restaurant> findRestaurantsByItemAttributes(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {

        List<Restaurant> restaurantList = new ArrayList<>();
        List<ItemEntity> itemEntities = itemRepository.findItemsByAttributesIn(searchString);
        List<String> itemIdList = new ArrayList<String>();

          for(ItemEntity ie : itemEntities)
          {
          itemIdList.add(ie.getItemId());
          }
          
        Optional<List<MenuEntity>> optionalMenuEntityList   = menuRepository.findMenusByItemsItemIdIn(itemIdList);
        Optional<List<RestaurantEntity>> optionalRestaurantEntityList = Optional.empty();

        if (optionalMenuEntityList.isPresent()) {
          List<MenuEntity> menuEntityList = optionalMenuEntityList.get();
          List<String> restaurantIdList = menuEntityList
              .stream()
              .map(MenuEntity::getRestaurantId)
              .collect(Collectors.toList());
          optionalRestaurantEntityList = restaurantRepository
              .findRestaurantsByRestaurantIdIn(restaurantIdList);
         }
         
         List<RestaurantEntity> masterRestaurantEntities = new ArrayList<>();
         if(optionalRestaurantEntityList.isPresent())
         {
          masterRestaurantEntities = optionalRestaurantEntityList.get();
          ModelMapper modelMapper = modelMapperProvider.get();

          for (RestaurantEntity restaurantEntity : masterRestaurantEntities) {
            if (isRestaurantCloseByAndOpen(restaurantEntity, currentTime, latitude, longitude,
                servingRadiusInKms)) {
                  restaurantList.add(modelMapper.map(restaurantEntity, Restaurant.class));
            }
          }
         }




     return restaurantList;
  }
  /**
   * Utility method to check if a restaurant is within the serving radius at a given time.
   * @return boolean True if restaurant falls within serving radius and is open, false otherwise
   */
  private boolean isRestaurantCloseByAndOpen(RestaurantEntity restaurantEntity,
      LocalTime currentTime, Double latitude, Double longitude, Double servingRadiusInKms) {
    if (isOpenNow(currentTime, restaurantEntity)) {
      return GeoUtils.findDistanceInKm(latitude, longitude,
          restaurantEntity.getLatitude(), restaurantEntity.getLongitude())
          < servingRadiusInKms;
    }
    return false;
  }

  //MULTI THREADED IMPLEMENTATION OF DATABASE OPERATIONS

  @Async
  @Override
  public Future<List<Restaurant>> findRestaurantsByNameAsync(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {
    // TODO Auto-generated method stub
    System.out.println("Search By Name by "+ Thread.currentThread().getName());
    List<Restaurant> restaurants = findRestaurantsByName(latitude, longitude, searchString, currentTime, servingRadiusInKms);
  
    return new AsyncResult<List<Restaurant>>(restaurants);
  }

  @Async
  @Override
  public Future<List<Restaurant>> findRestaurantsByAttributesAsync(Double latitude,
      Double longitude, String searchString, LocalTime currentTime, Double servingRadiusInKms) {
    // TODO Auto-generated method stub
    System.out.println("Search By Attribute by "+ Thread.currentThread().getName());
    List<Restaurant> restaurants = findRestaurantsByAttributes(latitude, longitude, searchString, currentTime, servingRadiusInKms);
    return new AsyncResult<List<Restaurant> >(restaurants);
  }
  @Async
  @Override
  public Future<List<Restaurant>> findRestaurantsByItemNameAsync(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {
    // TODO Auto-generated method stub
    System.out.println("Search By Item Name by "+ Thread.currentThread().getName());
    List<Restaurant> restaurants = findRestaurantsByItemName(latitude, longitude, searchString, currentTime, servingRadiusInKms);
    return new AsyncResult<List<Restaurant> >(restaurants);
  }

  @Async
  @Override
  public Future<List<Restaurant>> findRestaurantsByItemAttributesAsync(Double latitude,
      Double longitude, String searchString, LocalTime currentTime, Double servingRadiusInKms) {
    // TODO Auto-generated method stub
    System.out.println("Search By Item Attribute by "+ Thread.currentThread().getName());
    List<Restaurant> restaurants = findRestaurantsByItemAttributes(latitude, longitude, searchString, currentTime, servingRadiusInKms);
    return new AsyncResult<List<Restaurant>>(restaurants);
  }

 



}

