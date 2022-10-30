# DBLP_parser
DBLP parser of xml database in multiple CSV documents, useful for loading nodes in Neo4j
## CSV output examples
[here](https://polimi365-my.sharepoint.com/:f:/g/personal/10669287_polimi_it/EklWqHLKpF9HpmOlPRYrcn8B9rGf6gU43PQi_nXDMFDbFg?e=5ECOP4)

## Sources, Lib and Javadoc 
[here](https://polimi365-my.sharepoint.com/:f:/g/personal/10669287_polimi_it/Es2UUFbDQrZLsSVJ-I4nBW8BRy71Mcb1BubauFd3X2KC9Q?e=5oHncW)

## Inputs 
[here](https://polimi365-my.sharepoint.com/:f:/g/personal/10669287_polimi_it/EqgdVVaB5NhGncXjMW6NudoBjouwLLTXhpZmn6y1zS7D1A?e=LCsFw9)

NB: ‘‘‘ dblp_2015_cut.xml ‘‘‘ è stato tagliato a caso, quindi ci potrebbero essere degli errori a livello di dati ma compila comunque.
## Configuration Setup
![alt text](https://github.com/salvatorebuono02/DBLP_parser/blob/master/setup.png)







# Query Neo4J
TODO: Cambiare nome del field isbn in doi all'interno della query che carica i csv!

### Creation and Modification

- An author registers to the platform.
```sql
CREATE (a:Author {id: "b/MarcoBrambilla", name: "Marco Brambilla", url: "https://marco-brambilla.com/"}) 
RETURN a
```
- An author adds a publication: *the `Publication` node is added and connected to the author*.
```sql
MATCH (a:Author {id: "b/MarcoBrambilla"}) 
CREATE (p:Publication {id: "journals/data/BrambillaB22", title: "The role of graph databases in IT sector.", doi: "https://doi.org/10.3390/data7070093", last_mod: "2022-10-07", pages: "93", type: "article", url: "db/journals/data/data7.html#BrambillaB22", year: "2022"})
CREATE (a)-[r:PRODUCE]->(p) 
RETURN a,r,p
```
- The author changes his website url: *Edit the `url` field of the `Author`*.
Note: this command can be used to add the website url to an existing author that doesn't already have one, or to update the url field in case it already exists.
```sql
MATCH (a:Author {id: "b/MarcoBrambilla"})
SET a.url = "http://dbgroup.como.polimi.it/brambilla/"
RETURN a
```
- Add the references of a Publication (using DOI).
```sql
MATCH (publication:Publication {doi: "https://doi.org/10.3390/data7070093"}), (referencedPublication:Publication {doi: "http://dx.doi.org/10.1109/TPDS.2008.223"})
CREATE (publication)-[relation:REFERENCED]->(referencedPublication)
RETURN publication, relation, referencedPublication
```
- Add a new journal.
```sql

```
### Query to retrieve data

- Print all authors with whom a particular author has collaborated.
```sql
MATCH (a:Author)-[r1:PRODUCE]->(p:Publication)<-[r2:PRODUCE]-(coauthor:Author)
WHERE a.id = "36/1902"
RETURN a, coauthor
```
- Search authors by name: *all the homonyms will be printed.*
```sql
MATCH (a:Author {name: "Marco Brambilla"})
RETURN a
```
- Get the top 10 of the most contributing authors, together with the number of publications that they wrote. 
```sql
MATCH (author:Author)-[:PRODUCE]->(:Publication)
WITH author, count(*) AS publications
ORDER BY publications DESC
LIMIT 10
RETURN author, publications
```
- Get the total number of publications year by year.
**TODO**: Per questa query serve che la data sia di tipo date, non string!
