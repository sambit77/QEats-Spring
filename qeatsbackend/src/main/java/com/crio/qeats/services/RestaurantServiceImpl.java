
/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.services;

import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.exchanges.GetRestaurantsRequest;
import com.crio.qeats.exchanges.GetRestaurantsResponse;
import com.crio.qeats.repositoryservices.RestaurantRepositoryService;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.sound.midi.Soundbank;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class RestaurantServiceImpl implements RestaurantService {

  private final Double peakHoursServingRadiusInKms = 3.0;
  private final Double normalHoursServingRadiusInKms = 5.0;
  @Autowired
  private RestaurantRepositoryService restaurantRepositoryService;


  // TODO: CRIO_TASK_MODULE_RESTAURANTSAPI - Implement findAllRestaurantsCloseby.
  // Check RestaurantService.java file for the interface contract.
  @Override
  public GetRestaurantsResponse findAllRestaurantsCloseBy(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {

      List<Restaurant> restaurant;
      int h = currentTime.getHour();
      int m = currentTime.getMinute();

      if ((h >= 8 && h <= 9) || (h == 10 && m == 0) || (h == 13) || (h == 14 && m == 0) 
          || (h >= 19 && h <= 20) || (h == 21 && m == 0)) {
           // System.out.println("From if");
      restaurant = restaurantRepositoryService.findAllRestaurantsCloseBy(
          getRestaurantsRequest.getLatitude(), getRestaurantsRequest.getLongitude(), 
          currentTime, peakHoursServingRadiusInKms);
    } else {
      restaurant = restaurantRepositoryService.findAllRestaurantsCloseBy(
        getRestaurantsRequest.getLatitude(), getRestaurantsRequest.getLongitude(), 
        currentTime, normalHoursServingRadiusInKms);
    }
    List<Restaurant> newList = new ArrayList<Restaurant>();
    for(Restaurant r : restaurant)
    {   
      String wa = r.getName();
      for(int i = 0 ; i < wa.length() ; i++)
      {
        int code = (int) wa.charAt(i);
        //System.out.println("Rest Name"+wa);
        if(code < 0 || code > 127)
        {
          wa = wa.replace(wa.charAt(i), '?');   
        }
      }
      r.setName(wa);
      newList.add(r);
    }
    GetRestaurantsResponse response = new GetRestaurantsResponse(newList);
    log.info(response);
    return response;

  }
  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Implement findRestaurantsBySearchQuery. The request object has the search string.
  // We have to combine results from multiple sources:
  // 1. Restaurants by name (exact and inexact)
  // 2. Restaurants by cuisines (also called attributes)
  // 3. Restaurants by food items it serves
  // 4. Restaurants by food item attributes (spicy, sweet, etc)
  // Remember, a restaurant must be present only once in the resulting list.
  // Check RestaurantService.java file for the interface contract.
  @Override
  public GetRestaurantsResponse findRestaurantsBySearchQuery(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime){
        List<Restaurant> masterSerachResult = new ArrayList<>();
        Double latitude = getRestaurantsRequest.getLatitude();
        Double longitude = getRestaurantsRequest.getLongitude();
        String searchString = getRestaurantsRequest.getSearchFor();
        Double servingRadiusInKms = getServingRadiusInKms(currentTime);

        if(searchString == "")
        {
          return new GetRestaurantsResponse(masterSerachResult);
        }
        List<Restaurant> searchByName = restaurantRepositoryService.findRestaurantsByName(latitude, longitude, searchString, currentTime, servingRadiusInKms);
        List<Restaurant> searchByAttributes = restaurantRepositoryService.findRestaurantsByAttributes(latitude, longitude, searchString, currentTime, servingRadiusInKms);
        List<Restaurant> searchByItems = restaurantRepositoryService.findRestaurantsByItemName(latitude, longitude, searchString, currentTime, servingRadiusInKms);
        List<Restaurant> seacrhByItemAttributes = restaurantRepositoryService.findRestaurantsByItemAttributes(latitude, longitude, searchString, currentTime, servingRadiusInKms);
        System.out.println("Master Serach Result --------------- " +masterSerachResult.toString());
       if(searchByName != null)
       {
        for(Restaurant r : searchByName)
        {
          if(! isPresentInList(masterSerachResult,r))
          {
            masterSerachResult.add(r);
          }
        }
       }
       if(searchByAttributes != null)
       {
        for(Restaurant r : searchByAttributes)
        {
          if(! isPresentInList(masterSerachResult,r))
          {
            //System.out.println("size  " + masterSerachResult.size());
            masterSerachResult.add(r);
            //System.out.println("size  " + masterSerachResult.size());
          }
        }
       }

        if(searchByItems != null)
        {
          for(Restaurant r : searchByItems)
        {
          if(! isPresentInList(masterSerachResult,r))
          {
            masterSerachResult.add(r);
          }
        }
        }

       if(seacrhByItemAttributes != null)
       {
        for(Restaurant r : seacrhByItemAttributes)
        {
          if(! isPresentInList(masterSerachResult,r))
          {
            masterSerachResult.add(r);
          }
        }
       }

        System.out.println("Master Serach Result --------------- " +masterSerachResult.toString());
        System.out.println("size" + masterSerachResult.size());

    return new GetRestaurantsResponse(masterSerachResult);
  }

  public boolean isPresentInList(List<Restaurant> restaurants , Restaurant restaurantToSearch)
  {
    boolean val = false;
    for(Restaurant r : restaurants)
    {
      if(r.getRestaurantId().equals(restaurantToSearch.getRestaurantId()))
      {
        val = true;
        break;
      }
    }
    return val;
  }

  public Double getServingRadiusInKms(LocalTime currentTime)
  {
    int h = currentTime.getHour();
    int m = currentTime.getMinute();

    if ((h >= 8 && h <= 9) || (h == 10 && m == 0) || (h == 13) || (h == 14 && m == 0) 
        || (h >= 19 && h <= 20) || (h == 21 && m == 0)) 
        {
          return peakHoursServingRadiusInKms;
        }

        else
        {
          return normalHoursServingRadiusInKms;
        }
  }


  // TODO: CRIO_TASK_MODULE_MULTITHREADING
  // Implement multi-threaded version of RestaurantSearch.
  // Implement variant of findRestaurantsBySearchQuery which is at least 1.5x time faster than
  // findRestaurantsBySearchQuery.
  @Override
  public GetRestaurantsResponse findRestaurantsBySearchQueryMt(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) throws InterruptedException, ExecutionException {

        List<Restaurant> masterSerachResult = new ArrayList<>();
        Double latitude = getRestaurantsRequest.getLatitude();
        Double longitude = getRestaurantsRequest.getLongitude();
        String searchString = getRestaurantsRequest.getSearchFor();
        Double servingRadiusInKms = getServingRadiusInKms(currentTime);

        if(searchString == "")
        {
          return new GetRestaurantsResponse(masterSerachResult);
        }

        Future<List<Restaurant>> searchByNameAsync = restaurantRepositoryService.findRestaurantsByNameAsync(latitude, longitude, searchString, currentTime, servingRadiusInKms);
        Future<List<Restaurant>> searchByAttributesAsync = restaurantRepositoryService.findRestaurantsByAttributesAsync(latitude, longitude, searchString, currentTime, servingRadiusInKms);
        Future<List<Restaurant>> searchByItemsAsync = restaurantRepositoryService.findRestaurantsByItemNameAsync(latitude, longitude, searchString, currentTime, servingRadiusInKms);
        Future<List<Restaurant>> seacrhByItemAttributesAsync = restaurantRepositoryService.findRestaurantsByItemAttributesAsync(latitude, longitude, searchString, currentTime, servingRadiusInKms);
        List<Restaurant> searchByName = null;
        List<Restaurant> searchByAttributes = null;
        List<Restaurant> searchByItems = null;
        List<Restaurant> seacrhByItemAttributes = null;

       
        //System.out.println("Master Serach Result --------------- " +masterSerachResult.toString());

        Boolean isTaskOneDone = false;
        Boolean isTaskTwoDone = false;
        Boolean isTaskThreeDone = false;
        Boolean isTaskFourDone = false;
        int totalTaskCount = 4;

        while(true && totalTaskCount != 0)
        {
          if(searchByNameAsync.isDone() && (!isTaskOneDone))
          {
            searchByName = searchByNameAsync.get();
            isTaskOneDone = true;
            totalTaskCount--;
          }
          if(searchByAttributesAsync.isDone() && (!isTaskTwoDone))
          {
            searchByAttributes = searchByAttributesAsync.get();
            isTaskTwoDone = true;
            totalTaskCount--;
          }
          if(searchByItemsAsync.isDone() && (!isTaskThreeDone))
          {
            searchByItems = searchByItemsAsync.get();
            isTaskThreeDone = true;
            totalTaskCount--;
          }
          if(seacrhByItemAttributesAsync.isDone() && (!isTaskFourDone))
          {
            seacrhByItemAttributes = seacrhByItemAttributesAsync.get();
            isTaskFourDone = true;
            totalTaskCount--;
          }
        }

        if(searchByName != null)
        {
         for(Restaurant r : searchByName)
         {
           if(! isPresentInList(masterSerachResult,r))
           {
             masterSerachResult.add(r);
           }
         }
        }
        if(searchByAttributes != null)
        {
         for(Restaurant r : searchByAttributes)
         {
           if(! isPresentInList(masterSerachResult,r))
           {
             //System.out.println("size  " + masterSerachResult.size());
             masterSerachResult.add(r);
             //System.out.println("size  " + masterSerachResult.size());
           }
         }
        }
 
         if(searchByItems != null)
         {
           for(Restaurant r : searchByItems)
         {
           if(! isPresentInList(masterSerachResult,r))
           {
             masterSerachResult.add(r);
           }
         }
         }
 
        if(seacrhByItemAttributes != null)
        {
         for(Restaurant r : seacrhByItemAttributes)
         {
           if(! isPresentInList(masterSerachResult,r))
           {
             masterSerachResult.add(r);
           }
         }
        }
 

        //System.out.println("Master Serach Result --------------- " +masterSerachResult.toString());
        System.out.println("Search Result size" + masterSerachResult.size());

     return new GetRestaurantsResponse(masterSerachResult);
  }
}

