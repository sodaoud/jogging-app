package data

import (
	"log"

	"gopkg.in/mgo.v2"
)

//Mongo warpper
var Mongo *mgo.Session

//Connect create Connectios to mongoDb
func Connect() {

	log.Println("Connection to database")
	var err error
	Mongo, err = mgo.Dial("localhost")
	if err = Mongo.Ping(); err != nil {
		log.Println("Database Error", err)
	}
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
