package router

import (
	"encoding/json"
	"log"
	"net/http"

	"gopkg.in/mgo.v2/bson"

	"git.toptal.com/backend/data"

	"github.com/gorilla/context"
	"github.com/gorilla/mux"
)

func getTracks(w http.ResponseWriter, r *http.Request) {
	tracks := []data.Track{}
	if data.CheckConnection() {
		session := data.Mongo.Copy()
		defer session.Close()
		userid := context.Get(r, "userid").(bson.ObjectId)
		c := session.DB("test").C("track")
		c.Find(bson.M{"userid": userid}).All(&tracks)
	} else {
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte("Database Error"))
		return
	}
	w.Header().Set("Content-Type", "application/json; charset=UTF-8")
	w.WriteHeader(http.StatusOK)
	if err := json.NewEncoder(w).Encode(tracks); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte("Json marshaling Error"))
		log.Println("Json marshaling Error", err)
		return
	}
}

func getTrack(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id := vars["id"]
	var track data.Track
	if data.CheckConnection() {
		session := data.Mongo.Copy()
		defer session.Close()
		userid := context.Get(r, "userid").(bson.ObjectId)
		c := session.DB("test").C("track")
		if err := c.Find(bson.M{"userid": userid, "_id": bson.ObjectIdHex(id)}).One(&track); err != nil {
			w.WriteHeader(http.StatusOK)
			w.Write([]byte("Not Found"))
			return
		}
	} else {
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte("Database Error"))
		return
	}
	w.Header().Set("Content-Type", "application/json; charset=UTF-8")
	w.WriteHeader(http.StatusOK)
	if err := json.NewEncoder(w).Encode(track); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte("Json marshaling Error"))
		log.Println("Json marshaling Error", err)
		return
	}
}
func createTrack(w http.ResponseWriter, r *http.Request) {
	var track data.Track
	error := json.NewDecoder(r.Body).Decode(&track)
	if error != nil {
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte(error.Error()))
		log.Println("Error Unmarshal", error)
		return
	}
	if b, mes := track.Validate(); !b {
		w.WriteHeader(422)
		w.Write([]byte(mes))
		return
	}
	// set current user to the track
	track.UserID = context.Get(r, "userid").(bson.ObjectId)
	track.AverageSpeed = float32(track.Distance) / float32(track.Time)
	if data.CheckConnection() {
		session := data.Mongo.Copy()

		defer session.Close()
		c := session.DB("test").C("track")
		err := c.Insert(track)
		if err != nil {
			w.WriteHeader(http.StatusInternalServerError)
			w.Write([]byte("Database Error"))
			return
		}
	}
}
