package com.ankur.daily.meals.repository;

import com.ankur.daily.meals.model.Dish;
import java.util.List;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DishRepository extends Neo4jRepository<Dish, String> {

  @Query("""
    MATCH (d:Dish)-[r:CONTAINS]->(i:Ingredient)
    WHERE i.nameInEnglish = $ingredientName
    RETURN d,  collect(r),collect(i) as ingredients
    """)
  List<Dish> findByIngredients(String ingredientName);

  // Query to fetch all dishes with ingredients in a graph-like format
  @Query("""
           MATCH (d:Dish)-[r:CONTAINS]->(i:Ingredient)
           RETURN d AS dish, collect(r),collect(i) AS ingredients
           """)
  List<Dish> findAllDishesWithIngredients();
}

