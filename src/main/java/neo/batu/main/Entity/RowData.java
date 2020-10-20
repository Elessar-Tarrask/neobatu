package neo.batu.main.Entity;

import lombok.Data;

@Data
public class RowData {
    public RowData(String id, String type, String value) {
        this.id = id;
        this.type = type;
        this.value = value;
        if (!type.equals("check")) {
            this.key = value;
        } else {
            this.key = "['']";
        }
    }

    private String id;
    private String type;
    private String value;
    private String key;
}
