package neo.batu.main.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import feign.Response;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import neo.batu.main.Entity.ComponentData;
import neo.batu.main.repo.FeignClientRepo;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.*;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DriverTimeSheetService {

    private final FeignClientRepo feignClientRepo;
    private static final Gson gson = new Gson();

    @Value("${host}")
    private String url;
    private int rowcol = 1;

    public XSSFWorkbook getXlSXList(String dataUUID, String auth) throws IOException, URISyntaxException {
        List<ComponentData> mainData = getDataByDataUUID(dataUUID, auth, url);

        Resource resource = new ClassPathResource("driver-timesheet.xlsx");
        FileInputStream file = new FileInputStream(resource.getFile());
        XSSFWorkbook workbook = new XSSFWorkbook(file);
        XSSFSheet sheet = workbook.getSheetAt(0);

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

        setRoute(sheet, mainData, style);
        setDate(sheet, mainData, style);
        setTimeSheetLabel(sheet, mainData, style);
        setTimeSheet(sheet, mainData, style);
        setTimeSheetTotal(sheet, mainData, style);

        return workbook;
    }

    public List<ComponentData> getDataByDataUUID(String dataUUID, String auth, String url) throws IOException, URISyntaxException {
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
        rowcol += 10;
        ComponentData route = findComponentData(routeId, data);
        if (sheet.getRow((rowcol + 1) / 10) == null) {
            sheet.createRow((rowcol + 1) / 10);
        }
        if (route != null && route.getLabel() != null) {
            XSSFCell cell, cell1;
            cell = sheet.getRow((rowcol + 1) / 10).createCell((rowcol) % 10);
            cell.setCellValue("Номер маршрута:");
            cell.setCellStyle(style);
            cell1 = sheet.getRow((rowcol + 1) / 10).createCell((rowcol  + 1) % 10);
            cell1.setCellValue(route.getLabel());
            cell1.setCellStyle(style);
        }
    }

    public void setDate(XSSFSheet sheet, List<ComponentData> data, CellStyle style) {
        String dateId = "date-worked";
        rowcol += 20;
        ComponentData date = findComponentData(dateId, data);
        if (sheet.getRow((rowcol + 1) / 10) == null) {
            sheet.createRow((rowcol + 1) / 10);
        }
        if (date != null && date.getValue() != null) {
            XSSFCell cell, cell1;
            cell = sheet.getRow((rowcol + 1) / 10).createCell((rowcol) % 10);
            cell.setCellValue("Дата создания отчета:");
            cell.setCellStyle(style);
            cell1 = sheet.getRow((rowcol + 1) / 10).createCell((rowcol + 1) % 10);
            cell1.setCellValue(date.getValue());
            cell1.setCellStyle(style);
        }
    }

    public void setTimeSheetLabel(XSSFSheet sheet, List<ComponentData> data, CellStyle style) {
        String labelId = "label-zc0ofr";
        rowcol += 20;
        ComponentData label = findComponentData(labelId, data);
        if (sheet.getRow((rowcol) / 10) == null) {
            sheet.createRow((rowcol) / 10);
        }
        if (label != null && label.getLabel() != null) {
            XSSFCell cell;
            cell = sheet.createRow(rowcol / 10).createCell(rowcol % 10);
            cell.setCellValue(label.getLabel());
            cell.setCellStyle(style);
        }
    }

    public void setTimeSheet(XSSFSheet sheet, List<ComponentData> mainData, CellStyle style) {
        String tableId = "table-x191df";
        rowcol += 20;
        int count = 1;
        ComponentData table = findComponentData(tableId, mainData);
        if (table != null) {
            int end = countLabels(table.getData());
            if (table.getData() != null) {
                List<ComponentData> datas = table.getData();
                XSSFRow row = sheet.createRow(rowcol / 10);
                for (ComponentData data : datas) {
                    XSSFCell cell = row.createCell(count);
                    if (data.getLabel() != null) {
                        try {
                            cell.setCellValue(Double.parseDouble(data.getLabel()));
                        }catch (Exception e) {
                            cell.setCellValue(data.getLabel());
                        }
                    } else if (data.getValue() != null) {
                        try {
                            cell.setCellValue(Double.parseDouble(data.getValue()));
                        }catch (Exception e) {
                            cell.setCellValue(data.getValue());
                        }
                    }
                    cell.setCellStyle(style);
                    count++;
                    if (count > end) {
                        count = 1;
                        rowcol += 10;
                        row = sheet.createRow(rowcol / 10);
                    }
                }
            }
        }
    }

    public void setTimeSheetTotal(XSSFSheet sheet, List<ComponentData> mainData, CellStyle style) {
        String[] valueIds = {"listbox-qbkvn3_copy2", "label-0dxdvw_copy1", "t_itog"};
        int rowNum = rowcol / 10, colNum = 1;
        XSSFRow row = sheet.createRow(rowNum);

        for (String valueId : valueIds) {
            if (valueId.equals("t_itog")) {
                for (int i = 1; i <= 34;i++) {
                    ComponentData data = i != 33 ?
                            findComponentData(i + valueId, mainData) :
                            findComponentData("ra_zarp1", mainData);

                    XSSFCell cell = row.createCell(colNum + i - 1);

                    if (data != null && data.getValue() != null) {
                        try {
                            cell.setCellValue(Double.parseDouble(data.getValue()));
                        }catch (Exception e) {
                            cell.setCellValue(data.getValue());
                        }
                    }

                    cell.setCellStyle(style);

                }
            }else {
                ComponentData label = findComponentData(valueId, mainData);
                XSSFCell cell = row.createCell(colNum);

                if (label != null) {
                    if (label.getLabel() != null) {
                        try {
                            cell.setCellValue(Double.parseDouble(label.getLabel()));
                        }catch (Exception e) {
                            cell.setCellValue(label.getLabel());
                        }
                    }else if (label.getValue() != null) {
                        try {
                            cell.setCellValue(Double.parseDouble(label.getValue()));
                        }catch (Exception e) {
                            cell.setCellValue(label.getValue());
                        }
                    }
                }

                cell.setCellStyle(style);
            }
            colNum++;
        }
        rowcol += 20;
    }

    private int countLabels(List<ComponentData> datas) {
        int count = 0;
        for (ComponentData data : datas) {
            if (data.getType().equals("label")) {
                count++;
            }
        }
        return count;
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
}
