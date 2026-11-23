package com.demo.nimn.service.diet;

import com.demo.nimn.dto.diet.FoodSelectionDTO;
import com.demo.nimn.dto.diet.CreateFoodSelectionDTO;
import com.demo.nimn.entity.diet.FoodSelection;

import java.util.List;

public interface FoodSelectionService {
    FoodSelectionDTO getById(Long mealSelectionId, String ownerEmail);
    List<FoodSelection> convertToMealSelectionEntities(List<CreateFoodSelectionDTO> mealSelectionDTOS);
    List<FoodSelectionDTO> convertToMealSelectionDTOS(List<FoodSelection> foodSelectionEntities);
}