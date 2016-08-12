package data

import (
	"time"

	"gopkg.in/mgo.v2/bson"
)

// Track is the base sturcture of the application representing a jogging tarck
type Track struct {
	ID           bson.ObjectId `json:"id" bson:"_id,omitempty"`
	Date         time.Time     `json:"date"`
	Time         int           `json:"time"` // time in seconds
	AverageSpeed float32       `json:"avgSpd"`
	UserID       string        `json:"userid"`
}

// Tracks is a slice of Track
type Tracks []Track

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
