package router

import (
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"time"

	"gopkg.in/mgo.v2/bson"

	"git.toptal.com/backend/data"

	"github.com/gorilla/context"
	"github.com/gorilla/mux"
)

func getTracks(w http.ResponseWriter, r *http.Request) {
	var begin *time.Time
	var end *time.Time
	var sort *string
	if tmp, err := time.Parse(time.RFC3339, r.URL.Query().Get("begin")); err == nil {
		begin = &tmp
	} else if tmp, err := time.Parse("2006-01-02", r.URL.Query().Get("begin")); err == nil {
		begin = &tmp
	}
	if tmp, err := time.Parse(time.RFC3339, r.URL.Query().Get("end")); err == nil {
		end = &tmp
	} else if tmp, err := time.Parse("2006-01-02", r.URL.Query().Get("end")); err == nil {
		end = &tmp
	}

	sortP := r.URL.Query().Get("sort")
	if sortP == "" {
		sortP = "-date"
	}
	sort = &sortP
	tracks := []data.Track{}
	if data.CheckConnection() {
		session := data.Mongo.Copy()
		defer session.Close()
		userid := context.Get(r, "userid").(bson.ObjectId)
		c := session.DB("test").C("track")
		dateRange := bson.M{"$gte": time.Time{}}
		if begin != nil {
			fmt.Println(*begin)
		}
		if end != nil {
			fmt.Println(*end)
		}
		c.Find(bson.M{"userid": userid, "date": dateRange}).Sort(*sort).All(&tracks)
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
	track.Speed = float32(track.Distance) / float32(track.Time)
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
		w.WriteHeader(http.StatusCreated)
		json.NewEncoder(w).Encode(track)
	}
}
