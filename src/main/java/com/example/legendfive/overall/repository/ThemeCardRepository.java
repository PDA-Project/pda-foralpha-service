package com.example.legendfive.overall.repository;

import com.example.legendfive.overall.Entity.ThemeCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.ui.context.Theme;

import java.util.List;

public interface ThemeCardRepository extends JpaRepository<ThemeCard, Long> {

    List<ThemeCard> findByUserId(Long userId);
    @Query("SELECT t.themeName, COUNT(t.themeName) FROM ThemeCard t WHERE t.user.id = :userId GROUP BY t.themeName")
    List<Object[]> countByThemeNameAndUserId(Long userId);

    ThemeCard findByStockName(String stockName);
}
