package data

import (
	"time"

	"gopkg.in/mgo.v2/bson"
)

// Track is the base sturcture of the application representing a jogging tarck
type Track struct {
	ID           bson.ObjectId `json:"id" bson:"_id,omitempty"`
	Date         time.Time     `json:"date"`
	Distance     int           `json:"distance"`
	Time         int           `json:"time"` // time in seconds
	AverageSpeed float32       `json:"average_speed"`
	UserID       bson.ObjectId `json:"userid"`
}

// Validate the track
func (t *Track) Validate() (bool, string) {
	if t.Date.After(time.Now()) {
		return false, "Invalid date"
	}
	if t.Time <= 0 {
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
const ManagerRole string = "MANGER"

// AdminRole declaration
const AdminRole string = "ADMIN"

// User is the struc of User
type User struct {
	ID       bson.ObjectId `json:"id" bson:"_id,omitempty"`
	Username string        `json:"username"`
	Password []byte        `json:"-"`
	Roles    []string      `json:"Roles"`
}
