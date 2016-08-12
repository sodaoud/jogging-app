package router

import (
	"encoding/json"
	"log"
	"net/http"
	"time"

	"golang.org/x/crypto/bcrypt"

	mgo "gopkg.in/mgo.v2"
	"gopkg.in/mgo.v2/bson"

	"git.toptal.com/backend/data"

	jwt "github.com/dgrijalva/jwt-go"
	"github.com/dgrijalva/jwt-go/request"
	"github.com/gorilla/context"
)

func getTracks(w http.ResponseWriter, r *http.Request) {
	fs := []data.Track{}
	//check the connection
	if data.CheckConnection() {
		session := data.Mongo.Copy() //copy the session
		defer session.Close()
		c := session.DB("test").C("track")
		c.Find(nil).All(&fs)
	} else {
		panic("Database Error")
	}
	w.Header().Set("Content-Type", "application/json; charset=UTF-8")
	w.WriteHeader(http.StatusOK)
	if err := json.NewEncoder(w).Encode(fs); err != nil {
		panic(err)
	}
}

func createTrack(w http.ResponseWriter, r *http.Request) {
	track := data.Track{
		Date: time.Now(),
		Time: 1800,
	}
	if data.CheckConnection() {

		session := data.Mongo.Copy()

		defer session.Close()
		c := session.DB("test").C("track")
		err := c.Insert(track)
		if err != nil {
			log.Panic("Error creating track", err)
		}
	}
}

type userDto struct {
	Username string `json:"username"`
	Password string `json:"password"`
}

func createUser(w http.ResponseWriter, r *http.Request) {
	var u userDto
	error := json.NewDecoder(r.Body).Decode(&u)
	if error != nil {
		log.Panic("Error Unmarshal", error)
	}
	password := []byte(u.Password)

	// Hashing the password with the default cost of 10
	hashedPassword, err := bcrypt.GenerateFromPassword(password, bcrypt.DefaultCost)
	if err != nil {
		log.Panic("Can not bcrypt password", err)
	}
	user := data.User{
		Username: u.Username,
		Password: hashedPassword,
		Roles:    []string{data.UserRole},
	}
	if data.CheckConnection() {
		session := data.Mongo.Copy()

		defer session.Close()
		c := session.DB("test").C("user")
		err := c.EnsureIndex(mgo.Index{
			Key:    []string{"username"},
			Unique: true,
		})
		if err != nil {
			log.Panic("Same username", err)
		}
		err = c.Insert(user)
		if err != nil {
			log.Panic("Error creating user", err)
		}
	} else {
		log.Panic("Database Error")
	}
}

type customClaims struct {
	Username string   `json:"username"`
	Roles    []string `json:"roles"`
	jwt.StandardClaims
}

type render struct {
	Token string   `json:"token"`
	Roles []string `json:"roles"`
}

func login(w http.ResponseWriter, r *http.Request) {
	var u userDto
	error := json.NewDecoder(r.Body).Decode(&u)
	if error != nil {
		log.Panic("Error Unmarshal", error)
	}
	if data.CheckConnection() {
		session := data.Mongo.Copy()

		defer session.Close()
		c := session.DB("test").C("user")
		var user data.User
		err := c.Find(bson.M{"username": u.Username}).One(&user)
		if err != nil {
			log.Panic("Error user ", err)
		}
		if bcrypt.CompareHashAndPassword(user.Password, []byte(u.Password)) != nil {
			log.Panic("Bad password", err)
		}
		expireToken := time.Now().Add(time.Hour * 24).Unix()

		// We'll manually assign the claims but in production you'd insert values from a database
		claims := customClaims{
			user.Username,
			user.Roles,
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
			panic(err)
		}
	} else {
		log.Panic("Database Error")
	}
}

func validate(protectedPage http.HandlerFunc) http.HandlerFunc {
	return http.HandlerFunc(func(res http.ResponseWriter, req *http.Request) {
		req.Header.Get("Authorization")

		if token, err := request.ParseFromRequestWithClaims(req, request.OAuth2Extractor, &customClaims{}, func(token *jwt.Token) (interface{}, error) {
			return []byte("my-secret"), nil
		}); err == nil {
			claims := token.Claims.(*customClaims)
			context.Set(req, "username", claims.Username)
			context.Set(req, "roles", claims.Roles)
			protectedPage(res, req)
		} else {
			res.WriteHeader(http.StatusUnauthorized)
		}
	})
}
