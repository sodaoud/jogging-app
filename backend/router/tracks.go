package router

import (
	"encoding/json"
	"log"
	"net/http"
	"time"

	mgo "gopkg.in/mgo.v2"
	"gopkg.in/mgo.v2/bson"

	"git.toptal.com/backend/data"

	"github.com/gorilla/context"
	"github.com/gorilla/mux"
)

func getAllTracks(w http.ResponseWriter, r *http.Request) {
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
	userid := r.URL.Query().Get("userid")
	sort = &sortP
	tracks := []data.Track{}
	if data.CheckConnection() {
		session := data.Mongo.Copy()
		defer session.Close()
		c := session.DB("test").C("track")
		dateRange := bson.M{"$gte": time.Time{}}
		if begin != nil {
			dateRange["$gte"] = *begin
		}
		if end != nil {
			dateRange["$lte"] = *end
		}
		query := bson.M{"date": dateRange}
		if userid != "" {
			query["userid"] = bson.ObjectIdHex(userid)
		}
		c.Find(query).Sort(*sort).All(&tracks)
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

func getTrackForUser(w http.ResponseWriter, r *http.Request) {
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
	vars := mux.Vars(r)
	userid := vars["id"]
	sort = &sortP
	tracks := []data.Track{}
	if data.CheckConnection() {
		session := data.Mongo.Copy()
		defer session.Close()
		c := session.DB("test").C("track")
		dateRange := bson.M{"$gte": time.Time{}}
		if begin != nil {
			dateRange["$gte"] = *begin
		}
		if end != nil {
			dateRange["$lte"] = *end
		}
		query := bson.M{"date": dateRange}
		if userid != "" {
			query["userid"] = bson.ObjectIdHex(userid)
		}
		c.Find(query).Sort(*sort).All(&tracks)
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
			dateRange["$gte"] = *begin
		}
		if end != nil {
			dateRange["$lte"] = *end
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

	track.ID = bson.NewObjectId()
	track.UserID = context.Get(r, "userid").(bson.ObjectId)
	track.Speed = float32(track.Distance) / float32(track.Duration)
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
		w.Header().Set("Content-Type", "application/json; charset=UTF-8")
		w.WriteHeader(http.StatusCreated)
		json.NewEncoder(w).Encode(track)
	}
}

func createTrackForUser(w http.ResponseWriter, r *http.Request) {
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
	vars := mux.Vars(r)
	userid := vars["id"]

	track.ID = bson.NewObjectId()
	track.UserID = bson.ObjectIdHex(userid)
	track.Speed = float32(track.Distance) / float32(track.Duration)
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
		w.Header().Set("Content-Type", "application/json; charset=UTF-8")
		w.WriteHeader(http.StatusCreated)
		json.NewEncoder(w).Encode(track)
	}
}

func updateTrack(w http.ResponseWriter, r *http.Request) {
	var track data.Track
	vars := mux.Vars(r)
	id := vars["id"]
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
	if data.CheckConnection() {
		session := data.Mongo.Copy()

		defer session.Close()
		c := session.DB("test").C("track")

		//colQuerier := bson.M{"_id": bson.ObjectIdHex(id)}
		change := mgo.Change{
			Update: bson.M{"$set": bson.M{
				"speed":    float32(track.Distance) / float32(track.Duration),
				"duration": track.Duration,
				"distance": track.Distance,
				"date":     track.Date,
			}},
			ReturnNew: true,
		}

		userid := context.Get(r, "userid").(bson.ObjectId)
		err := c.Find(bson.M{"_id": bson.ObjectIdHex(id)}).One(&track)
		if userid != track.UserID && !hasRoleAdmin(context.Get(r, "roles").([]string)) {
			w.WriteHeader(http.StatusForbidden)
			return
		}
		_, err = c.Find(bson.M{"_id": bson.ObjectIdHex(id)}).Apply(change, &track)
		if err != nil {
			log.Println(err)
			w.WriteHeader(http.StatusInternalServerError)
			w.Write([]byte("Database Error"))
			return
		}
		w.Header().Set("Content-Type", "application/json; charset=UTF-8")
		w.WriteHeader(http.StatusOK)
		json.NewEncoder(w).Encode(track)
	}
}

func deleteTrack(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id := vars["id"]
	if data.CheckConnection() {
		session := data.Mongo.Copy()

		defer session.Close()
		c := session.DB("test").C("track")
		var track data.Track
		userid := context.Get(r, "userid").(bson.ObjectId)
		err := c.Find(bson.M{"_id": bson.ObjectIdHex(id)}).One(&track)
		if userid != track.UserID && !hasRoleAdmin(context.Get(r, "roles").([]string)) {
			w.WriteHeader(http.StatusForbidden)
			return
		}
		err = c.Remove(bson.M{"_id": bson.ObjectIdHex(id)})
		if err != nil {
			log.Println(err)
			w.WriteHeader(http.StatusInternalServerError)
			w.Write([]byte("Database Error"))
			return
		}
		w.WriteHeader(http.StatusOK)
	}
}
