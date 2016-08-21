package router

import (
	"encoding/json"
	"log"
	"net/http"

	"github.com/gorilla/context"
	"github.com/gorilla/mux"

	"git.toptal.com/backend/data"
	"golang.org/x/crypto/bcrypt"
	mgo "gopkg.in/mgo.v2"
	"gopkg.in/mgo.v2/bson"
)

type userDto struct {
	Username string   `json:"username"`
	Password string   `json:"password"`
	Roles    []string `json:"roles"`
}

func (u *userDto) validate() (bool, *errorDto) {
	if len([]rune(u.Username)) < 3 {
		return false, &errorDto{
			Error:   "USERNAME_ERROR",
			Message: "Username must contain at least 3 characters",
		}
	}
	if len([]rune(u.Password)) < 6 {
		return false, &errorDto{
			Error:   "PASSWORD_ERROR",
			Message: "Password must contain at least 6 characters",
		}
	}
	return true, nil
}

func createUser(w http.ResponseWriter, r *http.Request) {
	var u userDto
	w.Header().Set("Content-Type", "application/json; charset=UTF-8")
	error := json.NewDecoder(r.Body).Decode(&u)
	if error != nil {
		w.WriteHeader(http.StatusInternalServerError)
		dto := errorDto{
			Error:   "JSON_ERROR",
			Message: error.Error(),
		}
		json.NewEncoder(w).Encode(dto)
		log.Println("Error Unmarshal", error)
		return
	}
	if b, dto := u.validate(); b == false {
		w.WriteHeader(422)
		json.NewEncoder(w).Encode(dto)
		return
	}
	password := []byte(u.Password)

	// Hashing the password with the default cost of 10
	hashedPassword, err := bcrypt.GenerateFromPassword(password, bcrypt.DefaultCost)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		dto := errorDto{
			Error:   "BCRYPT_ERROR",
			Message: err.Error(),
		}
		json.NewEncoder(w).Encode(dto)
		log.Println("Error can not bcrypt password", error)
		return
	}
	roles := u.Roles
	if len(roles) == 0 {
		dto := errorDto{
			Error:   "ROLES_ERROR",
			Message: "Accounts must have at least one role",
		}
		json.NewEncoder(w).Encode(dto)
		return
	}
	user := data.User{
		ID:       bson.NewObjectId(),
		Username: u.Username,
		Password: hashedPassword,
		Roles:    roles,
	}
	if data.CheckConnection() {
		session := data.Mongo.Copy()

		defer session.Close()
		c := session.DB("test").C("user")
		if err := c.Find(bson.M{"username": u.Username}).One(&user); err == nil {
			w.WriteHeader(http.StatusConflict)
			dto := errorDto{
				Error:   "USERNAME_ERROR",
				Message: "The username " + user.Username + " is already used, please choose another username",
			}
			json.NewEncoder(w).Encode(dto)
			return
		}
		index := mgo.Index{
			Key:    []string{"username"},
			Unique: true,
		}
		c.EnsureIndex(index)
		err := c.Insert(user)
		if err != nil {
			w.WriteHeader(http.StatusInternalServerError)
			dto := errorDto{
				Error:   "INSERT_NOT_POSSIBLE",
				Message: err.Error(),
			}
			json.NewEncoder(w).Encode(dto)
			log.Println("Error insert user in Database", err)
			return
		}
		w.Header().Set("Content-Type", "application/json; charset=UTF-8")
		w.WriteHeader(http.StatusCreated)
		if err := json.NewEncoder(w).Encode(user); err != nil {
			w.WriteHeader(http.StatusInternalServerError)
			dto := errorDto{
				Error:   "JSON_ERROR",
				Message: err.Error(),
			}
			json.NewEncoder(w).Encode(dto)
			return
		}
	} else {
		w.WriteHeader(http.StatusInternalServerError)
		dto := errorDto{
			Error:   "DATABASE_ERROR",
			Message: "Unknown error",
		}
		json.NewEncoder(w).Encode(dto)
		return
	}
}

