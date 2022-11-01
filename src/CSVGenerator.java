//
// Copyright (c)2015, dblp Team (University of Trier and
// Schloss Dagstuhl - Leibniz-Zentrum fuer Informatik GmbH)
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// (1) Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//
// (2) Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
//
// (3) Neither the name of the dblp team nor the names of its contributors
// may be used to endorse or promote products derived from this software
// without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL DBLP TEAM BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.dblp.mmdb.*;
import org.dblp.mmdb.Publication;
import org.xml.sax.SAXException;


public class CSVGenerator {

    private static RecordDbInterface dblp = null;

    private static final String RESULTS_DIRECTORY_PATH = "results/";
    private static final int MAX_NUM_CITATIONS_PER_PUB = 10;
    private static final int MAX_NUM_PUBS_PER_CONTEXT = 10;
    private static final int INIT_NUM_AUTHORS = 10;
    private static final int MAX_NUM_VISITING_AUTHORS = 200;

    // Unique entries to be inserted in the db
    private static final Set<List<String>> author_entries = new HashSet<>();
    private static final Set<List<String>> association_entries = AssociationUtils.generateCSVEntriesViaJSON();
    private static final Set<List<String>> author_pub_entries = new HashSet<>(); // relation author->PRODUCE->publication
    private static final Set<List<String>> publication_entries = new HashSet<>();
    private static final Set<List<String>> context_entries = new HashSet<>();
    private static final Set<List<String>> citation_entries = new HashSet<>();
    private static final Set<List<String>> context_pubs_entries = new HashSet<>();
    private static final Set<List<String>> author_association_entries = new HashSet<>(); // relation author->PRODUCE->publication

    // set of authors that we will consider
    private static final List<Author> authors = new ArrayList<>();
    //List<Person> authors = new ArrayList<>();

    // set of publications we need to insert into the database given authors
    private static final Set<MyPublication> util_pubs = new HashSet<>();
    // set of contexts we need to insert into the database given authors
    private static final Set<Context> util_contexts = new HashSet<>();
    private static final Set<String> exploredContextNames = new HashSet<>();

