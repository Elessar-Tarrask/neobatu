package neo.batu.main.service;

import com.fasterxml.jackson.core.sym.NameN;
import feign.Response;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import neo.batu.main.Entity.RowData;
import neo.batu.main.Entity.TableData;
import neo.batu.main.repo.FeignClientRepo;
import org.apache.commons.io.IOUtils;
import org.apache.commons.math3.util.Precision;
import org.apache.poi.ss.formula.functions.T;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
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


    @Value("${hostMain}")
    private String url;

    public String test(String dataUUID) throws IOException, URISyntaxException {
        return getFileIdentifier(dataUUID);
    }

    public void updateTables(String dataUUID, String auth, Set<String> excludes) throws IOException, URISyntaxException {
        categories5Percent = excludes;
        String identifier = getFileIdentifier(dataUUID);
        XSSFWorkbook myWorkBook = null;
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

        List<BusData> busDataList = new ArrayList<>();

        TreeSet<String> categories = getDriveWayCategories(mySheet, busDataList);
        if (mySheet != null)
            saveTableCategoryIntoForm(categories, dataUUID, "table-categories");
        if (busDataList.size() > 0)
            saveTableBusesIntoForm(busDataList, dataUUID, "table_bus_data");

    }

    public XSSFWorkbook getXlSXList(String dataUUID, String auth, Set<String> excludes) throws IOException, URISyntaxException {
        categories5Percent = excludes;
        String identifier = getFileIdentifier(dataUUID);
        XSSFWorkbook myWorkBook = null;
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

        List<BusData> busDataList = new ArrayList<>();

        TreeSet<String> categories = getTableCategories(dataUUID);
        //if (mySheet != null)
        //saveTableCategoryIntoForm(categories, dataUUID, "table-categories");
        //if (busDataList.size() > 0)
        //saveTableBusesIntoForm(busDataList, dataUUID, "table_bus_data");

        for (String category : categories) {
            System.out.println(category);
        }

        ClassPathResource classPathResource = new ClassPathResource("shablon.xlsx");
        FileInputStream file = new FileInputStream(classPathResource.getFile());
        XSSFWorkbook workbook = new XSSFWorkbook(file);
        XSSFSheet sheet = workbook.getSheetAt(0);
        int totalNewRows = 0;
        for (BusData busData : busDataList) {
            totalNewRows = setLabels(sheet, totalNewRows, workbook);
            totalNewRows = setBusData(sheet, totalNewRows, workbook, busData, categories);
            totalNewRows = totalNewRows + 2;
        }
        return workbook;
    }

    private int setBusData(XSSFSheet sheet, Integer totalNewRows, XSSFWorkbook workbook, BusData busData, TreeSet<String> categories) {

        XSSFCellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        cellStyle.getFont().setBold(false);
        cellStyle.setWrapText(true);

        XSSFCellStyle cellStyleClone = cellStyle.clone();
        cellStyleClone.setAlignment(HorizontalAlignment.LEFT);
        cellStyleClone.setVerticalAlignment(VerticalAlignment.TOP);
        String busNumber = busData.getBusNumber();
        int startingRow = totalNewRows;
        for (String category : categories) {
            sheet.createRow(totalNewRows);
            if (!busNumber.isEmpty()) {

                //System.out.println("bus" + i);
                sheet.getRow(totalNewRows).createCell(0);
                sheet.getRow(totalNewRows).getCell(0).setCellValue(busNumber);
                sheet.getRow(totalNewRows).getCell(0).setCellStyle(cellStyleClone);
                busNumber = "";

                sheet.getRow(totalNewRows).createCell(1);
                sheet.getRow(totalNewRows).getCell(1).setCellValue(category);
                sheet.getRow(totalNewRows).getCell(1).setCellStyle(cellStyleClone);
                busNumber = "";
            } else {
                sheet.getRow(totalNewRows).createCell(1);
                sheet.getRow(totalNewRows).getCell(1).setCellValue(category);
                sheet.getRow(totalNewRows).getCell(1).setCellStyle(cellStyleClone);
                busNumber = "";
            }

            for (int i = 2; i < 8; i++) {
                try {
                    sheet.getRow(totalNewRows).createCell(i);
                    sheet.getRow(totalNewRows).getCell(i).setCellStyle(cellStyle);
                } catch (Exception err) {
                    System.out.println(err);
                    System.out.println(err.getMessage());
                }
            }

            for (CategoryEachData categoryEachData : busData.getCategoryEachData()) {
                if (category.equals(categoryEachData.getCategoryName())) {
                    Double[] eachLine = {0.0, 0.0, Double.valueOf(categoryEachData.getTariff()), categoryEachData.getCycles(),
                            categoryEachData.getBasic_price_sum(), categoryEachData.getBeneficiaries_sum(),
                            categoryEachData.getBasic_price_percent(), categoryEachData.getBeneficiaries_percent()};
                    for (int i = 2; i < 8; i++) {
                        try {
                            //System.out.println(i);
                            sheet.getRow(totalNewRows).getCell(i).setCellValue(eachLine[i]);

                        } catch (Exception err) {
                            System.out.println(err);
                            System.out.println(err.getMessage());
                        }
                    }
                }
            }
            totalNewRows++;
        }
        try {
            sheet.addMergedRegion(new CellRangeAddress(startingRow, totalNewRows - 1, 0, 0));
        } catch (Exception exception) {
            System.out.println("merging Exception");
            //System.out.println(exception);
        }
        return totalNewRows;
    }

    private int setLabels(XSSFSheet sheet, Integer totalNewRows, XSSFWorkbook workbook) {
        String[] labelsNames = {"Гос.№\n", "Наименование карт \"ОНАЙ\"\n", "Тариф карт\n", "Итого транзакции\n", "по тарифу 80 тг\n", "Льготники 40 тг.\n", "80 тг\n", "Льготники\n"};
        if (totalNewRows < sheet.getLastRowNum()) {
            sheet.shiftRows(totalNewRows, sheet.getLastRowNum(), 1, true, false);
        }
        sheet.createRow(totalNewRows);

        XSSFCellStyle xSSFCellStyle = workbook.createCellStyle();
        xSSFCellStyle.setBorderBottom(BorderStyle.THIN);
        xSSFCellStyle.setBorderLeft(BorderStyle.THIN);
        xSSFCellStyle.setBorderRight(BorderStyle.THIN);
        xSSFCellStyle.setBorderTop(BorderStyle.THIN);
        xSSFCellStyle.setAlignment(HorizontalAlignment.CENTER);
        xSSFCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        xSSFCellStyle.getFont().setBold(true);
        xSSFCellStyle.setWrapText(true);

        XSSFCellStyle cellStyleClone = xSSFCellStyle.clone();
        cellStyleClone.setAlignment(HorizontalAlignment.LEFT);
        cellStyleClone.setVerticalAlignment(VerticalAlignment.TOP);

        for (int i = 0; i < labelsNames.length; i++) {
            try {
                sheet.getRow(totalNewRows).createCell(i);
                sheet.getRow(totalNewRows).getCell(i).setCellValue(labelsNames[i]);
                if (i < 2) {
                    sheet.getRow(totalNewRows).getCell(i).setCellStyle(cellStyleClone);
                } else {
                    sheet.getRow(totalNewRows).getCell(i).setCellStyle(xSSFCellStyle);
                }
            } catch (Exception err) {
                System.out.println(err.getMessage());
            }
        }
        totalNewRows++;

        return totalNewRows;
    }

    private void saveTableBusesIntoForm(List<BusData> busDataList, String dataUUID, String tableID) {
        TableData tableData = new TableData(dataUUID, tableID);

        int i = 1;
        for (BusData busData : busDataList) {

            tableData.getData().add(new RowData("bus-number-b" + i, "textbox", busData.getBusNumber()));

            tableData.getData().add(new RowData("transactions_total-b" + i, "textbox", String.valueOf(busData.getCycles())));

            tableData.getData().add(new RowData("total_sum-b" + i, "textbox", String.valueOf(busData.getSum())));

            tableData.getData().add(new RowData("total_sum_percent-b" + i, "textbox", String.valueOf(busData.getBasic_price_percent())));

            tableData.getData().add(new RowData("beneficiaries_percent-b" + i, "textbox", String.valueOf(busData.getBeneficiaries_percent())));

            i++;
        }
        Response response = feignClientRepo.saveTableData(getAuthorization(), tableData);
    }

    private void saveTableCategoryIntoForm(TreeSet<String> categories, String dataUUID, String tableID) {
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

    private TreeSet<String> getDriveWayCategories(XSSFSheet mySheet, List<BusData> busDataList) {
        TreeSet<String> categories = new TreeSet<String>();
        Iterator<Row> it = mySheet.iterator();
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

                if (!cellValue.trim().isEmpty() && !cellValue.contains("Категория проездного") && !cellValue.contains("01.10")) {
                    // добавление имени категории в массив
                    categories.add(cellValue);


                    // получение значения транзакций
                    Double cycleValue = Double.NaN;
                    try {
                        cycleValue = row.getCell(6).getNumericCellValue();
                    } catch (Exception err) {
                        LOGGER.error(String.valueOf(err));
                    }
                    BusData busData = busDataList.get(busDataList.size() - 1);
                    // каждая строка с данными
                    CategoryEachData categoryEachData = new CategoryEachData(cellValue);

                    if (!cycleValue.isNaN()) {
                        busData.setCycles(busData.getCycles() + cycleValue);
                        categoryEachData.setCycles(cycleValue);
                    }

                    Double sumValue = 0.00;

                    try {
                        sumValue = row.getCell(7).getNumericCellValue();
                    } catch (Exception err) {
                        LOGGER.error(String.valueOf(err));
                    }

                    if (!cycleValue.isNaN()) {
                        busData.setSum(busData.getSum() + sumValue);
                        if (Arrays.asList(categoryWithNoPrice).contains(cellValue)) {
                            // 0
                        } else if (Arrays.asList(category40).contains(cellValue)) {
                            categoryEachData.setBeneficiaries_sum(sumValue);
                            categoryEachData.setTariff(40);
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
                            categoryEachData.setTariff(80);
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
                } else {
                    //System.out.println("Не прошедшие квалификацию " + cellValue);
                }

            }
        } catch (Exception err) {
            LOGGER.error(String.valueOf(err));
        }

//        for (String category : categories) {
//            System.out.println(category);
//        }

//        for (BusData busData : busDataList) {
//            System.out.println(busData);
//        }
        return categories;
    }

    private TreeSet<String> getTableCategories(String dataUUID) throws URISyntaxException, IOException {
        TreeSet<String> categories = new TreeSet<>();
        String[] categoryWithNoPrice = new String[]{};
        String[] categoriesArray = new String[]{};
        String URI = url + "rest" +
                "/api" +
                "/asforms" +
                "/data" +
                "/get" +
                "?dataUUID=" + dataUUID;

        JSONArray jsonArray = parseFeignToJSONArray(getRequest(getAuthorization(), URI));
        JSONArray results = jsonArray.optJSONObject(0).optJSONArray("data");

        int length = results.length();
        for (int i = 0; i < length; i++) {
            JSONObject object = results.optJSONObject(i);
            if (object.optString("id").equals("table-categories")) {
                JSONArray categoriesData = object.optJSONArray("data");
                for (int y = 0; y < categoriesData.length(); y++) {
                    JSONObject catObject = categoriesData.optJSONObject(y);
                    //System.out.println(catObject);
                    //System.out.println(catObject.optString("value"));

                    String catValue = catObject.optString("id");

                    if (!catValue.isEmpty() && catValue.contains("name")) {
                        if (!catObject.optString("value").isEmpty()) {
                            System.out.println("value " + catObject.optString("value"));
                            if (catValue.charAt(catValue.length() - 2) == 'b') {
                                System.out.println("first part = " + catValue.substring(catValue.length() - 1));
                            } else {
                                System.out.println("second part = " + catValue.substring(catValue.length() - 2));
                            }
                        }
                    } else if (!catValue.isEmpty() && catValue.contains("price")) {
                        if (!catObject.optString("value").isEmpty()) {

                        } else {

                        }
                        categoryWithNoPrice[categoryWithNoPrice.length] = "";
                    } else if (!catValue.isEmpty() && catValue.contains("percentage")) {

                    } else if (!catValue.isEmpty() && catValue.contains("param")) {

                    }
                }
            }
        }
        return categories;
    }

    private String getFileIdentifier(String dataUUID) throws URISyntaxException, IOException {
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

    private ByteArrayInputStream getFileByteArray(String identifier) throws URISyntaxException, IOException {
        String URI = url + "rest" +
                "/api" +
                "/storage" +
                "/file" +
                "/get" +
                "?identifier=" + identifier;
        return parseFeignToByteArray(getRequest(getAuthorization(), URI));
    }

    private ByteArrayInputStream parseFeignToByteArray(Response response) throws IOException {
        byte[] out = IOUtils.toByteArray(response.body().asInputStream());
        return new ByteArrayInputStream(out);
    }

    private JSONArray parseFeignToJSONArray(Response response) {
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

    private Response getRequest(String auth, String url) throws URISyntaxException {
        return feignClientRepo.getRequest(auth, new URI(url));
    }

    private String getAuthorization() {
        return "Basic MTox";
    }

    @Data
    public static class BusData {
        BusData(String busNumber) {
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
    public static class CategoryEachData {
        private String categoryName;

        CategoryEachData(String categoryName) {
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
