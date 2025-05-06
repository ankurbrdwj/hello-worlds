package com.ankur.daily.meals.model;


import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.List;

@Node
@NoArgsConstructor
@AllArgsConstructor
public class Dish {
  @Id
  private String dishName;
  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private List<Ingredient> ingredients;
  private String recipeUrl;
  private String cuisine; // New field for cuisine
  private String dishType; // New field for dish type

  // Getters and Setters for all fields
  public String getDishName() {
    return dishName;
  }

  public void setDishName(String dishName) {
    this.dishName = dishName;
  }

  public List<Ingredient> getIngredients() {
    return ingredients;
  }

  public void setIngredients(List<Ingredient> ingredients) {
    this.ingredients = ingredients;
  }

  public String getRecipeUrl() {
    return recipeUrl;
  }

  public void setRecipeUrl(String recipeUrl) {
    this.recipeUrl = recipeUrl;
  }

  public String getCuisine() {
    return cuisine;
  }

  public void setCuisine(String cuisine) {
    this.cuisine = cuisine;
  }

  public String getDishType() {
    return dishType;
  }

  public void setDishType(String dishType) {
    this.dishType = dishType;
  }
}
