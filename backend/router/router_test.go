package router_test

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"golang.org/x/crypto/bcrypt"
	"gopkg.in/mgo.v2/bson"

	"git.toptal.com/backend/data"
	"git.toptal.com/backend/router"
)

var (
	server       *httptest.Server
	reader       io.Reader //Ignore this for now
	signupURL    string
	loginURL     string
	trackURL     string
	allTrackURL  string
	adminToken   string
	managerToken string
	userToken    string
	trackID      string
)

func init() {
	server = httptest.NewServer(router.New())

	data.Connect()
	data.Database("tests")
	db, session := data.DB()
	defer session.Close()
	db.DropDatabase()
	createAdmin()

	signupURL = fmt.Sprintf("%s/signup", server.URL)
	loginURL = fmt.Sprintf("%s/login", server.URL)
	trackURL = fmt.Sprintf("%s/track", server.URL)
	allTrackURL = fmt.Sprintf("%s/track/all", server.URL)

}

func createAdmin() {
	password := []byte("admin")
	hashedPassword, _ := bcrypt.GenerateFromPassword(password, bcrypt.DefaultCost)
	roles := []string{"ADMIN"}
	user := data.User{
		ID:       bson.NewObjectId(),
		Username: "admin",
		Password: hashedPassword,
		Roles:    roles,
	}
	c, session := data.C("user")
	defer session.Close()
	c.Insert(user)
}

func TestSignUp(t *testing.T) {
	userJSON := `{"username": "user"}`
	reader = strings.NewReader(userJSON)
	request, err := http.NewRequest("POST", signupURL, reader)
	res, err := http.DefaultClient.Do(request)
	if err != nil {
		t.Error(err)
	}
	if res.StatusCode != 422 {
		t.Errorf("expected 422, get %d", res.StatusCode)
	}

	userJSON = `{"username": "user"`
	reader = strings.NewReader(userJSON)
	request, err = http.NewRequest("POST", signupURL, reader)
	res, err = http.DefaultClient.Do(request)
	if err != nil {
		t.Error(err)
	}
	if res.StatusCode != 400 {
		t.Errorf("expected 400, get %d", res.StatusCode)
	}

	userJSON = `{"username": "user", "password": "password"}`
	reader = strings.NewReader(userJSON)
	request, err = http.NewRequest("POST", signupURL, reader)
	res, err = http.DefaultClient.Do(request)
	if err != nil {
		t.Error(err)
	}
	if res.StatusCode != 201 {
		t.Errorf("expected 201, get %d", res.StatusCode)
	}

	userJSON = `{"username": "user", "password": "password"}`
	reader = strings.NewReader(userJSON)
	request, err = http.NewRequest("POST", signupURL, reader)
	res, err = http.DefaultClient.Do(request)
	if err != nil {
		t.Error(err)
	}
	if res.StatusCode != 409 {
		t.Errorf("expected 409, get %d", res.StatusCode)
	}

}

type token struct {
	Token string   `json:"token"`
	Roles []string `json:"roles"`
}
type Track struct {
	ID       string  `json:"id" bson:"_id,omitempty"`
	Date     string  `json:"date"`
	Distance int     `json:"distance"` // distance in meters
	Duration int     `json:"duration"` // time in seconds
	Speed    float32 `json:"speed"`
	UserID   string  `json:"userid"`
}

func TestLogin(t *testing.T) {
	userJSON := `{"username": "admin", "password": "admin"}`
	reader = strings.NewReader(userJSON)
	request, err := http.NewRequest("POST", loginURL, reader)
	res, err := http.DefaultClient.Do(request)
	if err != nil {
		t.Error(err)
	}
	if res.StatusCode != 200 {
		t.Errorf("expected 200: get %d", res.StatusCode)
	}
	defer res.Body.Close()
	to := token{}
	err = json.NewDecoder(res.Body).Decode(&to)
	if err != nil {
		t.Error(err)
	}
	adminToken = "Bearer " + to.Token

	userJSON = `{"username": "resu", "password": "password"}`
	reader = strings.NewReader(userJSON)
	request, err = http.NewRequest("POST", loginURL, reader)
	res, err = http.DefaultClient.Do(request)
	if err != nil {
		t.Error(err)
	}
	if res.StatusCode != 401 {
		t.Errorf("Success expected: %d", res.StatusCode)
	}

	userJSON = `{"username": "user", "password": "password"}`
	reader = strings.NewReader(userJSON)
	request, err = http.NewRequest("POST", loginURL, reader)
	res, err = http.DefaultClient.Do(request)
	if err != nil {
		t.Error(err)
	}
	if res.StatusCode != 200 {
		t.Errorf("expected 200, get %d", res.StatusCode)
	}
	defer res.Body.Close()
	err = json.NewDecoder(res.Body).Decode(&to)
	if err != nil {
		t.Error(err)
	}
	userToken = "Bearer " + to.Token
}

