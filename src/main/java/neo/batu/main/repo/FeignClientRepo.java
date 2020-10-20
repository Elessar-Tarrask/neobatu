package neo.batu.main.repo;

import feign.Headers;
import feign.Response;
import neo.batu.main.Entity.TableData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@FeignClient(value = "report", url = "${host}")
public interface FeignClientRepo {

    @GetMapping("")
    Response getRequest(@RequestHeader("Authorization") String Token, URI baseUrl);

    @PostMapping("")
    ResponseEntity<Void> postRequest(@RequestHeader("Authorization") String Token, URI baseUrl);

    @GetMapping("")
    ResponseEntity<Void> getAuth(@RequestHeader("Authorization") String Token, URI baseUrl);

    @RequestMapping(method = RequestMethod.POST, path = "/rest/api/asforms/data/append_table", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Headers("Content-Type: application/json")
    Response saveTableData(@RequestHeader(value = "Authorization", required = true) String authorizationHeader, @RequestBody TableData body);
}
