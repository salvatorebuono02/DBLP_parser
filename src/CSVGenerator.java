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


@SuppressWarnings("javadoc")
public class CSVGenerator {

    public static void main(String[] args) {

        final int MAX_ROWS = 20000;
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


        // MIO CODICE

        // Authors + Author -> pubs relation
        List<List<String>> list_authors = new ArrayList<>();
        List<List<String>> list_author_pubs = new ArrayList<>();
        int i = 0;
        for (Person person : dblp.getPersons()) {

            if(i == MAX_ROWS) break;

            // authors.csv
            List<String> row_author = new ArrayList<>();
            row_author.add(person.getPid());
            row_author.add(person.getPrimaryName().name());
            person.getFields("url").forEach(u -> row_author.add(u.value()));
            list_authors.add(row_author);


            // author_pubs_relation.csv
            List<String> row_author_pubs = new ArrayList<>();
            row_author_pubs.add(person.getPid());
            person.getPublications().forEach(p -> row_author_pubs.add(p.getKey()));
            if (person.getPublications() != null) list_author_pubs.add(row_author_pubs);

            i++;
        }

        /*
        // Publications
        List<List<String>> list_pubs = new ArrayList<>();
        i = 0;
        for (Publication publication : dblp.getPublications()) {
             if(i == 1000) break;

            // publications.csv
            List<String> row_pub = new ArrayList<>();
            row_pub.add(publication.getKey());
            row_pub.add(PublicationUtils.getID(publication));
            row_pub.add(publication.getTag());
            row_pub.add(PublicationUtils.getTitle(publication));
            row_pub.add(String.valueOf(publication.getYear()));
            row_pub.add(publication.getMdate());
            row_pub.add(PublicationUtils.getPages(publication));
            row_pub.add(PublicationUtils.getURL(publication));
            //publication.getFields().forEach(f -> row_pub.add(f.value()));
            if (publication.getToc() != null)
                publication.getToc().getPublications().forEach(p -> row_pub.add(PublicationUtils.getTitle(p)));
            list_pubs.add(row_pub);

            i++;
        }

        try {
            CSVWriter.convertToCSV(list_authors, "authors.csv");
            CSVWriter.convertToCSV(list_pubs, "publications.csv");
            CSVWriter.convertToCSV(list_author_pubs, "author_pubs_relation.csv");
        } catch (IOException e) {
            e.printStackTrace();
        }
        */

        // Alex's code below
        List<List<String>> list_article = new ArrayList<>();
        List<List<String>> list_book = new ArrayList<>();
        List<List<String>> list_thesis = new ArrayList<>();
        List<List<String>> list_incollection = new ArrayList<>();
        List<List<String>> list_proceeding = new ArrayList<>();
        List<List<String>> list_inproceeding = new ArrayList<>();
        List<List<String>> list_pub_in_pubs = new ArrayList<>();

        i = 0;
        for (Publication publication : dblp.getPublications()) {

            if (i == MAX_ROWS) break;

            List<String> row_articles = new ArrayList<>();
            List<String> row_book = new ArrayList<>();
            List<String> row_thesis = new ArrayList<>();
            List<String> row_incollection = new ArrayList<>();
            List<String> row_proceeding = new ArrayList<>();
            List<String> row_inproceeding = new ArrayList<>();
            List<String> row_pub_pubs = new ArrayList<>();

            if(!PublicationUtils.getID(publication).isEmpty()) {
                switch(publication.getTag()) {
                    // TODO add "cite" field for the citations (don't know if all the types can have this field)
                    case "book":
                        row_book.add(publication.getKey());
                        row_book.add(PublicationUtils.getID(publication));
                        row_book.add(PublicationUtils.getTitle(publication));
                        if (publication.getBooktitle() != null) row_book.add(publication.getBooktitle().getTitle()); //always null??
                        row_book.add(String.valueOf(publication.getYear()));
                        // TODO add "publisher" field
                        row_book.add(PublicationUtils.getPages(publication));
                        row_book.add(PublicationUtils.getURL(publication));
                        row_book.add(PublicationUtils.getCrossRef(publication));
                        row_book.add(publication.getMdate());
                        list_book.add(row_book);
                        break;
                    case "article":
                        row_articles.add(publication.getKey());
                        row_articles.add(PublicationUtils.getID(publication));
                        row_articles.add(PublicationUtils.getTitle(publication));
                        row_articles.add(String.valueOf(publication.getYear()));
                        row_articles.add(PublicationUtils.getPages(publication));
                        row_articles.add(PublicationUtils.getURL(publication));
                        row_articles.add(PublicationUtils.getCrossRef(publication));
                        row_articles.add(publication.getMdate());
                        list_article.add(row_articles);
                        break;
                    case "phdthesis","masterthesis":
                        row_thesis.add(publication.getKey());
                        row_thesis.add(PublicationUtils.getID(publication));
                        row_thesis.add(PublicationUtils.getTitle(publication));
                        row_thesis.add(String.valueOf(publication.getYear()));
                        row_thesis.add(PublicationUtils.getPages(publication));
                        row_thesis.add(PublicationUtils.getSchool(publication));
                        row_thesis.add(PublicationUtils.getURL(publication));
                        row_thesis.add(PublicationUtils.getCrossRef(publication));
                        row_thesis.add(publication.getMdate());
                        list_thesis.add(row_thesis);
                        break;
                    case "incollection":
                        row_incollection.add(publication.getKey());
                        row_incollection.add(PublicationUtils.getID(publication));
                        row_incollection.add(PublicationUtils.getTitle(publication));
                        row_incollection.add(String.valueOf(publication.getYear()));
                        row_incollection.add(PublicationUtils.getPages(publication));
                        row_incollection.add(PublicationUtils.getURL(publication));
                        row_incollection.add(PublicationUtils.getCrossRef(publication));
                        row_incollection.add(publication.getMdate());
                        list_incollection.add(row_incollection);
                        break;
                    case "proceedings":
                        // TODO add "booktitle" (maybe already done with .getBooktitle()) and "publisher" fields
                        row_proceeding.add(publication.getKey());
                        row_proceeding.add(PublicationUtils.getID(publication));
                        row_proceeding.add(PublicationUtils.getTitle(publication));
                        row_proceeding.add(String.valueOf(publication.getYear()));
                        row_proceeding.add(PublicationUtils.getPages(publication));
                        row_proceeding.add(PublicationUtils.getURL(publication));
                        row_proceeding.add(PublicationUtils.getCrossRef(publication));
                        row_proceeding.add(publication.getMdate());
                        list_proceeding.add(row_proceeding);
                        break;
                    case "inproceedings":
                        // TODO add "booktitle" (maybe already done with .getBooktitle())
                        row_inproceeding.add(publication.getKey());
                        row_inproceeding.add(PublicationUtils.getID(publication));
                        row_inproceeding.add(PublicationUtils.getTitle(publication));
                        row_inproceeding.add(String.valueOf(publication.getYear()));
                        row_inproceeding.add(PublicationUtils.getPages(publication));
                        row_inproceeding.add(PublicationUtils.getURL(publication));
                        row_inproceeding.add(PublicationUtils.getCrossRef(publication));
                        list_inproceeding.add(row_inproceeding);
                        row_proceeding.add(publication.getMdate());
                        break;
                }

                // pub_pubs_relation.csv
                if(publication.getToc()!=null && !publication.getToc().getPublications().isEmpty()){
                    row_pub_pubs.add(publication.getKey());
                    publication.getToc().getPublications().forEach(p->row_pub_pubs.add(p.getKey()));
                    list_pub_in_pubs.add(row_pub_pubs);
                }
            }

            i++;
        }

        try {
            CSVWriter.convertToCSV(list_authors, RESULTS_DIRECTORY_PATH + "authors.csv");
            CSVWriter.convertToCSV(list_author_pubs, RESULTS_DIRECTORY_PATH + "author_pubs_relation.csv");
            CSVWriter.convertToCSV(list_article, RESULTS_DIRECTORY_PATH + "articles.csv");
            CSVWriter.convertToCSV(list_book, RESULTS_DIRECTORY_PATH + "books.csv");
            CSVWriter.convertToCSV(list_thesis, RESULTS_DIRECTORY_PATH + "thesis.csv");
            CSVWriter.convertToCSV(list_incollection, RESULTS_DIRECTORY_PATH + "incollection.csv");
            CSVWriter.convertToCSV(list_proceeding, RESULTS_DIRECTORY_PATH + "proceedings.csv");
            CSVWriter.convertToCSV(list_inproceeding, RESULTS_DIRECTORY_PATH + "inproceedings.csv");
            CSVWriter.convertToCSV(list_pub_in_pubs,RESULTS_DIRECTORY_PATH + "related_pubs_relation.csv");
        } catch (IOException e) {
            e.printStackTrace();
        }











        // CODICE DI PROVA

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

