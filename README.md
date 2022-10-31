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

### Creation and Modification

- An author registers to the platform.
```sql
CREATE (a:Author {id: "b/MarcoBrambilla", name: "Marco Brambilla", url: "https://marco-brambilla.com/"}) 
RETURN a
```
- An author adds a publication: *the `Publication` node is added and connected to the author*.
```sql
MATCH (a:Author) 
WHERE a.id = "b/MarcoBrambilla"
CREATE (p:Publication {id: "journals/data/BrambillaB22", title: "The role of graph databases in IT sector.", doi: "https://doi.org/10.3390/data7070093", pages: "93-147", type: "article", url: "db/journals/data/data7.html#BrambillaB22", year: 2022})
CREATE (a)-[r:PRODUCE]->(p) 
RETURN a,r,p
```
- The author changes his website url: *Edit the `url` field of the `Author`*.
Note: this command can be used to add the website url to an existing author that doesn't already have one, or to update the url field in case it already exists.
```sql
MATCH (a:Author)
WHERE a.id = "b/MarcoBrambilla"
SET a.url = "http://dbgroup.como.polimi.it/brambilla/"
RETURN a
```
- Add the references of a Publication (using DOI).
```sql
MATCH (publication:Publication {doi: "http://dx.doi.org/10.1109/CEC.2009.4983141"}), (referencedPublication:Publication {doi: "http://dx.doi.org/10.1007/11821830_34"})
CREATE (publication)-[relation:REFERENCES]->(referencedPublication)
RETURN publication, relation, referencedPublication
```
- An author delete his profile and all the relationships with the publications that he wrote.
```sql
MATCH (a:Author)
WHERE a.id = "b/MarcoBrambilla"
DETACH DELETE a
```
### Query to retrieve data

 - Print all authors with whom a particular author has collaborated.
```sql
MATCH (a:Author)-[r1:PRODUCE]->(p:Publication)<-[r2:PRODUCE]-(coauthor:Author)
WHERE a.id = "42/7075"
RETURN a, coauthor, p
```
 - Search authors by name: *all the homonyms will be printed.*
```sql
MATCH (a:Author {name: "Marco Brambilla"})
RETURN a
```
 - Get the top 10 of the most contributing authors affiliated with a given association, together with the number of publications that they wrote. 
```sql
MATCH (author:Author)-[:PRODUCE]->(:Publication),
      (author)-[:AFFILIATED]->(association:Association)
WHERE association.id = "association/7"
WITH author, count(*) AS publications
ORDER BY publications DESC
LIMIT 10
RETURN author, publications
```
 - Get the total number of publications year by year, in descending order.
```sql
MATCH (p:Publication)
WITH p.year AS date, count(*) AS total
ORDER BY p.year DESC
RETURN date, total
```
 - Degrees of separation between two authors: 
 Let's call coauthors of a publication two author that have written together the same publication.
 Given two authors A and B, this query calculates the minimum number of intermediate coauthors to link A and B.
```sql
MATCH (a:Author {id:"25/4412"}), (b:Author {id:"73/2665"}),
	  p = shortestPath((a)-[:PRODUCE*]-(b))
RETURN SIZE([n IN nodes(p) WHERE n:Author]) - 2 AS DegreesOfSeparation
```
- Variant: Print all the authors that connects A to B, in this way if the author A wants to reach the author B, he can know who the intermediate authors are in order to contact them.
```sql
MATCH (a:Author {id:"25/4412"}), (b:Author {id:"73/2665"}), 
      p = shortestPath((a)-[:PRODUCE*]-(b)) 
RETURN p
```
 - Get, for every year after 1990, the most referenced paper of the year
```sql
MATCH (publication:Publication)-[:REFERENCES]->(referenced:Publication)
WHERE publication.year > 1990
WITH publication.year AS year, referenced AS referenced, count(*) AS numRef
WITH year AS year, max(numRef) AS maxNumRef

MATCH (pub:Publication)-[:REFERENCES]->(ref:Publication)  
WHERE pub.year = year 
WITH year AS year, ref AS referenced, count(*) AS numRef, maxNumRef AS maxNumRef
WHERE numRef = maxNumRef

RETURN year, head(collect(referenced.title)) AS title, maxNumRef AS references
ORDER BY year ASC
```
- Given 2 publications find the shortest path between them in terms of references. 
```sql
MATCH (a:Publication {id:"conf/ssdbm/ThomasH83"}), (b:Publication {id:"conf/vldb/TurnerHC79"}),
	  p = shortestPath((a)-[:REFERENCES*]-(b))
RETURN p
```
