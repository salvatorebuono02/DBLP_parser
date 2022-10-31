import org.dblp.mmdb.Publication;

import java.util.ArrayList;
import java.util.List;

public interface Context {

    void insertNewArticle(String articleID);

    List<String> getRelatedPublications();

    List<String> generateCSVEntry();

    List<String> generateCSVEntry(String publicationID);

    String getTitle();
}
