package sk.upjs.ics.presov.opendata.model;

public class DatasetInfo {

    private final long id;

    private final String name;

    private String date;

    private final String urlString;

    public DatasetInfo(long id, String name, String urlString) {
        this.id = id;
        this.name = name;
        this.urlString = urlString;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getUrlString() {
        return urlString;
    }

    @Override
    public String toString() {
        return name;
    }
}
