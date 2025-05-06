package com.ankur.daily.meals.model;


import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node
@Getter
@Setter
public class Ingredient {
  @Id
  private String nameInEnglish;
  private String nameInHindi;
  private String nameInGerman;

  public Ingredient(String nameInEnglish, String nameInHindi, String nameInGerman) {
    this.nameInEnglish=nameInEnglish;
    this.nameInHindi=nameInHindi;
    this.nameInGerman=nameInGerman;
  }

}