func getUser(w http.ResponseWriter, r *http.Request) {
	var user data.User
	if data.CheckConnection() {
		session := data.Mongo.Copy()
		defer session.Close()
		id := context.Get(r, "userid").(bson.ObjectId)
		c := session.DB("test").C("user")
		if err := c.Find(bson.M{"_id": id}).One(&user); err != nil {
			w.WriteHeader(http.StatusNoContent)
			errorDto := errorDto{
				Error:   "USER_NOT_FOUND",
				Message: "Your account has been deleted",
			}
			json.NewEncoder(w).Encode(errorDto)
			return
		}
	} else {
		w.WriteHeader(http.StatusInternalServerError)
		errorDto := errorDto{
			Error:   "DATABASE_ERROR",
			Message: "DATABASE_ERROR",
		}
		json.NewEncoder(w).Encode(errorDto)
		return
	}
	w.Header().Set("Content-Type", "application/json; charset=UTF-8")
	w.WriteHeader(http.StatusOK)
	if err := json.NewEncoder(w).Encode(user); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte("Json marshaling Error"))
		log.Println("Json marshaling Error", err)
		return
	}
}

func getUsers(w http.ResponseWriter, r *http.Request) {
	users := []data.User{}
	if data.CheckConnection() {
		session := data.Mongo.Copy()
		defer session.Close()
		c := session.DB("test").C("user")
		c.Find(nil).All(&users)
	} else {
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte("Database Error"))
		return
	}
	w.Header().Set("Content-Type", "application/json; charset=UTF-8")
	w.WriteHeader(http.StatusOK)
	if err := json.NewEncoder(w).Encode(users); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte("Json marshaling Error"))
		log.Println("Json marshaling Error", err)
		return
	}
}

func updateUser(w http.ResponseWriter, r *http.Request) {
	var u userDto
	vars := mux.Vars(r)
	id := vars["id"]
	w.Header().Set("Content-Type", "application/json; charset=UTF-8")
	error := json.NewDecoder(r.Body).Decode(&u)
	if error != nil {
		w.WriteHeader(http.StatusInternalServerError)
		dto := errorDto{
			Error:   "JSON_ERROR",
			Message: error.Error(),
		}
		json.NewEncoder(w).Encode(dto)
		log.Println("Error Unmarshal", error)
		return
	}
	if len([]rune(u.Username)) > 0 && len([]rune(u.Username)) < 3 {
		dto := &errorDto{
			Error:   "USERNAME_ERROR",
			Message: "Username must contain at least 3 characters",
		}
		json.NewEncoder(w).Encode(dto)
		return
	}
	if len([]rune(u.Password)) < 6 && len([]rune(u.Password)) > 0 {
		dto := &errorDto{
			Error:   "PASSWORD_ERROR",
			Message: "Password must contain at least 6 characters",
		}
		json.NewEncoder(w).Encode(dto)
		return
	}
	change := bson.M{}
	if len([]rune(u.Password)) > 0 {
		password := []byte(u.Password)

		// Hashing the password with the default cost of 10
		hashedPassword, err := bcrypt.GenerateFromPassword(password, bcrypt.DefaultCost)
		if err != nil {
			w.WriteHeader(http.StatusInternalServerError)
			dto := errorDto{
				Error:   "BCRYPT_ERROR",
				Message: err.Error(),
			}
			json.NewEncoder(w).Encode(dto)
			log.Println("Error can not bcrypt password", error)
			return
		}
		change["password"] = hashedPassword
	}
	if len([]rune(u.Username)) > 0 {
		change["username"] = u.Username
	}
	roles := u.Roles
	if len(roles) > 0 {
		change["roles"] = roles
	}
	if data.CheckConnection() {
		session := data.Mongo.Copy()

		defer session.Close()
		c := session.DB("test").C("user")
		var user data.User
		if err := c.Find(bson.M{"username": u.Username}).One(&user); err == nil {
			w.WriteHeader(http.StatusConflict)
			dto := errorDto{
				Error:   "USERNAME_ERROR",
				Message: "The username " + user.Username + " is already used, please choose another username",
			}
			json.NewEncoder(w).Encode(dto)
			return
		}
		index := mgo.Index{
			Key:    []string{"username"},
			Unique: true,
		}
		c.EnsureIndex(index)
		_, err := c.Find(bson.M{"_id": bson.ObjectIdHex(id)}).Apply(mgo.Change{
			Update:    bson.M{"$set": change},
			ReturnNew: true,
		}, &user)
		if err != nil {
			w.WriteHeader(http.StatusInternalServerError)
			dto := errorDto{
				Error:   "UPDATE_NOT_POSSIBLE",
				Message: err.Error(),
			}
			json.NewEncoder(w).Encode(dto)
			log.Println("Error insert user in Database", err)
			return
		}
		w.Header().Set("Content-Type", "application/json; charset=UTF-8")
		w.WriteHeader(http.StatusOK)
		if err := json.NewEncoder(w).Encode(user); err != nil {
			w.WriteHeader(http.StatusInternalServerError)
			dto := errorDto{
				Error:   "JSON_ERROR",
				Message: err.Error(),
			}
			json.NewEncoder(w).Encode(dto)
			return
		}
	} else {
		w.WriteHeader(http.StatusInternalServerError)
		dto := errorDto{
			Error:   "DATABASE_ERROR",
			Message: "Unknown Error",
		}
		json.NewEncoder(w).Encode(dto)
		return
	}
}

