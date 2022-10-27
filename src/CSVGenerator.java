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

import org.dblp.mmdb.Person;
import org.dblp.mmdb.Publication;
import org.dblp.mmdb.RecordDb;
import org.dblp.mmdb.RecordDbInterface;
import org.xml.sax.SAXException;


public class CSVGenerator {

    public static void main(String[] args) {

        final String RESULTS_DIRECTORY_PATH = "results/";

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

        // list of all publications we want to insert in the database
        List<Publication> util_pubs = new ArrayList<>();
        // List of authors in the database
        List<List<String>> list_authors = new ArrayList<>();
        List<List<String>> list_author_pubs = new ArrayList<>(); // relation author->PRODUCE->publication
        List<List<String>> list_publication = new ArrayList<>();
        List<List<String>> list_pub_in_pubs = new ArrayList<>();
        List<List<String>> list_context_pubs = new ArrayList<>();
        int i = 0;
        for (Person person : dblp.getPersons()) {
            if(i == 20000) break;

            // authors.csv
            if (!person.getPublications().isEmpty()) {
                List<String> row_author = new ArrayList<>();
                row_author.add(person.getPid());
                row_author.add(person.getPrimaryName().name());
                person.getFields("url").forEach(u -> row_author.add(u.value()));
                list_authors.add(row_author);

                // author_pubs_relation.csv
                for(Publication p : person.getPublications()) {
                    if (!util_pubs.contains(p)) util_pubs.add(p);
                    // Adding the following pair: < key of the author, key of the publication written by that author >
                    list_author_pubs.add(Arrays.asList(person.getPid(), p.getKey()));
                }
                i++;
            }
        }

        // construct all type of publication csv starting from the util publication list
        for (Publication publication : util_pubs) {
            List<String> entry_publication = new ArrayList<>();
            List<String> row_pub_pubs = new ArrayList<>();

            entry_publication.add(publication.getKey());
            entry_publication.add(PublicationUtils.getTypeOfISBN(publication));
            entry_publication.add(PublicationUtils.getTitle(publication));
            entry_publication.add(String.valueOf(publication.getYear()));
            entry_publication.add(PublicationUtils.getPublisher(publication));
            entry_publication.add(PublicationUtils.getPages(publication));
            entry_publication.add(PublicationUtils.getURL(publication));
            entry_publication.add(publication.getTag());

            // context_pubs_relation.csv
            if (publication.getTag().equals("proceedings")) {
                // publications in the given proceeding (i.e. the context of publication)
                List<String> pubs_in_proceedings = PublicationUtils.getPublicationsIn(publication);
                if(!pubs_in_proceedings.isEmpty()){
                    pubs_in_proceedings.forEach(p -> {
                        // Adding the following pair: < key of the context, key of the publication presented in that context >
                        list_context_pubs.add(Arrays.asList(publication.getKey(), p));
                     });
                }
            }

            entry_publication.add(PublicationUtils.getCrossRef(publication));
            entry_publication.add(publication.getMdate());
            list_publication.add(entry_publication);

            // pub_pubs_relation.csv (citations of a publication)
            // TODO è inutile farlo per i proceedings
            // TODO handle those values: "..."
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
            CSVWriter.convertToCSV(list_publication, RESULTS_DIRECTORY_PATH + "publication.csv");
            CSVWriter.convertToCSV(list_pub_in_pubs,RESULTS_DIRECTORY_PATH + "pub_pubs_relation.csv");
            CSVWriter.convertToCSV(list_context_pubs,RESULTS_DIRECTORY_PATH + "context_pubs_relation.csv");
        } catch (IOException e) {
            System.out.println("xxx");
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

