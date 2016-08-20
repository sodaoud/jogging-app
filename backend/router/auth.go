package router

import (
	"encoding/json"
	"log"
	"net/http"
	"reflect"
	"time"

	"git.toptal.com/backend/data"
	jwt "github.com/dgrijalva/jwt-go"
	"github.com/dgrijalva/jwt-go/request"
	"github.com/gorilla/context"
	"golang.org/x/crypto/bcrypt"
	mgo "gopkg.in/mgo.v2"
	"gopkg.in/mgo.v2/bson"
)

type customClaims struct {
	Username string        `json:"username"`
	Roles    []string      `json:"roles"`
	ID       bson.ObjectId `json:"id"`

	jwt.StandardClaims
}

type render struct {
	Token string   `json:"token"`
	Roles []string `json:"roles"`
}

func login(w http.ResponseWriter, r *http.Request) {
	var u userDto
	w.Header().Set("Content-Type", "application/json; charset=UTF-8")
	error := json.NewDecoder(r.Body).Decode(&u)
	if error != nil {
		w.WriteHeader(http.StatusUnauthorized)
		dto := errorDto{
			Error:   "JSON_ERROR",
			Message: error.Error(),
		}
		json.NewEncoder(w).Encode(dto)
		return
	}
	if data.CheckConnection() {
		session := data.Mongo.Copy()

		defer session.Close()
		c := session.DB("test").C("user")
		var user data.User
		err := c.Find(bson.M{"username": u.Username}).One(&user)
		if err != nil {
			w.WriteHeader(http.StatusUnauthorized)
			dto := errorDto{
				Error:   "USERNAME_ERROR",
				Message: "The username does not exist",
			}
			json.NewEncoder(w).Encode(dto)
			return
		}
		if bcrypt.CompareHashAndPassword(user.Password, []byte(u.Password)) != nil {
			w.WriteHeader(http.StatusUnauthorized)
			dto := errorDto{
				Error:   "PASSWORD_ERROR",
				Message: "The password enterend does not match with the username",
			}
			json.NewEncoder(w).Encode(dto)
			return
		}
		expireToken := time.Now().Add(time.Hour * 24).Unix()

		claims := customClaims{
			user.Username,
			user.Roles,
			user.ID,
			jwt.StandardClaims{
				ExpiresAt: expireToken,
				Issuer:    "toptal.com",
			},
		}

		token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)

		signedToken, _ := token.SignedString([]byte("my-secret"))

		w.Header().Set("Content-Type", "application/json; charset=UTF-8")
		w.WriteHeader(http.StatusOK)
		t := render{
			Token: signedToken,
			Roles: user.Roles,
		}
		if err := json.NewEncoder(w).Encode(t); err != nil {
			w.WriteHeader(http.StatusInternalServerError)
			dto := errorDto{
				Error:   "JSON_ERROR",
				Message: err.Error(),
			}
			json.NewEncoder(w).Encode(dto)
			log.Println("Json encoding Error", err)
			return
		}
	} else {
		w.WriteHeader(http.StatusInternalServerError)
		dto := errorDto{
			Error:   "DATABASE_ERROR",
			Message: "Can not connect to database",
		}
		json.NewEncoder(w).Encode(dto)
		log.Println("Database Error")
		return
	}
}

func manager(protectedPage http.HandlerFunc) http.HandlerFunc {
	return http.HandlerFunc(func(res http.ResponseWriter, req *http.Request) {

		roles := context.Get(req, "roles")
		s := reflect.ValueOf(roles)
		b := false
		for i := 0; i < s.Len(); i++ {
			if s.Index(i).String() == data.ManagerRole || s.Index(i).String() == data.AdminRole { // TODO find a better solution
				b = true
			}
		}
		if b {
			protectedPage(res, req)
		} else {
			res.WriteHeader(http.StatusForbidden)
		}
	})
}

func admin(protectedPage http.HandlerFunc) http.HandlerFunc {
	return http.HandlerFunc(func(res http.ResponseWriter, req *http.Request) {

		roles := context.Get(req, "roles")
		s := reflect.ValueOf(roles)
		b := false
		for i := 0; i < s.Len(); i++ {
			if s.Index(i).String() == data.AdminRole { // TODO find a better solution
				b = true
			}
		}
		if b {
			protectedPage(res, req)
		} else {
			res.WriteHeader(http.StatusForbidden)
		}
	})
}

func auth(protectedPage http.HandlerFunc) http.HandlerFunc {
	return http.HandlerFunc(func(res http.ResponseWriter, req *http.Request) {

		if token, err := request.ParseFromRequestWithClaims(req, request.OAuth2Extractor, &customClaims{}, func(token *jwt.Token) (interface{}, error) {
			return []byte("my-secret"), nil
		}); err == nil {
			claims := token.Claims.(*customClaims)
			context.Set(req, "username", claims.Username)
			context.Set(req, "userid", claims.ID)
			context.Set(req, "roles", claims.Roles)
			protectedPage(res, req)
		} else {
			res.WriteHeader(http.StatusUnauthorized)
		}
	})
}

func signup(w http.ResponseWriter, r *http.Request) {
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
	user := data.User{
		ID:       bson.NewObjectId(),
		Username: u.Username,
		Password: hashedPassword,
		Roles:    []string{data.UserRole},
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

		expireToken := time.Now().Add(time.Hour * 24).Unix()

		claims := customClaims{
			user.Username,
			user.Roles,
			user.ID,
			jwt.StandardClaims{
				ExpiresAt: expireToken,
				Issuer:    "toptal.com",
			},
		}

		token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)

		signedToken, _ := token.SignedString([]byte("my-secret"))

		w.Header().Set("Content-Type", "application/json; charset=UTF-8")
		w.WriteHeader(http.StatusCreated)
		t := render{
			Token: signedToken,
			Roles: user.Roles,
		}
		if err := json.NewEncoder(w).Encode(t); err != nil {
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
			Message: err.Error(),
		}
		json.NewEncoder(w).Encode(dto)
		log.Println("Database Error", err)
		return
	}
}
