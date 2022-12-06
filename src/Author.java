import org.dblp.mmdb.*;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Author extends Person {

    private final Person author;
    private final Association association;
    private final String email;

    public Author(Person author) {
        this.author = author;
        this.association = AssociationUtils.getRandomAssociation();
        this.email = this.generateEmail();
    }

    public List<String> getCoauthorNamesIn(Publication publication) {
        // removing author from coauthors list
        return publication.getNames().stream().map(PersonName::name).filter(n -> !n.equals(this.getPrimaryName().name())).toList();
    }

    public List<String> generateCSVEntry(boolean withMultipleURLs) {
        List<String> entry_author = new ArrayList<>();
        entry_author.add(this.getPid());
        entry_author.add(this.getPrimaryName().name());
        //TODO add orcid??
        /*
        List<String> urls = author.getFields("url").stream().map(Field::value).toList();
        if (!urls.isEmpty()) {
            entry_author.add(urls.get(0));
            // TODO if we want to insert a random one, simply put instead of ""
            urls.stream().filter(u -> PersonIDType.of(u) != null && PersonIDType.ORCID.matches(u)).findFirst().ifPresentOrElse(orcid -> entry_author.add(PersonIDType.ORCID.normalize(orcid)), () -> entry_author.add(""));
        }
         */
        if (withMultipleURLs)
            entry_author.add(this.getURLs());
        else
            entry_author.add(this.getFirstURL());
        entry_author.add(this.getOrcid());
        entry_author.add(this.getEmail());
        return entry_author;
    }

    public String getEmail() {
        return this.email;
    }

    private String generateEmail() {
        String first = this.author.getPrimaryName().first();
        String last = this.author.getPrimaryName().last();
        String suffix = this.author.getPrimaryName().suffix();
        String email;

        if (suffix != null)
            email = unaccent(first) + "." + unaccent(last) + "." + unaccent(suffix) + "@" + this.association.getUrlDomain();
        else
            email = unaccent(first) + "." + unaccent(last) + "@" + this.association.getUrlDomain();

        return email.replaceAll("\\s+","").replace("-", "").replace("..", ".").toLowerCase();

    }

    private String unaccent(String src) {
        if(src==null)
            return "";
        return Normalizer
                .normalize(src, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "");
    }

    public Association getAssociation() {
        return this.association;
    }

    public String getOrcid() {
        return this.author.getFields("url").stream().map(Field::value).filter(PersonIDType.ORCID::matchesUrl).map(PersonIDType.ORCID::getID).findFirst().orElse("");
    }

    public boolean hasOrcid() {
        return !this.getOrcid().equals("");
    }

    private String getFirstURL() {
        return this.getFields("url").stream().map(Field::value).filter(url -> !PersonIDType.ORCID.matchesUrl(url)).findFirst().orElse("");
    }

    public String getURLs() {
        return this.getFields("url").stream().map(Field::value).filter(url -> !PersonIDType.ORCID.matchesUrl(url)).collect(Collectors.joining("|"));
    }

    /**
     * Retrieve the PID of this person record.
     *
     * @return The PID.
     */
    @Override
    public String getPid() {
        return author.getPid();
    }

    /**
     * Retrieves all external ID types stored in this person record. The internal type
     * {@link PersonIDType#DBLP} will be ignored by this method.
     *
     * @return The types.
     */
    @Override
    public Set<PersonIDType> getIdTypes() {
        return author.getIdTypes();
    }

    /**
     * Retrieves all external IDs stored in this person record matching the given type. Querying for
     * {@link PersonIDType#DBLP} will just return a single-item list with the same ID result as
     * {@link #getPid()}.
     *
     * @param type The ID type.
     * @return The IDs.
     * @throws NullPointerException if the specified type is {@code null}.
     */
    @Override
    public List<String> getIds(PersonIDType type) {
        return author.getIds(type);
    }

    /**
     * Retrieves a sequential stream of all external ID types stored in this person record. The
     * internal type {@link PersonIDType#DBLP} will be ignored by this method.
     *
     * @return The stream of types.
     */
    @Override
    public Stream<PersonIDType> idTypes() {
        return author.idTypes();
    }

    /**
     * Retrieves a sequential stream of all external IDs stored in this person record matching the
     * given type. Querying for {@link PersonIDType#DBLP} will just return a single-item list with
     * the same ID result as {@link #getPid()}.
     *
     * @param type The ID type.
     * @return The stream of IDs.
     * @throws NullPointerException if the specified type is {@code null}.
     */
    @Override
    public Stream<String> ids(PersonIDType type) {
        return author.ids(type);
    }

    /**
     * Retrieve the most recent mdate String for this record and all attached publications.
     *
     * @return The aggregated mdate.
     */
    @Override
    public String getAggregatedMdate() {
        return author.getAggregatedMdate();
    }

    /**
     * Retrieves an unmodifiable list view of all publication by this person.
     *
     * @return The publications as unmodifiable list.
     */
    @Override
    public List<Publication> getPublications() {
        return author.getPublications();
    }

    /**
     * Returns a sequential stream of all publications by this person.
     *
     * @return The stream of publications.
     */
    @Override
    public Stream<Publication> publications() {
        return author.publications();
    }

    /**
     * Get the number of publications of this person.
     *
     * @return The number of publications.
     */
    @Override
    public int numberOfPublications() {
        return author.numberOfPublications();
    }

    /**
     * Get the primary name of this person.
     *
     * @return The person name.
     */
    @Override
    public PersonName getPrimaryName() {
        return author.getPrimaryName();
    }

    /**
     * Checks whether this person record carries a certain dblp name.
     *
     * @param name The complete name string to look up, including any possible homonym numbers.
     * @return {@code true} if this person has the given name, otherwise {@code false}.
     */
    @Override
    public boolean hasName(String name) {
        return author.hasName(name);
    }

    /**
     * Checks whether this person record carries a certain person name.
     *
     * @param name The person name to look up.
     * @return {@code true} if this person has the given name, otherwise {@code false}.
     */
    @Override
    public boolean hasName(PersonName name) {
        return author.hasName(name);
    }

    /**
     * Checks whether this person record is trivial, i.e., the record does not contain any
     * information beside a single person name. In particular, the record does <em>not</em> include:
     * <ul>
     * <li>attributes
     * <li>alias names
     * <li>home page URLs
     * <li>notes
     * <li>cite references
     * <li>is-not references
     * </ul>
     *
     * @return {@code true} if this person record is trivial, or {@code false} if any non-trivial
     *         information is present.
     */
    @Override
    public boolean isTrivial() {
        return author.isTrivial();
    }

    /**
     * Checks whether this person labeled as an unlisted profile.
     *
     * @return {@code true} if this person profile is unlisted, otherwise {@code false}.
     */
    @Override
    public boolean isNoShow() {
        return author.isNoShow();
    }

    /**
     * Checks whether this person should be considered a disambiguation pseudo person. If
     * {@code checkHeuristically == true} then this method will also perform heuristic checks.
     * <p>
     * The heuristic check consists of checking whether a homonymous name with id '0001' exists in
     * dblp.
     *
     * @param checkHeuristically Whether to check heuristically.
     * @return {@code true} if this person should be considered a disambiguation pseudo person,
     *         otherwise {@code false}.
     */
    @Override
    public boolean isDisambiguation(boolean checkHeuristically) {
        return author.isDisambiguation(checkHeuristically);
    }

    /**
     * Checks (explicitly and heuristically) whether this person should be considered a
     * disambiguation pseudo person.
     * <p>
     * The heuristic check consists of checking whether a homonymous name with id '0001' exists in
     * dblp.
     *
     * @return {@code true} if this person should be considered a disambiguation pseudo person,
     *         otherwise {@code false}.
     */
    @Override
    public boolean isDisambiguation() {
        return author.isDisambiguation();
    }

    /**
     * Checks whether this person should be considered a group pseudo person.
     *
     * @return {@code true} if this person should be considered a group pseudo person, otherwise
     *         {@code false}.
     */
    @Override
    public boolean isGroup() {
        return author.isGroup();
    }

    /**
     * Checks whether this person record has alias names.
     *
     * @return {@code true} if this person has alias names, otherwise {@code false}.
     */
    @Override
    public boolean hasAliases() {
        return author.hasAliases();
    }

    /**
     * Checks whether this person record contains additional person information. This includes:
     * <ul>
     * <li>home page URLs,
     * <li>notes (including is-not references), or
     * <li>cite references
     * </ul>
     *
     * @return {@code true} if this person contains additional information, otherwise {@code false}.
     */
    @Override
    public boolean hasPersonInfo() {
        return author.hasPersonInfo();
    }

    /**
     * Checks whether this person has explicitly disambiguated persons listed (i.e., entries in an
     * {@code <note type="isnot">} element).
     *
     * @return {@code true} if this person has explicitly disambiguated persons listed, otherwise
     *         {@code false}.
     */
    @Override
    public boolean hasIsNotPersons() {
        return author.hasIsNotPersons();
    }

    /**
     * Retrieves an unmodifiable list view of all explicitly disambiguated person names of this
     * person (i.e., all entries of the {@code <note type="isnot">} elements, if any).
     *
     * @return The explicitly disambiguated person names as unmodifiable list.
     */
    @Override
    public List<PersonName> getIsNotPersonNames() {
        return author.getIsNotPersonNames();
    }

    /**
     * Retrieves an unmodifiable list view of all explicitly disambiguated persons of this person
     * (i.e., all entries of the {@code <note type="isnot">} elements, if any).
     *
     * @return The explicitly disambiguated persons as unmodifiable list.
     */
    @Override
    public List<Person> getIsNotPersons() {
        return author.getIsNotPersons();
    }

    @Override
    public String getXml() {
        return author.getXml();
    }

    @Override
    public int compareTo(Person other) {
        return author.compareTo(other);
    }

    /**
     * The number of PersonNames stored in this record.
     *
     * @return The number of PersonNames.
     */
    @Override
    public int numberOfNames() {
        return author.numberOfNames();
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
        return author.nameAt(index);
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
        return author.indexOf(name);
    }

    /**
     * Retrieves an unmodifiable List view of the person names in this record.
     *
     * @return The person names as unmodifiable List.
     */
    @Override
    public List<PersonName> getNames() {
        return author.getNames();
    }

    /**
     * Returns a sequential stream with the person names in this record as its source.
     *
     * @return The stream of person names.
     */
    @Override
    public Stream<PersonName> names() {
        return author.names();
    }

    /**
     * Get the tag name of this record's root element.
     *
     * @return The tag name.
     */
    @Override
    public String getTag() {
        return author.getTag();
    }

    /**
     * Retrieve the key of this record.
     *
     * @return The key.
     */
    @Override
    public String getKey() {
        return author.getKey();
    }

    /**
     * Retrieve the mdate String of this record.
     *
     * @return The mdate.
     */
    @Override
    public String getMdate() {
        return author.getMdate();
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
        return author.hasAdditionalAttribute();
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
        return author.getAttributes();
    }

    /**
     * Returns a sequential Stream of the (name,value) map entries of all attributes in this
     * record's root element.
     *
     * @return The stream of map entries.
     */
    @Override
    public Stream<Map.Entry<String, String>> attributes() {
        return author.attributes();
    }

    /**
     * Returns a FieldReader for this record.
     *
     * @return The FieldReader.
     */
    @Override
    public FieldReader getFieldReader() {
        return author.getFieldReader();
    }

    /**
     * Retrieves an unmodifiable collection view of all fields in this record.
     *
     * @return The fields.
     */
    @Override
    public Collection<Field> getFields() {
        return author.getFields();
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
        return author.getFields(tags);
    }

    /**
     * Returns a sequential stream of all fields contained in this record.
     *
     * @return The stream of Fields.
     */
    @Override
    public Stream<Field> fields() {
        return author.fields();
    }

    /**
     * Returns a sequential stream of all fields matching one of the given field tag names.
     *
     * @param tags The tag names used to select the fields.
     * @return The stream of matching fields.
     */
    @Override
    public Stream<Field> fields(String... tags) {
        return author.fields(tags);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Person)
            return ((Person) obj).getKey().equals(this.getKey());
        return false;
    }

    @Override
    public int hashCode() {
        return author.hashCode();
    }

    @Override
    public String toString() {
        return author.toString();
    }

}
