package data

import (
	"time"

	"gopkg.in/mgo.v2/bson"
)

// Track is the base sturcture of the application representing a jogging tarck
type Track struct {
	ID       bson.ObjectId `json:"id" bson:"_id,omitempty"`
	Date     time.Time     `json:"date"`
	Distance int           `json:"distance"`
	Duration int           `json:"duration"` // time in seconds
	Speed    float32       `json:"speed"`
	UserID   bson.ObjectId `json:"userid"`
}

// Validate the track
func (t *Track) Validate() (bool, string) {
	if t.Date.After(time.Now()) {
		return false, "Invalid date"
	}
	if t.Duration <= 0 {
		return false, "time must be greater than 0"
	}
	if t.Distance <= 0 {
		return false, "distance must be greater than 0"
	}
	return true, ""
}

// UserRole declaration
const UserRole string = "USER"

// ManagerRole declaration
const ManagerRole string = "MANAGER"

// AdminRole declaration
const AdminRole string = "ADMIN"

// User is the struc of User
type User struct {
	ID       bson.ObjectId `json:"id" bson:"_id,omitempty"`
	Username string        `json:"username"`
	Password []byte        `json:"-"`
	Roles    []string      `json:"roles"`
	Profile  Profile       `json:"profile"`
}

// Profile of the user
type Profile struct {
	Unit      string  `json:"unit"`
	LastName  string  `json:"lastname"`
	FirstName string  `json:"firstname"`
	Age       int     `json:"age"`
	Sex       string  `json:"sex"`
	Weight    float32 `json:"weight"`
}
