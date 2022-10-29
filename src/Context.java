import org.dblp.mmdb.Publication;

import java.util.ArrayList;
import java.util.List;

public class Context {
    private final String type;
    private final String id;
    private final String name;
    private final Publication publication;

    private List<String> relatedPublications = new ArrayList<>();

    public Context(Publication publication) {
        this.type = "conf";
        this.id = publication.getKey();
        this.name = PublicationUtils.getTitle(publication);
        this.publication = publication;
        this.relatedPublications = PublicationUtils.getPublicationsIn(publication);
    }

    public Context(String journalName, String articleID) {
        this.type = "journal";
        this.name = journalName;
        this.id = generateContextKey();
        this.publication = null;
        this.relatedPublications.add(articleID);
    }

    private String generateContextKey() {
        // works with pub.getKey()
        /*
        int lastSlashIdx = name.length();
        for (int i = name.length() - 1; i >= 0; i--) {
            if (name.charAt(i) == '/')
                lastSlashIdx = i;
        }
        return name.substring(0, lastSlashIdx);
         */
        return type + "/" + name.hashCode();
    }
    public void insertNewArticle(String articleID) {
        this.relatedPublications.add(articleID);
    }

    public List<String> getRelatedPublications() {
        return relatedPublications;
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Publication getPublication() {
        return publication;
    }
}
