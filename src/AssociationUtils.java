import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

public abstract class AssociationUtils {

    private static final String ASSOC_JSON_FILE_NAME = "associations.json";

    private static List<Association> associations = new ArrayList<>();
    private static final Random random = new Random();
/*
    public static Set<List<String>> generateCSVEntries() {
        List<String> associationNames = new ArrayList<>(Arrays.asList("MIT", "Politecnico di Milano", "CERN", "Max " +
                        "Planck Institute", "Harvard University", "Stanford University", "University of Cambridge",
                "Brookhaven National Laboratory", "Bell Laboratories", "SLAC", "Politecnico di Bari", "Universita La " +
                        "Sapienza", "CNR", "Fermilab", "University of Oxford", "University of California"));

        Set<List<String>> association_entries = new HashSet<>();
        for (int i = 0; i < associationNames.size(); i++) {
            String id = "assoc/" + i+1;
            String name = associationNames.get(i);
            association_entries.add(Arrays.asList(id, name));
            associations.add(new Association(id, name));
        }

        return association_entries;
    }

 */

    public static Set<List<String>> generateCSVEntriesViaJSON() {
        try {
            JsonReader jsonReader = new JsonReader(new FileReader(ASSOC_JSON_FILE_NAME));
            Association[] associationsFromJson = new Gson().fromJson(jsonReader, Association[].class);
            associations.addAll(Arrays.asList(associationsFromJson));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Set<List<String>> association_entries = new HashSet<>();
        associations.forEach(a -> {
            String id = "assoc/" + (associations.indexOf(a) + 1);
            a.setId(id);
            association_entries.add(a.generateCSVEntry());
        });

        return association_entries;
    }

    private static Association createAssociation(JsonElement json) {
        return new Gson().fromJson(json, Association.class);
    }

    public static Association getRandomAssociation() {
        int i = random.nextInt(associations.size());
        return associations.get(i);
    }
}
