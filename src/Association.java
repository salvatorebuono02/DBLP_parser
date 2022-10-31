import java.util.Arrays;
import java.util.List;

public class Association {
    private String id;
    private String name;
    private String country;
    private String address;
    private String website;
    private String funding;

    public Association(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> generateCSVEntry() {
        return Arrays.asList(this.id, this.name, this.country, this.address, this.website, this.funding);
    }
}
