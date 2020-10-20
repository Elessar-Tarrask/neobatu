package neo.batu.main.service;

import feign.Response;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import neo.batu.main.Entity.RowData;
import neo.batu.main.Entity.TableData;
import neo.batu.main.repo.FeignClientRepo;
import org.apache.commons.io.IOUtils;
import org.apache.commons.math3.util.Precision;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class ALLTransportService {

    private final FeignClientRepo feignClientRepo;
    private static final Logger LOGGER = LoggerFactory.getLogger(ALLTransportService.class);
    private static List<String> list = Arrays.asList("99.98.04.03 Beeline USSD", "99.98.05.03 Beeline SMS", "99.98.05.04 Kcell SMS");
    private static Set<String> categories5Percent = new HashSet<>(list);
    private static String[] categoryWithNoPrice = {"01.01.02 ETK-Online (проездной)", "01.02.02.07 Карта школьника (проездной младше 15 лет)", "01.02.02.15 Карта школьника (проездной старше 15 лет)", "01.03.02 Карта студента (проездной)", "01.04.02 Социальная карта (проездной)", "01.06.02 Социальная карта многодетной матери (проездной)", "01.21.02 ETK-Design (проездной)", "02.02 Социальный проездной пенсионера старше 75 лет", "02.04 Льготная карта ветерана", "02.01 Социальная карта ветерана", "02.05 Социальная карта инвалида 1/2 группы", "02.06 Детская социальная карта инвалида до 18 лет", "02.07 Участник декабрьских событий", "04.01.02 ETK Брелок (проездной)", "10.02.02 Карта школьника Алматинская область (проездной)"};
    private static String[] category40 = {"01.02.07 Карта школьника (младше 15 лет)", "01.02.15 Карта школьника (старше 15 лет)", "01.03 Карта студента", "01.04 Социальная карта", "01.06 Социальная карта многодетной матери", "10.02 Карта школьника Алматинская область"};


    @Value("${host}")
    private String url;

    public String test(String dataUUID) throws IOException, URISyntaxException {
        return getFileIdentifier(dataUUID);
    }

    public XSSFWorkbook getXlSXList(String dataUUID, String auth, String tableID, Set<String> excludes) throws IOException, URISyntaxException {
        categories5Percent = excludes;
        String identifier = getFileIdentifier(dataUUID);
        XSSFWorkbook myWorkBook = null;
        System.out.println(identifier);
        XSSFSheet mySheet = null;

        try {
            myWorkBook = new XSSFWorkbook(getFileByteArray(identifier));
        } catch (Exception err) {
            LOGGER.error("no file");
        }
        try {
            mySheet = myWorkBook.getSheetAt(0);
            mySheet.iterator();
        } catch (Exception err) {
            System.out.println(err);
        }
        if (mySheet != null)
            saveTableIntoForm(getDriveWayCategories(mySheet), dataUUID, tableID);
        return myWorkBook;
    }

    public void saveTableIntoForm(TreeSet<String> categories, String dataUUID, String tableID) {
        TableData tableData = new TableData(dataUUID, tableID);

        int i = 1;
        for (String category : categories) {
            tableData.getData().add(new RowData("category_name-b" + i, "textbox", category));

            if (Arrays.asList(categoryWithNoPrice).contains(category)) {
                tableData.getData().add(new RowData("category-price-b" + i, "textbox", ""));
            } else if (Arrays.asList(category40).contains(category)) {
                tableData.getData().add(new RowData("category-price-b" + i, "textbox", "40"));
            } else {
                tableData.getData().add(new RowData("category-price-b" + i, "textbox", "80"));
            }

            if (categories5Percent.contains(category)) {
                tableData.getData().add(new RowData("category-percentage-b" + i, "textbox", "5"));
            } else {
                tableData.getData().add(new RowData("category-percentage-b" + i, "textbox", "7"));
            }

            tableData.getData().add(new RowData("check-param-b" + i, "check", "['1']"));
            i++;
        }

        feignClientRepo.saveTableData(getAuthorization(), tableData);
    }

    public TreeSet<String> getDriveWayCategories(XSSFSheet mySheet) {
        TreeSet<String> categories = new TreeSet();
        Iterator<Row> it = mySheet.iterator();
        List<BusData> busDataList = new ArrayList<>();
        try {
            while (it.hasNext()) {
                Row row = it.next();
                // получение номера автобуса
                String busNumber = row.getCell(0).getStringCellValue();
                if (!busNumber.trim().isEmpty() && (busNumber.contains("Кондуктор") || busNumber.contains("Итого:"))) {
                    int index = 1;
                    if (busNumber.contains("Кондуктор")) {
                        index = 3;
                    }
                    try {
                        String busN = mySheet.getRow(row.getRowNum() + index).getCell(0).getStringCellValue();
                        if (busN.length() > 7) {
                            busN = busN.substring(0, 7);
                        }
                        busDataList.add(new BusData(busN));
                    } catch (Exception err) {
                        System.out.println("ended");
                    }
                }

                // получение категорий
                String cellValue = row.getCell(1).getStringCellValue();
                if (!cellValue.trim().isEmpty() && !cellValue.contains("Категория проездного")) {
                    // добавление имени категории в массив
                    categories.add(cellValue);

                    // получение значения транзакций
                    Double cycleValue = row.getCell(6).getNumericCellValue();
                    BusData busData = busDataList.get(busDataList.size() - 1);
                    // каждая строка с данными
                    CategoryEachData categoryEachData = new CategoryEachData(cellValue);

                    if (!cycleValue.isNaN()) {
                        busData.setCycles(busData.getCycles() + cycleValue);
                        categoryEachData.setCycles(cycleValue);
                    }

                    Double sumValue = row.getCell(7).getNumericCellValue();
                    if (!cycleValue.isNaN()) {
                        busData.setSum(busData.getSum() + sumValue);
                        if (Arrays.asList(categoryWithNoPrice).contains(cellValue)) {
                            // 0
                        } else if (Arrays.asList(category40).contains(cellValue)) {
                            categoryEachData.setBeneficiaries_sum(sumValue);
                            if (categories5Percent.contains(cellValue)) {
                                // 5 percent
                                busData.setBeneficiaries_percent(Precision.round(busData.getBeneficiaries_percent() + (sumValue / 100 * 95), 3));
                                categoryEachData.setBeneficiaries_percent(Precision.round((sumValue / 100 * 95), 3));
                            } else {
                                // 7 percent
                                busData.setBeneficiaries_percent(Precision.round(busData.getBeneficiaries_percent() + (sumValue / 100 * 93), 3));
                                categoryEachData.setBeneficiaries_percent(Precision.round((sumValue / 100 * 93), 3));
                            }
                        } else {
                            categoryEachData.setBasic_price_sum(sumValue);
                            if (categories5Percent.contains(cellValue)) {
                                // 5 percent
                                busData.setBasic_price_percent(Precision.round(busData.getBasic_price_percent() + (sumValue / 100 * 95), 3));
                                categoryEachData.setBasic_price_percent(Precision.round((sumValue / 100 * 95), 3));
                            } else {
                                // 7 percent
                                busData.setBasic_price_percent(Precision.round(busData.getBasic_price_percent() + (sumValue / 100 * 93), 3));
                                categoryEachData.setBasic_price_percent(Precision.round((sumValue / 100 * 93), 3));
                            }
                        }
                    }

                    busData.getCategoryEachData().add(categoryEachData);
                }

            }
        } catch (Exception err) {
            System.out.println(err);
        }
        for (BusData busData : busDataList) {
            System.out.println(busData);
        }
        return categories;
    }

    public String getFileIdentifier(String dataUUID) throws URISyntaxException, IOException {
        String URI = url + "rest" +
                "/api" +
                "/asforms" +
                "/data" +
                "/get" +
                "?dataUUID=" + dataUUID;

        JSONArray jsonArray = parseFeignToJSONArray(getRequest(getAuthorization(), URI));
        JSONArray results = jsonArray.getJSONObject(0).getJSONArray("data");

        String identifier = "";
        int length = results.length();
        for (int i = 0; i < length; i++) {
            JSONObject object = results.getJSONObject(i);
            if (object.getString("id").equals("file-all-transport")) {
                identifier = object.getString("key");
            }
        }
        return identifier;
    }

    public ByteArrayInputStream getFileByteArray(String identifier) throws URISyntaxException, IOException {
        String URI = url + "rest" +
                "/api" +
                "/storage" +
                "/file" +
                "/get" +
                "?identifier=" + identifier;
        return parseFeignToByteArray(getRequest(getAuthorization(), URI));
    }

    public ByteArrayInputStream parseFeignToByteArray(Response response) throws IOException {
        byte[] out = IOUtils.toByteArray(response.body().asInputStream());
        return new ByteArrayInputStream(out);
    }

    public JSONArray parseFeignToJSONArray(Response response) {
        JSONArray result;
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(response.body().asInputStream()))) {
            String resp = buffer.lines().collect(Collectors.joining("\n"));
            result = new JSONArray(resp);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to process response body.", ex);
        }
        if (response != null) {
            response.close();
        }
        return result;
    }

    public String parseFeignToString(Response response) {
        String result;
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(response.body().asInputStream()))) {
            String resp = buffer.lines().collect(Collectors.joining("\n"));
            result = resp;
            System.out.println(resp);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to process response body.", ex);
        }
        if (response != null) {
            response.close();
        }
        return result;
    }

    public Response getRequest(String auth, String url) throws URISyntaxException {
        return feignClientRepo.getRequest(auth, new URI(url));
    }

    public String getAuthorization() {
        return "Basic MTox";
    }

    @Data
    public class BusData {
        public BusData(String busNumber) {
            this.busNumber = busNumber;
            this.cycles = 0.00;
            this.sum = 0.00;
            this.basic_price_percent = 0.00;
            this.beneficiaries_percent = 0.00;
            this.categoryEachData = new ArrayList<>();
        }

        private String busNumber;
        private Double cycles;
        private Double sum;
        private Double basic_price_percent;
        private Double beneficiaries_percent;
        private List<CategoryEachData> categoryEachData;
    }

    @Data
    public class CategoryEachData {
        private String categoryName;

        public CategoryEachData(String categoryName) {
            this.categoryName = categoryName;
            this.tariff = 0;
            this.cycles = 0.0;
            this.basic_price_sum = 0.0;
            this.beneficiaries_sum = 0.0;
            this.basic_price_percent = 0.0;
            this.beneficiaries_percent = 0.0;
        }

        private Integer tariff;
        private Double cycles;
        private Double basic_price_sum;
        private Double beneficiaries_sum;
        private Double basic_price_percent;
        private Double beneficiaries_percent;
    }
}
