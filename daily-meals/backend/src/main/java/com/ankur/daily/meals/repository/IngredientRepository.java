package com.ankur.daily.meals.repository;

import com.ankur.daily.meals.model.Ingredient;
import java.util.Optional;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface IngredientRepository extends Neo4jRepository<Ingredient, String> {
  // Query to check if an ingredient exists by its name
  @Query("""
           MATCH (i:Ingredient {nameInEnglish: $name})
           RETURN i
           """)
  Optional<Ingredient> findByName(String name);
}
