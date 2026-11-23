package com.demo.nimn.service.diet;

import com.demo.nimn.exception.ErrorCode;
import com.demo.nimn.exception.CustomException;
import com.demo.nimn.dto.diet.FoodSelectionDTO;
import com.demo.nimn.dto.diet.CreateFoodSelectionDTO;
import com.demo.nimn.entity.diet.FoodSelection;
import com.demo.nimn.entity.food.Food;
import com.demo.nimn.repository.diet.FoodSelectionRepository;
import com.demo.nimn.repository.food.FoodRepository;
import com.demo.nimn.service.food.FoodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FoodSelectionServiceImpl implements FoodSelectionService {

    private final FoodSelectionRepository foodSelectionRepository;
    private final FoodRepository foodRepository;
    private final FoodService foodService;

    @Autowired
    public FoodSelectionServiceImpl(FoodSelectionRepository foodSelectionRepository , FoodRepository foodRepository, FoodService foodService) {
        this.foodSelectionRepository = foodSelectionRepository;
        this.foodRepository = foodRepository;
        this.foodService = foodService;
    }

    // ID로 FoodSelection 엔티티 조회 후 DTO로 변환
    @Override
    public FoodSelectionDTO getById(Long mealSelectionId, String ownerEmail) {
        FoodSelection foodSelection = foodSelectionRepository.findById(mealSelectionId)
                .filter(selection -> ownerEmail.equals(selection.getUserEmail()))
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
        return convertToMealSelectionDTO(foodSelection);
    }

    // 클라이언트에서 보낸 요청 DTO를 실제 DB 저장용 FoodSelection 엔티티로 변환
    public FoodSelection convertToMealSelectionEntity(CreateFoodSelectionDTO mealSelectionDTO) {
        Food food = foodRepository.getReferenceById(mealSelectionDTO.getFoodId());

        return FoodSelection.builder()
                .userEmail(mealSelectionDTO.getUserEmail())
                .food(food)
                .mealTime(mealSelectionDTO.getFoodTime())
                .count(mealSelectionDTO.getCount())
                .build();
    }
    // DB에서 가져온 FoodSelection 엔티티를 클라이언트 응답용 DTO로 변환
    public FoodSelectionDTO convertToMealSelectionDTO(FoodSelection foodSelection) {
        return FoodSelectionDTO.builder()
                .foodSelectionId(foodSelection.getId())
                .userEmail(foodSelection.getUserEmail())
                .foodMenu(foodService.convertToFoodDTO(foodSelection.getFood()))
                .foodTime(foodSelection.getMealTime())
                .count(foodSelection.getCount())
                .build();
    }
    // DTO 리스트를 엔티티 리스트로 일괄 변환
    @Override
    public List<FoodSelection> convertToMealSelectionEntities(List<CreateFoodSelectionDTO> mealSelectionDTOS){
        return mealSelectionDTOS.stream().map(this::convertToMealSelectionEntity).collect(Collectors.toList());
    }
    // FoodSelection 엔티티 리스트를 DTO 리스트로 일괄 변환
    @Override
    public List<FoodSelectionDTO> convertToMealSelectionDTOS(List<FoodSelection> foodSelectionEntities){
        return foodSelectionEntities.stream().map(this::convertToMealSelectionDTO).collect(Collectors.toList());
    }


}