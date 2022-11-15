import org.dblp.mmdb.*;
import org.dblp.mmdb.Record;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MyPublication extends Publication {

    private final Publication publication;

    public MyPublication(Publication publication) {
        this.publication = publication;
    }

    public List<String> generateCSVEntry() {
        List<String> entry_publication = new ArrayList<>();
        entry_publication.add(this.getKey());
        entry_publication.add(this.getTag());
        entry_publication.add(this.getTitle());
        entry_publication.add(this.getDOI());
        entry_publication.add(String.valueOf(this.getYear()));
        entry_publication.add(this.getVolume());
        entry_publication.add(this.getPages());
        entry_publication.add(this.getPublisher());
        entry_publication.add(this.getURL());
        entry_publication.add(this.getISBN());
        entry_publication.add(this.getSchool());


        // TODO how to manage those differences? all in one csv?
        // TODO Editor field??


        return entry_publication;
    }

    public String getTitle() {
        Field title = this.getFields("title").stream().findFirst().orElse(null);
        if (title == null)
            return "";
        return title.value();
    }

    public String getPages() {
        Field pages = this.getFields("pages").stream().findFirst().orElse(null);
        if (pages == null)
            return "";
        return pages.value();
    }

    // TODO meaning?
    public String getTypeOfISBN() {
        Field DOI = this.getFields("ee").stream().findFirst().orElse(null); // DOI not always present... we should distinguish each type of pubs (some has ISBN)
        Field ISBN = this.getFields("isbn").stream().findFirst().orElse(null);
        if ( //publication.getIdTypes().contains(PublicationIDType.DOI) &&
                DOI != null)
            return DOI.value();
        if (//publication.getIdTypes().contains(PublicationIDType.ISBN) &&
                ISBN != null)
            return ISBN.value();
        return "";
    }

    public String getISBN() {
        Field ISBN = this.getFields("isbn").stream().findFirst().orElse(null);
        if (ISBN != null)
            return ISBN.value();
        return "";
    }

    public String getDOI() {
        Field DOI = this.getFields("ee").stream().findFirst().orElse(null);
        if (DOI != null && PublicationIDType.DOI.matchesUrl(DOI.value()))
            return PublicationIDType.DOI.getID(DOI.value());
        // return fake (but unique) id
        return String.valueOf(Math.abs(this.getTitle().hashCode()));
    }

    public String getURL() {
        Field URL = this.getFields("url").stream().findFirst().orElse(null);
        if (URL == null)
            return "";
        return URL.value();
    }

    public String getCrossRef(){
        Field crossref = this.getFields("crossref").stream().findFirst().orElse(null);
        if (crossref == null)
            return "";
        return crossref.value();
    }

    public boolean hasCrossRef() {
        return !this.getCrossRef().equals("");
    }

    public List<String> getCitations(){
        if(!this.getFields("cite").isEmpty())
            return this.getFields("cite").stream().map(Field::value).filter(c->!c.equals("...")).collect(Collectors.toList());
        return new ArrayList<>();
    }

    public List<String> getPublicationsIn(Publication context){
        if(context.getToc() != null){
            List<String> pubs = context.getToc().getPublications().stream().map(Record::getKey).collect(Collectors.toList());
            pubs.remove(context.getKey());
            return pubs;
        }
        return new ArrayList<>();
    }


    public String getSchool() {
        Field school = this.getFields("school").stream().findFirst().orElse(null);
        if (school == null)
            return "";
        return school.value();
    }

    public String getPublisher() {
        Field publisher = this.getFields("publisher").stream().findFirst().orElse(null);
        if (publisher == null)
            return "";
        return publisher.value();
    }

    public String getVolume() {
        Field volume = this.getFields("volume").stream().findFirst().orElse(null);
        if (volume == null)
            return "";
        return volume.value();
    }

    public String getSeries() {
        Field series = this.getFields("series").stream().findFirst().orElse(null);
        if (series == null)
            return "";
        return series.value();
    }


    /**
     * Retrieves all external ID types stored in this publication record. The internal type
     * {@link PublicationIDType#DBLP} will be ignored by this method.
     *
     * @return The types.
     */
    @Override
    public Set<PublicationIDType> getIdTypes() {
        return publication.getIdTypes();
    }

    /**
     * Retrieves all external IDs stored in this publication record matching the given type.
     * Querying for {@link PublicationIDType#DBLP} will just return a single-item list with the same
     * ID result as {@link #getKey()}.
     *
     * @param type The ID type.
     * @return The IDs.
     * @throws NullPointerException if the specified type is {@code null}.
     */
    @Override
    public List<String> getIds(PublicationIDType type) {
        return publication.getIds(type);
    }

    /**
     * Retrieves a sequential stream of all external ID types stored in this publication record. The
     * internal type {@link PublicationIDType#DBLP} will be ignored by this method.
     *
     * @return The stream of types.
     */
    @Override
    public Stream<PublicationIDType> idTypes() {
        return publication.idTypes();
    }

    /**
     * Retrieves a sequential stream of all external IDs stored in this publication record matching
     * the given type. Querying for {@link PublicationIDType#DBLP} will just return a single-item
     * stream with the same ID result as {@link #getKey()}.
     *
     * @param type The ID type.
     * @return The stream of IDs.
     * @throws NullPointerException if the specified type is {@code null}.
     */
    @Override
    public Stream<String> ids(PublicationIDType type) {
        return publication.ids(type);
    }

    /**
     * Get the table of contents object of this record.
     *
     * @return The toc.
     */
    @Override
    public TableOfContents getToc() {
        return publication.getToc();
    }

    /**
     * Get the publication stream venue of this record.
     *
     * @return The stream venue.
     */
    @Override
    public PublicationStreamTitle getPublicationStream() {
        return publication.getPublicationStream();
    }

    /**
     * Get the publication stream venue of this record as BookTitle object.
     *
     * @return The booktitle.
     */
    @Override
    public BookTitle getBooktitle() {
        return publication.getBooktitle();
    }

    /**
     * Get the publication stream venue of this record as Jornal object.
     *
     * @return The journal.
     */
    @Override
    public JournalTitle getJournal() {
        return publication.getJournal();
    }

    /**
     * Get the publishing year of this record.
     *
     * @return The year.
     */
    @Override
    public int getYear() {
        return publication.getYear();
    }

    @Override
    public String getXml() {
        return publication.getXml();
    }

    /**
     * The number of PersonNames stored in this record.
     *
     * @return The number of PersonNames.
     */
    @Override
    public int numberOfNames() {
        return publication.numberOfNames();
    }

    /**
     * Retrieves the PersonName at the specified index. An index ranges from <code>0</code> to
     * <code>getNumberOfNames() - 1</code>.
     *
     * @param index the index of the PersonName.
     * @return The PersonName at the specified index of this record.
     * @throws IndexOutOfBoundsException if the index is out of range {@code (index < 0 || index >=
     *              getNumberOfNames())}
     */
    @Override
    public PersonName nameAt(int index) {
        return publication.nameAt(index);
    }

    /**
     * Returns the index of the specified name within this record's the list of referenced person
     * names. An index ranges from <code>0</code> to <code>getNumberOfNames() - 1</code>. If no such
     * value exists, then {@code -1} is returned.
     *
     * @param name The person name.
     * @return The index of the first occurrence of the specified name, or {@code -1} if there is no
     *         such occurrence.
     */
    @Override
    public int indexOf(PersonName name) {
        return publication.indexOf(name);
    }

    /**
     * Retrieves an unmodifiable List view of the person names in this record.
     *
     * @return The person names as unmodifiable List.
     */
    @Override
    public List<PersonName> getNames() {
        return publication.getNames();
    }

    public List<String> getNamesString(){
        return publication.getNames().stream().map(n->n.name()).collect(Collectors.toList());
    }
    /**
     * Returns a sequential stream with the person names in this record as its source.
     *
     * @return The stream of person names.
     */
    @Override
    public Stream<PersonName> names() {
        return publication.names();
    }

    /**
     * Get the tag name of this record's root element.
     *
     * @return The tag name.
     */
    @Override
    public String getTag() {
        return publication.getTag();
    }

    /**
     * Retrieve the key of this record.
     *
     * @return The key.
     */
    @Override
    public String getKey() {
        return publication.getKey();
    }

    /**
     * Retrieve the mdate String of this record.
     *
     * @return The mdate.
     */
    @Override
    public String getMdate() {
        return publication.getMdate();
    }

    /**
     * Checks whether this record's root element has any attributes beside the mandatory 'key' and
     * 'mdate' attributes.
     *
     * @return <code>true</code> if the root element has any additional attributes, otherwise
     *         <code>false</code>.
     */
    @Override
    public boolean hasAdditionalAttribute() {
        return publication.hasAdditionalAttribute();
    }

    /**
     * Get a (name,value) map of all attributes in this record's root element.
     * <p>
     * This map always includes the mandatory attributes {@code key} and {@code mdate}.
     *
     * @return The (name,value) map.
     */
    @Override
    public Map<String, String> getAttributes() {
        return publication.getAttributes();
    }

    /**
     * Returns a sequential Stream of the (name,value) map entries of all attributes in this
     * record's root element.
     *
     * @return The stream of map entries.
     */
    @Override
    public Stream<Map.Entry<String, String>> attributes() {
        return publication.attributes();
    }

    /**
     * Returns a FieldReader for this record.
     *
     * @return The FieldReader.
     */
    @Override
    public FieldReader getFieldReader() {
        return publication.getFieldReader();
    }

    /**
     * Retrieves an unmodifiable collection view of all fields in this record.
     *
     * @return The fields.
     */
    @Override
    public Collection<Field> getFields() {
        return publication.getFields();
    }

    /**
     * Retrieves an unmodifiable collection view of all fields matching one of the given field tag
     * names.
     *
     * @param tags The tag names used to select the fields.
     * @return The matching fields.
     */
    @Override
    public Collection<Field> getFields(String... tags) {
        return publication.getFields(tags);
    }

    /**
     * Returns a sequential stream of all fields contained in this record.
     *
     * @return The stream of Fields.
     */
    @Override
    public Stream<Field> fields() {
        return publication.fields();
    }

    /**
     * Returns a sequential stream of all fields matching one of the given field tag names.
     *
     * @param tags The tag names used to select the fields.
     * @return The stream of matching fields.
     */
    @Override
    public Stream<Field> fields(String... tags) {
        return publication.fields(tags);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Publication)
            return ((Publication) obj).getKey().equals(this.getKey());
        return false;
    }

    @Override
    public int hashCode() {
        return publication.hashCode();
    }

    @Override
    public String toString() {
        return publication.toString();
    }

    public boolean hasContextInfo() {
        if (this.getTag().equals("inproceedings") || this.getTag().equals("incollection"))
            return this.hasCrossRef();
        return true;
    }
}
