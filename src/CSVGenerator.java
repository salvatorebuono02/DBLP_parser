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
import org.xml.sax.SAXException;


public class CSVGenerator {

    public static void main(String[] args) {

        final String RESULTS_DIRECTORY_PATH = "results/";
        final int INIT_NUM_PERSONS = 10;
        final int MAX_AUTHORS = 1000;

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
            return;
        }
        catch (final SAXException ex) {
            System.err.println("cannot parse XML: " + ex.getMessage());
            return;
        }
        System.out.format("MMDB ready: %d publs, %d pers\n\n", dblp.numberOfPublications(), dblp.numberOfPersons());


        // List of authors in the database
        List<List<String>> list_authors = new ArrayList<>();
        List<List<String>> list_author_pubs = new ArrayList<>(); // relation author->PRODUCE->publication
        List<List<String>> list_publications = new ArrayList<>();
        List<List<String>> list_contexts = new ArrayList<>();
        List<List<String>> list_pub_in_pubs = new ArrayList<>();
        List<List<String>> list_context_pubs = new ArrayList<>();

        // set of authors that we will consider
        List<Person> authors = new ArrayList<>();
        //List<Person> authors = new ArrayList<>();

        // set of publications we need to insert into the database given authors
        Set<Publication> util_pubs = new HashSet<>();
        // set of contexts we need to insert into the database given authors
        Set<Publication> util_contexts = new HashSet<>();

