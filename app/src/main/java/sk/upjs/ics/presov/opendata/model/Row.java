package sk.upjs.ics.presov.opendata.model;

import java.util.Map;

public class Row {

    private Map<String, String> values;

    public Row(Map<String, String> values) {
        this.values = values;
    }

    public Map<String, String> getValues() {
        return values;
    }

    public String get(String key) {
        return values.get(key);
    }
}
