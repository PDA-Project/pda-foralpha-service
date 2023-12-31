package com.example.legendfive.overall.Service;


import com.example.legendfive.overall.Entity.ThemeCard;
import com.example.legendfive.overall.Entity.User;
import com.example.legendfive.overall.dto.ThemeDto;
import com.example.legendfive.overall.repository.ThemeCardRepository;
import com.example.legendfive.overall.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class ThemeCardService {
    private final ThemeCardRepository themeCardRepository;
    private final UserRepository userRepository;

    public List<ThemeDto.ThemeCardListResponseDto> getThemeCardList(UUID userUuid) {
        User user = userRepository.findByUserId(userUuid)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        List<Object[]> themeCardCounts = themeCardRepository.countByThemeNameAndUserId(user.getId());

        return themeCardCounts.stream()
                .map(result -> {
                    String themeName = (String) result[0];
                    Long themeCount = (Long) result[1];
                    return ThemeDto.ThemeCardListResponseDto.builder()
                            .themeName(themeName)
                            .createdAt(user.getCreatedAt())
                            .themeCount(themeCount) // Convert Long to int
                            .userNickname(user.getNickname())
                            .build();
                })
                .collect(Collectors.toList());
    }

}