        int i = 0;
        for (Person person : dblp.getPersons()) {
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
                List<String> row_author = new ArrayList<>();
                row_author.add(person.getPid());
                row_author.add(person.getPrimaryName().name());
                person.getFields("url").forEach(u -> row_author.add(u.value()));
                list_authors.add(row_author);

                /*
                Collection<Person> coauthors = dblp.coauthors(person).collect(Collectors.toList());
                if (!coauthors.isEmpty()) authors.addAll(coauthors);
                 */

                // author_pubs_relation.csv
                for(Publication publication : person.getPublications()) {

                    // visiting coauthors
                    if (!stopAddingAuthors) {
                        // removing person from choautors list
                        List<String> coautors = publication.getNames().stream().map(PersonName::name).filter(n -> !n.equals(person.getPrimaryName().name())).toList();
                        for (String coauthor : coautors) {
                            Person coauthor_person = dblp.getPersonByName(coauthor);

                            /*
                            String coauthor_person_pid = coauthor_person.getPid();
                            // i need to insert in the map the coauthors never seen
                            if (!visited.containsKey(coauthor_person_pid))
                                visited.put(coauthor_person_pid, false);



                            if (!authors.contains(coauthor_person)) {
                                // visited.put(coauthor_person_pid, true);
                                if (authors.size() > MAX_AUTHORS) stopAddingAuthors = true;
                                //System.out.println("adding " + coauthor_person_pid);
                                authors.add(coauthor_person);
                            }

                             */

                            if (authors.size() > MAX_AUTHORS) stopAddingAuthors = true;

                            if (!authors.contains(coauthor_person)) authors.add(coauthor_person);
                        }

                    }
                    //TODO remove the proceedings (or adding in another relation for person - EDITOR_OF -> proceedings
                    if(!Objects.equals(publication.getTag(), "proceedings")){
                        util_pubs.add(publication);
                        // Adding the following pair: < key of the author, key of the publication written by that author >
                        list_author_pubs.add(Arrays.asList(person.getPid(), publication.getKey()));
                    }
                    else {
                        util_contexts.add(publication);
                    }
                }
            }
        }

        // proceedings.csv
        for (Publication publication : util_contexts){
            List<String> entry_publication = new ArrayList<>();
            entry_publication.add(publication.getKey());
            entry_publication.add(PublicationUtils.getTypeOfISBN(publication));
            entry_publication.add(PublicationUtils.getTitle(publication));
            entry_publication.add(String.valueOf(publication.getYear()));
            entry_publication.add(PublicationUtils.getPublisher(publication));
            entry_publication.add(PublicationUtils.getURL(publication));
            entry_publication.add(publication.getTag());

            //context_pub_relation.csv
            List<String> pubs_in_proceedings = PublicationUtils.getPublicationsIn(publication);
            if(!pubs_in_proceedings.isEmpty()){
                pubs_in_proceedings.forEach(p -> {
                    // Adding the following pair: < key of the context, key of the publication presented in that context >
                    list_context_pubs.add(Arrays.asList(publication.getKey(), p));
                });
            }
            list_contexts.add(entry_publication);
        }

        // construct all types of publication csv starting from the util publication list
        // publications.csv
        for (Publication publication : util_pubs) {
            List<String> entry_publication = new ArrayList<>();


            // TODO handle editor
            entry_publication.add(publication.getKey());
            entry_publication.add(PublicationUtils.getTypeOfISBN(publication));
            entry_publication.add(PublicationUtils.getTitle(publication));
            entry_publication.add(String.valueOf(publication.getYear()));
            entry_publication.add(PublicationUtils.getPages(publication));
            entry_publication.add(publication.getTag());


            entry_publication.add(PublicationUtils.getCrossRef(publication));
            entry_publication.add(publication.getMdate());
            list_publications.add(entry_publication);

            // pub_pubs_relation.csv (citations of a publication)
            // TODO è inutile farlo per i proceedings
            List<String> citations = PublicationUtils.getCitations(publication);
            if (!citations.isEmpty()) {
                citations.forEach(c -> {
                    // Adding the following pair: < key of the publication, key of another publication cited in that publication >
                    list_pub_in_pubs.add(Arrays.asList(publication.getKey(), c));
                });
            }
        }

        try {
            CSVWriter.convertToCSV(list_authors, RESULTS_DIRECTORY_PATH + "authors.csv");
            CSVWriter.convertToCSV(list_author_pubs, RESULTS_DIRECTORY_PATH + "author_pubs_relation.csv");
            CSVWriter.convertToCSV(list_publications, RESULTS_DIRECTORY_PATH + "publications.csv");
            CSVWriter.convertToCSV(list_pub_in_pubs,RESULTS_DIRECTORY_PATH + "pub_pubs_relation.csv");
            CSVWriter.convertToCSV(list_context_pubs,RESULTS_DIRECTORY_PATH + "context_pubs_relation.csv");
            CSVWriter.convertToCSV(list_contexts,RESULTS_DIRECTORY_PATH + "proceedings.csv");
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
        System.out.println("finding longest person name in dblp ...");
        String longestName = null;
        int longestNameLength = 0;
        for (PersonName name : dblp.getPersonNames()) {
            if (name.name().length() > longestNameLength) {
                longestName = name.name();
                longestNameLength = longestName.length();
            }
        }
        System.out.format("%s (%d chars)\n\n", longestName, longestNameLength);

        System.out.println("finding most prolific author in dblp ...");
        String prolificAuthorName = null;
        int prolificAuthorCount = 0;
        for (Person pers : dblp.getPersons()) {
            int publsCount = pers.numberOfPublications();
            if (publsCount > prolificAuthorCount) {
                prolificAuthorCount = publsCount;
                prolificAuthorName = pers.getPrimaryName().name();
            }
        }
        System.out.format("%s, %d records\n\n", prolificAuthorName, prolificAuthorCount);

        System.out.println("finding author with most coauthors in dblp ...");
        String connectedAuthorName = null;
        int connectedAuthorCount = 0;
        for (Person pers : dblp.getPersons()) {
            int coauthorCount = dblp.numberOfCoauthors(pers);
            if (coauthorCount > connectedAuthorCount) {
                connectedAuthorCount = coauthorCount;
                connectedAuthorName = pers.getPrimaryName().name();
            }
        }
        System.out.format("%s, %d coauthors\n\n", connectedAuthorName, connectedAuthorCount);

        System.out.println("finding coauthors of Jim Gray 0001 ...");
        Person jim = dblp.getPersonByName("Jim Gray 0001");
        for (int i = 0; i < dblp.numberOfCoauthorCommunities(jim); i++) {
            Collection<Person> coauthors = dblp.getCoauthorCommunity(jim, i);
            System.out.format("Group %d:\n", i);
            for (Person coauthor : coauthors) {
                System.out.format("  %s\n", coauthor.getPrimaryName().name());
            }
        }
        System.out.println();

        System.out.println("finding authors of FOCS 2010 ...");
        Comparator<Person> cmp = (Person o1,
                                  Person o2) -> o1.getPrimaryName().name().compareTo(o2.getPrimaryName().name());
        Map<Person, Integer> authors = new TreeMap<>(cmp);
        TableOfContents focs2010Toc = dblp.getToc("db/conf/focs/focs2010.bht");
        for (Publication publ : focs2010Toc.getPublications()) {
            for (PersonName name : publ.getNames()) {
                Person pers = name.getPerson();
                if (authors.containsKey(pers)) authors.put(pers, authors.get(pers) + 1);
                else authors.put(pers, 1);
            }
        }
        for (Person author : authors.keySet())
            System.out.format("  %dx %s\n", authors.get(author), author.getPrimaryName().name());
        System.out.println();

        System.out.println("finding URLs of FOCS 2010 publications ...");
        for (Publication publ : focs2010Toc.getPublications()) {
            for (Field fld : publ.getFields("ee")) {
                System.out.format("  %s\n", fld.value());
            }
        }

        System.out.println("done.");
         */
    }
}

