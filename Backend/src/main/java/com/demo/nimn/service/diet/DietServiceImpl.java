package com.demo.nimn.service.diet;

import com.demo.nimn.dto.diet.UserDailyFoodPlanDTO;
import com.demo.nimn.dto.diet.CreateWeeklyDietDTO;
import com.demo.nimn.dto.diet.DailyDietDTO;
import com.demo.nimn.dto.diet.CreateDailyDietDTO;
import com.demo.nimn.dto.diet.WeeklyDietDTO;
import com.demo.nimn.entity.diet.DailyDiet;
import com.demo.nimn.entity.diet.FoodSelection;
import com.demo.nimn.entity.diet.WeeklyDiet;
import com.demo.nimn.repository.diet.DailyDietRepository;
import com.demo.nimn.repository.diet.WeeklyDietRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DietServiceImpl implements DietService {


    private final DailyDietRepository dailyDietRepository;
    private final WeeklyDietRepository weeklyDietRepository;

    private final FoodSelectionService foodSelectionService;

    @Autowired
    public DietServiceImpl(FoodSelectionService foodSelectionService,
                           DailyDietRepository dailyDietRepository,
                           WeeklyDietRepository weeklyDietRepository) {

        this.dailyDietRepository = dailyDietRepository;
        this.weeklyDietRepository = weeklyDietRepository;
        this.foodSelectionService = foodSelectionService;
    }

    //DailyDiet

    // 특정 사용자(userEmail)의 특정 날짜(date)에 해당하는 식단 정보를 조회
    @Override
    public List<DailyDietDTO> getByUserEmailAndDate(String userEmail, LocalDate date) {
        return dailyDietRepository.findByUserEmailAndDate(userEmail, date).stream().map(this::convertToDailyDietDTO).collect(Collectors.toList());
    }

    // 특정 날짜의 모든 사용자 식단 정보를 조회
    @Override
    public List<DailyDietDTO> getByDate(LocalDate date) {
        return dailyDietRepository.findByDate(date).stream().map(this::convertToDailyDietDTO).collect(Collectors.toList());
    }

    // 클라이언트 요청 DTO를 실제 DB에 저장 가능한 DailyDiet 엔티티로 변환
    private DailyDiet convertToDailyDietEntity(CreateDailyDietDTO dailyDietDTO) {
        return DailyDiet.builder()
                .userEmail(dailyDietDTO.getUserEmail())
                .foodSelections(foodSelectionService.convertToMealSelectionEntities(dailyDietDTO.getFoodSelections()))
                .date(dailyDietDTO.getDate())
                .build();
    }

    // DB에서 가져온 DailyDiet 엔티티를 클라이언트에 전달할 DailyDietDTO로 변환
    private DailyDietDTO convertToDailyDietDTO(DailyDiet dailyDiet) {
        return DailyDietDTO.builder()
                .dailyDietId(dailyDiet.getId())
                .date(dailyDiet.getDate())
                .userEmail(dailyDiet.getUserEmail())
                .foodSelections(foodSelectionService.convertToMealSelectionDTOS(dailyDiet.getFoodSelections()))
                .build();
    }

    // 여러 개의 요청 DTO 리스트를 DailyDiet 엔티티 리스트로 변환
    @Override
    public List<DailyDiet> convertToDailyDietEntities(List<CreateDailyDietDTO> dailyDietDTOS) {
        return dailyDietDTOS.stream().map(this::convertToDailyDietEntity).collect(Collectors.toList());
    }

    // 여러 개의 DailyDiet 엔티티 리스트를 DTO 리스트로 변환
    @Override
    public List<DailyDietDTO> convertToDailyDietDTOS(List<DailyDiet> dailyDietEntities) {
        return dailyDietEntities.stream().map(this::convertToDailyDietDTO).collect(Collectors.toList());
    }


    //WeeklyDiet

    // 새로운 주간 식단 생성
    @Override
    @Transactional
    public WeeklyDietDTO createWeeklyDiet(String ownerEmail, CreateWeeklyDietDTO weeklyDietDTO) {
        // 요청 본문의 이메일 대신 로그인한 회원을 주간·일별·음식 선택 모두의 소유자로 정한다 (#8).
        weeklyDietDTO.setUserEmail(ownerEmail);
        weeklyDietDTO.getDailyDiets().forEach(daily -> {
            daily.setUserEmail(ownerEmail);
            daily.getFoodSelections().forEach(selection -> selection.setUserEmail(ownerEmail));
        });
        if (weeklyDietRepository.existsByCurrentWeeklyMealPlan(weeklyDietDTO.getStartDate(), weeklyDietDTO.getUserEmail())) {
            return null;
        }
        WeeklyDiet entity = convertToWeeklyDietEntity(weeklyDietDTO);
        weeklyDietRepository.save(entity);
        return convertToWeeklyDietDTO(entity);
    }

    // 특정 사용자의 최신 주간 식단 조회
    @Override
    public WeeklyDietDTO getWeeklyDietByUserEmail(String userEmail) {
        return convertToWeeklyDietDTO(weeklyDietRepository.findByUserEmail(userEmail));
    }

    // ID를 통해 특정 주간 식단 조회
    @Override
    public WeeklyDietDTO getWeeklyDietById(Long weeklyDietId) {
        WeeklyDiet weeklyDiet = weeklyDietRepository.findById(weeklyDietId).orElseThrow(() -> new RuntimeException("WeeklyDiet Not Found"));

        return convertToWeeklyDietDTO(weeklyDiet);
    }

    // WeeklyDietDTO를 WeeklyDiet 엔티티로 변환
    public WeeklyDiet convertToWeeklyDietEntity(CreateWeeklyDietDTO weeklyDietDTO) {
        return WeeklyDiet.builder()
                .userEmail(weeklyDietDTO.getUserEmail())
                .startDate(weeklyDietDTO.getStartDate())
                .endDate(weeklyDietDTO.getEndDate())
                .dailyDiets(convertToDailyDietEntities(weeklyDietDTO.getDailyDiets()))
                .build();
    }

    // 주간 식단 엔티티를 클라이언트용 DTO로 변환
    public WeeklyDietDTO convertToWeeklyDietDTO(WeeklyDiet weeklyDiet) {
        return WeeklyDietDTO.builder()
                .weeklyId(weeklyDiet.getId())
                .userEmail(weeklyDiet.getUserEmail())
                .startDate(weeklyDiet.getStartDate())
                .endDate(weeklyDiet.getEndDate())
                .dailyDiets(convertToDailyDietDTOS(weeklyDiet.getDailyDiets()))
                .build();
    }

    // DailyDiet에 포함된 FoodSelection 리스트에서 음식 ID만 추출
    public List<Long> convertToFoodMenuIds(DailyDiet dailyDiet) {
        List<Long> foodMenuIds = new ArrayList<>();
        for (FoodSelection foodSelection : dailyDiet.getFoodSelections()) {
            foodMenuIds.add(foodSelection.getFood().getId());
        }
        return foodMenuIds;
    }

    // 하루치 식단을 UserDailyMealPlanDTO로 변환
    public UserDailyFoodPlanDTO convertToDailyMealPlanDTO(DailyDiet dailyDiet) {
        return UserDailyFoodPlanDTO.builder()
                .date(dailyDiet.getDate())
                .foodMenuIds(convertToFoodMenuIds(dailyDiet))
                .build();
    }

    // 한 주간의 DailyDiet 리스트를 전부 UserDailyMealPlanDTO로 변환
    public List<UserDailyFoodPlanDTO> convertToDailyMealPlanDTOS(WeeklyDiet weeklyDiet) {
        List<UserDailyFoodPlanDTO> userDailyFoodPlanDTOS = new ArrayList<>();
        for (DailyDiet dailyDiet : weeklyDiet.getDailyDiets()) {
            userDailyFoodPlanDTOS.add(convertToDailyMealPlanDTO(dailyDiet));
        }
        return userDailyFoodPlanDTOS;
    }
}