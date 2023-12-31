package com.example.legendfive.overall.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public class StockDto {


    @Getter
    @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
    public static class stockPriceDodPercentageInS3Dto{

        private String stock_price;
        private String stock_dod_percentage;
    }

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StockListDto{
        private String email;
        private String title;
        private String content;
    }


    @Getter
    @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
    public static class stockDetailResponseDto{


        private String stock_code;
        private String stock_name;
        private String stock_theme_code;
        private String stock_present_price;
        private String stock_dod_percentage;
        private String stock_min_30;
        private String stock_max_30;
        private List<PriceDataDto> price_data;
        private List<MaLineDataDto> ma_line_data;
    }

    @Getter
    @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
    public static class stockPresentPriceInS3Dto{

        private String stock_code;
        private String stock_name;
        private String stock_theme_code;
        private String stock_present_price;
    }

    @Getter
    @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
    public static class stockPredictionRequsetDto{

        @NotNull
        private String stockCode;
        @NotNull
        private UUID userId;
        @NotNull
        private String investmentPeriod;
        @NotNull
        private String stockPresentPrice;
        @NotNull
        private Long inputPoint;
    }

    @Getter
    @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
    public static class stockPredictionResponseDto{

        private String message;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
    public static class SearchStockBrandResponseDto{
        private String stockCode;
        private String stockPrice;
        private String stockDodPercentage;
        private String stockPredictionCount;
        private String StockName;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
    public static class SearchStockThemeResponseDto{
        private String stockCode;
        private String stockPrice;
        private String stockDodPercentage;
        private String stockPredictionCount;
        private String StockName;
    }
}