func TestAuth(t *testing.T) {
	request, err := http.NewRequest("GET", trackURL, reader)
	res, err := http.DefaultClient.Do(request)
	if err != nil {
		t.Error(err)
	}
	if res.StatusCode != 401 {
		t.Errorf("expected 401, get %d", res.StatusCode)
	}

	request, err = http.NewRequest("GET", trackURL, reader)
	request.Header.Add("Authorization", userToken)
	res, err = http.DefaultClient.Do(request)
	if err != nil {
		t.Error(err)
	}
	if res.StatusCode != 200 {
		t.Errorf("200 expected, get %d", res.StatusCode)
	}

	// forbidden for simple user
	request, err = http.NewRequest("GET", allTrackURL, reader)
	request.Header.Add("Authorization", userToken)
	res, err = http.DefaultClient.Do(request)
	if err != nil {
		t.Error(err)
	}
	if res.StatusCode != 403 {
		t.Errorf("403 expected, get %d", res.StatusCode)
	}

	// allowed for admin
	request, err = http.NewRequest("GET", allTrackURL, reader)
	request.Header.Add("Authorization", adminToken)
	res, err = http.DefaultClient.Do(request)
	if err != nil {
		t.Error(err)
	}
	if res.StatusCode != 200 {
		t.Errorf("200 expected, get %d", res.StatusCode)
	}
}

func TestCreateTrack(t *testing.T) {
	// json well formed
	trackJSON := `{"date":"2016-08-22T00:00:00+01:00","distance":4000,"duration":7200}`
	reader := strings.NewReader(trackJSON)
	request, err := http.NewRequest("POST", trackURL, reader)
	request.Header.Add("Authorization", userToken)
	res, err := http.DefaultClient.Do(request)
	if err != nil {
		t.Error(err)
	}
	if res.StatusCode != 201 {
		t.Errorf("expected 201: %d", res.StatusCode)
	}

	// date malformed
	trackJSON = `{"date":"2016-08-22T00:00:00+0100","distance":4000,"duration":7200}`
	reader = strings.NewReader(trackJSON)
	request, err = http.NewRequest("POST", trackURL, reader)
	request.Header.Add("Authorization", userToken)
	res, err = http.DefaultClient.Do(request)
	if err != nil {
		t.Error(err)
	}
	if res.StatusCode != 400 {
		t.Errorf("expected 400: %d", res.StatusCode)
	}

	// not valid entery => 422
	trackJSON = `{"date":"2016-08-22T00:00:00+01:00","distance":-2,"duration":7200}`
	reader = strings.NewReader(trackJSON)
	request, err = http.NewRequest("POST", trackURL, reader)
	request.Header.Add("Authorization", userToken)
	res, err = http.DefaultClient.Do(request)
	if err != nil {
		t.Error(err)
	}
	if res.StatusCode != 422 {
		t.Errorf("expected 422: %d", res.StatusCode)
	}

	// Create another track to save its id
	trackJSON = `{"date":"2016-08-21T00:00:00+01:00","distance":4500,"duration":2700}`
	reader = strings.NewReader(trackJSON)
	request, err = http.NewRequest("POST", trackURL, reader)
	request.Header.Add("Authorization", userToken)
	res, err = http.DefaultClient.Do(request)
	if err != nil {
		t.Error(err)
	}
	if res.StatusCode != 201 {
		t.Errorf("expected 201: %d", res.StatusCode)
	}
	track := Track{}
	err = json.NewDecoder(res.Body).Decode(&track)
	if err != nil {
		t.Error(err)
	}
	trackID = track.ID
	if trackID == "" {
		t.Error("ID should not be nil or empty")
	}
}

func TestGetTrack(t *testing.T) {
	request, err := http.NewRequest("GET", trackURL, reader)
	request.Header.Add("Authorization", userToken)
	res, err := http.DefaultClient.Do(request)
	if err != nil {
		t.Error(err)
	}
	if res.StatusCode != 200 {
		t.Errorf("200 expected, get %d", res.StatusCode)
	}

	request, err = http.NewRequest("GET", trackURL, reader)
	request.Header.Add("Authorization", userToken)
	res, err = http.DefaultClient.Do(request)
	if err != nil {
		t.Error(err)
	}
	if res.StatusCode != 200 {
		t.Errorf("200 expected, get %d", res.StatusCode)
	}

}
