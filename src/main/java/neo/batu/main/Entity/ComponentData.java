package neo.batu.main.Entity;

import lombok.Data;
import neo.batu.main.service.AllRouteReportService;

import java.util.List;

@Data
public class ComponentData {
    private String id;
    private String type;
    private String value;
    private String key;
    private List<ComponentData> data;
    private String label;
}
