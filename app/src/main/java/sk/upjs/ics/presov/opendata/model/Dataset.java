package sk.upjs.ics.presov.opendata.model;

import java.util.List;
import java.util.Map;

public class Dataset {

    private final DatasetInfo datasetInfo;

    private final Map<String, String> columns;

    private final List<Row> rows;

    public Dataset(DatasetInfo datasetInfo, Map<String, String> columns, List<Row> rows) {
        this.columns = columns;
        this.datasetInfo = datasetInfo;
        this.rows = rows;
    }

    public DatasetInfo getDatasetInfo() {
        return datasetInfo;
    }

    public Map<String, String> getColumns() {
        return columns;
    }

    public List<Row> getRows() {
        return rows;
    }
}
