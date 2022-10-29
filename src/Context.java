import org.dblp.mmdb.Publication;

import java.util.ArrayList;
import java.util.List;

public class Context {
    private final String id;
    private final String name;

    private List<String> publications_related = new ArrayList<>();

    public Context(Publication publication) {
        this.id = publication.getKey();
        this.name = PublicationUtils.getTitle(publication);
        this.publications_related = PublicationUtils.getPublicationsIn(publication);
    }

    public Context(String journalName, String articleID) {
        this.id = generateContextKey();
        this.name = journalName;
        this.publications_related.add(articleID);
    }

    private String generateContextKey() {
        return "contextJournal/" + name.hashCode();
    }
    public void insertNewArticle(String articleID) {
        this.publications_related.add(articleID);
    }

    public List<String> getPublications_related() {
        return publications_related;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
