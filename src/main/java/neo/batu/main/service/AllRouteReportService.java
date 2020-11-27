package neo.batu.main.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import feign.Response;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import neo.batu.main.Entity.ComponentData;
import neo.batu.main.repo.FeignClientRepo;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AllRouteReportService {
    @Value("${host}")
    private String url;

    private static int rowcol = 12;

    private final FeignClientRepo feignClientRepo;
    private final DriverTimeSheetService driverTimeSheetService;
    private static final Gson gson = new Gson();

    public XSSFWorkbook fillTimeSheetByRoutesAndDate(String date, String auth) throws IOException, URISyntaxException {
        rowcol = 12;
        Resource resource = new ClassPathResource("all-route-report.xlsx");
        FileInputStream file = new FileInputStream(resource.getFile());
        XSSFWorkbook workbook = new XSSFWorkbook(file);
        XSSFSheet route = workbook.getSheetAt(0);
        XSSFSheet driver = workbook.getSheetAt(1);

        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        XSSFFont font = workbook.createFont();
        font.setFontName("Arial Narrow");
        style.setFont(font);

        List<List<Components>> results = getResults(auth, date);

        for (List<Components> components : results) {
            fillTimeSheet(route, style, components.get(0).getDataUUID(), auth);
            fillDriverSheet(driver, style, components.get(0).getDataUUID(), auth);
        }

        return workbook;
    }

    public List<List<Components>> getResults(String auth, String date) throws URISyntaxException {
        String[] routes = {
                "ezhednevnyi_otchet_v_razreze_avtobusov_1_marshrut",
                "ezhednevnyi_otchet_v_razreze_avtobusov_102_marshrut",
                "ezhednevnyi_otchet_v_razreze_avtobusov_122_marshrut",
                "ezhednevnyi_otchet_v_razreze_avtobusov_14_marshrut",
                "ezhednevnyi_otchet_v_razreze_avtobusov_15_marshrut",
                "ezhednevnyi_otchet_v_razreze_avtobusov_22_marshrut",
                "ezhednevnyi_otchet_v_razreze_avtobusov_46_marshrut",
                "ezhednevnyi_otchet_v_razreze_avtobusov_48_marshrut"
        };

        List<List<Components>> results = new ArrayList<>();

        for (String registryId : routes) {
            List<Components> result = getResult(auth, registryId, date);
            if (!result.isEmpty())
                results.add(result);
        }

        return results;
    }

    public void fillTimeSheet(XSSFSheet sheet, CellStyle style, String dataUUID, String auth) throws URISyntaxException {
        List<ComponentData> mainData = getDataByDataUUID(dataUUID, auth, url);

        setRoute(sheet, mainData, style);
        rowcol += 20;
        setDate(sheet, mainData, style);
        rowcol += 20;
//        setHeader(sheet, mainData, style);
//        rowcol+= 10;
        setTable(sheet, mainData, style);
        rowcol += 10;
        setFooter(sheet, mainData, style);
        rowcol += 30;
    }

    public void fillDriverSheet(XSSFSheet sheet, CellStyle style, String dataUUID, String auth) throws URISyntaxException {
        List<ComponentData> mainData = getDataByDataUUID(dataUUID, auth, url);
        driverTimeSheetService.setRoute(sheet, mainData, style);
        driverTimeSheetService.setDate(sheet, mainData, style);
        driverTimeSheetService.setTimeSheetLabel(sheet, mainData, style);
        driverTimeSheetService.setTimeSheet(sheet, mainData, style);
        driverTimeSheetService.setTimeSheetTotal(sheet, mainData, style);
    }

    public List<Components> getResult(String auth, String registryCode, String date) throws URISyntaxException {
        Type type = new TypeToken<Registry>() {
        }.getType();
        Response response = feignClientRepo.getRequest(auth, new URI(url + "rest/api/registry/data_ext?registryCode=" + registryCode +
                "&field=date-worked&condition=TEXT_EQUALS&value=" + date));
        Registry registry;
        try {
            try (BufferedReader buffer = new BufferedReader(new InputStreamReader(response.body().asInputStream()))) {
                String resp = buffer.lines().collect(Collectors.joining("\n"));
                resp = resp.replaceAll("date-worked", "date_worked");
                registry = gson.fromJson(resp, type);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to process response body.", ex);
        }

        return registry.getResult();
    }

    public List<ComponentData> getDataByDataUUID(String dataUUID, String auth, String url) throws URISyntaxException {
        Type type = new TypeToken<List<mainData>>() {
        }.getType();
        Response response = feignClientRepo.getRequest(auth, new URI(url + "rest/api/asforms/data/get?dataUUID=" + dataUUID));
        List<mainData> mainData;
        try {
            try (BufferedReader buffer = new BufferedReader(new InputStreamReader(response.body().asInputStream()))) {
                String resp = buffer.lines().collect(Collectors.joining("\n"));
                mainData = gson.fromJson(resp, type);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to process response body.", ex);
        }
        return mainData.get(0).getData();
    }

    public void setRoute(XSSFSheet sheet ,List<ComponentData> data, CellStyle style) {
        String routeId = "label-y823g6";
        ComponentData route = findComponentData(routeId, data);
        XSSFRow row = sheet.createRow(rowcol / 10);
        XSSFCell cell = row.createCell((rowcol - 1) % 10);
        cell.setCellStyle(style);
        cell.setCellValue("Номер маршрута:");
        if (route != null && route.getLabel() != null) {
            XSSFCell cell1 = row.createCell(rowcol % 10);
            cell1.setCellStyle(style);
            cell1.setCellValue(route.getLabel());
        }
    }


    public void setDate(XSSFSheet sheet, List<ComponentData> data, CellStyle style) {
        String dateId = "date-worked";
        ComponentData date = findComponentData(dateId, data);
        XSSFRow row = sheet.createRow(rowcol / 10);
        XSSFCell cell = row.createCell((rowcol - 1) % 10);
        cell.setCellStyle(style);
        cell.setCellValue("Дата создания отчета:");
        if (date != null && date.getValue() != null) {
            XSSFCell cell1 = row.createCell(rowcol % 10);
            cell1.setCellStyle(style);
            cell1.setCellValue(date.getValue());;
        }
    }

    public void setTable(XSSFSheet sheet, List<ComponentData> mainData, CellStyle style) {
        String tableId = "table-ku7cbe";
        ComponentData table = findComponentData(tableId, mainData);
        XSSFRow row = sheet.createRow(rowcol / 10);
        int columns = getLabelAmountInTable(table.getData());
        int counter = 1;
        for (ComponentData data : table.getData()) {
            if (counter > columns) {
                rowcol += 10;
                counter = 1;
                row = sheet.createRow(rowcol / 10);
            }
            XSSFCell cell = row.createCell(counter);
            if (data.getLabel() != null) {
                try {
                    cell.setCellValue(Double.parseDouble(data.getLabel()));
                }catch (Exception e) {
                    cell.setCellValue(data.getLabel());
                }
                style.setWrapText(true);
            }else if (data.getValue() != null && !data.getValue().equals("undefined")) {
                try {
                    cell.setCellValue(Double.parseDouble(data.getValue()));
                }catch (Exception e) {
                    cell.setCellValue(data.getValue());
                }
            }
            cell.setCellStyle(style);
            counter++;
        }
    }

    public void setHeader(XSSFSheet sheet, List<ComponentData> mainData, CellStyle style) {
        String[] labelIds = {"label-kr25nx", "label-2y6qe9", "label-ujyqk6", "label-u09d7u"};

    }

    public void setFooter(XSSFSheet sheet, List<ComponentData> mainData, CellStyle style) {
        String[] totalIds = {"listbox-qbkvn3", "label-we6ptc", "b"};
        XSSFRow row = sheet.createRow(rowcol / 10);
        int counter = 1;
        for (String ids : totalIds) {
            if (ids.equals("b")) {
                for (int i = 1;;i++) {
                    ComponentData data = findComponentData(ids + i, mainData);
                    if (data == null) {
                        break;
                    }
                    XSSFCell cell = row.createCell(counter + i - 1);
                    cell.setCellStyle(style);
                    if (data.getLabel() != null) {
                        try {
                            cell.setCellValue(Double.parseDouble(data.getLabel()));
                        }catch (Exception e) {
                            cell.setCellValue(data.getLabel());
                        }
                    } else if (data.getValue() != null && !data.getValue().equals("undefined")) {
                        try {
                            cell.setCellValue(Double.parseDouble(data.getValue()));
                        }catch (Exception e) {
                            cell.setCellValue(data.getValue());
                        }
                    }
                }
            } else {
                ComponentData data = findComponentData(ids, mainData);
                XSSFCell cell = row.createCell(counter);
                if (data != null) {
                    if (data.getLabel() != null) {
                        try {
                            cell.setCellValue(Double.parseDouble(data.getLabel()));
                        }catch (Exception e) {
                            cell.setCellValue(data.getLabel());
                        }
                    } else if (data.getValue() != null && !data.getValue().equals("undefined")) {
                        try {
                            cell.setCellValue(Double.parseDouble(data.getValue()));
                        }catch (Exception e) {
                            cell.setCellValue(data.getValue());
                        }
                    }
                }
                if (ids.equals("listbox-qbkvn3")) {
                    sheet.addMergedRegion(new CellRangeAddress(rowcol/10, rowcol/10, counter,counter + 1));
                    counter++;
                    XSSFCell cell1 = row.createCell(counter);
                    cell1.setCellStyle(style);
                }
                cell.setCellStyle(style);
            }
            counter++;
        }
    }

    private int getLabelAmountInTable(List<ComponentData> components) {
        int columns = 0;
        for (ComponentData data : components) {
            if (data.getType().equals("label")) {
                columns++;
                continue;
            }
            break;
        }
        return columns;
    }

    public ComponentData findComponentData (String id, List<ComponentData> mainData) {
        for (ComponentData ComponentData : mainData) {
            if (ComponentData.getId().equals(id)) {
                return ComponentData;
            }
        }
        return null;
    }

    @Data
    public static class mainData {
        private String uuid;
        private List<ComponentData> data;
    }

    @Data
    public static class Registry {
        List<Components> result;
    }

    @Data
    public static class Components {
        String dataUUID;
        FieldValue fieldValue;
    }

    @Data
    public static class FieldValue {
        String date_worked;
    }
}
