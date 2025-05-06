package com.ankur.daily.meals.service;

import com.ankur.daily.meals.dto.DishDetailsDto;
import com.ankur.daily.meals.model.Dish;
import com.ankur.daily.meals.model.Ingredient;
import com.ankur.daily.meals.repository.DishRepository;
import com.ankur.daily.meals.repository.IngredientRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DishService {


  @Autowired
  private IngredientRepository ingredientRepository;
  @Autowired
  private DishRepository dishRepository;
  @Autowired
  DatabaseMetadataService databaseMetadataService;;
  public void processDish(String name, double price) {

  }

  public DishDetailsDto fetchDetailsByIngredients(String ingredientName) {
    List<Dish> dishes = dishRepository.findByIngredients(ingredientName);
    if (dishes.isEmpty()) {
      throw new NoSuchElementException("No dishes found with ingredient: " + ingredientName);
    }
    return toDto(dishes.get(0));
  }

  public List<DishDetailsDto> searchDishesByIngredients(String ingredientName) {
    List<Dish> dishes = dishRepository.findByIngredients(ingredientName);
    return dishes.stream()
      .map(this::toDto)
      .collect(Collectors.toList());
  }

  public Map<String, List<Object>> fetchDishesGraph() {
    List<Dish> dishes = dishRepository.findAll();
    Map<String, List<Object>> graph = new HashMap<>();

    graph.put("nodes", new ArrayList<>());
    graph.put("links", new ArrayList<>());

    for (Dish dish : dishes) {
      graph.get("nodes").add(Map.of("name", dish.getDishName(), "type", "Dish"));

      for (Ingredient ingredient : dish.getIngredients()) {
        graph.get("nodes").add(Map.of("name", ingredient.getNameInEnglish(), "type", "Ingredient"));
        graph.get("links").add(Map.of("source", dish.getDishName(), "target", ingredient.getNameInEnglish()));
      }
    }

    return graph;
  }


  public Dish save(DishDetailsDto dishDetailsDto) {
    // Map DishDetailsDto to Dish
    Dish dish = new Dish();
    dish.setDishName(dishDetailsDto.dishName());
    dish.setRecipeUrl(dishDetailsDto.recipeUrl());
    dish.setCuisine(dishDetailsDto.cuisine());
    dish.setDishType(dishDetailsDto.dishType());

    List<Ingredient> ingredients = dishDetailsDto.ingredientNames().stream()
      .map(name -> ingredientRepository.findById(name)
        .orElseGet(() -> new Ingredient(name, null, null))) // Create if not found
      .collect(Collectors.toList());

    dish.setIngredients(ingredients);
    return dishRepository.save(dish);
  }

  private DishDetailsDto toDto(Dish dish) {
    List<String> ingredientNames = dish.getIngredients().stream()
      .map(Ingredient::getNameInEnglish)
      .collect(Collectors.toList());

    return new DishDetailsDto(
      dish.getDishName(),
      ingredientNames,
      dish.getRecipeUrl(),
      dish.getCuisine(),
      dish.getDishType()
    );
  }
}
