import org.dblp.mmdb.Publication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Conference implements Context {
    private final String id;
    private final String title;
    private final MyPublication publication;

    private final List<String> relatedPublications;

    public Conference(MyPublication publication) {
        this.publication = publication;
        this.id = publication.getKey();
        this.title = this.publication.getTitle();
        this.relatedPublications = this.publication.getPublicationsIn(publication);
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
        entry_context.add(this.getConfName());
        entry_context.add(String.valueOf(this.publication.getYear()));
        entry_context.add(this.publication.getVolume());
        entry_context.add(this.publication.getPublisher());
        entry_context.add(this.publication.getURL());
        //entry_context.add(context.getPublication().getTag());
        return entry_context;
    }

    private String getConfName() {
        String confName;
        if (this.publication.getBooktitle() != null)
            confName = this.publication.getBooktitle().getTitle();
        else
            confName = this.publication.getSeries();
        return confName;
    }

    @Override
    public List<String> generateCSVEntry(String publicationID) {
        return Arrays.asList(this.id, publicationID);
    }

    @Override
    public String getKey() {
        return this.id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Conference that = (Conference) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
