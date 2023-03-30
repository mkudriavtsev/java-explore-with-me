# Explore with me
ExploreWithMe backend project 

The app allows users to share information about interesting events and find company to participate in them.

### The application includes two services:
* Main service contains everything you need for the product to work
* Statistics service stores the number of views and allows you to make various selections to analyze the work of the application

### Main functionality:
* Create and share events
* Request for participate in events
* Moderation events: approve or reject requests
* Make compilations and pin it to the main board
* Collection of visitor statistics in the statistics service

### Technology stack:
Java 11, Spring Boot, Docker, PostgreSQL, Maven, Spring Data JPA

### System requirements:
* JDK 11
* Docker

### Startup instructions:
1. Download zip-file
2. Unpack zip-файл
3. Open app in IntellijIdea
4. mvn clean package
5. docker-compose up
6. Test app with postman tests collection in postman package