    public static void main(String[] args) {

        dblp = loadXML(args);

        // if we don't add orcid, useless
        int i = 0;
        List<Person> authors_with_orcid = dblp.getPersons().stream().filter(person1 -> {
            List<String> url = person1.getFields("url").stream().map(Field::value).filter(u -> PersonIDType.of(u) != null && PersonIDType.of(u).equals(PersonIDType.ORCID)).toList();
            return !url.isEmpty();
        }).toList();

        for (Person person : authors_with_orcid) {
            if (i == INIT_NUM_AUTHORS) break;
            Author author = new Author(person);
            authors.add(author);
            i++;
        }

        boolean stopAddingAuthors = false;
        for (int count = 0; count < authors.size(); count++){

            Author author = authors.get(count);

            // author.csv
            if (!author.getPublications().isEmpty()) {

                author_entries.add(author.generateCSVEntry());

                //author_association_relation.csv
                author_association_entries.add(Arrays.asList(author.getPid(), AssociationUtils.getRandomAssociation().getId()));

                for(Publication pub :  author.getPublications()) {
                    MyPublication publication = new MyPublication(pub);
                    // visiting coauthors
                    if (!stopAddingAuthors) {
                        for (String coauthor_name : author.getCoauthorNamesIn(publication)) {
                            Author coauthor = new Author(dblp.getPersonByName(coauthor_name));

                            if (authors.size() > MAX_NUM_VISITING_AUTHORS) stopAddingAuthors = true;

                            if (!authors.contains(coauthor)) authors.add(coauthor);
                        }
                    }

                    // filtering publications by type (context or publication)
                    distributePublication(publication, true);
                }
            }
        }


        // construct all types of publication csv starting from the util publication list
        // publications.csv
        for (MyPublication publication : util_pubs) {

            if (publication.getTag().equals("book")) {
                System.out.println("BOOK: " + publication.getFields().stream().map(field -> field.tag() + ": " + field.value() + " - ").collect(Collectors.joining(",")) + "\n");
                if (!publication.getCrossRef().equals(""))
                    System.out.println("book senza padre");
            }

            if (publication.getFields().stream().anyMatch(Field::hasAttributes))
                System.out.println("HAS ATTR: " + publication.getFields().stream().map(field -> field.tag() + ": " + field.value() + " - ").collect(Collectors.joining(",")) + "\n");

            addPublicationAndItsRelationEntries(publication, true);

            // pub_pubs_relation.csv (citations of a publication)
            List<String> citations = publication.getCitations();
            if (!citations.isEmpty()) {
                //if (citations.size() > MAX_NUM_CITATIONS_PER_PUB) citations = citations.subList(0, MAX_NUM_CITATIONS_PER_PUB);
                citations.forEach(cit -> {
                    // Adding the following pair: < key of the publication, key of another publication cited in that publication >
                    citation_entries.add(Arrays.asList(publication.getKey(), cit));

                    // Adding the citation as a publication in the db with related relations
                    addPublicationAndItsRelationEntries(dblp.getPublication(cit), true);
                });
            }
        }
        // adding possible contexts (book) of the current publication
        // NO. ASSUMPTION: book no context


        // contexts.csv
        for (Context context : util_contexts){
            context_entries.add(context.generateCSVEntry());

            //context_pub_relation.csv
            List<String> pubs_in_proceedings = context.getRelatedPublications().stream().limit(MAX_NUM_PUBS_PER_CONTEXT).collect(Collectors.toList());
            if(!pubs_in_proceedings.isEmpty()){
                pubs_in_proceedings.forEach(pub -> {
                    // Adding the following pair: < key of the context, key of the publication presented in that context >
                    context_pubs_entries.add(context.generateCSVEntry(pub));

                    // Adding the publication presented in that context as a publication in the db with related relations
                    addPublicationAndItsRelationEntries(dblp.getPublication(pub), false);
                });
            }
        }

        System.out.println("pub entry size: " + publication_entries.size());
        System.out.println("num of pubs with citations: " + (int) util_pubs.stream().filter(p -> !p.getCitations().isEmpty()).count());
        System.out.println("author_pubs_rel size: " + author_pub_entries.size());


        try {
            CSVWriter.convertToCSV(author_entries, RESULTS_DIRECTORY_PATH + "authors.csv");
            CSVWriter.convertToCSV(author_pub_entries, RESULTS_DIRECTORY_PATH + "author_pubs_relation.csv");
            CSVWriter.convertToCSV(publication_entries, RESULTS_DIRECTORY_PATH + "publications.csv");
            CSVWriter.convertToCSV(citation_entries,RESULTS_DIRECTORY_PATH + "pub_pubs_relation.csv");
            CSVWriter.convertToCSV(context_pubs_entries,RESULTS_DIRECTORY_PATH + "context_pubs_relation.csv");
            CSVWriter.convertToCSV(context_entries,RESULTS_DIRECTORY_PATH + "contexts.csv");
            CSVWriter.convertToCSV(association_entries,RESULTS_DIRECTORY_PATH + "associations.csv");
            CSVWriter.convertToCSV(author_association_entries,RESULTS_DIRECTORY_PATH + "author_association_relation.csv");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static RecordDbInterface loadXML(String[] args) {
        // we need to raise entityExpansionLimit because the dblp.xml has millions of entities
        System.setProperty("entityExpansionLimit", "1000");

        if (args.length != 2) {
            System.err.format("Usage: java %s <dblp-xml-file> <dblp-dtd-file>\n", CSVGenerator.class.getName());
            System.exit(0);
        }
        String dblpXmlFilename = args[0];
        String dblpDtdFilename = args[1];

        System.out.println("building the dblp main memory DB ...");
        try {
            dblp = new RecordDb(dblpXmlFilename, dblpDtdFilename, false);
        }
        catch (final IOException ex) {
            System.err.println("cannot read dblp XML: " + ex.getMessage());
            return null;
        }
        catch (final SAXException ex) {
            System.err.println("cannot parse XML: " + ex.getMessage());
            return null;
        }
        System.out.format("MMDB ready: %d publs, %d pers\n\n", dblp.numberOfPublications(), dblp.numberOfPersons());

        return dblp;
    }

    private static Optional<Context> getContextByName(String journalName) {
        return util_contexts.stream().filter(c -> c.getTitle().equals(journalName)).findFirst();
    }

    private static void distributePublication(MyPublication publication, boolean addToUtilPubs) {
        // TODO: we are losing relation author -> context
        if(publication.getTag().equals("proceedings")) {
            String contextTitle = PublicationUtils.getTitle(publication);
            if (!exploredContextNames.contains(contextTitle)) {
                util_contexts.add(new Conference(publication));
                exploredContextNames.add(contextTitle);
            }
        }
        else if (publication.getTag().equals("article")) {
            if (publication.getJournal() != null) {
                String contextName = publication.getJournal().getTitle();
                if (!exploredContextNames.contains(contextName)) {
                    util_contexts.add(new Journal(publication));
                    if (addToUtilPubs) util_pubs.add(publication);
                    exploredContextNames.add(contextName);
                } else {
                    getContextByName(contextName).ifPresent(c -> c.insertNewArticle(publication.getKey()));
                }
            }
        }
        else {
            if (addToUtilPubs) util_pubs.add(publication);
        }
    }

    private static void addPublicationAndItsRelationEntries(Publication publicationToAdd, boolean addAlsoItsPossibleContext) {
        MyPublication publication = new MyPublication(publicationToAdd);

        // add publicationToAdd's info
        publication_entries.add(publication.generateCSVEntry());

        // add all the authors publicationToAdd (both in authors.csv and author_pubs_relation.csv)
        publication.getNames().forEach(authorName -> {
            Author author = new Author(authorName.getPerson());
            author_entries.add(author.generateCSVEntry());
            // author_pubs_relation.csv
            // Adding the following pair: < key of the author, key of the publication written by that author >
            author_pub_entries.add(Arrays.asList(author.getPid(), publication.getKey()));
        });

        if (addAlsoItsPossibleContext) {
            if (publication.hasCrossRef()) {
                distributePublication(new MyPublication(dblp.getPublication(publication.getCrossRef())), false);
            }
        }
    }

}

