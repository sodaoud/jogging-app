# Joggign Tracking Application

The application is a test project for Toptal, the needs are:

* User must be able to create an account and log in
* When logged in, user can see, edit and delete his times he entered
* Implement at least three roles with different permission levels: a regular user would only be able to CRUD on their owned records, a user manager would be able to CRUD users, and an admin would be able to CRUD all records and users.
* Each time entry when entered has a date, distance, and time
* When displayed, each time entry has an average speed
* Filter by dates from-to
* Report on average speed & distance per week

Technical requirements:

* REST API, All User action must be performed via the API
* Unit testing
* Design

## Technologies Used

### Backend

The backend application is a Go application with Mongo database. The following libraries are used:

* [httprouter](https://godoc.org/github.com/julienschmidt/httprouter)
* [mgo.v2](https://godoc.org/gopkg.in/mgo.v2) 

### Frontend

The frontend application is an Android application, it works only in online mode (connected). The following libraries are used:

* [OkHttp](http://square.github.io/okhttp/)

### Tools

* Android Studio v 2.2
* Visual Studio Code

