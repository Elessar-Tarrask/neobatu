package neo.batu.main.RestCont;

import lombok.RequiredArgsConstructor;
import neo.batu.main.service.ALLTransportService;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping({"/api/parse/"})
@RequiredArgsConstructor
public class ALLTransportRestCont {

    private final ALLTransportService allTransportService;

    private static SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");

    @GetMapping("/test")
    public String getFilterList(@RequestParam(value = "dataUUID", required = true) String dataUUID) throws URISyntaxException, IOException {
        return allTransportService.test(dataUUID);
    }

    @GetMapping({"/template"})
    public ResponseEntity<ByteArrayResource> downloadTemplate(
            @RequestParam(value = "dataUUID", required = true) String dataUUID,
            @RequestParam(value = "tableID", required = true) String tableID,
            @RequestParam(value = "excludes", required = true) Set<String> excludes,
            @RequestHeader("Authorization") String auth) throws Exception {
        try {
            Date date = new Date();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            XSSFWorkbook workbook = allTransportService.getXlSXList(dataUUID, auth, tableID, excludes);
            HttpHeaders header = new HttpHeaders();
            header.setContentType(new MediaType("application", "force-download"));
            header.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sales_plan_" + formatter.format(date) + ".xlsx");
            workbook.write(stream);
            workbook.close();
            return new ResponseEntity<>(new ByteArrayResource(stream.toByteArray()),
                    header, HttpStatus.CREATED);
        } catch (Exception e) {
            System.out.println(e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
