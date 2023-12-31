package com.example.legendfive.overall.Service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.example.legendfive.overall.Entity.PredictionRecord;
import com.example.legendfive.overall.Entity.Stock;
import com.example.legendfive.overall.Entity.ThemeCard;
import com.example.legendfive.overall.Entity.User;
import com.example.legendfive.overall.dto.StockDto;
import com.example.legendfive.overall.repository.PredictionRecordRepository;
import com.example.legendfive.overall.repository.ThemeCardRepository;
import com.example.legendfive.overall.repository.UserRepository;
import com.example.legendfive.overall.repository.stock.StockRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.context.Theme;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.text.DecimalFormat;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

    private static final String S3_BUCKET_NAME = "api-test01";
    private static final String S3_FILE_PATH = "todayStock/";
    private final AmazonS3 amazonS3;
    private final StockRepository stockRepository;
    private final UserRepository userRepository;
    private final PredictionRecordRepository predictionRecordRepository;
    private final ThemeCardRepository themeCardRepository;
    /**
     * 검색 리스트에서 세부 종목을 하나 눌렀을때, S3에서 값을 가져와서 프론트로 전해줄 값
     * S3에 저장된 오늘 날짜의 주식 정보를 가져오는 메소드 -> 아침마다 예측에 사용
     */
    public StockDto.stockDetailResponseDto getStockDetailsFromS3(String stockCode) {

        Stock stock = stockRepository.findByStockCode(stockCode).orElseThrow(
                () -> new RuntimeException("해당 주식이 없습니다.")
        );

        try {

            S3Object s3Object = amazonS3.getObject(S3_BUCKET_NAME, S3_FILE_PATH + stock.getStockCode() + ".json");
            S3ObjectInputStream s3ObjectInputStream = s3Object.getObjectContent();

            String jsonContent = new BufferedReader(new InputStreamReader(s3ObjectInputStream))
                    .lines().collect(Collectors.joining("\n"));

            ObjectMapper objectMapper = new ObjectMapper();
            StockDto.stockDetailResponseDto stockDetail = objectMapper.readValue(jsonContent, StockDto.stockDetailResponseDto.class);

            s3ObjectInputStream.close();
            return stockDetail;

        } catch (AmazonS3Exception | IOException e) {
            e.printStackTrace();
            throw new AmazonS3Exception("Amazon S3에서 파일을 읽을 수 없습니다.");
        }
    }

    public StockDto.stockPriceDodPercentageInS3Dto getStockFromS33(String stockCode) throws ParseException {

        S3Object s3Object = amazonS3.getObject(S3_BUCKET_NAME, S3_FILE_PATH + stockCode + ".json");
        S3ObjectInputStream s3ObjectInputStream = s3Object.getObjectContent();

        String jsonContent = new BufferedReader(new InputStreamReader(s3ObjectInputStream))
                .lines().collect(Collectors.joining("\n"));

        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(jsonContent);

        return StockDto.stockPriceDodPercentageInS3Dto.builder()
                .stock_price((String) jsonObject.get("stock_present_price"))
                .stock_dod_percentage((String) jsonObject.get("stock_dod_percentage"))
                .build();
    }

    /**
     * 모든 유저를 매일 아침마다 주식이 올랐는지 확인하여 포인트 지급 로직
     */
    @Transactional
    @Scheduled(cron = "5 0 9 * * ?")
    public void checkIsStockUp() {


        //1. 모든 유저를 가져온다
        for (User user : userRepository.findAll()) {

            //2. 모든 유저의 예측 기록을 가져온다
            for (PredictionRecord predictionRecord : predictionRecordRepository.findByUser(user)) {

                //3. 예측 기록의 종료일이 오늘인지 확인한다
                if (Objects.equals(predictionRecord.getEndDay(), LocalDate.now())) {

                    //4. 종료일이 오늘이라면, S3에서 해당 주식의 현재 가격을 가져온다
                    StockDto.stockDetailResponseDto stockDetailResponseDto = getStockDetailsFromS3(predictionRecord.getStockCode().toString());
                    String stockEndPriceFromS3 = stockDetailResponseDto.getStock_present_price();

                    //수익률(+,-)
                    double earningRate = (double) (Integer.parseInt(stockEndPriceFromS3) - predictionRecord.getStockPresentPrice()) / predictionRecord.getStockPresentPrice();

                    // DecimalFormat을 사용하여 소수점 이하 숫자를 제외하고 출력
                    DecimalFormat decimalFormat = new DecimalFormat("#");
                    String formattedEarningRate = decimalFormat.format(earningRate * 100);

                    //계산된 포인트
                    int calculatedTotalPoint = (int) (earningRate * predictionRecord.getInputPoint() * 10);

                    if (user.getUserPoint() + calculatedTotalPoint < 0) {
                        user.updateUserPoint(0);
                    } else {
                        user.updateUserPoint(user.getUserPoint() + calculatedTotalPoint);
                    }

                    //예측하기 성공시, 테마 카드 저장
                    if (earningRate > 0) {

                        //테마 이름 가져옴
                        String themeName = stockDetailResponseDto.getStock_theme_code();
                        String stockName = stockDetailResponseDto.getStock_name();

                        if(themeCardRepository.findByStockName(stockName) != null){
                            log.info("이미 테마 카드에 저장되어 있습니다.");
                        }

                        else{
                            ThemeCard newThemeCard = ThemeCard.builder()
                                    .themeName(themeName)
                                    .stockName(stockName)
                                    .createdAt(LocalDate.now())
                                    .user(user)
                                    .build();

                            themeCardRepository.save(newThemeCard);
                        }
                    }

                    predictionRecord.updatePriceRatePoint(Integer.parseInt(stockEndPriceFromS3), String.valueOf(formattedEarningRate), calculatedTotalPoint);
                }
            }
        }

    }

    /**
     * 주식 예측하기
     **/
    @Transactional
    public StockDto.stockPredictionResponseDto predictStock(StockDto.stockPredictionRequsetDto stockPredictionRequsetDto) {

        Stock stock = stockRepository.findByStockCode(stockPredictionRequsetDto.getStockCode()).orElseThrow(
                () -> new IllegalArgumentException("해당 주식이 존재하지 않습니다.")
        );

        User user = userRepository.findByUserId(stockPredictionRequsetDto.getUserId()).orElseThrow(
                () -> new IllegalArgumentException("해당 유저가 존재하지 않습니다."));

        PredictionRecord existingPrediction = predictionRecordRepository.findByStockAndUserAndCreatedAt(stock, user, LocalDate.now()).orElse(null);

        if (existingPrediction != null) {
            return StockDto.stockPredictionResponseDto.builder()
                    .message("이미 해당 주식을 오늘 예측한 적이 있습니다.")
                    .build();
        }

        //사용자가 입력한 포인트
        Long inputPoint = stockPredictionRequsetDto.getInputPoint();

        if(inputPoint <= 0){
            return StockDto.stockPredictionResponseDto.builder()
                    .message("0 이상의 값을 입력해주세요.")
                    .build();
        }

        if (user.getUserPoint() - inputPoint < 0) {
            return StockDto.stockPredictionResponseDto.builder()
                    .message("포인트가 부족합니다.")
                    .build();
        }

        //주식 예측 기록 저장
        LocalDate now = LocalDate.now();
        LocalDate end_day = now.plusDays(Long.parseLong(stockPredictionRequsetDto.getInvestmentPeriod()));

        PredictionRecord predictionRecord = PredictionRecord.builder()
                .stock(stock)
                .user(user)
                .stockPresentPrice(Integer.parseInt(stockPredictionRequsetDto.getStockPresentPrice()))
                .predictionRecordUuid(UUID.randomUUID())
                .endDay(end_day)
                .isPublic(true)
                .earnedPoint(0)
                .stockIncreaseRate("0")
                .inputPoint(stockPredictionRequsetDto.getInputPoint())
                .stockCode(stock.getStockCode()).build();

        predictionRecordRepository.save(predictionRecord);

        int themeCount = (int) predictionRecordRepository.findByUser(user).stream().map(predictionRecord1 -> predictionRecord1.getStock().getStockCode()).distinct().count();
        int userPredictedStockNumber = (int) predictionRecordRepository.findByUser(user).stream().map(predictionRecord1 -> predictionRecord1.getStock().getStockCode()).count();
        log.info("userPredictedStockNumber: " + userPredictedStockNumber);

        if(userPredictedStockNumber < 3){
            log.info("투자성향분석중, 종목 3개 미만");
        }else {
            log.info("themeCount: " + themeCount);
            if (themeCount <= 3) {
                updateUserInvestType(user, "집중투자형");
            } else if (themeCount <= 5) {
                updateUserInvestType(user, "중립형");
            } else {
                updateUserInvestType(user, "분산투자형");
            }
        }

        //user에서 포인트 차감 로직
        user.updateUserPoint((int) (user.getUserPoint() - inputPoint));

        return StockDto.stockPredictionResponseDto.builder()
                .message("주식 예측 기록 저장 완료")
                .build();

    }

    public Page<StockDto.SearchStockBrandResponseDto> searchStockByBrandName(String brandName, Pageable pageable) {
        Page<Stock> searchResults = stockRepository.findByStockNameContainingIgnoreCase(brandName, pageable);

        // Page를 DTO로 변환
        return searchResults.map(stockEntity -> {
            try {
                log.info("stockCode: " + stockEntity.getStockCode());
                log.info("stockPriceDataFromS3: " + getStockFromS33(stockEntity.getStockCode()));

                //주식의 예측 기록을 가져온다.
                String stockPredictionCount = String.valueOf(predictionRecordRepository.countByStockCode(stockEntity.getStockCode()));
                StockDto.stockPriceDodPercentageInS3Dto stockPriceDodPercentageInS3Dto = getStockFromS33(stockEntity.getStockCode());
                log.info("stockPriceDodPercentageInS3Dto" + stockPriceDodPercentageInS3Dto);
                log.info("stockPrecitionCount: " + stockPredictionCount);


                return StockDto.SearchStockBrandResponseDto.builder()
                        .stockCode(stockEntity.getStockCode())
                        .stockPrice(stockPriceDodPercentageInS3Dto.getStock_price())
                        .stockDodPercentage(stockPriceDodPercentageInS3Dto.getStock_dod_percentage())
                        .stockPredictionCount(stockPredictionCount)
                        .StockName(stockEntity.getStockName())
                        .build();
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public Page<StockDto.SearchStockThemeResponseDto> searchStockByThemeName(String themeName, Pageable pageable) {
        log.info("themeName: " + themeName);
        log.info("pageable: " + pageable.toString());
        Page<Stock> searchResults = stockRepository.findByThemeName(themeName, pageable);
        log.info("searchResults: " + searchResults.toString());
        // Page를 DTO로 변환
        return searchResults.map(stockEntity -> {
            try {

                //주식의 예측 기록을 가져온다.
                String stockPredictionCount = String.valueOf(predictionRecordRepository.countByStockCode(stockEntity.getStockCode()));
                StockDto.stockPriceDodPercentageInS3Dto stockPriceDodPercentageInS3Dto = getStockFromS33(stockEntity.getStockCode());
                log.info("stockPriceDodPercentageInS3Dto" + stockPriceDodPercentageInS3Dto);
                log.info("stockPrecitionCount: " + stockPredictionCount);

                return StockDto.SearchStockThemeResponseDto.builder()
                        .stockCode(stockEntity.getStockCode())
                        .stockPrice(stockPriceDodPercentageInS3Dto.getStock_price())
                        .stockDodPercentage(stockPriceDodPercentageInS3Dto.getStock_dod_percentage())
                        .stockPredictionCount(stockPredictionCount)
                        .StockName(stockEntity.getStockName())
                        .build();
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void updateUserInvestType(User user, String investType){
        user = user.toBuilder()
                .userInvestType(investType)
                .build();
        log.info(investType);

        userRepository.save(user);
    }
}
