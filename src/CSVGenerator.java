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

import org.dblp.mmdb.*;
import org.xml.sax.SAXException;


public class CSVGenerator {

    public static void main(String[] args) {

        final String RESULTS_DIRECTORY_PATH = "results/";
        final int INIT_NUM_PERSONS = 10;
        final int MAX_NUM_AUTHORS = 500;

        RecordDbInterface dblp = loadXML(args);


        // List of authors in the database
        List<List<String>> author_entries = new ArrayList<>();
        List<List<String>> author_pub_entries = new ArrayList<>(); // relation author->PRODUCE->publication
        List<List<String>> publication_entries = new ArrayList<>();
        List<List<String>> context_entries = new ArrayList<>();
        List<List<String>> citation_entries = new ArrayList<>();
        List<List<String>> context_pubs_entries = new ArrayList<>();

        // set of authors that we will consider
        List<Person> authors = new ArrayList<>();
        //List<Person> authors = new ArrayList<>();

        // set of publications we need to insert into the database given authors
        Set<Publication> util_pubs = new HashSet<>();
        // set of contexts we need to insert into the database given authors
        Set<Publication> util_contexts = new HashSet<>();

        int i = 0;
        List<Person> authors_with_orcid = dblp.getPersons().stream().filter(person1 -> {
            List<String> url = person1.getFields("url").stream().map(Field::value).filter(u -> PersonIDType.of(u) != null && PersonIDType.of(u).equals(PersonIDType.ORCID)).toList();
            return !url.isEmpty();

        }).toList();

        for (Person person : authors_with_orcid) {
            if (i == INIT_NUM_PERSONS) break;
            authors.add(person);
            i++;
        }

        boolean stopAddingAuthors = false;
        //Map<String, Boolean> visited = new HashMap<>();
        for (int count = 0; count < authors.size(); count++){

            Person person = authors.get(count);
            System.out.println("popping " + person.getPrimaryName().name() + ", " + person.getPid());
            //Person person = authors.get(i);
            //visited.put(person.getPid(), true);

            // authors.csv
            if (!person.getPublications().isEmpty()) {

                author_entries.add(generateCSVEntry(person));

                // author_pubs_relation.csv
                for(Publication publication : person.getPublications()) {

                    // visiting coauthors
                    if (!stopAddingAuthors) {
                        // removing person from choautors list
                        List<String> coautors = publication.getNames().stream().map(PersonName::name).filter(n -> !n.equals(person.getPrimaryName().name())).toList();
                        for (String coauthor : coautors) {
                            Person coauthor_person = dblp.getPersonByName(coauthor);

                            if (authors.size() > MAX_NUM_AUTHORS) stopAddingAuthors = true;

                            if (!authors.contains(coauthor_person)) authors.add(coauthor_person);
                        }

                    }
                    //TODO remove the proceedings (or adding in another relation for person - EDITOR_OF -> proceedings
                    if(!Objects.equals(publication.getTag(), "proceedings") || !Objects.equals(publication.getTag(), "book")){
                        util_pubs.add(publication);
                        // Adding the following pair: < key of the author, key of the publication written by that author >
                        author_pub_entries.add(Arrays.asList(person.getPid(), publication.getKey()));
                    }
                    else {
                        util_contexts.add(publication);
                    }
                }
            }
        }


        // construct all types of publication csv starting from the util publication list
        // publications.csv
        for (Publication publication : util_pubs) {

            /*
            if (publication.getKey().equals("books/daglib/p/JinM12"))
                System.out.println("ciao");


            if(publication.getKey().equals("conf/adbis/NardelliP99"))
                PublicationUtils.getCitations(publication);

             */

            /*
            // TODO handle editor
            entry_publication.add(publication.getKey());
            entry_publication.add(PublicationUtils.getTypeOfISBN(publication));
            entry_publication.add(PublicationUtils.getTitle(publication));
            entry_publication.add(String.valueOf(publication.getYear()));
            entry_publication.add(PublicationUtils.getPages(publication));
            entry_publication.add(publication.getTag());


            //entry_publication.add(PublicationUtils.getCrossRef(publication));
            entry_publication.add(publication.getMdate());

             */
            publication_entries.add(generateCSVEntry(publication));

            // pub_pubs_relation.csv (citations of a publication)
            // TODO è inutile farlo per i proceedings
            List<String> citations = PublicationUtils.getCitations(publication);
            if (!citations.isEmpty()) {
                citations.forEach(c -> {
                    // Adding the following pair: < key of the publication, key of another publication cited in that publication >
                    citation_entries.add(Arrays.asList(publication.getKey(), c));

                    publication_entries.add(generateCSVEntry(dblp.getPublication(c))); // add the citation as a publication in our db
                });
            }

            // adding possible contexts (book) of the current publication
            Publication book = dblp.getPublication(PublicationUtils.getCrossRef(publication));
            if (book != null) util_contexts.add(book);
        }

        // contexts.csv
        for (Publication context : util_contexts){
            context_entries.add(generateCSVEntry(context));

            //context_pub_relation.csv
            List<String> pubs_in_proceedings = PublicationUtils.getPublicationsIn(context);
            if(!pubs_in_proceedings.isEmpty()){
                pubs_in_proceedings.forEach(p -> {
                    // Adding the following pair: < key of the context, key of the publication presented in that context >
                    context_pubs_entries.add(Arrays.asList(context.getKey(), p));

                    publication_entries.add(generateCSVEntry(dblp.getPublication(p))); // add the p as a publication in our db
                });
            }
        }

        try {
            CSVWriter.convertToCSV(author_entries, RESULTS_DIRECTORY_PATH + "authors.csv");
            CSVWriter.convertToCSV(author_pub_entries, RESULTS_DIRECTORY_PATH + "author_pubs_relation.csv");
            CSVWriter.convertToCSV(publication_entries, RESULTS_DIRECTORY_PATH + "publications.csv");
            CSVWriter.convertToCSV(citation_entries,RESULTS_DIRECTORY_PATH + "pub_pubs_relation.csv");
            CSVWriter.convertToCSV(context_pubs_entries,RESULTS_DIRECTORY_PATH + "context_pubs_relation.csv");
            CSVWriter.convertToCSV(context_entries,RESULTS_DIRECTORY_PATH + "contexts.csv");
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
        RecordDbInterface dblp;
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

    private static List<String> generateCSVEntry(Person author) {
        List<String> entry_author = new ArrayList<>();
        entry_author.add(author.getPid());
        entry_author.add(author.getPrimaryName().name());
        List<String> urls = author.getFields("url").stream().map(Field::value).toList();
        if (!urls.isEmpty()) {
            entry_author.add(urls.get(0));
            // TODO if we want to insert a random one, simply put instead of ""
            urls.stream().filter(u -> PersonIDType.of(u) != null && PersonIDType.of(u).equals(PersonIDType.ORCID)).findFirst().ifPresentOrElse(orcid -> entry_author.add(PersonIDType.ORCID.normalize(orcid)), () -> entry_author.add(""));
        }
        // first affiliation (assumption only one)
        // TODO if we want to insert a random one, simply put instead of ""
        author.getFields("note").stream().findFirst().ifPresentOrElse(uni -> entry_author.add(uni.value()), () -> entry_author.add(""));

        return entry_author;
    }

    private static List<String> generateCSVEntry(Publication publication) {
        List<String> entry_publication = new ArrayList<>();
        entry_publication.add(publication.getKey());
        // entry_publication.add(PublicationUtils.getTypeOfISBN(context));
        entry_publication.add(PublicationUtils.getTitle(publication));

        // TODO how to manage those differences? all in one csv?
        // TODO Editor field??
        switch (publication.getTag()) {
            case "proceeding" -> {
                entry_publication.add(String.valueOf(publication.getYear()));
                entry_publication.add(PublicationUtils.getPublisher(publication));
                entry_publication.add(PublicationUtils.getURL(publication));
                entry_publication.add(publication.getTag());
            }
            case "book" -> {
                entry_publication.add(String.valueOf(publication.getYear()));
                entry_publication.add(PublicationUtils.getVolume(publication));
                entry_publication.add(PublicationUtils.getPublisher(publication));
                entry_publication.add(PublicationUtils.getURL(publication));
                //TODO add ISBN
                entry_publication.add(publication.getTag());
            }
            case "mastersthesis", "phdthesis" -> {
                entry_publication.add(String.valueOf(publication.getYear()));
                entry_publication.add(PublicationUtils.getSchool(publication));
                entry_publication.add(PublicationUtils.getURL(publication));
                entry_publication.add(publication.getTag());
            }
            case "inproceedings", "incollection" -> {
                entry_publication.add(String.valueOf(publication.getYear()));
                if(publication.getBooktitle() != null)
                    entry_publication.add(publication.getBooktitle().getTitle());
                else
                    entry_publication.add("");
                entry_publication.add(PublicationUtils.getVolume(publication));
                entry_publication.add(PublicationUtils.getPages(publication));
                entry_publication.add(PublicationUtils.getURL(publication));
                entry_publication.add(publication.getTag());
            }
            case "article" -> {
                entry_publication.add(String.valueOf(publication.getYear()));
                if(publication.getJournal() != null)
                    entry_publication.add(publication.getJournal().getTitle());
                else
                    entry_publication.add("");
                entry_publication.add(PublicationUtils.getVolume(publication));
                entry_publication.add(PublicationUtils.getPages(publication));
                entry_publication.add(PublicationUtils.getURL(publication));
                entry_publication.add(publication.getTag());
            }
        }


        return entry_publication;
    }
}

