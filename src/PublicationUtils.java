import org.dblp.mmdb.Field;
import org.dblp.mmdb.Publication;
import org.dblp.mmdb.PublicationIDType;
import org.dblp.mmdb.Record;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class PublicationUtils {

    public static String getTitle(Publication publication) {
        Field title = publication.getFields("title").stream().findFirst().orElse(null);
        if (title == null)
            return "";
        return title.value();
    }

    public static String getPages(Publication publication) {
        Field pages = publication.getFields("pages").stream().findFirst().orElse(null);
        if (pages == null)
            return "";
        return pages.value();
    }

    // TODO meaning??
    public static String getTypeOfISBN(Publication publication) {
        Field DOI = publication.getFields("ee").stream().findFirst().orElse(null); // DOI not always present... we should distinguish each type of pubs (some has ISBN)
        Field ISBN = publication.getFields("isbn").stream().findFirst().orElse(null);
        if ( //publication.getIdTypes().contains(PublicationIDType.DOI) &&
                DOI != null)
            return DOI.value();
        if (//publication.getIdTypes().contains(PublicationIDType.ISBN) &&
            ISBN != null)
            return ISBN.value();
        return "";
    }


    public static String getURL(Publication publication) {
        Field URL = publication.getFields("url").stream().findFirst().orElse(null);
        if (URL == null)
            return "";
        return URL.value();
    }

    public static String getCrossRef(Publication publication){
        Field crossref = publication.getFields("cite").stream().findFirst().orElse(null);
        if (crossref == null)
            return "";
        return crossref.value();
    }

    public static List<String> getCitations(Publication publication){
        if(!publication.getFields("cite").isEmpty())
            return publication.getFields("cite").stream().map(Field::value).filter(c->!c.equals("...")).collect(Collectors.toList());
        return new ArrayList<>();
    }

    public static List<String> getPublicationsIn(Publication context){
        if(context.getToc() != null){
            List<String> pubs = context.getToc().getPublications().stream().map(Record::getKey).collect(Collectors.toList());
            pubs.remove(context.getKey());
            return pubs;
        }
        return new ArrayList<>();
    }


    public static String getSchool(Publication publication) {
        Field school = publication.getFields("school").stream().findFirst().orElse(null);
        if (school == null)
            return "";
        return school.value();
    }

    public static String getPublisher(Publication publication) {
        Field publisher = publication.getFields("publisher").stream().findFirst().orElse(null);
        if (publisher == null)
            return "";
        return publisher.value();
    }
}
