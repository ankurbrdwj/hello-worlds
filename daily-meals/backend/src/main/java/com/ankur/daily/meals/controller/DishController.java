package com.ankur.daily.meals.controller;

import com.ankur.daily.meals.dto.DishDetailsDto;
import com.ankur.daily.meals.model.Dish;
import com.ankur.daily.meals.repository.DishRepository;
import com.ankur.daily.meals.repository.IngredientRepository;
import com.ankur.daily.meals.service.DishService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dishes")
public class DishController {

  @Autowired
  private DishService dishService;

  @GetMapping("{ingredient}")
  public DishDetailsDto findByIngredients(@PathVariable("ingredient") String ingredient) {
    return dishService.fetchDetailsByIngredients(ingredient);
  }

  @GetMapping("/search")
  List<DishDetailsDto> search(@RequestParam("query") String title) {
    return dishService.searchDishesByIngredients(title);
  }

  @GetMapping("/graph")
  public Map<String, List<Object>> getGraph() {
    return dishService.fetchDishesGraph();
  }


  @PostMapping
  public Dish saveDish(@RequestBody DishDetailsDto dishDetails) {
   return dishService.save(dishDetails);
  }

}
