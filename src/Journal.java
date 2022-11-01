import org.dblp.mmdb.Publication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Journal implements Context {
    //private final String type = "journal";
    private final String id;
    private final String title;
    private final Publication publication;

    private final List<String> relatedPublications = new ArrayList<>();

    public Journal(Publication publication) {
        this.publication = publication;
        this.id = generateContextKey();
        this.title = publication.getJournal().getTitle();
        this.relatedPublications.add(publication.getKey());
    }

    private String generateContextKey() {
        String pubKey = publication.getKey();
        int lastSlashIdx = pubKey.length();
        for (int i = 0; i < pubKey.length(); i++) {
            if (pubKey.charAt(i) == '/')
                lastSlashIdx = i;
        }
        return pubKey.substring(0, lastSlashIdx);

        //return type + "/" + title.hashCode();
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    @Override
    public List<String> getRelatedPublications() {
        return this.relatedPublications;
    }


    @Override
    public void insertNewArticle(String articleID) {
        this.relatedPublications.add(articleID);
    }

    @Override
    public List<String> generateCSVEntry() {
        List<String> entry_context = new ArrayList<>();
        entry_context.add(this.id);
        entry_context.add(this.title);
        return entry_context;
    }

    @Override
    public List<String> generateCSVEntry(String publicationID) {
        return Arrays.asList(this.id, publicationID);
    }
}
