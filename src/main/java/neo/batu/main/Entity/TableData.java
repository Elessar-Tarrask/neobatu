package neo.batu.main.Entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TableData {
    private String uuid;

    public TableData(String uuid, String tableId) {
        this.uuid = uuid;
        this.tableId = tableId;
        this.data = new ArrayList<>();
    }

    private String tableId;
    private List<RowData> data;
}
