package router

import (
	"encoding/json"
	"log"
	"net/http"
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

type userDto struct {
	Username string `json:"username"`
	Password string `json:"password"`
}

func (u *userDto) validate() (bool, string) {
	if len([]rune(u.Username)) < 6 {
		return false, "Username must contain at least 6 characters"
	}
	if len([]rune(u.Password)) < 6 {
		return false, "Password must contain at least 6 characters"
	}
	return true, ""
}

func login(w http.ResponseWriter, r *http.Request) {
	var u userDto
	error := json.NewDecoder(r.Body).Decode(&u)
	if error != nil {
		w.WriteHeader(http.StatusUnauthorized)
		w.Write([]byte("Unmarshal Error"))
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
			w.Write([]byte("Wrong username"))
			return
		}
		if bcrypt.CompareHashAndPassword(user.Password, []byte(u.Password)) != nil {
			w.WriteHeader(http.StatusUnauthorized)
			w.Write([]byte("Wrong password"))
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
			w.Write([]byte(err.Error()))
			log.Println("Json encoding Error", err)
			return
		}
	} else {
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte("Database Error"))
		log.Println("Database Error")
		return
	}
}

func auth(protectedPage http.HandlerFunc) http.HandlerFunc {
	return http.HandlerFunc(func(res http.ResponseWriter, req *http.Request) {
		req.Header.Get("Authorization")

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
	error := json.NewDecoder(r.Body).Decode(&u)
	if error != nil {
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte(error.Error()))
		log.Println("Error Unmarshal", error)
		return
	}
	if b, mes := u.validate(); b == false {
		w.WriteHeader(422)
		w.Write([]byte(mes))
		return
	}
	password := []byte(u.Password)

	// Hashing the password with the default cost of 10
	hashedPassword, err := bcrypt.GenerateFromPassword(password, bcrypt.DefaultCost)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte(error.Error()))
		log.Println("Error can not bcrypt password", error)
		return
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
		index := mgo.Index{
			Key:    []string{"username"},
			Unique: true,
		}
		if err := c.EnsureIndex(index); err != nil { // TODO test if this works
			w.WriteHeader(http.StatusConflict)
			w.Write([]byte(err.Error()))
			return
		}
		err := c.Insert(user)
		if err != nil {
			w.WriteHeader(http.StatusInternalServerError)
			w.Write([]byte(err.Error()))
			log.Println("Error insert user in Database", err)
			return
		}
	} else {
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte(err.Error()))
		log.Println("Database Error", err)
		return
	}
}
