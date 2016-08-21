package data

import (
	"log"

	"gopkg.in/mgo.v2"
)

//Mongo warpper
var Mongo *mgo.Session
var database string

//Connect create Connectios to mongoDb
func Connect() {
	log.Println("Connection to database")
	var err error
	Mongo, err = mgo.Dial("localhost")
	if err = Mongo.Ping(); err != nil {
		log.Println("Database Error", err)
	}
	database = "test"
}

// Database change name
func Database(db string) {
	database = db
}

//CheckConnection check if Connected to mongoDb
func CheckConnection() bool {
	if Mongo == nil {
		Connect()
	}
	if Mongo != nil {
		return true
	}
	return false
}

// C return a collection with the given name
func C(col string) (*mgo.Collection, *mgo.Session) {
	if CheckConnection() == false {
		log.Panic("Database Error")
	}
	session := Mongo.Copy()
	return session.DB(database).C(col), session
}