func updateUserProfile(w http.ResponseWriter, r *http.Request) {
	var profile data.Profile
	vars := mux.Vars(r)
	id := vars["id"]
	error := json.NewDecoder(r.Body).Decode(&profile)
	if error != nil {
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte(error.Error()))
		log.Println("Error Unmarshal", error)
		return
	}

	if data.CheckConnection() {
		session := data.Mongo.Copy()

		defer session.Close()
		c := session.DB("test").C("user")

		change := mgo.Change{
			Update:    bson.M{"$set": bson.M{"profile": profile}},
			ReturnNew: true,
		}

		userid := context.Get(r, "userid").(bson.ObjectId)
		if bson.ObjectIdHex(id) != userid &&
			!hasRoleAdmin(context.Get(r, "roles").([]string)) &&
			!hasRoleManager(context.Get(r, "roles").([]string)) {
			w.WriteHeader(http.StatusForbidden)
			return
		}
		var user data.User
		_, err := c.Find(bson.M{"_id": bson.ObjectIdHex(id)}).Apply(change, &user)
		if err != nil {
			log.Println(err)
			w.WriteHeader(http.StatusInternalServerError)
			w.Write([]byte("Database Error"))
			return
		}
		w.Header().Set("Content-Type", "application/json; charset=UTF-8")
		w.WriteHeader(http.StatusOK)
		json.NewEncoder(w).Encode(user)
	}
}

func deleteUser(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id := vars["id"]
	if data.CheckConnection() {
		session := data.Mongo.Copy()

		defer session.Close()
		c := session.DB("test").C("user")
		err := c.Remove(bson.M{"_id": bson.ObjectIdHex(id)})
		if err != nil {
			log.Println(err)
			w.WriteHeader(http.StatusInternalServerError)
			w.Write([]byte("Database Error"))
			return
		}

		c = session.DB("test").C("track")

		_, err = c.RemoveAll(bson.M{"userid": bson.ObjectIdHex(id)})
		if err != nil {
			log.Println(err)
			w.WriteHeader(http.StatusInternalServerError)
			w.Write([]byte("Database Error"))
			return
		}
		w.WriteHeader(http.StatusOK)
	}
}
