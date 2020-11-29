package neo.batu.main.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import feign.Response;
import lombok.RequiredArgsConstructor;
import neo.batu.main.Entity.ComponentData;
import neo.batu.main.Entity.Components;
import neo.batu.main.Entity.Registry;
import neo.batu.main.repo.FeignClientRepo;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
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
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StorageParser {
    @Value("${host}")
    private String url;
    private final Gson gson = new Gson();
    private final SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");

    private static int rowcol = 0;

    private final FeignClientRepo feignClientRepo;


    public XSSFWorkbook downloadTemplate(String date, String auth) throws IOException, URISyntaxException {
        Resource resource = new ClassPathResource("зарплата.xlsx");
        FileInputStream file = new FileInputStream(resource.getFile());
        XSSFWorkbook workbook = new XSSFWorkbook(file);
        XSSFSheet sheet = workbook.getSheetAt(0);
        sheet.createFreezePane(0,1);
        sheet.createFreezePane(0,2);

        CellStyle style = setStyle(workbook);

        List<List<Components>> results = getResults(auth, date);

        String date_format = format_date(date);

        fillTimeSheet(sheet, style, results, auth, date_format);

        return workbook;
    }

    private void fillTimeSheet(XSSFSheet sheet, CellStyle style, List<List<Components>> results, String auth, String date) throws URISyntaxException {
        int counter = 0;
        String tableId = "table-2yfcpd";
        for (List<Components> components : results) {
            String dataUUID = components.get(0).getDataUUID();
            List<ComponentData> mainData = getDataByDataUUID(dataUUID, auth, url);
            ComponentData tableData = findComponentData(tableId, mainData);
            if (counter++ == 0) {
                setHeader(sheet, style, date, tableData.getData());
            }
            setTable(sheet, style, tableData.getData());
            setTotal(sheet, style, mainData);
        }
    }

    private void setTotal(XSSFSheet sheet, CellStyle style, List<ComponentData> datas) {
        String[] ids = {"label-0dxdvw", "f1", "f2", "f3", "f4", "f5", "f6", "f71", "f72", "f73", "f8", "f9", "f10", "f11", "f12"};
        XSSFRow row = sheet.createRow(rowcol / 10);
        XSSFCellStyle cellStyle = sheet.getWorkbook().createCellStyle();
        cellStyle.cloneStyleFrom(style);
        cellStyle.setFillForegroundColor(IndexedColors.SKY_BLUE.getIndex());
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        int counter = 0;
        for (String id : ids) {
            XSSFCell cell = row.createCell(counter++);
            cell.setCellStyle(cellStyle);
            ComponentData data = findComponentData(id, datas);
            if (data != null) {
                if (data.getValue() != null) {
                    String str = data.getValue().replaceAll(" ", "");
                    if (str.matches("-?\\d+(\\.\\d+)?")) {
                        cell.setCellValue(Double.parseDouble(data.getValue()));
                    } else {
                        cell.setCellValue(data.getValue());
                    }
                } else if (data.getLabel() != null) {
                    cell.setCellValue(data.getLabel());
                }
            }
        }
        rowcol += 10;
    }

    private void setTable(XSSFSheet sheet, CellStyle style, List<ComponentData> datas) {
        String[] ids = {"fio", "c1", "c2", "c3", "c4", "c5", "c6", "c71", "c72", "c73", "c8", "c9", "c10", "c11", "c12"};

        for (int i = 1;;i++) {
            int counter = 0;
            XSSFRow row = sheet.createRow(rowcol / 10);
            for (String id : ids) {
                XSSFCell cell = row.createCell(counter++);
                cell.setCellStyle(style);
                ComponentData data = findComponentData(id + "-b" + i, datas);
                if (data == null) {
                    return;
                }
                if (data.getValue() != null) {
                    if (data.getValue().matches("-?\\d+(\\.\\d+)?")) {
                        cell.setCellValue(Double.parseDouble(data.getValue()));
                    }else {
                        cell.setCellValue(data.getValue());
                    }
                }
            }
            rowcol += 10;
        }
    }

    private void setHeader(XSSFSheet sheet, CellStyle style, String date, List<ComponentData> data) {
        String[] headers = {
                "label-stm5rf", "label-leyy3e", "label-leyy3e_copy1",
                "label-leyy3e_copy2", "label-leyy3e_copy3", "label-leyy3e_copy4",
                "label-leyy3e_copy5", "textbox-0uu21x", "textbox-gmk3kl",
                "textbox-gmk3kl_copy1", "label-leyy3e_copy7", "label-leyy3e_copy8",
                "label-leyy3e_copy9", "label-leyy3e_copy10", "label-leyy3e_copy11"
        };

        XSSFRow up_head = sheet.createRow(rowcol / 10);
        up_head.createCell(0).setCellValue("Заработная плата водителей");
        up_head.createCell(1).setCellValue("Месяц:");
        up_head.createCell(2).setCellValue(date);
        rowcol += 10;

        XSSFRow down_head = sheet.createRow(rowcol / 10);
        for (int i = 0; i < headers.length; i++) {
            XSSFCell cell = down_head.createCell(i);
            cell.setCellStyle(style);
            ComponentData label = findComponentData(headers[i], data);
            if (label != null) {
                if (label.getLabel() != null) {
                    cell.setCellValue(label.getLabel());
                }else {
                    cell.setCellValue(label.getValue());
                }
            }
        }
        rowcol += 10;
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

    private String format_date(String date) {
        String[] nums = date.split("\\.");
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                .withLocale(new Locale("ru"))
                .format(LocalDate.of(Integer.parseInt(nums[2]), Integer.parseInt(nums[1]), Integer.parseInt(nums[0])));
    }

    private CellStyle setStyle(XSSFWorkbook workbook) {
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
        return style;
    }

    public ComponentData findComponentData (String id, List<ComponentData> mainData) {
        for (ComponentData ComponentData : mainData) {
            if (ComponentData.getId().equals(id)) {
                return ComponentData;
            }
        }
        return null;
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
        Type type = new TypeToken<List<AllRouteReportService.mainData>>() {
        }.getType();
        Response response = feignClientRepo.getRequest(auth, new URI(url + "rest/api/asforms/data/get?dataUUID=" + dataUUID));
        List<AllRouteReportService.mainData> mainData;
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
}
