//package com.example.legendfive.save.stock;
//
//import com.example.legendfive.Entity.Stock;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//import javax.annotation.PostConstruct;
//
//@RequiredArgsConstructor
//@Service
//public class SaveStockApiList {
//
//
//    private final StockApiRepository stockApiRepository;
//
//    private final String STOCK_DEFAULT_URL = "http://apis.data.go.kr/1160100/service/GetMarketIndexInfoService";
//
//    private final RestTemplate restTemplate;
//
//    public void getAllStockList() {
//
//        String url = STOCK_DEFAULT_URL + "/getStockMarketIndex";
//
//        UriComponents uriBuilder = UriComponentsBuilder.fromHttpUrl(url)
//                .queryParam("serviceKey", openApiSecretInfo.getServiceKey())
//                .queryParam("numOfRows", 5)
//                .queryParam("pageNo", 1)
//                .queryParam("resultType", "json")
//                .queryParam("beginBasDt", dateConfig.getFromFiveDaysAgoToNow())
//                .queryParam("idxNm", "코스피")
//                .build();
//
//        ResponseEntity<String> responseData = restTemplate.getForEntity(uriBuilder.toUriString(), String.class);
//
//        String responseDataBody = responseData.getBody();
//
//        try {
//            JSONParser jsonParser = new JSONParser();
//            JSONObject jsonObject = (JSONObject) jsonParser.parse(responseDataBody);
//            JSONObject response = (JSONObject) jsonObject.get("response");
//
//            JSONObject body = (JSONObject) response.get("body");
//
//            JSONObject items = (JSONObject) body.get("items");
//
//            JSONArray item = (JSONArray) items.get("item");
//
//            for (int i = 0; i < item.size(); i++) {
//                JSONObject tmp = (JSONObject) item.get(i);
//                KOSPIStockIndex infoObj = new KOSPIStockIndex(
//                        i + (long) 1,
//                        (String) tmp.get("basDt"),
//                        (String) tmp.get("idxNm"),
//                        (String) tmp.get("idxCsf"),
//                        (String) tmp.get("epyItmsCnt"),
//                        (String) tmp.get("clpr"),
//                        (String) tmp.get("vs"),
//                        (String) tmp.get("fltRt"),
//                        (String) tmp.get("mkp"),
//                        (String) tmp.get("hipr"),
//                        (String) tmp.get("lopr"),
//                        (String) tmp.get("trqu"),
//                        (String) tmp.get("trPrc"),
//                        (String) tmp.get("lstgMrktTotAmt"),
//                        (String) tmp.get("lsYrEdVsFltRg"),
//                        (String) tmp.get("lsYrEdVsFltRt"),
//                        (String) tmp.get("yrWRcrdHgst"),
//                        (String) tmp.get("yrWRcrdHgstDt"),
//                        (String) tmp.get("yrWRcrdLwst"),
//                        (String) tmp.get("yrWRcrdLwstDt"),
//                        (String) tmp.get("basPntm"),
//                        (String) tmp.get("basIdx")
//                );
//                kospiStockIndexRepository.save(infoObj);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    // 매일 오전 11시 5분에 DB에 있는 주식시세정보 데이터 삭제
//    @Scheduled(cron = "0 5 11 * * *", zone = "Asia/Seoul")
//    public void deleteKOSPIStockList() {
//        kospiStockIndexRepository.deleteAll();
//    }
//}
