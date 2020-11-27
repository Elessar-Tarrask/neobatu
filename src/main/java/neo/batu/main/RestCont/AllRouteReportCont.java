package neo.batu.main.RestCont;

import lombok.RequiredArgsConstructor;
import neo.batu.main.service.AllRouteReportService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping({"/api/routes/"})
@RequiredArgsConstructor
public class AllRouteReportCont {

    @Autowired
    AllRouteReportService allRouteReportService;

    private static SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");

    @GetMapping({"/template"})
    public ResponseEntity<ByteArrayResource> downloadTemplate(
            @RequestParam(value = "date") String date,
            @RequestParam("Authorization") String auth) throws Exception {
        try {
            Date cur_date = new Date();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            XSSFWorkbook workbook = allRouteReportService.fillTimeSheetByRoutesAndDate(date, auth);
            HttpHeaders header = new HttpHeaders();
            header.setContentType(new MediaType("application", "force-download"));
            header.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=all-route-report_" + formatter.format(cur_date) + ".xlsx");
            workbook.write(stream);
            workbook.close();
            return new ResponseEntity<>(new ByteArrayResource(stream.toByteArray()),
                    header, HttpStatus.CREATED);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
