package com.ankur.daily.meals.dto;

import java.util.List;

public record DishDetailsDto(
    String dishName,
    List<String> ingredientNames,
    String recipeUrl,
    String cuisine,
    String dishType
  ) {}

